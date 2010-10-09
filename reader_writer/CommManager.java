/**
 * 
 */
package reader_writer;
import reader_writer.message.*;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import javax.xml.bind.*;
//import javax.xml.transform.stream.StreamSource;

/**
 * @author neal
 * 
 * Handles overall communication, an interface for the application.
 * Timeouts in this class are in ms.
 */
public class CommManager implements Runnable, CommInterface
{
	/**
	 * Default timeout for receiving datagrams on a socket
	 */
	public static final int miRecvTimeout = 50;
	/**
	 * Communication manager thread loop timeout
	 */
	public static final int miLoopTimeout = 500;
	/**
	 * Maximum data packets received before sending an Ack
	 */
	public static final int miMaxDataRecv = 5;
	/**
	 * Minimum valid unicast port number 
	 */
	public static final int miMinPortNumber = 49152;
	/**
	 * Maximum valid unicast port number
	 */
	public static final int miMaxPortNumber = 65535;
	/**
	 * Maximum UDP packet length according to Java
	 */
	public static final int miMaxBufferLength = 65507;
	/**
	 * Timeout to determine if the application should create its own group
	 */
	public static final int miJoinTimeout = 3000;
	/**
	 * Number times to retry sending of ChangeList (join) message
	 */
	public static final int miJoinRetry = 3;	
	/**
	 * Timeout for polling queues
	 */
	public static final int miPollTimeout = 10;
	/**
	 * Timeout to determine if no response to NACK
	 */
	public static final int miNackTimeout = 500;	
	/**
	 * Number times to retry sending a message
	 */
	public static final int miMsgRetry = 2;		
	/**
	 * Timeout for sending ack when max data messages is not reached
	 */
	public static final int miInitialAckSendTimeout = 250;		
	
	/**
	 * The name of the Communication Manager
	 */
	protected String name;
	/**
	 * Debug module
	 */
	protected UserInterface mDebug = null;
	/**
	 * Queue where all messages are received
	 */
	protected final LinkedBlockingQueue<Message> mRecvQueue;
	/**
	 * The ordered queue where data messages reside and will be ordered
	 */
	protected final List<Message> mUnOrderedQueue;
	/**
	 * The ordered queue where messages will be eventually sent to the application
	 */
	protected final List<Message> mOrderedQueue;
	/**
	 * Queue where the application place messages it wants to send
	 */
	protected final LinkedBlockingQueue<Message> mSendQueue;
	/**
	 * The application will wait for messages in this queue
	 */
	protected final LinkedBlockingQueue<Message> mProcessQueue;
	
	/**
	 * Receives and sends to the multicast socket
	 */
	protected MulticastHandler mMHandler;
	/**
	 * Receives and sends to the unicast socket
	 */
	protected UnicastHandler mUHandler;
	/**
	 * Factory to create messages
	 */
	ObjectFactory of;
	
	InetAddress myInetAdd = InetAddress.getLocalHost();
	ProcessIdentity mProcID;
	/**
	 * The application's view counter
	 */
	BigInteger mViewCounter;
	/**
	 * The application's message sequence number
	 */
	BigInteger mMsgCounter;
	/**
	 * Current view id of the process
	 */
	ViewIdentity mViewID = null;
	/**
	 * Members of the group, also serves as a basis on to whom the token will be passed
	 */
	List<ProcessIdentity> mMembers; // ToDo: Collection with efficient search
	/**
	 * Latest ack or new list message
	 */
	Message mLatestACK = null;
	/**
	 * Number of acks or new lists
	 */
	int mNumberAcks = 0;
	/**
	 * The timestamp of the group
	 */
	BigInteger mGlobalTimestamp;
	
	/**
	 * RMP States for the Communication Manager's state machine
	 *
	 */
	public enum RMP_State
	{
		JOIN,
		MEMBER,
		TOKEN,
		LEAVE
	}
	RMP_State mState = RMP_State.JOIN;
	Object mStateLock = new Object();
	
//	Timer mTimer;
//	class TimeoutInterruptor extends TimerTask  
//	{
//		Thread client;
//		
//		public TimeoutInterruptor(Thread t)
//		{
//			client = t;
//		}
//		public TimeoutInterruptor(CommManager t)
//		{
//			client = (Thread)t;
//		}
//		
//	    public void run (  )   
//	    {
//	      System.out.println ( "Timeout!" ) ;
//	      client.interrupt();
//	      mTimer.cancel (  ) ; //Terminate the thread
//	    }
//	}
	
	/**
	 * 
	 */
	public CommManager(String pAddress, int pPort, UserInterface ui) throws IOException, JAXBException
	{
		this("CommManagerThread",pAddress,pPort,ui);
	}

	/**
	 * 
	 */
	public CommManager(String name, String pAddress, int pPort, UserInterface ui) throws IOException, JAXBException
	{
		this.name = name;
		
		/**
		 * Set the debug interface
		 */
		mDebug = ui;
		/**
		 * Set the queues
		 */
		mRecvQueue = new LinkedBlockingQueue<Message>();
		mUnOrderedQueue = new ArrayList<Message>();
		mOrderedQueue = new ArrayList<Message>();
		mSendQueue = new LinkedBlockingQueue<Message>();
		mProcessQueue = new LinkedBlockingQueue<Message>();
		/**
		 * Set the proc id
		 */
		mProcID = new ProcessIdentity();
		mProcID.setIp(myInetAdd.getHostName());
		mProcID.setPort(miMinPortNumber + (int)(Math.random()*((miMaxPortNumber - miMinPortNumber) + 1)));
		/**
		 * Initialize the view counter
		 */
		mViewCounter = new BigInteger("0");
		/**
		 * Initialize the message counter
		 */
		mMsgCounter = new BigInteger("0");
		/**
		 * Initialize the members
		 */
		mMembers = new ArrayList<ProcessIdentity>();
		/**
		 * Initialize and start the multicast socket handler
		 */
		mMHandler = new MulticastHandler(pAddress,pPort,ui,this);
		new Thread(mMHandler).start();
		/**
		 * Initialize and start the unicast udp socket handler
		 */
		mUHandler = new UnicastHandler(mProcID.getPort(),ui,this);
		new Thread(mUHandler).start();
		/**
		 * Initialize the timer
		 */
		//mTimer = new Timer();
//		jaxbContext = JAXBContext.newInstance("reader_writer.message");
		of = new ObjectFactory();
//		marshaller = jaxbContext.createMarshaller();
//		unmarshaller = jaxbContext.createUnmarshaller();
	}

