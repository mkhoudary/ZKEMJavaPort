/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ps.purelogic.zkem4j;

import com.fingerprints.ClassFactory;
import com.fingerprints.IZKEM;
import com.fingerprints.events._IZKEMEvents;
import com4j.EventCookie;
import com4j.Holder;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.icmp4j.IcmpPingRequest;
import org.icmp4j.IcmpPingUtil;
import ps.purelogic.zkem4j.notifications.AttendanceNotification;
import ps.purelogic.zkem4j.notifications.EnrollmentNotification;
import ps.purelogic.zkem4j.notifications.ReadTimeChangeNotification;

/**
 *
 * @author mkhoudary
 */
public class AttendanceTerminal extends Observable {

    private final static long TIME_TO_CHECK_IN_MILLIS = 70 * 1000;
    private final static long ALLOWED_CLOCK_DIFFERENCE = 50000;
    private final static int MACHINE_NO = 1;
    private final int id;
    private final String name;
    private final String blockNo;
    private final String blockName;
    private int bandId;
    private final String brandName;
    private final String ipAddress;
    private final int portNo;
    private final int password;
    private AttendanceTerminalStatus terminalStatus = AttendanceTerminalStatus.DISCONNECTED;
    private IZKEM connector;
    private final static int ALL_EVENTS_MASK = 65535;
    private long clockTimestamp;
    private Timer checkTimer;
    private EventCookie clockEvents;
    private boolean withError = false;
    private long errorTimestamp;
    private long lastReadTimestamp;
    private AttendanceTerminalWarning warningType = AttendanceTerminalWarning.NONE;

