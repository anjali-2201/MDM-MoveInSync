package service;

import models.AuditLog;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Centralized service for recording and querying audit logs.
 * All logs are immutable once written.
 */
public class AuditService {

    private final List<AuditLog> globalLogs = new ArrayList<>();
    private int logCounter = 1;

    // ─── Writing Logs ─────────────────────────────────────────────────────────

    public AuditLog log(String deviceImei, String adminId, String action,
                        String details, boolean isFailure) {
        String logId = "LOG-" + String.format("%04d", logCounter++);
        AuditLog entry = new AuditLog(logId, deviceImei, adminId, action, details, isFailure);
        globalLogs.add(entry);
        return entry;
    }

    public AuditLog logSystem(String deviceImei, String action, String details, boolean isFailure) {
        return log(deviceImei, "SYSTEM", action, details, isFailure);
    }

    // ─── Querying Logs ────────────────────────────────────────────────────────

    /** Get all logs for a specific device */
    public List<AuditLog> getLogsForDevice(String imei) {
        return globalLogs.stream()
                .filter(l -> l.getDeviceImei().equals(imei))
                .collect(Collectors.toList());
    }

    /** Get all failure logs */
    public List<AuditLog> getFailureLogs() {
        return globalLogs.stream()
                .filter(AuditLog::isFailure)
                .collect(Collectors.toList());
    }

    /** Get all logs by a specific admin */
    public List<AuditLog> getLogsByAdmin(String adminId) {
        return globalLogs.stream()
                .filter(l -> adminId.equals(l.getAdminId()))
                .collect(Collectors.toList());
    }

    /** Get all logs globally */
    public List<AuditLog> getAllLogs() {
        return Collections.unmodifiableList(globalLogs);
    }

    /** Print full audit trail for a device */
    public void printDeviceAuditTrail(String imei) {
        System.out.println("\n========= AUDIT TRAIL | Device: " + imei + " =========");
        List<AuditLog> deviceLogs = getLogsForDevice(imei);
        if (deviceLogs.isEmpty()) {
            System.out.println("  No logs found for this device.");
        } else {
            deviceLogs.forEach(l -> System.out.println("  " + l));
        }
        System.out.println("=====================================================\n");
    }

    /** Print all failure logs */
    public void printFailureReport() {
        System.out.println("\n========= FAILURE REPORT =========");
        List<AuditLog> failures = getFailureLogs();
        if (failures.isEmpty()) {
            System.out.println("  ✅ No failures recorded.");
        } else {
            failures.forEach(l -> System.out.println("  " + l));
        }
        System.out.println("===================================\n");
    }
}