	/**
	 * Compares two Message IDs
	 * @param msgid1
	 * @param msgid2
	 * @return 0 if equal
	 * @return 1 if not equal
	 */
	private int compareMsgID(MessageIdentity msgid1, MessageIdentity msgid2)
	{
		if (msgid1.getMsgseq().equals(msgid2.getMsgseq()))
		{
			if(msgid1.getProcid().getIp().compareTo(msgid2.getProcid().getIp()) == 0 &&
					msgid1.getProcid().getPort() == msgid2.getProcid().getPort())
			{
				return 0;
			}
		}
		return 1;
	}	
	
	private int compareViewID(ViewIdentity viewid1, ViewIdentity viewid2)
	{
		if (viewid1.getCounter().equals(viewid2.getCounter()))
		{
			if (viewid1.getProcid().getIp().compareTo(viewid2.getProcid().getIp() ) == 0 &&
					viewid1.getProcid().getPort() == viewid2.getProcid().getPort())
			{
				return 0;
			}
		}
		return 1;
	}
	
	/**
	 * Searches for a message in list with the give msgid
	 * @param list
	 * @param msgid
	 * @return null if not found
	 * @return reference to message is returned if found
	 */
	private Message searchForMessage(List<Message> list, MessageIdentity msgid)
	{
		if (list == null)
		{
			return null;
		}
		Iterator<Message> iterator = list.iterator();
		while (iterator.hasNext())
		{
			Message m = (Message)iterator.next();
			if (compareMsgID(m.getMsgid(), msgid) == 0)
			{
				return m;
			}
		}
		return null;
	}
	
	/**
	 * 
	 *
	 */
	private ProcessIdentity getNextTokenSite()
	{
//		int i = mMembers.indexOf(mProcID);
//		if (i+1 >= mMembers.size())
//			return mMembers.get(0);
//		else	
//			return mMembers.get(i+1);
		int i = 0;
		Iterator iterator = mMembers.iterator();
		ProcessIdentity procid;
		while(iterator.hasNext())
		{
			procid = (ProcessIdentity)iterator.next();
			System.out.println(procid.getPort());
			
		}
		iterator = mMembers.iterator();
		while(iterator.hasNext())
		{
			procid = (ProcessIdentity)iterator.next();
			if (procid.getIp().compareTo(mProcID.getIp()) == 0 &&
					procid.getPort() == mProcID.getPort())
			{
				break;
			}
			i++;
		}
//		if (iterator.hasNext())
//		{
//			return (ProcessIdentity)iterator.next();
//		}else
//		{
//			iterator = mMembers.iterator();
//			return (ProcessIdentity)iterator.next();
//		}
//		System.out.println("index: " + i);
		if (i+1 >= mMembers.size())
			return mMembers.get(0);
		else
			return mMembers.get(i+1);
	}
	
