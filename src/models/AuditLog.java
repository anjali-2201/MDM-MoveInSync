package models;

import java.time.LocalDateTime;

/**
 * Represents a single immutable audit log entry.
 * Every action in the system is recorded here for full traceability.
 */
public class AuditLog {

    // ─── Fields ───────────────────────────────────────────────────────────────
    private final String logId;
    private final String deviceImei;      // Which device this log belongs to
    private final String adminId;         // Who performed the action (null if system)
    private final String action;          // e.g., "UPDATE_SCHEDULED", "DOWNLOAD_STARTED"
    private final String details;         // Extra context/description
    private final LocalDateTime timestamp;
    private final boolean isFailure;      // true if this log represents a failure

    // ─── Constructor ──────────────────────────────────────────────────────────
    public AuditLog(String logId, String deviceImei, String adminId,
                    String action, String details, boolean isFailure) {
        this.logId = logId;
        this.deviceImei = deviceImei;
        this.adminId = adminId;
        this.action = action;
        this.details = details;
        this.timestamp = LocalDateTime.now();
        this.isFailure = isFailure;
    }

    // ─── Getters (immutable - no setters) ─────────────────────────────────────
    public String getLogId() { return logId; }
    public String getDeviceImei() { return deviceImei; }
    public String getAdminId() { return adminId; }
    public String getAction() { return action; }
    public String getDetails() { return details; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isFailure() { return isFailure; }

    @Override
    public String toString() {
        String failureTag = isFailure ? " [FAILURE]" : "";
        String actor = (adminId != null) ? " | Admin: " + adminId : " | Actor: SYSTEM";
        return String.format("[%s]%s | Device: %s%s | %s - %s",
                timestamp, failureTag, deviceImei, actor, action, details);
    }
}