    public AttendanceTerminal(int id, String name, String blockNo, String blockName, int bandId, String brandName, String ipAddress, int portNo, int password, long lastReadTimestamp) {
        this.id = id;
        this.name = name;
        this.blockNo = blockNo;
        this.blockName = blockName;
        this.bandId = bandId;
        this.brandName = brandName;
        this.ipAddress = ipAddress;
        this.portNo = portNo;
        this.password = password;
        this.lastReadTimestamp = lastReadTimestamp;

        initializeTerminalConnector();
    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the blockNo
     */
    public String getBlockNo() {
        return blockNo;
    }

    /**
     * @return the blockName
     */
    public String getBlockName() {
        return blockName;
    }

    /**
     * @return the brandName
     */
    public String getBrandName() {
        return brandName;
    }

    /**
     * @return the ipAddress
     */
    public String getIpAddress() {
        return ipAddress;
    }

    /**
     * @return the portNo
     */
    public int getPortNo() {
        return portNo;
    }

    /**
     * @return the password
     */
    public int getPassword() {
        return password;
    }

    /**
     * @return the termialStatus
     */
    public AttendanceTerminalStatus getTermialStatus() {
        return terminalStatus;
    }

    /**
     * @param termialStatus the termialStatus to set
     */
    public synchronized void setTermialStatus(AttendanceTerminalStatus termialStatus, boolean withError) {
        this.terminalStatus = termialStatus;
        this.withError = withError;

        setChanged();
        notifyObservers(this.terminalStatus.getNotifiaction());
    }

    private void initializeTerminalConnector() {
        connector = ClassFactory.createCZKEM();
        clockEvents = connector.advise(_IZKEMEvents.class, new MyEventsReceiver());

        //checkTimer = new Timer("Attendance Terminal No. " + id + ".", true);
        //checkTimer.schedule(new ClockChecker(), 0, TIME_TO_CHECK_IN_MILLIS);
    }

    private boolean pingServer() {
        IcmpPingRequest request = IcmpPingUtil.createIcmpPingRequest();
        request.setHost(ipAddress);
        request.setPacketSize(32);
        request.setTimeout(5000);
        request.setTtl(1000);

        return IcmpPingUtil.executePingRequest(request).getSuccessFlag();
    }

    public void connect() {
        if (terminalStatus.isBusy() || terminalStatus == AttendanceTerminalStatus.IDLE || terminalStatus == AttendanceTerminalStatus.DISABLED) {
            return;
        }

        setTermialStatus(AttendanceTerminalStatus.NETWORK_PING, true);

        if (!pingServer()) {
            setTermialStatus(AttendanceTerminalStatus.DISCONNECTED, true);

            return;
        }

        connector.setCommPassword(password);

        setTermialStatus(AttendanceTerminalStatus.CONNECTING, false);
        boolean connected = connector.connect_Net(ipAddress, portNo);

        if (connected) {
            connector.regEvent(MACHINE_NO, ALL_EVENTS_MASK);
            Holder<String> myHolder = new Holder<String>();
            
            connector.getFirmwareVersion(MACHINE_NO, myHolder);
            
            System.out.println(myHolder.value);

            return;
        }

        setTermialStatus(AttendanceTerminalStatus.DISCONNECTED, true);
    }

    public void disconnect() {
        if (terminalStatus.isBusy() || terminalStatus == AttendanceTerminalStatus.DISCONNECTED) {
            return;
        }

        if (!pingServer()) {
            return;
        }

        connector.disconnect();

        setTermialStatus(AttendanceTerminalStatus.DISCONNECTED, true);
    }

    public void finalizeTerminal() {
        clockEvents.close();
        checkTimer.cancel();

        disconnect();
    }

    public AttendanceRecordsStatement readAttendanceRecordsFrom(long fromTimestamp) {
        return readAttendanceRecordsFrom(fromTimestamp, 0);
    }

    public AttendanceRecordsStatement readAttendanceRecordsForDay(long timestamp) {
        Calendar dateCalendar = Calendar.getInstance();
        dateCalendar.setTime(new Date(timestamp));

        dateCalendar.set(Calendar.HOUR, 0);
        dateCalendar.set(Calendar.MINUTE, 0);
        dateCalendar.set(Calendar.SECOND, 0);

        long fromTimestamp = dateCalendar.getTimeInMillis();

        dateCalendar.set(Calendar.DATE, dateCalendar.get(Calendar.DATE) + 1);

        long toTimestamp = dateCalendar.getTimeInMillis();

        return readAttendanceRecordsFrom(fromTimestamp, toTimestamp);
    }

    public List<EnrolledEmployee> readEnrolledEmployees() {
        if (terminalStatus != AttendanceTerminalStatus.DISABLED && terminalStatus != AttendanceTerminalStatus.IDLE && terminalStatus != AttendanceTerminalStatus.WARNED) {
            return null; /*Should throw an exception */

        }

        List<EnrolledEmployee> enrolledEmployees = new ArrayList<EnrolledEmployee>();

        setTermialStatus(AttendanceTerminalStatus.READING, false);

        connector.readAllUserID(MACHINE_NO);

        Holder<String> dwEnrollNumber = new Holder<String>();
        Holder<String> dwName = new Holder<String>();
        Holder<String> dwPassword = new Holder<String>();
        Holder<Integer> dwPrivilege = new Holder<Integer>();
        Holder<Boolean> dwEnabled = new Holder<Boolean>();

        while (connector.ssR_GetAllUserInfo(MACHINE_NO, dwEnrollNumber, dwName, dwPassword, dwPrivilege, dwEnabled)) {
            enrolledEmployees.add(new EnrolledEmployee(dwEnrollNumber.value, dwName.value, dwPassword.value, dwPrivilege.value, id, dwEnabled.value));

            dwEnrollNumber = new Holder<String>();
            dwName = new Holder<String>();
            dwPassword = new Holder<String>();
            dwPrivilege = new Holder<Integer>();
            dwEnabled = new Holder<Boolean>();
        }

        setTermialStatus(AttendanceTerminalStatus.IDLE, false);

        disconnect();
        connect();

        return enrolledEmployees;
    }

    public AttendanceRecordsStatement readAttendanceRecordsFrom(long fromTimestamp, long toTimestamp) {
        if (terminalStatus != AttendanceTerminalStatus.DISABLED && terminalStatus != AttendanceTerminalStatus.IDLE && terminalStatus != AttendanceTerminalStatus.WARNED) {
            return null; /*Should throw an exception */

        }

        setTermialStatus(AttendanceTerminalStatus.READING, false);

        List<AttendanceRecord> attendance = new ArrayList<AttendanceRecord>();

        Calendar dateCalendar = Calendar.getInstance();
        dateCalendar.setTime(new Date(fromTimestamp));

        refreshClockTimestamp();
        
        if (connector.readLastestLogData(MACHINE_NO, 0, dateCalendar.get(Calendar.YEAR), dateCalendar.get(Calendar.MONTH) + 1, dateCalendar.get(Calendar.DATE), dateCalendar.get(Calendar.HOUR), dateCalendar.get(Calendar.MINUTE), dateCalendar.get(Calendar.SECOND))) {
            Holder<String> dwEnrollNumber = new Holder<String>();
            Holder<Integer> dwVerifyMode = new Holder<Integer>();
            Holder<Integer> dwInOutMode = new Holder<Integer>();
            Holder<Integer> dwYear = new Holder<Integer>();
            Holder<Integer> dwMonth = new Holder<Integer>();
            Holder<Integer> dwDay = new Holder<Integer>();
            Holder<Integer> dwHour = new Holder<Integer>();
            Holder<Integer> dwMinute = new Holder<Integer>();
            Holder<Integer> dwSecond = new Holder<Integer>();
            Holder<Integer> dwWorkCode = new Holder<Integer>();

            while (connector.ssR_GetGeneralLogData(1, dwEnrollNumber, dwVerifyMode, dwInOutMode, dwYear, dwMonth, dwDay, dwHour, dwMinute, dwSecond, dwWorkCode)) {
                dateCalendar.set(dwYear.value, dwMonth.value - 1, dwDay.value, dwHour.value, dwMinute.value, dwSecond.value);
                long attendanceTime = dateCalendar.getTimeInMillis();

                if (toTimestamp != 0 && attendanceTime > toTimestamp) {
                    break;
                }

                attendance.add(new AttendanceRecord(id, dwEnrollNumber.value, dwInOutMode.value == 0 ? AttendanceOperation.IN : AttendanceOperation.OUT, attendanceTime, getClockTimestamp()));

                dwEnrollNumber = new Holder<String>();
                dwVerifyMode = new Holder<Integer>();
                dwInOutMode = new Holder<Integer>();
                dwYear = new Holder<Integer>();
                dwMonth = new Holder<Integer>();
                dwDay = new Holder<Integer>();
                dwHour = new Holder<Integer>();
                dwMinute = new Holder<Integer>();
                dwSecond = new Holder<Integer>();
                dwWorkCode = new Holder<Integer>();
            }
        }

        setTermialStatus(AttendanceTerminalStatus.IDLE, false);

        disconnect();
        connect();

        return new AttendanceRecordsStatement(id, getClockTimestamp(), System.currentTimeMillis(), attendance);
    }

    public AttendanceRecordsStatement readAttendanceRecordsOld() {
        if (terminalStatus != AttendanceTerminalStatus.DISABLED && terminalStatus != AttendanceTerminalStatus.IDLE && terminalStatus != AttendanceTerminalStatus.WARNED) {
            return null; /*Should throw an exception */

        }

        setTermialStatus(AttendanceTerminalStatus.READING, false);

        Calendar dateCalendar = Calendar.getInstance();
        List<AttendanceRecord> attendance = new ArrayList<AttendanceRecord>();

        refreshClockTimestamp();

        if (connector.readAllGLogData(MACHINE_NO)) {
            Holder<Integer> dwMachineNumber = new Holder<Integer>();
            Holder<Integer> dwMachineNumber2 = new Holder<Integer>();
            Holder<Integer> dwEnrollNumber = new Holder<Integer>();
            Holder<Integer> dwVerifyMode = new Holder<Integer>();
            Holder<Integer> dwInOutMode = new Holder<Integer>();
            Holder<Integer> dwYear = new Holder<Integer>();
            Holder<Integer> dwMonth = new Holder<Integer>();
            Holder<Integer> dwDay = new Holder<Integer>();
            Holder<Integer> dwHour = new Holder<Integer>();
            Holder<Integer> dwMinute = new Holder<Integer>();

            while (connector.getGeneralLogData(1, dwMachineNumber, dwEnrollNumber, dwMachineNumber2, dwVerifyMode, dwInOutMode, dwYear, dwMonth, dwDay, dwHour, dwMinute)) {
                dateCalendar.set(dwYear.value, dwMonth.value - 1, dwDay.value, dwHour.value, dwMinute.value, 0);
                long attendanceTime = dateCalendar.getTimeInMillis();

                attendance.add(new AttendanceRecord(id, String.valueOf(dwEnrollNumber.value), dwInOutMode.value == 0 ? AttendanceOperation.IN : AttendanceOperation.OUT, attendanceTime, 0));

                dwMachineNumber = new Holder<Integer>();
                dwMachineNumber2 = new Holder<Integer>();
                dwEnrollNumber = new Holder<Integer>();
                dwVerifyMode = new Holder<Integer>();
                dwInOutMode = new Holder<Integer>();
                dwYear = new Holder<Integer>();
                dwMonth = new Holder<Integer>();
                dwDay = new Holder<Integer>();
                dwHour = new Holder<Integer>();
                dwMinute = new Holder<Integer>();
            }
        }

        setTermialStatus(AttendanceTerminalStatus.IDLE, false);

        disconnect();
        connect();

        return new AttendanceRecordsStatement(id, getClockTimestamp(), System.currentTimeMillis(), attendance);
    }

    public List<String> getUserTemplates(String employeeNo) {
        if (terminalStatus != AttendanceTerminalStatus.DISABLED && terminalStatus != AttendanceTerminalStatus.IDLE && terminalStatus != AttendanceTerminalStatus.WARNED) {
            return null; /*Should throw an exception */

        }

        List<String> fingerPrints = new ArrayList<String>();

        setTermialStatus(AttendanceTerminalStatus.READING, false);

        if (connector.readAllTemplate(MACHINE_NO)) {
            for (int fingerIndex = 0; fingerIndex < 10; fingerIndex++) {
                Holder<String> fingerPrint = new Holder<String>();
                Holder<Integer> fingerPrintLength = new Holder<Integer>();

                if (connector.ssR_GetUserTmpStr(MACHINE_NO, employeeNo, fingerIndex, fingerPrint, fingerPrintLength)) {
                    fingerPrints.add(fingerPrint.value);
                } else {
                    fingerPrints.add("");
                }
            }
        }

        setTermialStatus(AttendanceTerminalStatus.IDLE, false);

        return fingerPrints;
    }

    public void setTerminalEnabled(boolean enabled) {
        if (terminalStatus != AttendanceTerminalStatus.DISABLED && terminalStatus != AttendanceTerminalStatus.IDLE && terminalStatus != AttendanceTerminalStatus.WARNED) {
            return; /*Should throw an exception */

        }

        connector.enableDevice(MACHINE_NO, enabled);

        if (enabled) {
            setTermialStatus(AttendanceTerminalStatus.IDLE, false);
        } else {
            setTermialStatus(AttendanceTerminalStatus.DISABLED, false);
        }
    }

    public void setUserTemplates(String employeeNo, String fullName, List<String> templates) {
        if (terminalStatus != AttendanceTerminalStatus.IDLE && terminalStatus != AttendanceTerminalStatus.WARNED) {
            return; /*Should throw an exception */

        }

        setTermialStatus(AttendanceTerminalStatus.WRITING, false);

        if (createUser(employeeNo, fullName)) {
            for (int fingerIndex = 0; fingerIndex < templates.size(); fingerIndex++) {
                String templateData = templates.get(fingerIndex);
                connector.setUserTmpExStr(MACHINE_NO, employeeNo, fingerIndex, 1, templateData);
            }
        }

        setTermialStatus(AttendanceTerminalStatus.IDLE, false);
    }

    public boolean deleteUser(String employeeNo) {
        if (terminalStatus != AttendanceTerminalStatus.DISABLED && terminalStatus != AttendanceTerminalStatus.IDLE && terminalStatus != AttendanceTerminalStatus.WARNED) {
            return false; /*Should throw an exception */

        }

        setTermialStatus(AttendanceTerminalStatus.WRITING, false);

        boolean result = false;

        if (connector.readAllUserID(MACHINE_NO)) {
            result = connector.ssR_DeleteEnrollDataExt(MACHINE_NO, employeeNo, 12);
        }

        setTermialStatus(AttendanceTerminalStatus.IDLE, false);

        return result;
    }

    public boolean deleteUserFinger(String employeeNo, int fingerIndex) {
        if (terminalStatus != AttendanceTerminalStatus.DISABLED && terminalStatus != AttendanceTerminalStatus.IDLE && terminalStatus != AttendanceTerminalStatus.WARNED) {
            return false; /*Should throw an exception */

        }

        setTermialStatus(AttendanceTerminalStatus.WRITING, false);

        boolean result = false;

        if (connector.readAllUserID(MACHINE_NO)) {
            result = connector.ssR_DeleteEnrollData(MACHINE_NO, employeeNo, fingerIndex);
        }

        setTermialStatus(AttendanceTerminalStatus.IDLE, false);

        return result;
    }

    public boolean createUser(String employeeNo, String fullName) {
        return connector.ssR_SetUserInfo(MACHINE_NO, employeeNo, fullName, "", 0, true);
    }

    public boolean startIdentification() {
        if (terminalStatus != AttendanceTerminalStatus.DISABLED && terminalStatus != AttendanceTerminalStatus.IDLE && terminalStatus != AttendanceTerminalStatus.WARNED) {
            return false;
        }

        return connector.startIdentify();
    }

    public boolean startEnrollment(String employeeNo, int fingerIndex) {
        if (terminalStatus != AttendanceTerminalStatus.DISABLED && terminalStatus != AttendanceTerminalStatus.IDLE && terminalStatus != AttendanceTerminalStatus.WARNED) {
            return false;
        }

        return connector.startEnrollEx(employeeNo, fingerIndex, 1);
    }

    public boolean startCardEnrollment(String employeeNo, String cardNo) {
        if (terminalStatus != AttendanceTerminalStatus.DISABLED && terminalStatus != AttendanceTerminalStatus.IDLE && terminalStatus != AttendanceTerminalStatus.WARNED) {
            return false;
        }

        if (connector.setStrCardNumber(cardNo)) {
            if (connector.ssR_SetUserInfo(MACHINE_NO, employeeNo, "Mohammed El Khoudary", "", 2, true)) {
                return true;
            }

            return true;
        }

        return false;
    }
    
    public String getEmployeeCardNumber(String employeeNo) {
        if (terminalStatus != AttendanceTerminalStatus.DISABLED && terminalStatus != AttendanceTerminalStatus.IDLE && terminalStatus != AttendanceTerminalStatus.WARNED) {
            return null;
        }
        
        Holder<String> name = new Holder<String>();
        Holder<String> password = new Holder<String>();
        Holder<Integer> privilege = new Holder<Integer>();
        Holder<Boolean> enabled = new Holder<Boolean>();

        if (connector.ssR_GetUserInfo(MACHINE_NO, employeeNo, name, password, privilege, enabled)) {
            Holder<String> cardNumber = new Holder<String>();
            
            if (connector.getStrCardNumber(cardNumber)) {
                return cardNumber.value;
            }

            return null;
        }

        return null;
    }


    public boolean enableEmployee(String employeeNo) {
        if (terminalStatus != AttendanceTerminalStatus.DISABLED && terminalStatus != AttendanceTerminalStatus.IDLE && terminalStatus != AttendanceTerminalStatus.WARNED) {
            return false;
        }

        return connector.ssR_EnableUser(MACHINE_NO, employeeNo, true);
    }

    public boolean disableEmployee(String employeeNo) {
        if (terminalStatus != AttendanceTerminalStatus.DISABLED && terminalStatus != AttendanceTerminalStatus.IDLE && terminalStatus != AttendanceTerminalStatus.WARNED) {
            return false;
        }

        return connector.ssR_EnableUser(MACHINE_NO, employeeNo, false);
    }

    public void deleteMessage() {
        if (terminalStatus != AttendanceTerminalStatus.IDLE && terminalStatus != AttendanceTerminalStatus.WARNED) {
            return; /*Should throw an exception */

        }

        connector.deleteSMS(MACHINE_NO, 1);
    }

    public void setMessage() {
        try {
            if (terminalStatus != AttendanceTerminalStatus.IDLE && terminalStatus != AttendanceTerminalStatus.WARNED) {
                return; /*Should throw an exception */

            }

            String message = "يرجى مراجعة الدائرة المالية لتحديث بصمتك المخفية.";
            String encodedMessage = new String(message.getBytes("utf-8"));

            connector.setSMS(MACHINE_NO, 1, 254, 5, "2014-11-18 08:21:00", encodedMessage);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(AttendanceTerminal.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void sendFile() {
        if (terminalStatus != AttendanceTerminalStatus.IDLE && terminalStatus != AttendanceTerminalStatus.WARNED) {
            return; /*Should throw an exception */

        }

        System.out.println(connector.sendFile(MACHINE_NO, "d:\\ad_0.jpg"));
    }

    public void setUserMessage() {
        if (terminalStatus != AttendanceTerminalStatus.IDLE && terminalStatus != AttendanceTerminalStatus.WARNED) {
            return; /*Should throw an exception */

        }

        connector.ssR_SetUserSMS(MACHINE_NO, "4443", 1);
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();

        checkTimer.cancel();
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }

    /**
     * @return the clockTimestamp
     */
    public long getClockTimestamp() {
        return clockTimestamp;
    }

    public void refreshClockTimestamp() {
        Holder<Integer> year = new Holder<Integer>();
        Holder<Integer> month = new Holder<Integer>();
        Holder<Integer> day = new Holder<Integer>();
        Holder<Integer> hour = new Holder<Integer>();
        Holder<Integer> minute = new Holder<Integer>();
        Holder<Integer> second = new Holder<Integer>();

        connector.getDeviceTime(MACHINE_NO, year, month, day, hour, minute, second);

        Calendar timeToSet = Calendar.getInstance();
        timeToSet.set(year.value, month.value - 1, day.value, hour.value, minute.value, 0);

        long timeReading = timeToSet.getTimeInMillis();

        if (timeReading > 32530932732350L) {
            connector.disconnect();
            withError = true;

            return;
        }

        clockTimestamp = timeReading;
    }

    /**
     * @return the warningType
     */
    public AttendanceTerminalWarning getWarningType() {
        return warningType;
    }

    /**
     * @param warningType the warningType to set
     */
    public void setWarningType(AttendanceTerminalWarning warningType) {
        this.warningType = warningType;
    }

    /**
     * @return the bandId
     */
    public int getBandId() {
        return bandId;
    }

    /**
     * @return the lastReadTimestamp
     */
    public long getLastReadTimestamp() {
        return lastReadTimestamp;
    }

    /**
     * @param lastReadTimestamp the lastReadTimestamp to set
     */
    public void setLastReadTimestamp(long lastReadTimestamp) {
        this.lastReadTimestamp = lastReadTimestamp;

        setChanged();
        notifyObservers(new ReadTimeChangeNotification(lastReadTimestamp));
    }

    private class MyEventsReceiver extends _IZKEMEvents {

        @Override
        public void onConnected() {
            setTermialStatus(AttendanceTerminalStatus.IDLE, false);
        }

        @Override
        public void onDisConnected() {
            setTermialStatus(AttendanceTerminalStatus.DISCONNECTED, false);
        }

        @Override
        public void onAttTransactionEx(String enrollNumber, int isInValid, int attState, int verifyMethod, int year, int month, int day, int hour, int minute, int second, int workCode) {
            Calendar timeToSet = Calendar.getInstance();
            timeToSet.set(year, month - 1, day, hour, minute, second);

            refreshClockTimestamp();

            setChanged();
            notifyObservers(new AttendanceNotification(new AttendanceRecord(id, enrollNumber, attState == 0 ? AttendanceOperation.IN : AttendanceOperation.OUT, timeToSet.getTimeInMillis(), getClockTimestamp())));
        }

        @Override
        public void onEnrollFingerEx(
                String enrollNumber,
                int fingerIndex,
                int actionResult,
                int templateLength) {
            setChanged();
            notifyObservers(new EnrollmentNotification(enrollNumber, fingerIndex, actionResult));
        }

        @Override
        public void onHIDNum(
                int cardNumber) {
            System.out.println(cardNumber);
        }
    }

    private class ClockChecker extends TimerTask {

        @Override
        public void run() {
            if (terminalStatus == AttendanceTerminalStatus.DISCONNECTED && withError) {
                connect();

                return;
            }

            if (terminalStatus != AttendanceTerminalStatus.IDLE && terminalStatus != AttendanceTerminalStatus.WARNED) {
                return;
            }

            /*refreshClockTimestamp();

             System.out.println(new Date(clockTimestamp) + " " + new Date(System.currentTimeMillis()));*/
            if (Math.abs(clockTimestamp - System.currentTimeMillis()) > ALLOWED_CLOCK_DIFFERENCE) {
                if (terminalStatus == AttendanceTerminalStatus.WARNED) {
                    setWarningType(AttendanceTerminalWarning.NONE);
                    setTermialStatus(AttendanceTerminalStatus.IDLE, false);
                    errorTimestamp = 0;
                }
            } else {
                if (terminalStatus == AttendanceTerminalStatus.IDLE) {
                    setWarningType(AttendanceTerminalWarning.WRONG_TIMING);
                    setTermialStatus(AttendanceTerminalStatus.WARNED, true);
                    errorTimestamp = System.currentTimeMillis();
                }
            }
        }
    }
}
