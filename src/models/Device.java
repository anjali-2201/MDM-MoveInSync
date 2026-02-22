package models;

import java.time.LocalDateTime;

/**
 * Represents a mobile device registered in the MDM system.
 * Each device is uniquely identified by its IMEI number.
 */
public class Device {

    // ─── Fields ───────────────────────────────────────────────────────────────
    private String imei;           // Primary unique identifier
    private String appVersion;     // Current installed app version
    private String deviceOS;       // e.g., "Android 13", "iOS 16"
    private String deviceModel;    // e.g., "Samsung Galaxy S23"
    private LocalDateTime lastOpenTime; // Last time the app was opened
    private String region;         // e.g., "Bangalore", "Chennai"
    private String clientTag;      // Client-specific customization tag
    private DeviceStatus status;   // ACTIVE or INACTIVE

    // ─── Enum ─────────────────────────────────────────────────────────────────
    public enum DeviceStatus {
        ACTIVE, INACTIVE
    }

    // ─── Constructor ──────────────────────────────────────────────────────────
    public Device(String imei, String appVersion, String deviceOS,
                  String deviceModel, String region, String clientTag) {
        this.imei = imei;
        this.appVersion = appVersion;
        this.deviceOS = deviceOS;
        this.deviceModel = deviceModel;
        this.region = region;
        this.clientTag = clientTag;
        this.lastOpenTime = LocalDateTime.now();
        this.status = DeviceStatus.ACTIVE;
    }

    // ─── Heartbeat (called every time app is opened) ──────────────────────────
    public void recordHeartbeat() {
        this.lastOpenTime = LocalDateTime.now();
        this.status = DeviceStatus.ACTIVE;
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────
    public String getImei() { return imei; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public String getDeviceOS() { return deviceOS; }

    public String getDeviceModel() { return deviceModel; }

    public LocalDateTime getLastOpenTime() { return lastOpenTime; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getClientTag() { return clientTag; }

    public DeviceStatus getStatus() { return status; }
    public void setStatus(DeviceStatus status) { this.status = status; }

    @Override
    public String toString() {
        return String.format("Device[IMEI=%s, Version=%s, OS=%s, Region=%s, Status=%s, LastOpen=%s]",
                imei, appVersion, deviceOS, region, status, lastOpenTime);
    }
}
