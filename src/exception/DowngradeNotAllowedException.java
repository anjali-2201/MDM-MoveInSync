package exception;

/**
 * Thrown when an admin tries to schedule a downgrade (target < current version).
 */
public class DowngradeNotAllowedException extends RuntimeException {
    public DowngradeNotAllowedException(String fromVersion, String toVersion) {
        super("DOWNGRADE BLOCKED: Cannot downgrade from v" + fromVersion +
              " to v" + toVersion + ". Downgrades are strictly prohibited.");
    }
}