	/**
	 * 
	 */
	public void run()
	{
		Message m,recv_msg = null, temp_msg = null, got_msg = null;
		NewList nl_message;
		Nack nack_message;
		Ack ack_message;
		ChangeList change_list;
		long startTime;
		Iterator iterator;
		int i;
		
		while (true)
		{
			/**
			 * <ul>
			 * <li> Check if application is to be unloaded
			 */
			if (MainDriver.QuitFlag.get())
			{
				System.out.println("CommManager: exiting loop");
				break;
				//ToDo: Change state to leaving
			}
			
			/**
			 * <li> Check state and act accordingly
			 * <ol>
			 */
			switch (mState)
			{
				/**
				 * <li> Joining a group
				 * <ul>
				 */
				case JOIN:
					System.out.println("CommManager State: Joining a group");
					mDebug.setStatus("Joining the group");
					int retry=0;
					startTime = System.currentTimeMillis();

					/**
					 * <li> Create the change list message
					 */
					change_list = of.createChangeList();
					change_list.setCommand(ChangeViewCommands.JOIN);
					m = of.createMessage();
					m.setMsgid(of.createMessageIdentity());
					m.getMsgid().setProcid(mProcID);
					synchronized(mMsgCounter)
					{
						m.getMsgid().setMsgseq(new BigInteger(mMsgCounter.toByteArray()));
						mMsgCounter = mMsgCounter.add(BigInteger.ONE);
					}
					m.setMsgtype(MessageType.LIST_CHANGE_REQUEST);
					m.setParam(change_list);
					/*
					 * <li> Insert it into the multicast send queue
					 */
					mMHandler.sendMulticastMessage(m);
					while (true)
					{
						/*
						 * Start the timer for timeout
						 */
						//mTimer.schedule(new TimeoutInterruptor(this), 5000);
						/**
						 * <li> Discard incoming messages and just wait for the NewList
						 */
						try
						{
							recv_msg = mRecvQueue.poll(miPollTimeout,TimeUnit.MILLISECONDS);
						} catch (InterruptedException ie)
						{
							// do nothing
						}
						if (recv_msg != null && 
//								((ParamType)recv_msg.getParam()).getStatus() == ErrorType.VALID &&
								recv_msg.getMsgtype() == MessageType.NEW_LIST && 
								recv_msg.getParam() instanceof NewList)
						{
							nl_message = (NewList)recv_msg.getParam();
							if (nl_message.getNextToken().getIp().compareTo(mProcID.getIp()) ==  0 &&
									nl_message.getNextToken().getPort() == mProcID.getPort())
							{
								/**
								 * <li> If a new list message for this node is received, process the new viewid
								 * and members
								 */
//								mViewID = new ViewIdentity();
//								mViewID.setProcid(new ProcessIdentity());
//								mViewID.getProcid().setIp(nl_message.getNewViewid().getProcid().getIp());
//								mViewID.getProcid().setPort(nl_message.getNewViewid().getProcid().getPort());
//								mViewID.setCounter(new BigInteger(nl_message.getNewViewid().getCounter().toByteArray()));
								mViewID = nl_message.getNewViewid();
								mGlobalTimestamp = nl_message.getTimestamp();
								iterator = nl_message.getMembers().iterator();
								while (iterator.hasNext())
								{
									mMembers.add((ProcessIdentity)iterator.next());
								
								}
								/**
								 * <li> add the New List message to the ordered queue
								 */
								recv_msg.setTimestamp(new BigInteger(mGlobalTimestamp.toByteArray()));
								mOrderedQueue.add(recv_msg);
								mNumberAcks++;
								mLatestACK = recv_msg;
								/**
								 * <li> Ask for missing data/s
								 */
								iterator = nl_message.getDataPackets().iterator();
								while (iterator.hasNext())
								{
									/**
									 * <li> Send a NACK
									 */
									nack_message = of.createNack();
									nack_message.setMissingMsgid((MessageIdentity)iterator.next());
									m = of.createMessage();
									m.setViewid(mViewID);
									m.setMsgid(of.createMessageIdentity());
									m.getMsgid().setProcid(mProcID);
									synchronized(mMsgCounter)
									{
										m.getMsgid().setMsgseq(new BigInteger(mMsgCounter.toByteArray()));
										mMsgCounter = mMsgCounter.add(BigInteger.ONE);
									}
									m.setMsgtype(MessageType.NACK);
									m.setParam(nack_message);
									retry = 0;
									startTime = System.currentTimeMillis();
									while (true)
									{
										mUHandler.sendUnicastMessage(m,recv_msg.getMsgid().getProcid().getIp(),
												recv_msg.getMsgid().getProcid().getPort() );
										try
										{
											temp_msg = mRecvQueue.poll(miPollTimeout,TimeUnit.MILLISECONDS);
										} catch (InterruptedException ie)
										{
											// do nothing
										}
										/**
										 * <li> Wait for the requested data message and ignore non-data messages
										 */
										if (temp_msg != null && 
											((ParamType)temp_msg.getParam()).getStatus() == ErrorType.VALID &&
											temp_msg.getMsgtype() != MessageType.NEW_LIST && 
											temp_msg.getMsgtype() != MessageType.ACK && 
											temp_msg.getMsgtype() != MessageType.CONFIRM &&
											temp_msg.getMsgtype() != MessageType.LIST_CHANGE_REQUEST &&
											temp_msg.getMsgtype() != MessageType.NACK)
										{
											/**
											 * <li> Ignore messages belonging to a different group
											 */
											if (compareViewID(temp_msg.getViewid(),mViewID) != 0)
											{
												continue;
											}
											/**
											 * <li> If this is the one we need, place it in the ordered queue.
											 */
											if (compareMsgID(temp_msg.getMsgid(), nack_message.getMissingMsgid()) == 0)
											{
												/**
												 * No need to add timestamp, assume it has one 
												 * because it is requested through NACK
												 */
												mOrderedQueue.add(temp_msg);
												mGlobalTimestamp = mGlobalTimestamp.add(BigInteger.ONE);
												break;
											}
											/**
											 * <li> Add other data messages to the unordered list
											 */
											mUnOrderedQueue.add(temp_msg);
											/* ToDo: Add retry to other site */
											if ((System.currentTimeMillis() - startTime) > miNackTimeout)
											{
												if (retry >= miMsgRetry) break;
											}
										}
									}
								}
								/**
								 * Send out confirmation
								 */
								
								break;
							}
						} 
						if ((System.currentTimeMillis() - startTime) > miJoinTimeout) 
						{
							if (retry >= miJoinRetry )
							 break;
							else
							{
								mMHandler.sendMulticastMessage(m);
								retry++;
							}							
						}
					}
					if (mViewID == null)
					{
						/**
						 * <li> Because no new list message is received, create own group
						 */
						mViewID = new ViewIdentity();
						mViewID.setProcid(mProcID);
						mViewID.setCounter(mViewCounter);
						/**
						 * add self to list of members
						 */
						mMembers.add(mProcID);
						/**
						 * add a new list message to self
						 */
						nl_message = of.createNewList();
						nl_message.setNewViewid(mViewID);
						nl_message.setTimestamp(new BigInteger("0")); // create first timestamp or message for this group
						nl_message.getMembers().add(mProcID);
						nl_message.setNextToken(mProcID);
						m = of.createMessage();
						m.setMsgid(of.createMessageIdentity());
						m.getMsgid().setProcid(mProcID);
						synchronized(mMsgCounter)
						{
							m.getMsgid().setMsgseq(new BigInteger(mMsgCounter.toByteArray()));
							mMsgCounter = mMsgCounter.add(BigInteger.ONE);
						}
						m.setMsgtype(MessageType.NEW_LIST);
						m.setTimestamp(nl_message.getTimestamp());
						m.setParam(nl_message);
						mOrderedQueue.add(m);
						mNumberAcks++;
						mLatestACK = m;
						mGlobalTimestamp = new BigInteger("0");
					}
					
					/**
					 * <li> Immediately participate in the ring
					 */
					synchronized(mStateLock)
					{
						mState = RMP_State.TOKEN;
						mStateLock.notifyAll(); // notify all who called connect()
					}
					break;
				/**
				 * </ul>
				 * <li> Acting as a token site
				 * <ul>
				 */
				case TOKEN:
					System.out.println("CommManager State: Token Site");
					mDebug.setStatus("Token Site");
					/**
					 * <li> If number of Acks or New List is greater than member size,
					 * dequeue and place to the application's receive queue until the number
					 * is less than or equal to the member size
					 */
					i = mNumberAcks - mMembers.size();
					while( i > 0)
					{
						m = mOrderedQueue.remove(0);
						if (m.getMsgtype() == MessageType.ACK)
						{
							i--;
							mNumberAcks--;
							continue;
						}
						if (m.getMsgtype() == MessageType.NEW_LIST)
						{
							i--;
							mNumberAcks--;
						}
						mProcessQueue.add(m);
						mDebug.printOutProcessReport(m.getMsgid().getProcid().getIp() + ":"
								+ m.getMsgid().getProcid().getPort() + ": "
								+ m.getMsgtype() + m.getTimestamp());
					}
					/**
					 * <li> Create data message list
					 */
					ack_message = new Ack();
					startTime = System.currentTimeMillis(); // record starting time for possibly sending null ack
					/**
					 * <li> Check unordered data messages, drop duplicates
					 */
					int num_data_msg = 0;
					iterator = mUnOrderedQueue.iterator();
					while (iterator.hasNext())
					{
						m = (Message)iterator.next();
						if (m.getTimestamp() != null &&
							m.getTimestamp().compareTo(mLatestACK.getTimestamp()) <= 0)
						{
							iterator.remove(); // drop duplicate nack responded data message
						}
						//ToDo: add detection for duplicate data messages (add latest message sequence sent on group members)
						ack_message.getDataPackets().add(m.getMsgid());
						if (num_data_msg < miMaxDataRecv) 
							num_data_msg++;
						else
							break;	
					}
					/**
					 * <li> If data messages at unordered queue is not enough, get more from receive buffer
					 */
					boolean cl_msg_flag = false;
					while (num_data_msg < miMaxDataRecv &&
							((System.currentTimeMillis() - startTime) < miInitialAckSendTimeout))
					{
						try
						{
							recv_msg = mRecvQueue.poll(miPollTimeout,TimeUnit.MILLISECONDS);
						} catch (InterruptedException ie)
						{
							// do nothing
						}
						if (recv_msg != null)
						{
							System.out.println("got this: " + recv_msg.toString());
							/**
							 * <li> Ignore messages belonging to a different group
							 */
							if (recv_msg.getViewid() == null)
							{
								if (recv_msg.getMsgtype() != MessageType.LIST_CHANGE_REQUEST)
								{
									continue;
								}
								/**
								 * <li> Ignore duplicate change list messages
								 */
								change_list = (ChangeList)recv_msg.getParam();
								if (change_list.getCommand() == ChangeViewCommands.JOIN)
								{
									// Check if duplicate
									retry = 0; // just used as flag
									iterator = mMembers.iterator();
									while (iterator.hasNext())
									{
										ProcessIdentity proc_id = (ProcessIdentity)iterator.next();
										if (proc_id.getIp().compareTo(recv_msg.getMsgid().getProcid().getIp()) == 0 &&
												proc_id.getPort() == recv_msg.getMsgid().getProcid().getPort())
										{
											retry = 1;
											break;
										}
									}
									if (retry == 1)
									{
										System.out.println("Duplicate join ignored");
										continue;
									}
								} else if (change_list.getCommand() == ChangeViewCommands.LEAVE)
								{
									// Check if duplicate
									retry = 0; // just used as flag
									iterator = mMembers.iterator();
									while (iterator.hasNext())
									{
										ProcessIdentity proc_id = (ProcessIdentity)iterator.next();
										if (proc_id.getIp().compareTo(recv_msg.getMsgid().getProcid().getIp() ) == 0 &&
												proc_id.getPort() == recv_msg.getMsgid().getProcid().getPort())
										{
											retry = 1;
											break;
										}
									}
									if (retry == 0)
									{
										System.out.println("Duplicate leave ignored");
										continue;									
									}
								}
							}else if (compareViewID(recv_msg.getViewid(),mViewID) != 0)
							{
								continue;
							}
							if (recv_msg.getMsgtype() == MessageType.LIST_CHANGE_REQUEST)
							{
								// send out new list asap
								cl_msg_flag = true;
								break;
							}
							if (recv_msg.getMsgtype() == MessageType.NACK)
							{
								// respond
								nack_message = (Nack)recv_msg.getParam();
								m = searchForMessage(mOrderedQueue,nack_message.getMissingMsgid());
								if (m != null && m.getParam() != null)
								{
									temp_msg = of.createMessage();
									temp_msg.setMsgid(m.getMsgid());
									temp_msg.setMsgtype(m.getMsgtype());
									temp_msg.setTimestamp(m.getTimestamp());
									temp_msg.setViewid(m.getViewid());
									temp_msg.setParam(m.getParam());
									mUHandler.sendUnicastMessage(temp_msg,recv_msg.getMsgid().getProcid().getIp(),
											recv_msg.getMsgid().getProcid().getPort() );
								}
								continue;
							}
							if (recv_msg.getMsgtype() == MessageType.ACK ||
									recv_msg.getMsgtype() == MessageType.CONFIRM ||
									recv_msg.getMsgtype() == MessageType.NEW_LIST)
							{
								// ignore
								continue;
							}
							/**
							 * <li> Add data message to list in ack and in unordered queue
							 */
							ack_message.getDataPackets().add(recv_msg.getMsgid());
							mUnOrderedQueue.add(recv_msg);
							num_data_msg++;
						}
					}
					if (cl_msg_flag)
					{
						/**
						 * <li> If a change list is received, process it and send out a new list
						 */
						change_list = (ChangeList)recv_msg.getParam();
						if (change_list.getCommand() == ChangeViewCommands.JOIN)
						{
							// add new member next to this
							iterator = mMembers.iterator();
							i = 0;
							ProcessIdentity procid;
							while(iterator.hasNext())
							{
								procid = (ProcessIdentity)iterator.next();
								if (procid.getIp().compareTo(mProcID.getIp()) == 0 &&
										procid.getPort() == mProcID.getPort())
								{
									break;
								}
								i++;
							}
							System.out.println("Adding at " + (i+1));
							mMembers.add(i+1,recv_msg.getMsgid().getProcid());
						}else if (change_list.getCommand() == ChangeViewCommands.LEAVE)
						{
							System.out.println("removing a member!");
							// find and remove the member
							iterator = mMembers.iterator();
							while (iterator.hasNext())
							{
								ProcessIdentity proc_id = (ProcessIdentity)iterator.next();
								if (proc_id.getIp().compareTo(recv_msg.getMsgid().getProcid().getIp()) == 0 &&
										proc_id.getPort() == recv_msg.getMsgid().getProcid().getPort())
								{
									iterator.remove();
									break;
								}
							}
						}
						nl_message = of.createNewList();
						// transfer ack data packets to new list
						iterator = ack_message.getDataPackets().iterator();
						while (iterator.hasNext())
						{
							nl_message.getDataPackets().add((MessageIdentity)iterator.next());
							iterator.remove();
						}
						// add members
						iterator = mMembers.iterator();
						while (iterator.hasNext())
						{
							nl_message.getMembers().add((ProcessIdentity)iterator.next());
						}
						// create new view id
						nl_message.setNewViewid(new ViewIdentity());
						nl_message.getNewViewid().setProcid(mProcID);
						mViewCounter = mViewCounter.add(BigInteger.ONE);
						nl_message.getNewViewid().setCounter(new BigInteger(mViewCounter.toByteArray()));
						nl_message.setNextToken(getNextTokenSite());
						mGlobalTimestamp = mGlobalTimestamp.add(BigInteger.ONE);
						nl_message.setTimestamp(new BigInteger(mGlobalTimestamp.toByteArray()));
						m = of.createMessage();
						m.setMsgid(of.createMessageIdentity());
						m.getMsgid().setProcid(mProcID);
						synchronized(mMsgCounter)
						{
							m.getMsgid().setMsgseq(new BigInteger(mMsgCounter.toByteArray()));
							mMsgCounter = mMsgCounter.add(BigInteger.ONE);
						}
						m.setMsgtype(MessageType.NEW_LIST);
						m.setParam(nl_message);
						m.setViewid(mViewID);
						mMHandler.sendMulticastMessage(m);
					}else
					{
						/**
						 * else send out an ack
						 */
						ack_message.setNextToken(getNextTokenSite());
						mGlobalTimestamp = mGlobalTimestamp.add(BigInteger.ONE);
						ack_message.setTimestamp(new BigInteger(mGlobalTimestamp.toByteArray()));
						m = of.createMessage();
						m.setMsgid(new MessageIdentity());
						m.getMsgid().setProcid(mProcID);
						synchronized(mMsgCounter)
						{
							m.getMsgid().setMsgseq(new BigInteger(mMsgCounter.toByteArray()));
							mMsgCounter = mMsgCounter.add(BigInteger.ONE);
						}
						m.setMsgtype(MessageType.ACK);
						m.setParam(ack_message);
						m.setViewid(mViewID);
						mMHandler.sendMulticastMessage(m);
					}
					/**
					 * <li> ToDo: Wait for confirmation
					 */
					/**
					 * <li> Change state to member
					 */
					synchronized(mStateLock)
					{
						System.out.println("CommManager State: Member");
						mState = RMP_State.MEMBER;
						mDebug.setStatus("Member");
					}
					break;
				/**
				 * </ul>
				 * <li> Member of a ring, kind of a default state
				 * <ul>
				 */
				case MEMBER:
					boolean im_next_token = false;
					try
					{
						recv_msg = mRecvQueue.poll(miPollTimeout,TimeUnit.MILLISECONDS);
					} catch (InterruptedException ie)
					{
						// do nothing
					}
					if (recv_msg == null)
					{
						// loop again
						break;
					}
					/**
					 * <li> Ignore messages belonging to a different group
					 */
					if (recv_msg.getViewid() != null && compareViewID(recv_msg.getViewid(),mViewID) != 0)
					{
						break;
					}
					/**
					 * <li> Ignore confirm and list change requests, they should be handled by the token site
					 */
					if (recv_msg.getMsgtype() == MessageType.CONFIRM ||
							recv_msg.getMsgtype() == MessageType.LIST_CHANGE_REQUEST)
					{
						// ignore
						break;
					}
					if (recv_msg.getMsgtype() == MessageType.ACK)
					{
						ack_message = (Ack)recv_msg.getParam();
						/**
						 * <li> Get new timestamp
						 */
						mGlobalTimestamp = ack_message.getTimestamp();
						/**
						 * <li> Add Ack message to the ordered queue
						 */
						recv_msg.setTimestamp(new BigInteger(mGlobalTimestamp.toByteArray()));
						mOrderedQueue.add(recv_msg);
						mNumberAcks++;
						mLatestACK = recv_msg;
						/**
						 * <li> Order data messages or add null messages at the unordered queue 
						 * according to the message ids attached to ACK
						 */
						iterator = ack_message.getDataPackets().iterator();
						while (iterator.hasNext())
						{
							MessageIdentity message_id = (MessageIdentity)iterator.next();
							mGlobalTimestamp = mGlobalTimestamp.add(BigInteger.ONE);
							m = searchForMessage(mUnOrderedQueue, message_id);
							if (m != null)
							{
								mUnOrderedQueue.remove(m);
								m.setTimestamp(new BigInteger(mGlobalTimestamp.toByteArray()));
								mOrderedQueue.add(m);
							}else
							{
								m = of.createMessage();
								m.setMsgid(message_id);
								m.setParam(null);
								mOrderedQueue.add(m);
							}
						}
						/**
						 * <li> Check if next token site is this site
						 */
						if (ack_message.getNextToken().getIp().compareTo(mProcID.getIp()) == 0 &&
								ack_message.getNextToken().getPort() == mProcID.getPort())
						{
							im_next_token = true;
						}
					}else if (recv_msg.getMsgtype() == MessageType.NEW_LIST)
					{
						nl_message = (NewList)recv_msg.getParam();
						/**
						 * <li> Get new timestamp
						 */
						mGlobalTimestamp = nl_message.getTimestamp();
						/**
						 * <li> Apply membership changes
						 */
						mViewID = nl_message.getNewViewid();
						mMembers = new ArrayList<ProcessIdentity>();
						iterator = nl_message.getMembers().iterator();
						while (iterator.hasNext())
						{
							mMembers.add((ProcessIdentity)iterator.next());
						}
						/**
						 * <li> Add New List message to the ordered queue
						 */
						recv_msg.setTimestamp(new BigInteger(mGlobalTimestamp.toByteArray()));
						mOrderedQueue.add(recv_msg);
						mNumberAcks++;
						mLatestACK = recv_msg;
						/**
						 * <li> Order data messages or add null messages at the unordered queue 
						 * according to the message ids attached to ACK
						 */
						iterator = nl_message.getDataPackets().iterator();
						while (iterator.hasNext())
						{
							MessageIdentity message_id = (MessageIdentity)iterator.next();
							mGlobalTimestamp = mGlobalTimestamp.add(BigInteger.ONE);
							m = searchForMessage(mUnOrderedQueue, message_id);
							if (m != null)
							{
								mUnOrderedQueue.remove(m);
								m.setTimestamp(new BigInteger(mGlobalTimestamp.toByteArray()));
								mOrderedQueue.add(m);
							}else
							{
								m = of.createMessage();
								m.setMsgid(message_id);
								m.setParam(null);
								mOrderedQueue.add(m);
							}
						}
						/**
						 * <li> Check if next token site is this site
						 */
						if (nl_message.getNextToken().getIp().compareTo(mProcID.getIp()) == 0 &&
								nl_message.getNextToken().getPort() == mProcID.getPort())
						{
							im_next_token = true;
						}
					}else if (recv_msg.getMsgtype() == MessageType.NACK)
					{
						// respond
						nack_message = (Nack)recv_msg.getParam();
						m = searchForMessage(mOrderedQueue,nack_message.getMissingMsgid());
						if (m != null && m.getParam() != null)
						{
							temp_msg = of.createMessage();
							temp_msg.setMsgid(m.getMsgid());
							temp_msg.setMsgtype(m.getMsgtype());
							temp_msg.setTimestamp(m.getTimestamp());
							temp_msg.setViewid(m.getViewid());
							temp_msg.setParam(m.getParam());
							mUHandler.sendUnicastMessage(temp_msg,recv_msg.getMsgid().getProcid().getIp(),
									recv_msg.getMsgid().getProcid().getPort() );
						} 
					}else
					{
						/**
						 * <li> Enqueue data messages unto Unordered queue
						 */
						mUnOrderedQueue.add(recv_msg);
					}
					/**
					 * <li> Make sure we have all messages before accepting the token
					 */
					if (im_next_token)
					{
						/**
						 * <li> Check the OrderedQueue for missing messages
						 */
						iterator = mOrderedQueue.iterator();
						while (iterator.hasNext())
						{
							m = (Message)iterator.next();
							if (m.getParam() == null)
							{
								/**
								 * <li> Send a NACK for a missing message
								 */
								nack_message = of.createNack();
								nack_message.setMissingMsgid(m.getMsgid());
								temp_msg = of.createMessage();
								temp_msg.setViewid(mViewID);
								temp_msg.setMsgid(of.createMessageIdentity());
								temp_msg.getMsgid().setProcid(mProcID);
								synchronized(mMsgCounter)
								{
									m.getMsgid().setMsgseq(new BigInteger(mMsgCounter.toByteArray()));
									mMsgCounter = mMsgCounter.add(BigInteger.ONE);
								}
								temp_msg.setMsgtype(MessageType.NACK);
								temp_msg.setParam(nack_message);
								retry = 0;
								startTime = System.currentTimeMillis();
								while (true)
								{
									mUHandler.sendUnicastMessage(temp_msg,recv_msg.getMsgid().getProcid().getIp(),
											recv_msg.getMsgid().getProcid().getPort() );
									try
									{
										got_msg = mRecvQueue.poll(miPollTimeout,TimeUnit.MILLISECONDS);
									} catch (InterruptedException ie)
									{
										// do nothing
									}
									/**
									 * <li> Wait for the requested data message and ignore non-data messages
									 */
									if (got_msg != null && 
										((ParamType)got_msg.getParam()).getStatus() == ErrorType.VALID &&
										got_msg.getMsgtype() != MessageType.NEW_LIST && 
										got_msg.getMsgtype() != MessageType.ACK && 
										got_msg.getMsgtype() != MessageType.CONFIRM &&
										got_msg.getMsgtype() != MessageType.LIST_CHANGE_REQUEST &&
										got_msg.getMsgtype() != MessageType.NACK)
									{
										/**
										 * <li> Ignore messages belonging to a different group
										 */
										if (compareViewID(got_msg.getViewid(),mViewID) != 0)
										{
											continue;
										}
										/**
										 * <li> If this is the one we need, place it in the ordered queue.
										 */
										if (compareMsgID(got_msg.getMsgid(), nack_message.getMissingMsgid()) == 0)
										{
											/**
											 * Update the empty slot at ordered queue
											 */
											m.setMsgtype(got_msg.getMsgtype());
											m.setParam(got_msg.getParam());
											m.setTimestamp(got_msg.getTimestamp());
											m.setViewid(got_msg.getViewid());
											break;
										}
										/**
										 * <li> Add other data messages to the unordered list
										 */
										mUnOrderedQueue.add(got_msg);
										/* ToDo: Add retry to other site */
										if ((System.currentTimeMillis() - startTime) > miNackTimeout)
										{
											if (retry >= miMsgRetry) break;
										}
									}
								}
							}
						}
						/**
						 * <li> Send out confirmation
						 */
						
						/**
						 * <li> Be the token site
						 */
						synchronized(mStateLock)
						{
							mState = RMP_State.TOKEN;
						}
					}
					break;
				/**
				 * </ul>
				 * <li> Leaving the group
				 * <ul>
				 */
				case LEAVE:
//					System.out.println("CommManager State: Leaving the group");
//					mDebug.setStatus("Joining the group");
//					int retry=0;
//					startTime = System.currentTimeMillis();
//
//					/**
//					 * <li> Create the change list message
//					 */
//					change_list = of.createChangeList();
//					change_list.setCommand(ChangeViewCommands.JOIN);
//					m = of.createMessage();
//					m.setMsgid(of.createMessageIdentity());
//					m.getMsgid().setProcid(mProcID);
//					synchronized(mMsgCounter)
//					{
//						m.getMsgid().setMsgseq(new BigInteger(mMsgCounter.toByteArray()));
//						mMsgCounter = mMsgCounter.add(BigInteger.ONE);
//					}
//					m.setMsgtype(MessageType.LIST_CHANGE_REQUEST);
//					m.setParam(change_list);
//					/*
//					 * <li> Insert it into the multicast send queue
//					 */
//					mMHandler.sendMulticastMessage(m);
//					while (true)
//					{
//						/*
//						 * Start the timer for timeout
//						 */
//						//mTimer.schedule(new TimeoutInterruptor(this), 5000);
//						/**
//						 * <li> Discard incoming messages and just wait for the NewList
//						 */
//						try
//						{
//							recv_msg = mRecvQueue.poll(miPollTimeout,TimeUnit.MILLISECONDS);
//						} catch (InterruptedException ie)
//						{
//							// do nothing
//						}
//						if (recv_msg != null && 
////								((ParamType)recv_msg.getParam()).getStatus() == ErrorType.VALID &&
//								recv_msg.getMsgtype() == MessageType.NEW_LIST && 
//								recv_msg.getParam() instanceof NewList)
//						{
//							nl_message = (NewList)recv_msg.getParam();
//							if (nl_message.getNextToken().getIp().compareTo(mProcID.getIp()) ==  0 &&
//									nl_message.getNextToken().getPort() == mProcID.getPort())
//							{
//								/**
//								 * <li> If a new list message for this node is received, process the new viewid
//								 * and members
//								 */
////								mViewID = new ViewIdentity();
////								mViewID.setProcid(new ProcessIdentity());
////								mViewID.getProcid().setIp(nl_message.getNewViewid().getProcid().getIp());
////								mViewID.getProcid().setPort(nl_message.getNewViewid().getProcid().getPort());
////								mViewID.setCounter(new BigInteger(nl_message.getNewViewid().getCounter().toByteArray()));
//								mViewID = nl_message.getNewViewid();
//								mGlobalTimestamp = nl_message.getTimestamp();
//								iterator = nl_message.getMembers().iterator();
//								while (iterator.hasNext())
//								{
//									mMembers.add((ProcessIdentity)iterator.next());
//								
//								}
//								/**
//								 * <li> add the New List message to the ordered queue
//								 */
//								recv_msg.setTimestamp(new BigInteger(mGlobalTimestamp.toByteArray()));
//								mOrderedQueue.add(recv_msg);
//								mNumberAcks++;
//								mLatestACK = recv_msg;
//								/**
//								 * <li> Ask for missing data/s
//								 */
//								iterator = nl_message.getDataPackets().iterator();
//								while (iterator.hasNext())
//								{
//									/**
//									 * <li> Send a NACK
//									 */
//									nack_message = of.createNack();
//									nack_message.setMissingMsgid((MessageIdentity)iterator.next());
//									m = of.createMessage();
//									m.setViewid(mViewID);
//									m.setMsgid(of.createMessageIdentity());
//									m.getMsgid().setProcid(mProcID);
//									synchronized(mMsgCounter)
//									{
//										m.getMsgid().setMsgseq(new BigInteger(mMsgCounter.toByteArray()));
//										mMsgCounter = mMsgCounter.add(BigInteger.ONE);
//									}
//									m.setMsgtype(MessageType.NACK);
//									m.setParam(nack_message);
//									retry = 0;
//									startTime = System.currentTimeMillis();
//									while (true)
//									{
//										mUHandler.sendUnicastMessage(m,recv_msg.getMsgid().getProcid().getIp(),
//												recv_msg.getMsgid().getProcid().getPort() );
//										try
//										{
//											temp_msg = mRecvQueue.poll(miPollTimeout,TimeUnit.MILLISECONDS);
//										} catch (InterruptedException ie)
//										{
//											// do nothing
//										}
//										/**
//										 * <li> Wait for the requested data message and ignore non-data messages
//										 */
//										if (temp_msg != null && 
//											((ParamType)temp_msg.getParam()).getStatus() == ErrorType.VALID &&
//											temp_msg.getMsgtype() != MessageType.NEW_LIST && 
//											temp_msg.getMsgtype() != MessageType.ACK && 
//											temp_msg.getMsgtype() != MessageType.CONFIRM &&
//											temp_msg.getMsgtype() != MessageType.LIST_CHANGE_REQUEST &&
//											temp_msg.getMsgtype() != MessageType.NACK)
//										{
//											/**
//											 * <li> Ignore messages belonging to a different group
//											 */
//											if (compareViewID(temp_msg.getViewid(),mViewID) != 0)
//											{
//												continue;
//											}
//											/**
//											 * <li> If this is the one we need, place it in the ordered queue.
//											 */
//											if (compareMsgID(temp_msg.getMsgid(), nack_message.getMissingMsgid()) == 0)
//											{
//												/**
//												 * No need to add timestamp, assume it has one 
//												 * because it is requested through NACK
//												 */
//												mOrderedQueue.add(temp_msg);
//												mGlobalTimestamp = mGlobalTimestamp.add(BigInteger.ONE);
//												break;
//											}
//											/**
//											 * <li> Add other data messages to the unordered list
//											 */
//											mUnOrderedQueue.add(temp_msg);
//											/* ToDo: Add retry to other site */
//											if ((System.currentTimeMillis() - startTime) > miNackTimeout)
//											{
//												if (retry >= miMsgRetry) break;
//											}
//										}
//									}
//								}
//								/**
//								 * Send out confirmation
//								 */
//								
//								break;
//							}
//						} 
//						if ((System.currentTimeMillis() - startTime) > miJoinTimeout) 
//						{
//							if (retry >= miJoinRetry )
//							 break;
//							else
//							{
//								mMHandler.sendMulticastMessage(m);
//								retry++;
//							}							
//						}
//					}
//					if (mViewID == null)
//					{
//						/**
//						 * <li> Because no new list message is received, create own group
//						 */
//						mViewID = new ViewIdentity();
//						mViewID.setProcid(mProcID);
//						mViewID.setCounter(mViewCounter);
//						/**
//						 * add self to list of members
//						 */
//						mMembers.add(mProcID);
//						/**
//						 * add a new list message to self
//						 */
//						nl_message = of.createNewList();
//						nl_message.setNewViewid(mViewID);
//						nl_message.setTimestamp(new BigInteger("0")); // create first timestamp or message for this group
//						nl_message.getMembers().add(mProcID);
//						nl_message.setNextToken(mProcID);
//						m = of.createMessage();
//						m.setMsgid(of.createMessageIdentity());
//						m.getMsgid().setProcid(mProcID);
//						synchronized(mMsgCounter)
//						{
//							m.getMsgid().setMsgseq(new BigInteger(mMsgCounter.toByteArray()));
//							mMsgCounter = mMsgCounter.add(BigInteger.ONE);
//						}
//						m.setMsgtype(MessageType.NEW_LIST);
//						m.setTimestamp(nl_message.getTimestamp());
//						m.setParam(nl_message);
//						mOrderedQueue.add(m);
//						mNumberAcks++;
//						mLatestACK = m;
//						mGlobalTimestamp = new BigInteger("0");
//					}
//					
//					/**
//					 * <li> Immediately participate in the ring
//					 */
//					synchronized(mStateLock)
//					{
//						mState = RMP_State.TOKEN;
//						mStateLock.notifyAll(); // notify all who called connect()
//					}					
					break;
			}
			/**
			 * </ol>
			 */
			
			/**
			 * <li> Sleep for a very little time, just to switch to other threads
			 */
//			try
//			{
//				Thread.sleep(1000);
//			}catch (InterruptedException ioe)
//			{
//				System.out.println(ioe.toString());
//			}
		}
		/**
		 * </ul>
		 * Begin deinitialization
		 */
		close();
	}
	
