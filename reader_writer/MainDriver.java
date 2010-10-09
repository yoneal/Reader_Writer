/**
 * 
 */
package reader_writer;

import java.util.concurrent.atomic.*;

/**
 * @author neal
 *
 */
public class MainDriver 
{	

	public static AtomicBoolean QuitFlag = new AtomicBoolean(false);
	
	/**
	 * @param args
	 */
	public static void main(String[] args) 
	{
		// Create frames and panels
		MainFrame mfMain = new MainFrame();
		mfMain.showFrame("Reader-Writer Demo");
		
	}
	
	

}
