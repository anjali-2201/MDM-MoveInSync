package service;

import models.Device;
import repository.DeviceRegistry;
import repository.VersionRepository;
import workflow.UpdateWorkflow;

import java.util.List;
import java.util.Map;

/**
 * Provides real-time dashboard and monitoring data.
 * Shows version heatmaps, region-wise adoption, success/failure rates.
 */
public class DashboardService {

    private final DeviceRegistry deviceRegistry;
    private final VersionRepository versionRepository;
    private final UpdateScheduler updateScheduler;
    private final AuditService auditService;

    public DashboardService(DeviceRegistry deviceRegistry,
                            VersionRepository versionRepository,
                            UpdateScheduler updateScheduler,
                            AuditService auditService) {
        this.deviceRegistry = deviceRegistry;
        this.versionRepository = versionRepository;
        this.updateScheduler = updateScheduler;
        this.auditService = auditService;
    }

    // ─── Main Dashboard ───────────────────────────────────────────────────────

    public void printFullDashboard() {
        System.out.println("\n╔══════════════════════════════════════════════════════╗");
        System.out.println("║         MDM REAL-TIME MONITORING DASHBOARD           ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");

        printDeviceOverview();
        printVersionHeatmap();
        printRegionBreakdown();
        printRolloutProgress();
        printInactiveDevices(30);
    }

    // ─── Device Overview ──────────────────────────────────────────────────────

    public void printDeviceOverview() {
        int total = deviceRegistry.getTotalDeviceCount();
        System.out.println("\n📱 DEVICE OVERVIEW");
        System.out.println("  Total Registered Devices : " + total);
        System.out.println("  All Versions             : " +
                deviceRegistry.getVersionDistribution().keySet());
    }

    // ─── Version Heatmap ─────────────────────────────────────────────────────

    public void printVersionHeatmap() {
        System.out.println("\n🗺️  VERSION DISTRIBUTION (Heatmap)");
        Map<String, Long> dist = deviceRegistry.getVersionDistribution();
        int total = deviceRegistry.getTotalDeviceCount();

        dist.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .forEach(e -> {
                double pct = total > 0 ? (e.getValue() * 100.0 / total) : 0;
                String bar = "█".repeat((int) (pct / 5));
                System.out.printf("  v%-10s | %-20s | %3d devices (%.1f%%)%n",
                        e.getKey(), bar, e.getValue(), pct);
            });
    }

    // ─── Region Breakdown ─────────────────────────────────────────────────────

    public void printRegionBreakdown() {
        System.out.println("\n🌍 REGION-WISE DEVICE BREAKDOWN");
        Map<String, Long> breakdown = deviceRegistry.getRegionBreakdown();
        breakdown.forEach((region, count) ->
                System.out.printf("  %-15s : %d device(s)%n", region, count));
    }

    // ─── Rollout Progress ─────────────────────────────────────────────────────

    public void printRolloutProgress() {
        List<UpdateWorkflow> workflows = updateScheduler.getActiveWorkflows();
        System.out.println("\n📊 ACTIVE UPDATE ROLLOUT PROGRESS");

        if (workflows.isEmpty()) {
            System.out.println("  No active rollouts.");
            return;
        }

        long completed = workflows.stream()
                .filter(w -> w.getCurrentState() == UpdateWorkflow.UpdateState.INSTALL_COMPLETED)
                .count();
        long failed = workflows.stream()
                .filter(w -> w.getCurrentState() == UpdateWorkflow.UpdateState.FAILED)
                .count();
        long inProgress = workflows.size() - completed - failed;

        double successRate = workflows.size() > 0 ? (completed * 100.0 / workflows.size()) : 0;

        System.out.printf("  Total Workflows  : %d%n", workflows.size());
        System.out.printf("  ✅ Completed     : %d%n", completed);
        System.out.printf("  ❌ Failed        : %d%n", failed);
        System.out.printf("  🔄 In Progress   : %d%n", inProgress);
        System.out.printf("  Success Rate     : %.1f%%%n", successRate);
    }

    // ─── Inactive Devices Alert ───────────────────────────────────────────────

    public void printInactiveDevices(int thresholdDays) {
        List<Device> inactive = deviceRegistry.getInactiveDevices(thresholdDays);
        System.out.println("\n⚠️  INACTIVE DEVICES (>" + thresholdDays + " days)");
        if (inactive.isEmpty()) {
            System.out.println("  All devices are active.");
        } else {
            inactive.forEach(d ->
                    System.out.println("  IMEI: " + d.getImei()
                            + " | Last Open: " + d.getLastOpenTime()
                            + " | Region: " + d.getRegion()));
        }
    }
}
