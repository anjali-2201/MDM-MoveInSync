package repository;

import exception.DeviceNotFoundException;
import models.Device;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Central store for all registered devices.
 * Uses IMEI as the primary key (HashMap for O(1) lookup).
 */
public class DeviceRegistry {

    // IMEI → Device mapping
    private final Map<String, Device> devices = new HashMap<>();

    // ─── Registration ─────────────────────────────────────────────────────────

    /**
     * Register a new device. Throws exception if already registered.
     */
    public void registerDevice(Device device) {
        if (devices.containsKey(device.getImei())) {
            throw new IllegalArgumentException(
                    "Device already registered: IMEI=" + device.getImei());
        }
        devices.put(device.getImei(), device);
        System.out.println("✅ Device registered: " + device.getImei() + " | Region: " + device.getRegion());
    }

    /**
     * Heartbeat API: called every time the app is opened.
     * Updates last open time and validates the device exists.
     */
    public void heartbeat(String imei, String currentVersion) {
        Device device = getDeviceOrThrow(imei);
        device.recordHeartbeat();
        device.setAppVersion(currentVersion);
        System.out.println("💓 Heartbeat received from: " + imei + " | Version: " + currentVersion);
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    public Device getDevice(String imei) {
        return getDeviceOrThrow(imei);
    }

    /** Returns all devices */
    public List<Device> getAllDevices() {
        return new ArrayList<>(devices.values());
    }

    /** Returns devices in a specific region */
    public List<Device> getDevicesByRegion(String region) {
        return devices.values().stream()
                .filter(d -> d.getRegion().equalsIgnoreCase(region))
                .collect(Collectors.toList());
    }

    /** Returns devices running a specific app version */
    public List<Device> getDevicesByVersion(String version) {
        return devices.values().stream()
                .filter(d -> d.getAppVersion().equals(version))
                .collect(Collectors.toList());
    }

    /** Returns devices by region AND version */
    public List<Device> getDevicesByRegionAndVersion(String region, String version) {
        return devices.values().stream()
                .filter(d -> d.getRegion().equalsIgnoreCase(region)
                          && d.getAppVersion().equals(version))
                .collect(Collectors.toList());
    }

    /** Returns devices by client tag */
    public List<Device> getDevicesByClientTag(String clientTag) {
        return devices.values().stream()
                .filter(d -> d.getClientTag().equalsIgnoreCase(clientTag))
                .collect(Collectors.toList());
    }

    /** Returns devices inactive beyond a certain number of days */
    public List<Device> getInactiveDevices(int inactiveDays) {
        LocalDateTime threshold = LocalDateTime.now().minusDays(inactiveDays);
        return devices.values().stream()
                .filter(d -> d.getLastOpenTime().isBefore(threshold))
                .collect(Collectors.toList());
    }

    /** Returns a version distribution map: version → count */
    public Map<String, Long> getVersionDistribution() {
        return devices.values().stream()
                .collect(Collectors.groupingBy(Device::getAppVersion, Collectors.counting()));
    }

    /** Returns a region breakdown map: region → count */
    public Map<String, Long> getRegionBreakdown() {
        return devices.values().stream()
                .collect(Collectors.groupingBy(Device::getRegion, Collectors.counting()));
    }

    public int getTotalDeviceCount() {
        return devices.size();
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private Device getDeviceOrThrow(String imei) {
        Device device = devices.get(imei);
        if (device == null) throw new DeviceNotFoundException(imei);
        return device;
    }
}
