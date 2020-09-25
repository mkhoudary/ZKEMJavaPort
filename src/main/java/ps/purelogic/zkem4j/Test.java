/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ps.purelogic.zkem4j;

import java.text.SimpleDateFormat;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JFrame;
import ps.purelogic.zkem4j.notifications.AttendanceNotification;
import ps.purelogic.zkem4j.notifications.StatusChangeNotification;

/**
 *
 * @author mkhoudary
 */
public class Test implements Observer {

    private AttendanceTerminal terminal;

    public Test() {
        prepare();
    }

    private void prepare() {
        AttendanceTerminal aTerminal = new AttendanceTerminal(1, "Supermarket Main", "B", "Base", 1, "zktem", "192.168.1.44", 4370, 0, 0L);
        aTerminal.addObserver(this);

        aTerminal.connect();

        AttendanceRecordsStatement stmt = aTerminal.readAttendanceRecordsOld();

        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

        for (AttendanceRecord record : stmt.getRecords()) {
            System.out.println(record.getEmployeeNo() + " " + record.getOperation() + " " + format.format(record.getAttendanceTime()));
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setVisible(true);

        Test test = new Test();
    }

    @Override
    public void update(Observable o, Object arg) {
        if (arg instanceof StatusChangeNotification) {
            StatusChangeNotification notification = (StatusChangeNotification) arg;

            System.out.println(o.toString() + " > " + notification.getStatus());

        } else if (arg instanceof AttendanceNotification) {
            AttendanceNotification notification = (AttendanceNotification) arg;

            System.out.println(notification.getRecord().getEmployeeNo() + " " + notification.getRecord().getAttendanceTime() + " " + notification.getRecord().getTerminalId());

        }
    }
}
