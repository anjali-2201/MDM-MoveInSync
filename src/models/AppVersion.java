package models;

import java.time.LocalDate;

/**
 * Represents a published app version in the MDM version repository.
 * Once published, a version is IMMUTABLE.
 */
public class AppVersion {

    // ─── Fields ───────────────────────────────────────────────────────────────
    private final String versionCode;     // e.g., "4.2.1"
    private final String versionName;     // e.g., "Summer Release 2024"
    private final LocalDate releaseDate;
    private final String minOS;           // Minimum supported OS version
    private final String maxOS;           // Maximum supported OS version
    private final String customizationTag; // e.g., "ClientA", "Global", "Chennai-Specific"
    private final boolean mandatory;      // true = force upgrade, false = optional
    private final String releaseNotes;

    // ─── Constructor ──────────────────────────────────────────────────────────
    public AppVersion(String versionCode, String versionName, LocalDate releaseDate,
                      String minOS, String maxOS, String customizationTag,
                      boolean mandatory, String releaseNotes) {
        this.versionCode = versionCode;
        this.versionName = versionName;
        this.releaseDate = releaseDate;
        this.minOS = minOS;
        this.maxOS = maxOS;
        this.customizationTag = customizationTag;
        this.mandatory = mandatory;
        this.releaseNotes = releaseNotes;
    }

    // ─── Getters (no setters - version is immutable once published) ───────────
    public String getVersionCode() { return versionCode; }
    public String getVersionName() { return versionName; }
    public LocalDate getReleaseDate() { return releaseDate; }
    public String getMinOS() { return minOS; }
    public String getMaxOS() { return maxOS; }
    public String getCustomizationTag() { return customizationTag; }
    public boolean isMandatory() { return mandatory; }
    public String getReleaseNotes() { return releaseNotes; }

    /**
     * Compares two version strings numerically.
     * e.g., "4.2" vs "4.3" → returns -1 (4.2 is lower)
     */
    public static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int maxLen = Math.max(parts1.length, parts2.length);

        for (int i = 0; i < maxLen; i++) {
            int p1 = (i < parts1.length) ? Integer.parseInt(parts1[i]) : 0;
            int p2 = (i < parts2.length) ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) return Integer.compare(p1, p2);
        }
        return 0;
    }

    @Override
    public String toString() {
        return String.format("AppVersion[Code=%s, Name=%s, Released=%s, Tag=%s, Mandatory=%b]",
                versionCode, versionName, releaseDate, customizationTag, mandatory);
    }
}
