package team.projectpulse.system.exception;

/**
 * A deactivation of a student who is already deactivated (UC-STU-deactivate-student, extension 6a).
 * Answered with 409, so a stale screen is reported rather than silently accepted.
 */
public class StudentAlreadyDeactivatedException extends RuntimeException {

    public StudentAlreadyDeactivatedException(String studentFullName) {
        super(studentFullName + " is already deactivated.");
    }

}
