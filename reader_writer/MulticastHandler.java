/**
 * 
 */
package reader_writer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.LinkedBlockingQueue;
import java.math.BigInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import reader_writer.message.ErrorType;
import reader_writer.message.Message;
import reader_writer.message.ObjectFactory;
import reader_writer.message.ParamType;

/**
 * @author Neal
 * Handles communication for the multicast socket
 */
public class MulticastHandler implements Runnable
{
	/**
	 * The name of this thread
	 */
	protected String name;
	
	/**
	 * Default port number <int> for the multicast group
	 */
	public static int miPort = 4445; 
	/**
	 * Default port number <int> for the multicast group
	 */
	public static String msPort = "4445";
	/**
	 * Default multicast address of the multicast group
	 */
	public static String msMuAddress = "230.0.0.1";
	/**
	 * Multicast Socket
	 */
	protected MulticastSocket mSocket = null;
	/**
	 * Group internet address
	 */
	protected InetAddress mAddress;
	/**
	 * Debug module
	 */
	protected UserInterface mDebug = null;
	/**
	 * Communication manager module
	 */
	protected CommManager mComm = null;
	/**
	 * Queue for multicast sends
	 */
	protected final LinkedBlockingQueue<Message> mMulticastSendQueue;
	
	private JAXBContext jaxbContext;
	private ObjectFactory of;
	private Marshaller marshaller;
	private Unmarshaller unmarshaller;

	/**
	 * 
	 */
	public MulticastHandler(String pAddress, int pPort, UserInterface ui, CommManager cm) throws IOException, JAXBException
	{
		this("CommManagerThread",pAddress,pPort,ui,cm);
	}

	/**
	 * 
	 */
	public MulticastHandler (String name, String pAddress, int pPort, UserInterface ui, CommManager cm) throws IOException, JAXBException
	{
		this.name = name;
		/**
		 * Set multicast settings
		 */
		mAddress = InetAddress.getByName(pAddress);
		mSocket = new MulticastSocket(pPort);
		mSocket.setSoTimeout(CommManager.miRecvTimeout);
		mSocket.joinGroup(mAddress);
		/**
		 * Print out the settings
		 */
		System.out.println("Multicast Settings:");
		System.out.println("Interface: " + mSocket.getNetworkInterface().getDisplayName());
		System.out.println("Host Internet Address: " + mSocket.getInetAddress());
		System.out.println("Local Internet Address: " + mSocket.getLocalAddress());
		System.out.println("Port: " + mSocket.getPort());
		System.out.println("Local Port: " + mSocket.getLocalPort());
		mDebug = ui;
		mComm = cm;
		mMulticastSendQueue = new LinkedBlockingQueue<Message>();
		jaxbContext = JAXBContext.newInstance("reader_writer.message");
		of = new ObjectFactory();
		marshaller = jaxbContext.createMarshaller();
		unmarshaller = jaxbContext.createUnmarshaller();
	}

	public void close()
	{
		try
		{
			mSocket.leaveGroup(mAddress);
			mSocket.close();
		} catch (IOException ioe)
		{
			// do nothing
		}
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
	 * 
	 */
	public void run()
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] recv_buffer = new byte[CommManager.miMaxBufferLength];
		DatagramPacket recv_packet = new DatagramPacket(recv_buffer, recv_buffer.length);
		boolean error_flag = false;
		JAXBElement<Message> msg;
		Message m;
		
		/**
		 * <ul>
		 */
		while (true)
		{
			/**
			 * <li> Check if application is to be unloaded
			 */
			if (MainDriver.QuitFlag.get())
			{
				System.out.println("MulticastHandler: exiting loop");
				break;
			}
			
			/**
			 * <li> Check if there is something to send
			 */
			m = mMulticastSendQueue.poll();
			if (m != null)
			{
				/**
				 * <ol>
				 * <li> Marshall the message into xml
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
					 * <li> If there is an error in creating the xml string, place an error response message on the read queue?
					 */
//					ParamType generic_param = of.createParamType();
//					generic_param.setStatus(ErrorType.XML_ERROR);
//					Message resp_mesg = of.createMessage();
//					resp_mesg.setViewid(m.getViewid());
//					resp_mesg.setMsgid(m.getMsgid());
//					resp_mesg.setMsgtype(m.getMsgtype());
//					resp_mesg.setProcid(m.getProcid());
//					resp_mesg.setParam(read_resp_param);
//					mProcessQueue.offer(resp_mesg);
					
					error_flag = true;
				}
				
				/**
				 * <li> Send to the group
				 * </ol>
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
			 * <li> Check if there is something to receive
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
			 * <ol>
			 * <li> Unmarshall read packet
			 */
			if (!error_flag)
			{
				try
				{
					/**
					 * <li> Parse the packet
					 */
					String dummy = new String(recv_packet.getData(),0,recv_packet.getLength());
					StringBuffer recv_msg = new StringBuffer(dummy);
					//ByteArrayInputStream bais = new ByteArrayInputStream(recv_packet.getData());
					mDebug.printOutRecvReport(recv_msg.toString());
					//mDebug.printOutRecvReport(dummy);
					msg = (JAXBElement<Message>)unmarshaller.unmarshal( new StreamSource( new StringReader( recv_msg.toString() ) ) );
					//msg = (JAXBElement<Message>)unmarshaller.unmarshal( bais );
					
					/**
					 * <li> Insert to CommManager's reveive queue
					 * </ol>
					 */
					mComm.insertToRecvQueue(msg.getValue());
				} catch (Exception e)
				{
					System.out.println(e.toString());
					e.printStackTrace();
				}
			}
			
			/**
			 * <li> Sleep for a very little time, just to switch to other threads
			 */
			//try
			//{
				//Thread.sleep(CommManager.miLoopTimeout);
				Thread.yield();
			//}catch (InterruptedException ioe)
			//{
				//System.out.println(ioe.toString());
			//}
		}
		/**
		 * Begin deinitialization
		 */
		close();
	}

	public int sendMulticastMessage(Message message)
	{
		mMulticastSendQueue.add(message);
		return 0;
	}
	
}
