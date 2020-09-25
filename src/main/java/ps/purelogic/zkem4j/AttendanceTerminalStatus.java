/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ps.purelogic.zkem4j;

import ps.purelogic.zkem4j.notifications.StatusChangeNotification;

/**
 *
 * @author mkhoudary
 */
public enum AttendanceTerminalStatus {

    DISCONNECTED(false, "غير متصل"),
    NETWORK_PING(true, "فحص الشبكة"),
    CONNECTING(true, "جاري الاتصال"),
    IDLE(false, "مستعد"),
    DISABLED(true, "محظور"),
    WARNED(false, "مستعد (تحذير)"),
    READING(true, "قيد القراءة"),
    WRITING(true, "قيد الكتابة");

    private final boolean busy;
    private final String label;
    private final StatusChangeNotification notifiaction;
    
    private AttendanceTerminalStatus(boolean busy, String label) {
        this.busy = busy;
        this.label = label;
        
        notifiaction = new StatusChangeNotification(this);
    }

    /**
     * @return the busy
     */
    public boolean isBusy() {
        return busy;
    }

    /**
     * @return the notifiaction
     */
    public StatusChangeNotification getNotifiaction() {
        return notifiaction;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
    
}