	public void close()
	{
		
	}
	
	/**
	 * Send a reliable multicast message
	 */
	public int sendMessage(Message message)
	{
		message.setMsgid(of.createMessageIdentity());
		message.getMsgid().setProcid(mProcID);
		synchronized(mMsgCounter)
		{
			message.getMsgid().setMsgseq(new BigInteger(mMsgCounter.toByteArray()));
			mMsgCounter = mMsgCounter.add(BigInteger.ONE);
		}
		mMHandler.sendMulticastMessage(message);
		//mSendQueue.add(message);
		return 0;
	}
	
	/**
	 * Blocking request to receive a multicast message
	 */
	public Message receiveMessage()
	{
		try
		{
			return mProcessQueue.take();
		}catch (InterruptedException ie)
		{
			ie.toString();
			return null;
		}
	}
	
	/**
	 * 
	 * @param message
	 * @return
	 */
	public int insertToRecvQueue(Message message)
	{
		// Add to receive queue
		mRecvQueue.add(message);
		// Log received message
		if (message.getViewid() != null)
			mDebug.printOutRecvReport("ViewId: " + message.getViewid().getProcid().getIp() + 
					":" + message.getViewid().getProcid().getPort() + ":" +
					message.getViewid().getCounter());
		mDebug.printOutRecvReport("MsgId: " + message.getMsgid().getProcid().getIp() +
				":" + message.getMsgid().getProcid().getPort() + ":" +
				message.getMsgid().getMsgseq());
		mDebug.printOutRecvReport("Message type:" + message.getMsgtype());
		return 0;
	}	
	
	public void connect()
	{
		synchronized(mStateLock)
		{
			if (mState == RMP_State.JOIN)
			{
				try
				{
					mStateLock.wait();
				}catch (InterruptedException ie)
				{
					// do nothing, we just woke up!
				}
			}
		}
	}
}
