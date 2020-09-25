/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ps.purelogic.zkem4j.notifications;

import ps.purelogic.zkem4j.AttendanceRecord;

/**
 *
 * @author mkhoudary
 */
public class AttendanceNotification implements Notification {
    
    private AttendanceRecord record;
    
    public AttendanceNotification(AttendanceRecord record) {
        this.record = record;
    }

    /**
     * @return the record
     */
    public AttendanceRecord getRecord() {
        return record;
    }
    
}
