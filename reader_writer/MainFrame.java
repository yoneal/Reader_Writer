package reader_writer;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main Frame for the reader writer package.
 * @author neal
 * @version 1.0.0
 *
 */
public class MainFrame extends JFrame
{
	public MainFrame()
	{
		this.setSize(300,150);
		SetupPanel spSetup = new SetupPanel(this);
		//this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		CleanUpAdapter window_handler = new CleanUpAdapter();
		this.addWindowListener(window_handler);
		this.getContentPane().add(spSetup,BorderLayout.CENTER);
	}

	public void showFrame()
	{
		this.setVisible(true);
	}
	
	public void showFrame(String title)
	{
		this.setTitle(title);
		this.setVisible(true);
	}

	/**
	 * Handle window events
	 */
	private class CleanUpAdapter extends WindowAdapter
	{
		public void windowClosing(WindowEvent we)
		{
			synchronized(MainDriver.UnloadLock)
			{
				MainDriver.QuitFlag.set(true);
				System.out.println("Application is now closing..");
				if (MainDriver.ErrorFlag.get() == false)
				{
					try
					{
						MainDriver.UnloadLock.wait();
					}catch (InterruptedException ie)
					{
						// do nothing
					}
				}
			}
			// Wait for communication handler to finish
			System.exit(0);
		}
	}
}
