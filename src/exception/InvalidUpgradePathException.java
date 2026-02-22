package exception;

/** Thrown when a version upgrade path is not allowed by the compatibility matrix. */
public class InvalidUpgradePathException extends RuntimeException {
    public InvalidUpgradePathException(String fromVersion, String toVersion) {
        super("INVALID UPGRADE PATH: Direct upgrade from v" + fromVersion +
              " to v" + toVersion + " is not allowed. Check the compatibility matrix.");
    }
}
