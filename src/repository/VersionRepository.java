package repository;

import exception.InvalidUpgradePathException;
import models.AppVersion;

import java.util.*;

/**
 * Stores all published app versions and manages the version compatibility matrix.
 * Once a version is published, it is IMMUTABLE.
 */
public class VersionRepository {

    // versionCode → AppVersion
    private final Map<String, AppVersion> versions = new LinkedHashMap<>();

    /**
     * Compatibility matrix: fromVersion → list of allowed direct target versions.
     * If a direct path doesn't exist, an intermediate upgrade is required.
     */
    private final Map<String, List<String>> compatibilityMatrix = new HashMap<>();

    // ─── Version Management ───────────────────────────────────────────────────

    public void publishVersion(AppVersion version) {
        if (versions.containsKey(version.getVersionCode())) {
            throw new IllegalArgumentException(
                    "Version already published: " + version.getVersionCode());
        }
        versions.put(version.getVersionCode(), version);
        System.out.println(" Version published: " + version.getVersionCode()
                + " | Mandatory: " + version.isMandatory());
    }

    public AppVersion getVersion(String versionCode) {
        AppVersion v = versions.get(versionCode);
        if (v == null) throw new IllegalArgumentException("Version not found: " + versionCode);
        return v;
    }

    public boolean versionExists(String versionCode) {
        return versions.containsKey(versionCode);
    }

    public List<AppVersion> getAllVersions() {
        return new ArrayList<>(versions.values());
    }

    // ─── Compatibility Matrix ─────────────────────────────────────────────────

    /**
     * Define an allowed direct upgrade path.
     * e.g., addAllowedUpgradePath("4.0", "4.3") means 4.0 → 4.3 is allowed directly.
     */
    public void addAllowedUpgradePath(String fromVersion, String toVersion) {
        compatibilityMatrix
                .computeIfAbsent(fromVersion, k -> new ArrayList<>())
                .add(toVersion);
        System.out.println(" Upgrade path added: v" + fromVersion + " → v" + toVersion);
    }

    /**
     * Check if a direct upgrade from fromVersion to toVersion is allowed.
     */
    public boolean isDirectUpgradeAllowed(String fromVersion, String toVersion) {
        List<String> allowed = compatibilityMatrix.getOrDefault(fromVersion, Collections.emptyList());
        return allowed.contains(toVersion);
    }

    /**
     * Find the required intermediate versions for an upgrade path.
     * Uses BFS to find the shortest valid upgrade path.
     * Returns null if no path exists.
     *
     * Example: 3.8 → 4.3 might return [3.8, 4.0, 4.3]
     */
    public List<String> findUpgradePath(String fromVersion, String toVersion) {
        if (fromVersion.equals(toVersion)) return List.of(fromVersion);

        // BFS
        Queue<List<String>> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        queue.add(new ArrayList<>(List.of(fromVersion)));
        visited.add(fromVersion);

        while (!queue.isEmpty()) {
            List<String> path = queue.poll();
            String current = path.get(path.size() - 1);

            List<String> neighbors = compatibilityMatrix.getOrDefault(current, Collections.emptyList());
            for (String next : neighbors) {
                if (next.equals(toVersion)) {
                    path.add(next);
                    return path;
                }
                if (!visited.contains(next)) {
                    visited.add(next);
                    List<String> newPath = new ArrayList<>(path);
                    newPath.add(next);
                    queue.add(newPath);
                }
            }
        }
        return null; // No path found
    }

    /**
     * Validates that the upgrade path is valid.
     * Throws InvalidUpgradePathException if not allowed.
     */
    public void validateUpgradePath(String fromVersion, String toVersion) {
        List<String> path = findUpgradePath(fromVersion, toVersion);
        if (path == null) {
            throw new InvalidUpgradePathException(fromVersion, toVersion);
        }
        if (path.size() > 2) {
            System.out.println("  Intermediate upgrades required: " + path);
        }
    }

    /** Print the full compatibility matrix */
    public void printCompatibilityMatrix() {
        System.out.println("\n========= VERSION COMPATIBILITY MATRIX =========");
        for (Map.Entry<String, List<String>> entry : compatibilityMatrix.entrySet()) {
            System.out.println("  v" + entry.getKey() + " → " + entry.getValue());
        }
        System.out.println("================================================\n");
    }
}
