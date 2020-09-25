/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ps.purelogic.zkem4j.notifications;

/**
 *
 * @author mkhoudary
 */
public class EnrollmentNotification implements Notification {
    
    private String employeeNo;
    private int fingerIndex;
    private int status;
    
    public EnrollmentNotification(String employeeNo, int fingerIndex, int status) {
        this.employeeNo = employeeNo;
        this.fingerIndex = fingerIndex;
        this.status = status;
    }

    /**
     * @return the employeeNo
     */
    public String getEmployeeNo() {
        return employeeNo;
    }

    /**
     * @return the fingerIndex
     */
    public int getFingerIndex() {
        return fingerIndex;
    }

    /**
     * @return the status
     */
    public int getStatus() {
        return status;
    }
    
}
