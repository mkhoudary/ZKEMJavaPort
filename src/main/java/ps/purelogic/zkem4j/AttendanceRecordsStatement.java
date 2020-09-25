/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ps.purelogic.zkem4j;

import java.util.List;

/**
 *
 * @author mkhoudary
 */
public class AttendanceRecordsStatement {

    private long terminalId;
    private long clockTimestamp;
    private long systemTimestamp;
    private List<AttendanceRecord> records;

    public AttendanceRecordsStatement(long terminalId, long clockTimestamp, long systemTimestamp, List<AttendanceRecord> records) {
        this.terminalId = terminalId;
        this.clockTimestamp = clockTimestamp;
        this.systemTimestamp = systemTimestamp;
        this.records = records;
    }

    /**
     * @return the terminalId
     */
    public long getTerminalId() {
        return terminalId;
    }

    /**
     * @return the clockTimestamp
     */
    public long getClockTimestamp() {
        return clockTimestamp;
    }

    /**
     * @return the systemTimestamp
     */
    public long getSystemTimestamp() {
        return systemTimestamp;
    }

    /**
     * @return the records
     */
    public List<AttendanceRecord> getRecords() {
        return records;
    }
    
    
}
