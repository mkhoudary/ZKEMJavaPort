/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ps.purelogic.zkem4j.notifications;

import ps.purelogic.zkem4j.AttendanceTerminalStatus;

/**
 *
 * @author mkhoudary
 */
public class StatusChangeNotification implements Notification {
    
    private AttendanceTerminalStatus status;
    
    public StatusChangeNotification(AttendanceTerminalStatus status) {
        this.status = status;
    }

    /**
     * @return the status
     */
    public AttendanceTerminalStatus getStatus() {
        return status;
    }
    
}
