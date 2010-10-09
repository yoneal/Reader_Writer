/**
 * 
 */
package reader_writer;

/**
 * @author neal
 *
 */
public interface UserInterface 
{
	public String getStatus();
	public void setStatus(String status);
	public void printOutSendReport(String send_report);
	public void printOutRecvReport(String recv_report);
	public void printOutProcessReport(String proc_report);
}
