/**
 * @author Neal Sebastian
 */
package reader_writer;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Iterator;

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
 * @author Nerisse
 *
 */
public class UnicastHandler implements Runnable
{
	/**
	 * The name of this thread
	 */
	protected String name;
	/**
	 * Default port number <int> for the multicast group
	 */
	public static int miPort;
	DatagramSocket mSocket;
	//SocketAddress mAddress;
	/**
	 * Debug module
	 */
	protected UserInterface mDebug = null;
	/**
	 * Communication manager module
	 */
	protected CommManager mComm = null;	
	/**
	 * Queue where the application place messages it wants to send
	 */
	protected final LinkedBlockingQueue<UnicastMessage> mUnicastSendQueue;	
	
	private JAXBContext jaxbContext;
	private ObjectFactory of;
	private Marshaller marshaller;
	private Unmarshaller unmarshaller;	
	
	
	public UnicastHandler(int port, UserInterface ui, CommManager cm) throws IOException
	{
		miPort = port;
		mSocket = new DatagramSocket(miPort);
		mSocket.setSoTimeout(CommManager.miRecvTimeout);
		mUnicastSendQueue = new LinkedBlockingQueue<UnicastMessage>();
		mComm = cm;
		mDebug = ui;
	}
	
	/**
	 * Check if the specified port is available, shamelessly ripped from the Apache Mina project
	 *
	 * @param port the port to check for availability
	 * @return true if port is available and false if port is not available
	 */
	public static boolean available(int port) {
	    if (port < CommManager.miMinPortNumber || port > CommManager.miMaxPortNumber) {
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

	public void close()
	{
		mSocket.close();
	}
	
//	public void run()
//	{
//		try
//		{
//			Selector selector = Selector.open();
//			mChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
//			
//			while (true)
//			{
//				/**
//				 * <li> Check if application is to be unloaded
//				 */
//				if (MainDriver.QuitFlag.get())
//				{
//					System.out.println("MulticastHandler: exiting loop");
//					break;
//				}
//				selector.select();
//				Set read_keys = selector.selectedKeys();
//				if (!read_keys.isEmpty())
//				{
//					Iterator iterator = read_keys.iterator();
//					while (iterator.hasNext())
//					{
//						SelectionKey key = (SelectionKey)iterator.next();
//						iterator.remove();
//						if (key.isReadable())
//						{
//							
//						}else if (key.isWritable())
//						{
//							
//						}
//							
//					}
//				}
//				
//				
//				Thread.yield();
//			}
//		}catch (IOException ioe)
//		{
//			System.out.println(ioe.toString());
//			ioe.printStackTrace();
//		}
//		close();
//	}

	public void run()
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] recv_buffer = new byte[CommManager.miMaxBufferLength];
		DatagramPacket recv_packet = new DatagramPacket(recv_buffer, recv_buffer.length);
		boolean error_flag = false;
		JAXBElement<Message> msg;
		UnicastMessage um;
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
				System.out.println("UnicastHandler: exiting loop");
				break;
			}
			
			/**
			 * <li> Check if there is something to send
			 */
			um = mUnicastSendQueue.poll();
			if (um != null)
			{
				m = um.getMessage();
				System.out.println("got a message to send on unicast");
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
				 * <li> Send to peer
				 * </ol>
				 */
				if (!error_flag)
				{
					try
					{
						String send_msg = baos.toString();
						DatagramPacket dgram = new DatagramPacket(send_msg.getBytes(), send_msg.length(),
								um.getDestAddr(), um.getDestPort());
						mSocket.send(dgram);
						mDebug.printOutSendReport("Unicast send: " + send_msg);
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
					//mDebug.printOutRecvReport(recv_msg.toString());
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

	public int sendUnicastMessage(Message m, String dest_addr, int dest_port)
	{
		UnicastMessage um = new UnicastMessage();
		um.setMessage(m);
		try
		{
			um.setDestAddr(InetAddress.getByName(dest_addr));
		} catch(UnknownHostException uhe)
		{
			System.out.println(uhe.toString());
			return 1;
		}
		um.setDestPort(dest_port);
		mUnicastSendQueue.add(um);
		return 0;
	}
}


	