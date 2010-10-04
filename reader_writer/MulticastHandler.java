/**
 * 
 */
package reader_writer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;

import reader_writer.message.ErrorType;
import reader_writer.message.Message;
import reader_writer.message.ObjectFactory;
import reader_writer.message.ReadResponseParam;

/**
 * @author Neal
 * Handles communication for the multicast socket
 */
public class MulticastHandler 
{
	/**
	 * The name of the Communication Manager
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
	protected InetAddress mAddress = InetAddress.getByName(msMuAddress);
	/**
	 * Debug module
	 */
	protected UserInterface mDebug = null;
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
	public MulticastHandler(String pAddress, int pPort, UserInterface ui) throws IOException, JAXBException
	{
		this("CommManagerThread",pAddress,pPort,ui);
	}

	/**
	 * 
	 */
	public MulticastHandler (String name, String pAddress, int pPort, UserInterface ui) throws IOException, JAXBException
	{
		this.name = name;
		/**
		 * Set multicast settings
		 */
		mAddress = InetAddress.getByName(pAddress);
		mSocket = new MulticastSocket(pPort);
		mSocket.setSoTimeout(CommManager.miRecvTimeout);
		mSocket.joinGroup(mAddress);
		mDebug = ui;
		
		mMulticastSendQueue = new LinkedBlockingQueue<Message>();
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
		
		while (true)
		{
			/**
			 * <>
			 * Check if application is to be unloaded
			 */
			if (MainDriver.QuitFlag.get())
			{
				System.out.println("MulticastHandler: exiting loop");
				break;
			}
			
			/**
			 * Check if there is something to send
			 */
			m = mMulticastSendQueue.poll();
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


}
