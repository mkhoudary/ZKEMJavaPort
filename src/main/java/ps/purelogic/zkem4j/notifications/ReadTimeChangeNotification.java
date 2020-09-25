/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ps.purelogic.zkem4j.notifications;

/**
 *
 * @author mkhoudary
 */
public class ReadTimeChangeNotification implements Notification {

    private long readTimeRecord;

    public ReadTimeChangeNotification(long readTimeRecord) {
        this.readTimeRecord = readTimeRecord;
    }

    /**
     * @return the readTimeRecord
     */
    public long getReadTimeRecord() {
        return readTimeRecord;
    }
}
