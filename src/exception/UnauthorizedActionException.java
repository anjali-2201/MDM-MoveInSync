package exception;

/** Thrown when an admin does not have permission to perform an action. */
public class UnauthorizedActionException extends RuntimeException {
    public UnauthorizedActionException(String adminId, String action) {
        super("UNAUTHORIZED: Admin [" + adminId + "] does not have permission to [" + action + "]");
    }
}
