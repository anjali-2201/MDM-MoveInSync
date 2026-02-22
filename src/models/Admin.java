package models;

/**
 * Represents an admin user in the MDM system.
 * Uses Role-Based Access Control (RBAC).
 */
public class Admin {

    // ─── Role Enum ────────────────────────────────────────────────────────────
    public enum Role {
        SUPER_ADMIN,    // Can do everything including delete
        RELEASE_MANAGER, // Can schedule updates, needs approval for mandatory
        VIEWER          // Read-only access (dashboard viewing only)
    }

    // ─── Fields ───────────────────────────────────────────────────────────────
    private final String adminId;
    private final String name;
    private final String email;
    private Role role;
    private boolean active;

    // ─── Constructor ──────────────────────────────────────────────────────────
    public Admin(String adminId, String name, String email, Role role) {
        this.adminId = adminId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.active = true;
    }

    // ─── Permission Checks ────────────────────────────────────────────────────

    /** Can this admin schedule an update? */
    public boolean canScheduleUpdates() {
        return role == Role.SUPER_ADMIN || role == Role.RELEASE_MANAGER;
    }

    /** Can this admin approve mandatory updates? */
    public boolean canApproveMandatoryUpdates() {
        return role == Role.SUPER_ADMIN;
    }

    /** Can this admin publish new app versions to the repository? */
    public boolean canPublishVersions() {
        return role == Role.SUPER_ADMIN;
    }

    /** Can this admin view dashboards and audit logs? */
    public boolean canViewDashboard() {
        return true; // All roles can view
    }

    // ─── Getters & Setters ────────────────────────────────────────────────────
    public String getAdminId() { return adminId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return String.format("Admin[ID=%s, Name=%s, Role=%s, Active=%b]",
                adminId, name, role, active);
    }
}
