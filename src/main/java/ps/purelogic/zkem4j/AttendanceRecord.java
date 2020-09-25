/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ps.purelogic.zkem4j;

/**
 *
 * @author mkhoudary
 */
public class AttendanceRecord {

    private long terminalId;
    private String employeeNo;
    private AttendanceOperation operation;
    private long attendanceTime;
    private long terminalTime;

    public AttendanceRecord(long terminalId, String employeeNo, AttendanceOperation operation, long attendanceTime, long terminalTime) {
        this.terminalId = terminalId;
        this.employeeNo = employeeNo;
        this.operation = operation;
        this.attendanceTime = attendanceTime;
        this.terminalTime = terminalTime;
    }

    /**
     * @return the id
     */
    public long getTerminalId() {
        return terminalId;
    }

    /**
     * @return the employeeNo
     */
    public String getEmployeeNo() {
        return employeeNo;
    }

    /**
     * @return the operation
     */
    public AttendanceOperation getOperation() {
        return operation;
    }

    /**
     * @return the attendanceTime
     */
    public long getAttendanceTime() {
        return attendanceTime;
    }

    /**
     * @return the terminalTime
     */
    public long getTerminalTime() {
        return terminalTime;
    }
}
