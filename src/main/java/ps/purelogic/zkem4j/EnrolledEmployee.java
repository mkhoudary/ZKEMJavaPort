/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package ps.purelogic.zkem4j;

/**
 *
 * @author mkhoudary
 */
public class EnrolledEmployee {

    private String enrollmentId;
    private String name;
    private String password;
    private int privilege;
    private int terminalId;
    private boolean enabled;

    public EnrolledEmployee(String enrollmentId, String name, String password, int privilege, int terminalId, boolean enabled) {
        this.enrollmentId = enrollmentId;
        this.name = name;
        this.password = password;
        this.privilege = privilege;
        this.terminalId = terminalId;
        this.enabled = enabled;
    }

    /**
     * @return the enrollmentId
     */
    public String getEnrollmentId() {
        return enrollmentId;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return the privilege
     */
    public int getPrivilege() {
        return privilege;
    }

    /**
     * @return the enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * @return the terminalId
     */
    public int getTerminalId() {
        return terminalId;
    }
}
