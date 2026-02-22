package main;

import exception.DowngradeNotAllowedException;
import exception.InvalidUpgradePathException;
import exception.UnauthorizedActionException;
import models.Admin;
import models.AppVersion;
import models.Device;
import repository.DeviceRegistry;
import repository.VersionRepository;
import service.AuditService;
import service.DashboardService;
import service.UpdateScheduler;
import workflow.UpdateWorkflow;

import java.time.LocalDate;
import java.util.*;

/**
 * ═══════════════════════════════════════════════════════════════
 *   MDM SYSTEM FOR MOVEINSYNC — INTERACTIVE CONSOLE PLATFORM
 *   Full menu-driven admin console. Everything runs step by step.
 * ═══════════════════════════════════════════════════════════════
 */
public class Main {

    static DeviceRegistry deviceRegistry   = new DeviceRegistry();
    static VersionRepository versionRepo   = new VersionRepository();
    static AuditService auditService       = new AuditService();
    static UpdateScheduler updateScheduler = new UpdateScheduler(deviceRegistry, versionRepo, auditService);
    static DashboardService dashboard      = new DashboardService(deviceRegistry, versionRepo, updateScheduler, auditService);
    static Scanner scanner = new Scanner(System.in);
    static Admin currentAdmin = null;

    static List<Admin> adminStore = Arrays.asList(
            new Admin("ADMIN-001", "Rajesh Kumar", "rajesh@moveinsync.com", Admin.Role.SUPER_ADMIN),
            new Admin("ADMIN-002", "Priya Sharma", "priya@moveinsync.com",  Admin.Role.RELEASE_MANAGER),
            new Admin("ADMIN-003", "Amit Verma",   "amit@moveinsync.com",   Admin.Role.VIEWER)
    );

