package workflow;

import models.AuditLog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Represents the full lifecycle of an update for a single device.
 * Implements a STATE MACHINE pattern.
 *
 * States:
 * SCHEDULED → NOTIFIED → DOWNLOAD_STARTED → DOWNLOAD_COMPLETED
 *           → INSTALL_STARTED → INSTALL_COMPLETED
 *           (or) → FAILED at any stage
 */
public class UpdateWorkflow {

    // ─── State Enum ───────────────────────────────────────────────────────────
    public enum UpdateState {
        SCHEDULED,
        NOTIFIED,
        DOWNLOAD_STARTED,
        DOWNLOAD_COMPLETED,
        INSTALL_STARTED,
        INSTALL_COMPLETED,
        FAILED
    }

    // ─── Fields ───────────────────────────────────────────────────────────────
    private final String workflowId;
    private final String deviceImei;
    private final String fromVersion;
    private final String toVersion;
    private final String scheduledByAdminId;
    private UpdateState currentState;
    private String failureReason;
    private String failedAtStage;
    private int retryCount;
    private static final int MAX_RETRIES = 3;

    // Each workflow keeps its own timeline of events (audit trail)
    private final List<AuditLog> timeline;

    // ─── Constructor ──────────────────────────────────────────────────────────
    public UpdateWorkflow(String deviceImei, String fromVersion,
                          String toVersion, String scheduledByAdminId) {
        this.workflowId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.deviceImei = deviceImei;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.scheduledByAdminId = scheduledByAdminId;
        this.currentState = UpdateState.SCHEDULED;
        this.retryCount = 0;
        this.timeline = new ArrayList<>();

        // Log initial state
        addToTimeline("UPDATE_SCHEDULED",
                "Update from v" + fromVersion + " to v" + toVersion + " scheduled by Admin: " + scheduledByAdminId,
                false);
    }

    // ─── State Transition Methods ─────────────────────────────────────────────

    public void notifyDevice() {
        assertState(UpdateState.SCHEDULED);
        this.currentState = UpdateState.NOTIFIED;
        addToTimeline("DEVICE_NOTIFIED", "Push notification sent to device.", false);
    }

    public void startDownload() {
        assertState(UpdateState.NOTIFIED);
        this.currentState = UpdateState.DOWNLOAD_STARTED;
        addToTimeline("DOWNLOAD_STARTED", "Device acknowledged and started downloading v" + toVersion, false);
    }

    public void completeDownload() {
        assertState(UpdateState.DOWNLOAD_STARTED);
        this.currentState = UpdateState.DOWNLOAD_COMPLETED;
        addToTimeline("DOWNLOAD_COMPLETED", "Download of v" + toVersion + " finished successfully.", false);
    }

    public void startInstallation() {
        assertState(UpdateState.DOWNLOAD_COMPLETED);
        this.currentState = UpdateState.INSTALL_STARTED;
        addToTimeline("INSTALL_STARTED", "Installation of v" + toVersion + " initiated.", false);
    }

    public void completeInstallation() {
        assertState(UpdateState.INSTALL_STARTED);
        this.currentState = UpdateState.INSTALL_COMPLETED;
        addToTimeline("INSTALL_COMPLETED", "✅ Device successfully updated to v" + toVersion, false);
    }

    /**
     * Mark the workflow as failed at the current stage.
     * Supports retry up to MAX_RETRIES times.
     */
    public void markFailed(String reason) {
        this.failedAtStage = currentState.name();
        this.failureReason = reason;
        this.currentState = UpdateState.FAILED;
        addToTimeline("FAILED",
                "❌ Failed at stage [" + failedAtStage + "]. Reason: " + reason, true);
    }

    /**
     * Retry from the failed stage (if retries remaining).
     */
    public boolean retry() {
        if (currentState != UpdateState.FAILED) {
            System.out.println("Workflow is not in FAILED state. Cannot retry.");
            return false;
        }
        if (retryCount >= MAX_RETRIES) {
            addToTimeline("RETRY_EXHAUSTED",
                    "Max retries (" + MAX_RETRIES + ") reached. Manual intervention required.", true);
            return false;
        }

        retryCount++;
        // Re-enter the state before the failure
        // We re-enter NOTIFIED so the device can restart from download
        this.currentState = UpdateState.NOTIFIED;
        this.failureReason = null;
        this.failedAtStage = null;

        addToTimeline("RETRY_ATTEMPT",
                "Retry attempt #" + retryCount + " of " + MAX_RETRIES, false);
        return true;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void assertState(UpdateState expectedState) {
        if (this.currentState != expectedState) {
            throw new IllegalStateException(
                    "Invalid transition: Expected state " + expectedState +
                    " but current state is " + currentState);
        }
    }

    private void addToTimeline(String action, String details, boolean isFailure) {
        AuditLog log = new AuditLog(
                UUID.randomUUID().toString().substring(0, 6),
                deviceImei,
                scheduledByAdminId,
                action,
                details,
                isFailure
        );
        timeline.add(log);
    }

    /** Print the full visual timeline for this device's update */
    public void printTimeline() {
        System.out.println("\n========= UPDATE TIMELINE | Workflow: " + workflowId + " =========");
        System.out.printf("  Device: %s | %s → %s%n%n", deviceImei, fromVersion, toVersion);
        for (AuditLog log : timeline) {
            System.out.println("  " + log.toString());
        }
        System.out.println("  Current State: " + currentState);
        System.out.println("=============================================================\n");
    }

    // ─── Getters ──────────────────────────────────────────────────────────────
    public String getWorkflowId() { return workflowId; }
    public String getDeviceImei() { return deviceImei; }
    public String getFromVersion() { return fromVersion; }
    public String getToVersion() { return toVersion; }
    public UpdateState getCurrentState() { return currentState; }
    public String getFailureReason() { return failureReason; }
    public String getFailedAtStage() { return failedAtStage; }
    public int getRetryCount() { return retryCount; }
    public List<AuditLog> getTimeline() { return Collections.unmodifiableList(timeline); }
}
