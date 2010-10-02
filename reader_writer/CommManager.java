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
 * Handles overall communication, an interface for the application 
 */
public class CommManager implements Runnable, CommInterface 
{
	/**
	 * Default port number <int> for the multicast group
	 */
	public static final int miPort = 4445; 
	/**
	 * Default port number <int> for the multicast group
	 */
	public static final String msPort = "4445";
	/**
	 * Default multicast address of the multicast group
	 */
	public static final String msMuAddress = "230.0.0.1";
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
	 * Multicast Socket
	 */
	protected MulticastSocket mSocket = null;
	protected DatagramSocket mUniSocket = null;
	protected InetAddress mAddress = InetAddress.getByName(msMuAddress);
	protected UserInterface mDebug = null;
	/**
	 * Queue where the application place messages it wants to send
	 */
	protected final LinkedBlockingQueue<Message> mSendQueue;
	/**
	 * Queue where the comm manager place unordered messages
	 */
	protected final LinkedBlockingQueue<Message> mRecvQueue;
	/**
	 * The ordered queue where messages will be eventually sent to the application
	 */
	protected final LinkedBlockingQueue<Message> mOrderedQueue;
	/**
	 * The application will wait for messages in this queue
	 */
	protected final LinkedBlockingQueue<Message> mProcessQueue;
	
	private JAXBContext jaxbContext;
	private ObjectFactory of;
	private Marshaller marshaller;
	private Unmarshaller unmarshaller;
	
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
		 * Set multicast settings
		 */
		mAddress = InetAddress.getByName(pAddress);
		mSocket = new MulticastSocket(pPort);
		mSocket.setSoTimeout(miRecvTimeout);
		mSocket.joinGroup(mAddress);
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
		jaxbContext = JAXBContext.newInstance("reader_writer.message");
		of = new ObjectFactory();
		marshaller = jaxbContext.createMarshaller();
		unmarshaller = jaxbContext.createUnmarshaller();
	}
	
	/**
	 * 
	 * @return
	 */
	public int connectToGroup()
	{
		try
		{
			mSocket.joinGroup(mAddress);
		}catch (IOException ioe)
		{
			System.out.println(ioe.toString());
			ioe.printStackTrace();
			return 1;
		}
		return 0;
	}
	
	/**
	 * Check if the specified port is available, shamelessly ripped from the Apache Mina project
	 *
	 * @param port the port to check for availability
	 * @return true if port is available and false if port is not available
	 */
	public static boolean available(int port) {
	    if (port < miMinPortNumber || port > miMaxPortNumber) {
	        throw new IllegalArgumentException("Invalid port number range: " + port);
	    }

	    ServerSocket ss = null;
	    DatagramSocket ds = null;
	    try {
	        ss = new ServerSocket(port);
	        ss.setReuseAddress(true);
	        ds = new DatagramSocket(port);
	        ds.setReuseAddress(true);
	        return true;
	    } catch (IOException e) {
	    } finally {
	        if (ds != null) {
	            ds.close();
	        }

	        if (ss != null) {
	            try {
	                ss.close();
	            } catch (IOException e) {
	                /* should not be thrown */
	            }
	        }
	    }

	    return false;
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
}
