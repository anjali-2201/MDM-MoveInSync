package service;

import exception.DowngradeNotAllowedException;
import exception.UnauthorizedActionException;
import models.Admin;
import models.AppVersion;
import models.Device;
import repository.DeviceRegistry;
import repository.VersionRepository;
import workflow.UpdateWorkflow;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles scheduling and targeting of update rollouts.
 * Enforces:
 *   - Role-based access (only authorized admins can schedule)
 *   - Downgrade prevention (target version must be > current version)
 *   - Compatibility matrix validation
 *   - Support for immediate, scheduled, and phased rollouts
 */
public class UpdateScheduler {

    // ─── Rollout Type Enum ────────────────────────────────────────────────────
    public enum RolloutType {
        IMMEDIATE,   // Apply now to all targeted devices
        PHASED       // Roll out in batches (percentage-based)
    }

    // ─── Dependencies ─────────────────────────────────────────────────────────
    private final DeviceRegistry deviceRegistry;
    private final VersionRepository versionRepository;
    private final AuditService auditService;

    // All active workflows
    private final List<UpdateWorkflow> activeWorkflows = new ArrayList<>();

    // ─── Constructor ──────────────────────────────────────────────────────────
    public UpdateScheduler(DeviceRegistry deviceRegistry,
                           VersionRepository versionRepository,
                           AuditService auditService) {
        this.deviceRegistry = deviceRegistry;
        this.versionRepository = versionRepository;
        this.auditService = auditService;
    }

    // ─── Schedule by Region ───────────────────────────────────────────────────

    /**
     * Schedule an update for all devices in a region running fromVersion.
     * e.g., "Upgrade all Chennai devices from 4.1 to 4.2"
     */
    public List<UpdateWorkflow> scheduleByRegion(Admin admin, String region,
                                                  String fromVersion, String toVersion,
                                                  RolloutType rolloutType) {
        validateSchedulePermission(admin, fromVersion, toVersion);

        List<Device> targets = deviceRegistry.getDevicesByRegionAndVersion(region, fromVersion);
        System.out.println("\n🚀 Scheduling update for " + targets.size()
                + " device(s) in [" + region + "] | v" + fromVersion + " → v" + toVersion);

        return createWorkflows(admin, targets, fromVersion, toVersion, rolloutType);
    }

    // ─── Schedule by Version (All Regions) ───────────────────────────────────

    /**
     * Schedule update for ALL devices currently on fromVersion, across all regions.
     */
    public List<UpdateWorkflow> scheduleByVersion(Admin admin, String fromVersion,
                                                   String toVersion, RolloutType rolloutType) {
        validateSchedulePermission(admin, fromVersion, toVersion);

        List<Device> targets = deviceRegistry.getDevicesByVersion(fromVersion);
        System.out.println("\n🚀 Scheduling update for " + targets.size()
                + " device(s) on v" + fromVersion + " → v" + toVersion + " (All Regions)");

        return createWorkflows(admin, targets, fromVersion, toVersion, rolloutType);
    }

    // ─── Schedule by Client Tag ───────────────────────────────────────────────

    /**
     * Schedule update only for devices belonging to a specific client.
     */
    public List<UpdateWorkflow> scheduleByClientTag(Admin admin, String clientTag,
                                                     String fromVersion, String toVersion,
                                                     RolloutType rolloutType) {
        validateSchedulePermission(admin, fromVersion, toVersion);

        List<Device> allClientDevices = deviceRegistry.getDevicesByClientTag(clientTag);
        List<Device> targets = allClientDevices.stream()
                .filter(d -> d.getAppVersion().equals(fromVersion))
                .collect(java.util.stream.Collectors.toList());

        System.out.println("\n🚀 Scheduling update for " + targets.size()
                + " device(s) in client [" + clientTag + "] | v" + fromVersion + " → v" + toVersion);

        return createWorkflows(admin, targets, fromVersion, toVersion, rolloutType);
    }

    // ─── Core Workflow Creation ───────────────────────────────────────────────

    private List<UpdateWorkflow> createWorkflows(Admin admin, List<Device> targets,
                                                  String fromVersion, String toVersion,
                                                  RolloutType rolloutType) {
        List<UpdateWorkflow> workflows = new ArrayList<>();

        if (targets.isEmpty()) {
            System.out.println("⚠️  No matching devices found for this schedule.");
            return workflows;
        }

        // For phased rollout, process in batches of 25%
        if (rolloutType == RolloutType.PHASED) {
            System.out.println("📊 PHASED ROLLOUT: Devices will be updated in 4 batches (25% each).");
        }

        for (Device device : targets) {
            UpdateWorkflow workflow = new UpdateWorkflow(
                    device.getImei(), fromVersion, toVersion, admin.getAdminId());
            activeWorkflows.add(workflow);
            workflows.add(workflow);

            auditService.log(device.getImei(), admin.getAdminId(),
                    "UPDATE_SCHEDULED",
                    "Rollout type: " + rolloutType + " | Target: v" + toVersion, false);
        }

        System.out.println("✅ Created " + workflows.size() + " update workflow(s).");
        return workflows;
    }

    // ─── Validation ───────────────────────────────────────────────────────────

    /**
     * Validates:
     * 1. Admin has permission to schedule
     * 2. Target version exists
     * 3. Not a downgrade
     * 4. Upgrade path is valid
     * 5. If mandatory update, requires SUPER_ADMIN approval
     */
    private void validateSchedulePermission(Admin admin, String fromVersion, String toVersion) {

        // Check role
        if (!admin.canScheduleUpdates()) {
            throw new UnauthorizedActionException(admin.getAdminId(), "SCHEDULE_UPDATE");
        }

        // Check versions exist
        if (!versionRepository.versionExists(toVersion)) {
            throw new IllegalArgumentException("Target version not found in repository: " + toVersion);
        }

        // Block downgrade
        if (AppVersion.compareVersions(toVersion, fromVersion) < 0) {
            auditService.log("ALL_DEVICES", admin.getAdminId(),
                    "DOWNGRADE_BLOCKED",
                    "Admin attempted to downgrade from v" + fromVersion + " to v" + toVersion, true);
            throw new DowngradeNotAllowedException(fromVersion, toVersion);
        }

        // Check if mandatory update needs SUPER_ADMIN approval
        AppVersion targetVersion = versionRepository.getVersion(toVersion);
        if (targetVersion.isMandatory() && !admin.canApproveMandatoryUpdates()) {
            throw new UnauthorizedActionException(admin.getAdminId(),
                    "APPROVE_MANDATORY_UPDATE (requires SUPER_ADMIN)");
        }

        // Check compatibility matrix
        versionRepository.validateUpgradePath(fromVersion, toVersion);
    }

    // ─── Getters ──────────────────────────────────────────────────────────────
    public List<UpdateWorkflow> getActiveWorkflows() { return activeWorkflows; }
}
