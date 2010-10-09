/**
 * 
 * 
 */

package reader_writer;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Color;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JFrame;
import javax.swing.SwingConstants;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * JPanel for connection setup.
 * This panel is initially shown on startup of the application.
 * Shows a UI where a user can setup the network connection needed by the application.
 * @author neal
 * @version 1.0.0
 *
 */
public class SetupPanel extends JPanel implements ActionListener
{
	private JTextField tfGroup, tfPort;
	private JButton bConnect, bClear;
	private JFrame fMainFrame;
	/**
	 * Constructor for SetupPanel
	 * @param main_frame set this to the frame of the app
	 */
	public SetupPanel(JFrame main_frame)
	{
		// ToDo: Set background color
		this.setLayout(new GridLayout(3,2,10,5));
		this.fMainFrame = main_frame;
		
		// Setup labels and buttons
		JLabel lGroup = new JLabel("Group:"); 
		JLabel lPort = new JLabel("Port:");
		tfGroup = new JTextField(MulticastHandler.msMuAddress);
		tfPort = new JTextField(MulticastHandler.msPort);
		bConnect = new JButton("Connect");
		bClear = new JButton("Clear");
		
		//lGroup.setOpaque(true);
		//lGroup.setBackground(Color.black);
		//lGroup.setForeground(Color.white);
		
		//lPort.setOpaque(true);
		//lPort.setBackground(Color.black);
		//lPort.setForeground(Color.white);
				
		// Place the components
		this.add(lGroup);
		this.add(tfGroup);
		this.add(lPort);
		this.add(tfPort);
		this.add(bConnect);
		this.add(bClear);
		
		// Add listeners
		bConnect.addActionListener(this);
		bClear.addActionListener(this);
	}	
	
	public void actionPerformed(ActionEvent evt)
	{
		String actionCommand = evt.getActionCommand();
		if (actionCommand.equals("Connect"))
		{
			MainPanel mpMain = new MainPanel(fMainFrame);			
			
			// Show the Main Panel
			this.fMainFrame.getContentPane().remove(this);
			this.fMainFrame.getContentPane().add(mpMain,BorderLayout.CENTER);
			//this.fMainFrame.setSize(300,600);
			this.fMainFrame.setSize(300,300);
			//this.fMainFrame.repaint();
			
			// Start the communication manager
			CommManager cmCommManager;
			try
			{
				cmCommManager = new CommManager(tfGroup.getText(),Integer.parseInt(tfPort.getText()),mpMain);
				new Thread(cmCommManager).start();
			} catch (Exception e)
			{
				System.out.println(e.toString());
				e.printStackTrace();
				mpMain.setStatus("Initialization Failed!");
				return;
			}
			// Start the reader/writer app
//			ApplicationDriver adReaderWriter = new ApplicationDriver(cmCommManager);
//			new Thread(adReaderWriter).start();
		}
	}
}
