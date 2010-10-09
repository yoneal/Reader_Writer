/**
 * 
 */
package reader_writer;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.DefaultListModel;
import javax.swing.JScrollPane;
import javax.swing.JList;
import javax.swing.border.*;
import javax.swing.BorderFactory;
import javax.swing.event.*;
import javax.swing.ListSelectionModel;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * @author neal
 *
 */
public class MainPanel extends JPanel implements UserInterface, ActionListener
{
	private JLabel lStatus;
	private DefaultListModel lmGroup, lmSent, lmRecv, lmProcess;
	private JScrollPane spGroup, spSent, spRecv, spProcess;
	private JList lstGroup, lstSent, lstRecv, lstProcess;
	private JButton bAdvanced;
	private JFrame fMainFrame;
	
	private boolean mAdvToggle = false;
	
	/**
	 * 
	 */
	//private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public MainPanel(JFrame main_frame)
	{
//		 ToDo: Set background color
		//this.setLayout(new GridLayout(4,1));
		this.fMainFrame = main_frame;
		this.setLayout(new GridLayout(3,1));
		
		lStatus = new JLabel("Initializing..");
		Border bdrStatus = BorderFactory.createEtchedBorder();
		lStatus.setBorder(BorderFactory.createTitledBorder(bdrStatus,"Status:") );
		this.add(lStatus);
		
		lmGroup = new DefaultListModel();
		lstGroup = new JList(lmGroup);
		lstGroup.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		spGroup = new JScrollPane(lstGroup);
		Border bdrGroup = BorderFactory.createEtchedBorder();
		spGroup.setBorder(BorderFactory.createTitledBorder(bdrGroup,"Group:") );
		this.add(spGroup);
		
		bAdvanced = new JButton("Show Advanced View");
		bAdvanced.addActionListener(this);
		this.add(bAdvanced);
		
		lmSent = new DefaultListModel();
		lstSent = new JList(lmSent);
		lstSent.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		spSent = new JScrollPane(lstSent);
		Border bdrSent = BorderFactory.createEtchedBorder();
		spSent.setBorder(BorderFactory.createTitledBorder(bdrSent,"Send:") );
		//this.add(spSent);
	
		lmRecv = new DefaultListModel();
		lstRecv = new JList(lmRecv);
		lstRecv.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		spRecv = new JScrollPane(lstRecv);
		Border bdrRecv = BorderFactory.createEtchedBorder();
		spRecv.setBorder(BorderFactory.createTitledBorder(bdrGroup,"Receive:") );
		//this.add(spRecv);
		
		lmProcess = new DefaultListModel();
		lstProcess = new JList(lmProcess);
		lstProcess.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		spProcess = new JScrollPane(lstProcess);
		Border bdrProcess = BorderFactory.createEtchedBorder();
		spProcess.setBorder(BorderFactory.createTitledBorder(bdrProcess,"Reader/Writer:") );
		//this.add(spProcess);	
	}

	public void actionPerformed(ActionEvent evt)
	{
		String actionCommand = evt.getActionCommand();
		if (actionCommand.equals("Hide Advanced View"))
		{
			bAdvanced.setText("Show Advanced View");
			((GridLayout)(this.getLayout())).setRows(3);
			this.removeAll();
			this.add(lStatus);
			this.add(spGroup);
			this.add(bAdvanced);
			this.fMainFrame.setSize(300,300);
			this.fMainFrame.repaint();
		}
		if (actionCommand.equals("Show Advanced View"))
		{
			bAdvanced.setText("Hide Advanced View");
			((GridLayout)(this.getLayout())).setRows(6);
			this.add(lStatus);
			this.add(spGroup);
			this.add(spSent);
			this.add(spRecv);
			this.add(spProcess);
			this.add(bAdvanced);
			this.fMainFrame.setSize(300,800);
			this.fMainFrame.repaint();
		}		
	}	
	
	/**
	 * 
	 * @return
	 */
	public String getStatus()
	{
		return lStatus.getText();
	}
	public void setStatus(String status)
	{
		lStatus.setText(status);
	}
	
	public void printOutSendReport(String send_report)
	{
		lmSent.addElement(send_report);
		lstSent.setModel(lmSent);
		lstSent.revalidate();
		lstSent.repaint();
	}
	public void printOutRecvReport(String recv_report)
	{
		lmRecv.addElement(recv_report);
		this.fMainFrame.repaint();
	}
	public void printOutProcessReport(String proc_report)
	{
		
	}
}
