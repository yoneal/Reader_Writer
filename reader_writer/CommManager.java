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
import javax.xml.transform.stream.StreamSource;

/**
 * @author neal
 * Handles overall communication, an interface for the application.
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
	public static final int miLoopTimeout = 10;
	public static final int miMaxDataRecv = 3;
	public static final int miMinPortNumber = 49152;
	public static final int miMaxPortNumber = 65535;
	public static final int miMaxBufferLength = 65507;
	/**
	 * The name of the Communication Manager
	 */
	protected String name;
	/**
	 * Debug module
	 */
	protected UserInterface mDebug = null;
	/**
	 * Queue where the application place messages it wants to send
	 */
	protected final LinkedBlockingQueue<Message> mUnicastSendQueue;
	/**
	 * Queue where the application place messages it wants to send
	 */
	protected final LinkedBlockingQueue<Message> mMulticastSendQueue;
	/**
	 * Queue where all messages are received
	 */
	protected final LinkedBlockingQueue<Message> mRecvQueue;
	/**
	 * The ordered queue where data messages reside and will be ordered
	 */
	protected final LinkedBlockingQueue<Message> mUnOrderedQueue;
	/**
	 * The ordered queue where messages will be eventually sent to the application
	 */
	protected final LinkedBlockingQueue<Message> mOrderedQueue;
	/**
	 * Queue where the application place messages it wants to send
	 */
	protected final LinkedBlockingQueue<Message> mSendQueue;
	/**
	 * The application will wait for messages in this queue
	 */
	protected final LinkedBlockingQueue<Message> mProcessQueue;
	
	protected MulticastHandler mMHandler;
	protected UnicastHandler mUHandler;
	
//	private JAXBContext jaxbContext;
//	private ObjectFactory of;
//	private Marshaller marshaller;
//	private Unmarshaller unmarshaller;
	
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
		 * Set unicast settings
		 */
		//int uni_port = 
		//mUniSocket = new DatagramSocket();
		/**
		 * Set others
		 */
		mDebug = ui;
		mSendQueue = new LinkedBlockingQueue<Message>();
		mRecvQueue = new LinkedBlockingQueue<Message>();
		mOrderedQueue = new LinkedBlockingQueue<Message>();
		mProcessQueue = new LinkedBlockingQueue<Message>();
//		jaxbContext = JAXBContext.newInstance("reader_writer.message");
//		of = new ObjectFactory();
//		marshaller = jaxbContext.createMarshaller();
//		unmarshaller = jaxbContext.createUnmarshaller();
	}
	
	/**
	 * 
	 */
	public void run()
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] recv_buffer = new byte[miMaxBufferLength];
		DatagramPacket recv_packet = new DatagramPacket(recv_buffer, recv_buffer.length);
		boolean error_flag = false;
		JAXBElement<Message> msg;
		Message m;
		
		while (true)
		{
			/**
			 * <>
			 * Check if application is to be unloaded
			 */
			if (MainDriver.QuitFlag.get())
			{
				System.out.println("CommManager: exiting loop");
				break;
			}
			
			/**
			 * Check if there is something to send
			 */
			m = mSendQueue.poll();
			if (m != null)
			{
				System.out.println("got a message to send");
				/**
				 * Marshall the message into xml
				 */
				error_flag = false;
				try
				{
					msg = of.createMessage(m);
					baos.reset();
		            marshaller.marshal( msg, baos );
				}catch ( JAXBException jbe )
				{
					System.out.println(jbe.toString());
					jbe.printStackTrace();
					/**
					 * Place an error response message on the read queue
					 */
					ReadResponseParam read_resp_param = of.createReadResponseParam();
					read_resp_param.setReturn(ErrorType.XML_ERROR);
					Message resp_mesg = of.createMessage();
					resp_mesg.setMsgid(m.getMsgid());
					resp_mesg.setMsgtype(m.getMsgtype());
					resp_mesg.setProcid(m.getProcid());
					resp_mesg.setParam(read_resp_param);
					mProcessQueue.offer(resp_mesg);
					error_flag = true;
				}
				
				/**
				 * Send to group
				 */
				if (!error_flag)
				{
					try
					{
						String send_msg = baos.toString();
						DatagramPacket dgram = new DatagramPacket(send_msg.getBytes(), send_msg.length(),
								InetAddress.getByName(msMuAddress), miPort);
						mSocket.send(dgram);
						mDebug.printOutSendReport("Multicast send: " + send_msg);
					} catch (Exception e)
					{
						System.out.println(e.toString());
						e.printStackTrace();
					}
				}	
			}
				
			/**
			 * Check if there is something to receive
			 */
			try
			{
				error_flag = false;
				recv_packet.setLength(recv_buffer.length);
				mSocket.receive(recv_packet);
			}catch (SocketTimeoutException ste)
			{
				error_flag = true;
			}catch (IOException ioe)
			{
				System.out.println(ioe.toString());
				ioe.printStackTrace();
				error_flag = true;
			}
			
			/**
			 * Unmarshall read packet
			 */
			if (!error_flag)
			{
				try
				{
					String dummy = new String(recv_packet.getData(),0,recv_packet.getLength());
					StringBuffer recv_msg = new StringBuffer(dummy);
					//ByteArrayInputStream bais = new ByteArrayInputStream(recv_packet.getData());
					mDebug.printOutRecvReport(recv_msg.toString());
					//mDebug.printOutRecvReport(dummy);
					msg = (JAXBElement<Message>)unmarshaller.unmarshal( new StreamSource( new StringReader( recv_msg.toString() ) ) );
					//msg = (JAXBElement<Message>)unmarshaller.unmarshal( bais );
					/*
					m = (Message)msg.getValue();
					System.out.println("\nParsed message:");
					System.out.println(m.getMsgid());
					System.out.println(m.getMsgtype());
					System.out.println(m.getProcid());
					if( m.getParam() instanceof ReadRequestParam ){
						ReadRequestParam rrp = (ReadRequestParam)m.getParam();
						System.out.println(rrp.getFileurn());
					} else {
						System.out.println("Not a read request param");
					}
					*/
				} catch (Exception e)
				{
					System.out.println(e.toString());
					e.printStackTrace();
				}
			}
			
			/**
			 * Sleep for a very little time, just to switch to other threads
			 */
			try
			{
				Thread.sleep(1000);
			}catch (InterruptedException ioe)
			{
				System.out.println(ioe.toString());
			}
		}
		/**
		 * Begin deinitialization
		 */
	}
	
	public int sendMessage(Message message)
	{
		mSendQueue.add(message);
		return 0;
	}
	
	public Message receiveMessage()
	{
		try
		{
			return mRecvQueue.take();
		}catch (InterruptedException ie)
		{
			ie.toString();
			return null;
		}
	}
	
	public int insertToRecvQueue(Message message)
	{
		return 0;
	}
	
	
}
