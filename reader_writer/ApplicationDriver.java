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
	
	ApplicationDriver(CommInterface comm_interface)
	{
		mCommInterface = comm_interface;
		of = new ObjectFactory();
	}
	
	public void run()
	{
		while (true)
		{
			/**
			 * <>
			 * Check if application is to be unloaded
			 */
			if (MainDriver.QuitFlag.get())
			{
				System.out.println("ApplicationDirver: exiting loop");
				break;
			}
			
			try
			{
				ReadRequestParam rrp = of.createReadRequestParam();
				rrp.setFileurn("file1");
				Message m = of.createMessage();
				m.setMsgid(new BigInteger("1"));
				m.setMsgtype(MessageType.READ_REQUEST);
				m.setProcid("1");
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
