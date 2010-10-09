/**
 * 
 */
package reader_writer;

import reader_writer.message.*;

import java.io.*;
import java.math.BigInteger;
import java.util.concurrent.*;
import javax.xml.bind.*;


/**
 * @author neal
 *
 */
public class ApplicationDriver implements Runnable
{
	CommInterface mCommInterface;
	ObjectFactory of;
	ProcessIdentity myID;
	BigInteger mMsgSeq;
	
	ApplicationDriver(CommInterface comm_interface)
	{
		mCommInterface = comm_interface;
		of = new ObjectFactory();
		mMsgSeq = new BigInteger("0");
	}
	
	public void run()
	{
		/**
		 * <ul>
		 * <li> Connect to the Communication Interface
		 */
		mCommInterface.connect();
		while (true)
		{
			/**
			 * <li> Check if application is to be unloaded
			 */
			if (MainDriver.QuitFlag.get())
			{
				System.out.println("ApplicationDirver: exiting loop");
				break;
			}
			
			try // ToDo: add own messeage sequence numbers
			{
				ReadRequestParam rrp = of.createReadRequestParam();
				rrp.setFileurn("file1");
				Message m = of.createMessage();
				mMsgSeq.add(BigInteger.ONE);
				m.setMsgtype(MessageType.READ_REQUEST);
				m.setParam(rrp);
				mCommInterface.sendMessage(m);
				System.out.println("Sleeping for " + ((2 + (int)(Math.random()*((5 - 2) + 1))) * 1000));
				Thread.sleep( (2 + (int)(Math.random()*((5 - 2) + 1))) * 1000);
			} catch (InterruptedException ie)
			{
				// just ignore
			}
		}
	}
}