    public static void main(String[] args) {
        printBanner();
        preloadSampleData();
        loginScreen();

        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1"  -> moduleDeviceManagement();
                case "2"  -> moduleVersionRepository();
                case "3"  -> moduleScheduleUpdate();
                case "4"  -> moduleWorkflowSimulator();
                case "5"  -> moduleAuditTrail();
                case "6"  -> moduleDashboard();
                case "7"  -> moduleAdminManagement();
                case "8"  -> moduleUpgradePathFinder();
                case "9"  -> switchAdmin();
                case "0"  -> { running = false; printGoodbye(); }
                default   -> printError("Invalid option. Please enter 0-9.");
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // LOGIN
    // ════════════════════════════════════════════════════════════════════════
    static void loginScreen() {
        printSectionHeader("ADMIN LOGIN");
        System.out.println("  Available Accounts:");
        System.out.println("  [ADMIN-001] Rajesh Kumar    -> SUPER_ADMIN");
        System.out.println("  [ADMIN-002] Priya Sharma    -> RELEASE_MANAGER");
        System.out.println("  [ADMIN-003] Amit Verma      -> VIEWER");
        System.out.println();
        while (currentAdmin == null) {
            System.out.print("  Enter Admin ID: ");
            String id = scanner.nextLine().trim().toUpperCase();
            currentAdmin = findAdmin(id);
            if (currentAdmin == null) printError("Not found. Try ADMIN-001, ADMIN-002, or ADMIN-003.");
        }
        printSuccess("Welcome, " + currentAdmin.getName() + "! Role: " + currentAdmin.getRole());
        pause();
    }

    // ════════════════════════════════════════════════════════════════════════
    // MODULE 1: DEVICE MANAGEMENT
    // ════════════════════════════════════════════════════════════════════════
    static void moduleDeviceManagement() {
        boolean back = false;
        while (!back) {
            printSectionHeader("MODULE 1: DEVICE MANAGEMENT");
            System.out.println("  [1] Register a New Device");
            System.out.println("  [2] View All Devices");
            System.out.println("  [3] Search by Region");
            System.out.println("  [4] Search by App Version");
            System.out.println("  [5] Search by Region + Version");
            System.out.println("  [6] Simulate Heartbeat");
            System.out.println("  [7] View Inactive Devices");
            System.out.println("  [0] Back");
            System.out.print("\n  Your choice: ");
            String c = scanner.nextLine().trim();
            switch (c) {
                case "1" -> registerDevice();
                case "2" -> viewAllDevices();
                case "3" -> { System.out.print("\n  Region: "); printDeviceList(deviceRegistry.getDevicesByRegion(scanner.nextLine().trim())); pause(); }
                case "4" -> { System.out.print("\n  Version: "); printDeviceList(deviceRegistry.getDevicesByVersion(scanner.nextLine().trim())); pause(); }
                case "5" -> searchByRegionAndVersion();
                case "6" -> simulateHeartbeat();
                case "7" -> { System.out.print("\n  Inactive threshold (days): "); int d = safeInt(30); printDeviceList(deviceRegistry.getInactiveDevices(d)); pause(); }
                case "0" -> back = true;
                default  -> printError("Invalid option.");
            }
        }
    }

    static void registerDevice() {
        printSectionHeader("REGISTER NEW DEVICE");
        System.out.print("  IMEI Number     : "); String imei    = scanner.nextLine().trim();
        System.out.print("  App Version     : "); String version = scanner.nextLine().trim();
        System.out.print("  Device OS       : "); String os      = scanner.nextLine().trim();
        System.out.print("  Device Model    : "); String model   = scanner.nextLine().trim();
        System.out.print("  Region/City     : "); String region  = scanner.nextLine().trim();
        System.out.print("  Client Tag      : "); String client  = scanner.nextLine().trim();
        try {
            Device device = new Device(imei, version, os, model, region, client);
            deviceRegistry.registerDevice(device);
            auditService.log(imei, currentAdmin.getAdminId(), "DEVICE_REGISTERED", "By " + currentAdmin.getName(), false);
            printSuccess("Device registered!");
        } catch (Exception e) { printError(e.getMessage()); }
        pause();
    }

    static void viewAllDevices() {
        printSectionHeader("ALL REGISTERED DEVICES");
        List<Device> all = deviceRegistry.getAllDevices();
        if (all.isEmpty()) { System.out.println("  No devices registered."); }
        else {
            System.out.printf("  %-22s %-10s %-14s %-18s %-10s%n", "IMEI", "Version", "Region", "Model", "Status");
            printDivider();
            for (Device d : all)
                System.out.printf("  %-22s %-10s %-14s %-18s %-10s%n",
                        d.getImei(), d.getAppVersion(), d.getRegion(), d.getDeviceModel(), d.getStatus());
            System.out.println("\n  Total: " + all.size() + " device(s)");
        }
        pause();
    }

    static void searchByRegionAndVersion() {
        System.out.print("\n  Region  : "); String region  = scanner.nextLine().trim();
        System.out.print("  Version : "); String version = scanner.nextLine().trim();
        printDeviceList(deviceRegistry.getDevicesByRegionAndVersion(region, version));
        pause();
    }

    static void simulateHeartbeat() {
        System.out.print("\n  IMEI Number         : "); String imei    = scanner.nextLine().trim();
        System.out.print("  Current App Version : "); String version = scanner.nextLine().trim();
        try { deviceRegistry.heartbeat(imei, version); printSuccess("Heartbeat recorded!"); }
        catch (Exception e) { printError(e.getMessage()); }
        pause();
    }

    static void printDeviceList(List<Device> list) {
        System.out.println();
        if (list.isEmpty()) { System.out.println("  No matching devices found."); return; }
        list.forEach(d -> System.out.println("  " + d));
        System.out.println("  Found: " + list.size() + " device(s)");
    }

    // ════════════════════════════════════════════════════════════════════════
    // MODULE 2: VERSION REPOSITORY
    // ════════════════════════════════════════════════════════════════════════
    static void moduleVersionRepository() {
        boolean back = false;
        while (!back) {
            printSectionHeader("MODULE 2: VERSION REPOSITORY");
            System.out.println("  [1] View All Versions");
            System.out.println("  [2] Publish New Version  [SUPER_ADMIN only]");
            System.out.println("  [3] View Compatibility Matrix");
            System.out.println("  [4] Add Upgrade Path     [SUPER_ADMIN only]");
            System.out.println("  [0] Back");
            System.out.print("\n  Your choice: ");
            String c = scanner.nextLine().trim();
            switch (c) {
                case "1" -> { viewAllVersions(); pause(); }
                case "2" -> publishVersion();
                case "3" -> { versionRepo.printCompatibilityMatrix(); pause(); }
                case "4" -> addUpgradePath();
                case "0" -> back = true;
                default  -> printError("Invalid option.");
            }
        }
    }

    static void viewAllVersions() {
        printSectionHeader("ALL PUBLISHED VERSIONS");
        List<AppVersion> versions = versionRepo.getAllVersions();
        if (versions.isEmpty()) { System.out.println("  No versions published."); return; }
        System.out.printf("  %-10s %-22s %-13s %-18s %-10s%n", "Code", "Name", "Released", "Tag", "Mandatory");
        printDivider();
        for (AppVersion v : versions)
            System.out.printf("  %-10s %-22s %-13s %-18s %-10s%n",
                    v.getVersionCode(), v.getVersionName(), v.getReleaseDate(),
                    v.getCustomizationTag(), v.isMandatory() ? "YES ***" : "No");
    }

    static void publishVersion() {
        if (!currentAdmin.canPublishVersions()) { printError("Only SUPER_ADMIN can publish versions."); pause(); return; }
        printSectionHeader("PUBLISH NEW VERSION");
        System.out.print("  Version Code      : "); String code      = scanner.nextLine().trim();
        System.out.print("  Version Name      : "); String name      = scanner.nextLine().trim();
        System.out.print("  Min OS            : "); String minOS     = scanner.nextLine().trim();
        System.out.print("  Max OS            : "); String maxOS     = scanner.nextLine().trim();
        System.out.print("  Customization Tag : "); String tag       = scanner.nextLine().trim();
        System.out.print("  Mandatory? (y/n)  : "); boolean mandatory = scanner.nextLine().trim().equalsIgnoreCase("y");
        System.out.print("  Release Notes     : "); String notes     = scanner.nextLine().trim();
        try {
            versionRepo.publishVersion(new AppVersion(code, name, LocalDate.now(), minOS, maxOS, tag, mandatory, notes));
            auditService.log("ALL", currentAdmin.getAdminId(), "VERSION_PUBLISHED", "v" + code, false);
            printSuccess("Version v" + code + " published!");
        } catch (Exception e) { printError(e.getMessage()); }
        pause();
    }

    static void addUpgradePath() {
        if (!currentAdmin.canPublishVersions()) { printError("Only SUPER_ADMIN can modify the matrix."); pause(); return; }
        System.out.print("\n  From Version : "); String from = scanner.nextLine().trim();
        System.out.print("  To Version   : "); String to   = scanner.nextLine().trim();
        try { versionRepo.addAllowedUpgradePath(from, to); printSuccess("Path added: v" + from + " -> v" + to); }
        catch (Exception e) { printError(e.getMessage()); }
        pause();
    }

    // ════════════════════════════════════════════════════════════════════════
    // MODULE 3: SCHEDULE UPDATE
    // ════════════════════════════════════════════════════════════════════════
    static void moduleScheduleUpdate() {
        if (!currentAdmin.canScheduleUpdates()) { printError("Your role cannot schedule updates."); pause(); return; }
        boolean back = false;
        while (!back) {
            printSectionHeader("MODULE 3: SCHEDULE UPDATE");
            System.out.println("  [1] Schedule by Region");
            System.out.println("  [2] Schedule by Version (All Regions)");
            System.out.println("  [3] Schedule by Client Tag");
            System.out.println("  [0] Back");
            System.out.print("\n  Your choice: ");
            String c = scanner.nextLine().trim();
            switch (c) {
                case "1" -> doSchedule("region");
                case "2" -> doSchedule("version");
                case "3" -> doSchedule("client");
                case "0" -> back = true;
                default  -> printError("Invalid option.");
            }
        }
    }

    static void doSchedule(String type) {
        printSectionHeader("SCHEDULE UPDATE");
        String tag = null;
        if (type.equals("region"))  { System.out.print("  Region       : "); tag = scanner.nextLine().trim(); }
        if (type.equals("client"))  { System.out.print("  Client Tag   : "); tag = scanner.nextLine().trim(); }
        System.out.print("  From Version : "); String from = scanner.nextLine().trim();
        System.out.print("  To Version   : "); String to   = scanner.nextLine().trim();
        System.out.print("  Rollout (1=IMMEDIATE, 2=PHASED): ");
        UpdateScheduler.RolloutType rt = scanner.nextLine().trim().equals("2")
                ? UpdateScheduler.RolloutType.PHASED : UpdateScheduler.RolloutType.IMMEDIATE;
        try {
            List<UpdateWorkflow> workflows;
            if (type.equals("region"))       workflows = updateScheduler.scheduleByRegion(currentAdmin, tag, from, to, rt);
            else if (type.equals("client"))  workflows = updateScheduler.scheduleByClientTag(currentAdmin, tag, from, to, rt);
            else                             workflows = updateScheduler.scheduleByVersion(currentAdmin, from, to, rt);
            printSuccess("Created " + workflows.size() + " workflow(s). Go to Module 4 to execute them.");
        } catch (DowngradeNotAllowedException | InvalidUpgradePathException | UnauthorizedActionException e) {
            printError(e.getMessage());
        } catch (Exception e) { printError("Error: " + e.getMessage()); }
        pause();
    }

    // ════════════════════════════════════════════════════════════════════════
    // MODULE 4: WORKFLOW SIMULATOR
    // ════════════════════════════════════════════════════════════════════════
    static void moduleWorkflowSimulator() {
        boolean back = false;
        while (!back) {
            printSectionHeader("MODULE 4: WORKFLOW SIMULATOR");
            List<UpdateWorkflow> all = updateScheduler.getActiveWorkflows();
            if (all.isEmpty()) { System.out.println("  No workflows yet. Use Module 3 to schedule updates first."); pause(); return; }

            System.out.printf("  %-4s %-22s %-8s %-8s %-22s%n", "#", "Device IMEI", "From", "To", "Current State");
            printDivider();
            for (int i = 0; i < all.size(); i++) {
                UpdateWorkflow w = all.get(i);
                System.out.printf("  %-4d %-22s %-8s %-8s %-22s%n",
                        i + 1, w.getDeviceImei(), w.getFromVersion(), w.getToVersion(), w.getCurrentState());
            }

            System.out.println("\n  [1] Advance Workflow (next step)");
            System.out.println("  [2] Mark as FAILED");
            System.out.println("  [3] Retry Failed Workflow");
            System.out.println("  [4] View Full Timeline");
            System.out.println("  [5] AUTO-COMPLETE (run all steps instantly)");
            System.out.println("  [0] Back");
            System.out.print("\n  Your choice: ");
            String c = scanner.nextLine().trim();
            switch (c) {
                case "1" -> advanceWorkflow(all);
                case "2" -> failWorkflow(all);
                case "3" -> retryWorkflow(all);
                case "4" -> { System.out.print("\n  Workflow #: "); int idx = getIndex(all.size()); if (idx >= 0) { all.get(idx).printTimeline(); pause(); } }
                case "5" -> autoComplete(all);
                case "0" -> back = true;
                default  -> printError("Invalid option.");
            }
        }
    }

    static void advanceWorkflow(List<UpdateWorkflow> all) {
        System.out.print("\n  Workflow #: ");
        int idx = getIndex(all.size()); if (idx < 0) return;
        UpdateWorkflow w = all.get(idx);
        try {
            switch (w.getCurrentState()) {
                case SCHEDULED          -> w.notifyDevice();
                case NOTIFIED           -> w.startDownload();
                case DOWNLOAD_STARTED   -> w.completeDownload();
                case DOWNLOAD_COMPLETED -> w.startInstallation();
                case INSTALL_STARTED    -> {
                    w.completeInstallation();
                    deviceRegistry.getDevice(w.getDeviceImei()).setAppVersion(w.getToVersion());
                    auditService.log(w.getDeviceImei(), currentAdmin.getAdminId(), "VERSION_UPDATED", "Now on v" + w.getToVersion(), false);
                }
                case INSTALL_COMPLETED -> printError("Already completed.");
                case FAILED            -> printError("Workflow failed. Use Retry option.");
            }
            printSuccess("State is now: " + w.getCurrentState());
        } catch (Exception e) { printError(e.getMessage()); }
        pause();
    }

    static void failWorkflow(List<UpdateWorkflow> all) {
        System.out.print("\n  Workflow #: "); int idx = getIndex(all.size()); if (idx < 0) return;
        System.out.print("  Reason for failure: "); String reason = scanner.nextLine().trim();
        UpdateWorkflow w = all.get(idx);
        try {
            w.markFailed(reason);
            auditService.log(w.getDeviceImei(), currentAdmin.getAdminId(), "WORKFLOW_FAILED", reason, true);
            printSuccess("Workflow marked FAILED at: " + w.getFailedAtStage());
        } catch (Exception e) { printError(e.getMessage()); }
        pause();
    }

    static void retryWorkflow(List<UpdateWorkflow> all) {
        System.out.print("\n  Workflow #: "); int idx = getIndex(all.size()); if (idx < 0) return;
        UpdateWorkflow w = all.get(idx);
        if (w.retry()) {
            auditService.log(w.getDeviceImei(), currentAdmin.getAdminId(), "WORKFLOW_RETRIED", "Attempt #" + w.getRetryCount(), false);
            printSuccess("Retry initiated. State: " + w.getCurrentState());
        } else { printError("Cannot retry. Max retries reached or not in FAILED state."); }
        pause();
    }

    static void autoComplete(List<UpdateWorkflow> all) {
        System.out.print("\n  Workflow #: "); int idx = getIndex(all.size()); if (idx < 0) return;
        UpdateWorkflow w = all.get(idx);
        try {
            if (w.getCurrentState() == UpdateWorkflow.UpdateState.SCHEDULED)          w.notifyDevice();
            if (w.getCurrentState() == UpdateWorkflow.UpdateState.NOTIFIED)           w.startDownload();
            if (w.getCurrentState() == UpdateWorkflow.UpdateState.DOWNLOAD_STARTED)   w.completeDownload();
            if (w.getCurrentState() == UpdateWorkflow.UpdateState.DOWNLOAD_COMPLETED) w.startInstallation();
            if (w.getCurrentState() == UpdateWorkflow.UpdateState.INSTALL_STARTED) {
                w.completeInstallation();
                deviceRegistry.getDevice(w.getDeviceImei()).setAppVersion(w.getToVersion());
                auditService.log(w.getDeviceImei(), currentAdmin.getAdminId(), "VERSION_UPDATED", "Auto-completed to v" + w.getToVersion(), false);
            }
            printSuccess("Device " + w.getDeviceImei() + " is now on v" + w.getToVersion());
            w.printTimeline();
        } catch (Exception e) { printError("Could not complete: " + e.getMessage()); }
        pause();
    }

    // ════════════════════════════════════════════════════════════════════════
    // MODULE 5: AUDIT TRAIL
    // ════════════════════════════════════════════════════════════════════════
    static void moduleAuditTrail() {
        boolean back = false;
        while (!back) {
            printSectionHeader("MODULE 5: AUDIT TRAIL");
            System.out.println("  [1] View Audit Trail for a Device");
            System.out.println("  [2] View All Failure Logs");
            System.out.println("  [3] View Logs by Admin");
            System.out.println("  [4] View All Logs");
            System.out.println("  [0] Back");
            System.out.print("\n  Your choice: ");
            String c = scanner.nextLine().trim();
            switch (c) {
                case "1" -> { System.out.print("\n  Device IMEI: "); auditService.printDeviceAuditTrail(scanner.nextLine().trim()); pause(); }
                case "2" -> { auditService.printFailureReport(); pause(); }
                case "3" -> {
                    System.out.print("\n  Admin ID: "); String aid = scanner.nextLine().trim().toUpperCase();
                    printSectionHeader("LOGS BY ADMIN: " + aid);
                    auditService.getLogsByAdmin(aid).forEach(l -> System.out.println("  " + l));
                    pause();
                }
                case "4" -> {
                    printSectionHeader("ALL AUDIT LOGS");
                    auditService.getAllLogs().forEach(l -> System.out.println("  " + l));
                    pause();
                }
                case "0" -> back = true;
                default  -> printError("Invalid option.");
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // MODULE 6: DASHBOARD
    // ════════════════════════════════════════════════════════════════════════
    static void moduleDashboard() {
        boolean back = false;
        while (!back) {
            printSectionHeader("MODULE 6: REAL-TIME DASHBOARD");
            System.out.println("  [1] Full Dashboard");
            System.out.println("  [2] Device Overview");
            System.out.println("  [3] Version Heatmap");
            System.out.println("  [4] Region Breakdown");
            System.out.println("  [5] Rollout Progress");
            System.out.println("  [0] Back");
            System.out.print("\n  Your choice: ");
            String c = scanner.nextLine().trim();
            switch (c) {
                case "1" -> { dashboard.printFullDashboard(); pause(); }
                case "2" -> { dashboard.printDeviceOverview(); pause(); }
                case "3" -> { dashboard.printVersionHeatmap(); pause(); }
                case "4" -> { dashboard.printRegionBreakdown(); pause(); }
                case "5" -> { dashboard.printRolloutProgress(); pause(); }
                case "0" -> back = true;
                default  -> printError("Invalid option.");
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // MODULE 7: ADMIN MANAGEMENT
    // ════════════════════════════════════════════════════════════════════════
    static void moduleAdminManagement() {
        printSectionHeader("MODULE 7: ADMIN MANAGEMENT");
        System.out.printf("  %-12s %-18s %-22s %-10s%n", "ID", "Name", "Role", "Active");
        printDivider();
        for (Admin a : adminStore)
            System.out.printf("  %-12s %-18s %-22s %-10s%n", a.getAdminId(), a.getName(), a.getRole(), a.isActive() ? "YES" : "NO");
        System.out.println("\n  Currently logged in: " + currentAdmin.getName() + " [" + currentAdmin.getRole() + "]");
        pause();
    }

    // ════════════════════════════════════════════════════════════════════════
    // MODULE 8: UPGRADE PATH FINDER
    // ════════════════════════════════════════════════════════════════════════
    static void moduleUpgradePathFinder() {
        printSectionHeader("MODULE 8: UPGRADE PATH FINDER");
        System.out.print("  From Version : "); String from = scanner.nextLine().trim();
        System.out.print("  To Version   : "); String to   = scanner.nextLine().trim();
        List<String> path = versionRepo.findUpgradePath(from, to);
        if (path == null) {
            printError("No valid path found from v" + from + " to v" + to + ". Check compatibility matrix.");
        } else if (path.size() == 2) {
            printSuccess("Direct upgrade allowed: v" + from + " -> v" + to);
        } else {
            printSuccess("Requires " + (path.size() - 1) + " step(s):");
            StringBuilder sb = new StringBuilder();
            for (String v : path) { if (sb.length() > 0) sb.append(" -> "); sb.append("v").append(v); }
            System.out.println("  Path: " + sb);
        }
        pause();
    }

    static void switchAdmin() { currentAdmin = null; loginScreen(); }

    // ════════════════════════════════════════════════════════════════════════
    // SAMPLE DATA
    // ════════════════════════════════════════════════════════════════════════
    static void preloadSampleData() {
        // Suppress output during preload
        versionRepo.publishVersion(new AppVersion("3.8", "Legacy Build",       LocalDate.of(2023,1,10),  "Android 10","Android 12","Global",           false,"Initial build"));
        versionRepo.publishVersion(new AppVersion("4.0", "Spring Release",     LocalDate.of(2023,6,1),   "Android 10","Android 13","Global",           false,"Major UI overhaul"));
        versionRepo.publishVersion(new AppVersion("4.1", "Bugfix Release",     LocalDate.of(2023,9,15),  "Android 10","Android 14","Global",           false,"Bug fixes"));
        versionRepo.publishVersion(new AppVersion("4.2", "Chennai Pilot",      LocalDate.of(2024,1,1),   "Android 11","Android 14","Chennai-Specific", false,"Region-specific build"));
        versionRepo.publishVersion(new AppVersion("4.3", "Security Patch",     LocalDate.of(2024,3,1),   "Android 11","Android 15","Global",           true, "Critical security patch"));
        versionRepo.addAllowedUpgradePath("3.8","4.0"); versionRepo.addAllowedUpgradePath("4.0","4.1");
        versionRepo.addAllowedUpgradePath("4.0","4.3"); versionRepo.addAllowedUpgradePath("4.1","4.2");
        versionRepo.addAllowedUpgradePath("4.1","4.3"); versionRepo.addAllowedUpgradePath("4.2","4.3");
        deviceRegistry.registerDevice(new Device("IMEI-BLR-001","4.1","Android 13","Pixel 7",       "Bangalore","Global"));
        deviceRegistry.registerDevice(new Device("IMEI-BLR-002","4.1","Android 12","OnePlus 10",    "Bangalore","Global"));
        deviceRegistry.registerDevice(new Device("IMEI-BLR-003","4.2","Android 14","Samsung S23",   "Bangalore","Global"));
        deviceRegistry.registerDevice(new Device("IMEI-CHN-001","4.1","Android 13","Redmi Note 12", "Chennai",  "Chennai-Specific"));
        deviceRegistry.registerDevice(new Device("IMEI-CHN-002","4.2","Android 14","Vivo V27",      "Chennai",  "Chennai-Specific"));
        deviceRegistry.registerDevice(new Device("IMEI-HYD-001","3.8","Android 11","Realme GT",     "Hyderabad","Global"));
        deviceRegistry.registerDevice(new Device("IMEI-HYD-002","4.0","Android 12","Moto G84",      "Hyderabad","Global"));
        deviceRegistry.registerDevice(new Device("IMEI-MUM-001","4.1","Android 13","iPhone 14",     "Mumbai",   "Global"));
        System.out.println("  [System] Sample data preloaded. 8 devices | 5 versions | 6 upgrade paths ready.");
    }

    // ════════════════════════════════════════════════════════════════════════
    // UI HELPERS
    // ════════════════════════════════════════════════════════════════════════
    static void printBanner() {
        System.out.println();
        System.out.println("*============================================================*");
        System.out.println("|                                                            |");
        System.out.println("|     MOVEINSYNC - MOBILE DEVICE MANAGEMENT SYSTEM          |");
        System.out.println("|              Interactive Admin Console v1.0               |");
        System.out.println("|                                                            |");
        System.out.println("*============================================================*");
        System.out.println();
    }

    static void printMainMenu() {
        System.out.println();
        System.out.println("*============================================================*");
        System.out.println("|                      MAIN MENU                            |");
        System.out.println("|============================================================|");
        System.out.printf ("|  Logged in: %-20s  Role: %-16s|%n", currentAdmin.getName(), currentAdmin.getRole());
        System.out.println("|============================================================|");
        System.out.println("|  [1]  Device Management                                   |");
        System.out.println("|  [2]  Version Repository                                  |");
        System.out.println("|  [3]  Schedule Update                                     |");
        System.out.println("|  [4]  Workflow Simulator                                  |");
        System.out.println("|  [5]  Audit Trail                                         |");
        System.out.println("|  [6]  Real-Time Dashboard                                 |");
        System.out.println("|  [7]  Admin Management                                    |");
        System.out.println("|  [8]  Upgrade Path Finder                                 |");
        System.out.println("|  [9]  Switch Admin Account                                |");
        System.out.println("|  [0]  Exit                                                |");
        System.out.println("*============================================================*");
        System.out.print("  Your choice: ");
    }

    static void printSectionHeader(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
        System.out.println();
    }

    static void printDivider() { System.out.println("  " + "-".repeat(70)); }
    static void printSuccess(String msg) { System.out.println("\n  >> SUCCESS: " + msg); }
    static void printError(String msg)   { System.out.println("\n  >> ERROR: " + msg); }
    static void pause() { System.out.print("\n  Press ENTER to continue..."); scanner.nextLine(); }

    static Admin findAdmin(String id) {
        for (Admin a : adminStore) if (a.getAdminId().equals(id)) return a;
        return null;
    }

    static int getIndex(int max) {
        try {
            int idx = Integer.parseInt(scanner.nextLine().trim()) - 1;
            if (idx < 0 || idx >= max) { printError("Invalid selection."); return -1; }
            return idx;
        } catch (Exception e) { printError("Enter a valid number."); return -1; }
    }

    static int safeInt(int def) {
        try { return Integer.parseInt(scanner.nextLine().trim()); }
        catch (Exception e) { return def; }
    }

    static void printGoodbye() {
        System.out.println();
        System.out.println("*============================================================*");
        System.out.println("|    Thank you for using Moveinsync MDM System. Goodbye!    |");
        System.out.println("*============================================================*");
    }
}
