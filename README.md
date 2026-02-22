# Mobile Device Management (MDM) System — Moveinsync
### Java Console Application | System Design Assignment

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Tech Stack](#2-tech-stack)
3. [Project Structure](#3-project-structure)
4. [How to Compile and Run](#4-how-to-compile-and-run)
5. [System Architecture](#5-system-architecture)
6. [Module-by-Module Documentation](#6-module-by-module-documentation)
   - [Module 1: Device Management](#module-1-device-management)
   - [Module 2: Version Repository](#module-2-version-repository)
   - [Module 3: Update Scheduling](#module-3-update-scheduling)
   - [Module 4: Workflow Simulator (State Machine)](#module-4-workflow-simulator-state-machine)
   - [Module 5: Audit Trail](#module-5-audit-trail)
   - [Module 6: Real-Time Dashboard](#module-6-real-time-dashboard)
   - [Module 7: Admin Management (RBAC)](#module-7-admin-management-rbac)
   - [Module 8: Upgrade Path Finder](#module-8-upgrade-path-finder)
7. [OOP Concepts Used](#7-oop-concepts-used)
8. [Exception Handling](#8-exception-handling)
9. [Design Patterns Used](#9-design-patterns-used)
10. [Sample Data (Preloaded)](#10-sample-data-preloaded)
11. [Trade-offs and Design Decisions](#11-trade-offs-and-design-decisions)
12. [System Flow Diagram](#12-system-flow-diagram)

---

## 1. Project Overview

The **Mobile Device Management (MDM) System** is a centralized platform built for Moveinsync to manage all mobile devices running the Moveinsync app. It provides:

- Complete device registration and real-time tracking via heartbeat
- Structured app version lifecycle management with an immutable version repository
- Controlled update rollouts targeting specific regions, versions, or client groups
- Strict downgrade prevention at both scheduling and device level
- A state-machine-driven update workflow with retry support
- Full audit trail and timeline tracking for every action
- Role-based access control for admin governance
- An interactive console interface where every action is performed step by step

The system is built entirely in **Java** using core OOP principles, data structures, and design patterns — no external frameworks or libraries required.

---

## 2. Tech Stack

| Component        | Technology        |
|------------------|-------------------|
| Language         | Java 21           |
| Paradigm         | Object-Oriented   |
| UI               | Console (Scanner) |
| Data Storage     | In-Memory (HashMap, ArrayList) |
| Build Tool       | Manual (javac)    |
| Dependencies     | None (pure Java)  |

---

## 3. Project Structure

```
MDM/
├── src/
│   ├── models/
│   │   ├── Device.java              → Device data model (IMEI, version, region, OS, status)
│   │   ├── AppVersion.java          → Immutable app version metadata
│   │   ├── AuditLog.java            → Immutable audit log entry with timestamp
│   │   └── Admin.java               → Admin user with role-based permissions
│   │
│   ├── repository/
│   │   ├── DeviceRegistry.java      → Central device store (HashMap<IMEI, Device>)
│   │   └── VersionRepository.java   → Version store + compatibility matrix (BFS)
│   │
│   ├── service/
│   │   ├── UpdateScheduler.java     → Schedules updates by region/version/client tag
│   │   ├── AuditService.java        → Global immutable audit log manager
│   │   └── DashboardService.java    → Real-time monitoring and stats
│   │
│   ├── workflow/
│   │   └── UpdateWorkflow.java      → State machine: SCHEDULED → ... → COMPLETED/FAILED
│   │
│   ├── exception/
│   │   ├── DowngradeNotAllowedException.java
│   │   ├── DeviceNotFoundException.java
│   │   ├── UnauthorizedActionException.java
│   │   └── InvalidUpgradePathException.java
│   │
│   └── main/
│       └── Main.java                → Interactive console platform (menu-driven)
│
└── out/                             → Compiled .class files go here
```

---

## 4. How to Compile and Run

### Prerequisites
- Java JDK 21 installed
- Windows Command Prompt or Terminal

### Step 1 — Navigate to the src folder
```cmd
cd C:\Users\<your-name>\Desktop\MDM\src
```

### Step 2 — Create the output folder
```cmd
mkdir ..\out
```

### Step 3 — Compile all Java files
```cmd
javac -d ..\out models\*.java exception\*.java repository\*.java workflow\*.java service\*.java main\*.java
```

### Step 4 — Run the application
```cmd
cd ..\out
java main.Main
```

### Full copy-paste sequence
```cmd
cd C:\Users\<your-name>\Desktop\MDM\src
mkdir ..\out
javac -d ..\out models\*.java exception\*.java repository\*.java workflow\*.java service\*.java main\*.java
cd ..\out
java main.Main
```

---

## 5. System Architecture

```
+----------------------------------------------------------+
|                    main/Main.java                        |
|           (Interactive Console - Menu Driven)            |
+----------------------------------------------------------+
          |           |           |           |
          v           v           v           v
   +----------+  +----------+  +--------+  +---------+
   | Update   |  | Audit    |  | Dash   |  | Update  |
   | Scheduler|  | Service  |  | board  |  |Workflow |
   +----------+  +----------+  +--------+  +---------+
          |           |                        |
          v           v                        v
   +------------------+              +------------------+
   | DeviceRegistry   |              | State Machine    |
   | VersionRepository|              | (7 States)       |
   +------------------+              +------------------+
          |
          v
   +------------------+
   | models/          |
   | Device           |
   | AppVersion       |
   | AuditLog         |
   | Admin            |
   +------------------+
```

---

## 6. Module-by-Module Documentation

---

### Module 1: Device Management

**File:** `repository/DeviceRegistry.java`, `models/Device.java`

#### What it does
Maintains a central registry of all mobile devices running the Moveinsync app. Every device is uniquely identified by its **IMEI number** stored in a `HashMap<String, Device>` for O(1) lookup performance.

#### Key Features
- **Device Registration** — Each device is registered with IMEI, app version, OS, model, region, and client tag
- **Heartbeat API** — Every time a device opens the app, it calls the heartbeat which updates `lastOpenTime` and validates the current version
- **Search by Region** — Retrieve all devices in a specific city
- **Search by Version** — Find all devices still on an older version
- **Search by Region + Version** — Combined query for targeted rollouts
- **Inactive Device Detection** — Identifies devices that haven't opened the app in N days

#### Device Status
```
ACTIVE   → Device has opened the app recently
INACTIVE → Device hasn't been seen past the defined threshold
```

#### Device Fields
| Field         | Type          | Description                          |
|---------------|---------------|--------------------------------------|
| imei          | String        | Primary unique identifier            |
| appVersion    | String        | Currently installed app version      |
| deviceOS      | String        | e.g., "Android 13"                   |
| deviceModel   | String        | e.g., "Samsung Galaxy S23"           |
| lastOpenTime  | LocalDateTime | Last heartbeat timestamp             |
| region        | String        | City/region e.g., "Bangalore"        |
| clientTag     | String        | Client customization e.g., "Global"  |
| status        | DeviceStatus  | ACTIVE or INACTIVE                   |

---

### Module 2: Version Repository

**File:** `repository/VersionRepository.java`, `models/AppVersion.java`

#### What it does
Maintains an **immutable repository** of all published app versions along with a **compatibility matrix** that defines which version upgrades are allowed directly and which require intermediate steps.

#### Key Features
- **Immutable Versions** — Once published, a version cannot be modified (no setters on AppVersion)
- **Version Metadata** — Each version stores code, name, release date, OS range, client tag, mandatory flag, and release notes
- **Compatibility Matrix** — A `HashMap<String, List<String>>` that maps each version to its allowed direct upgrade targets
- **BFS Upgrade Path Finder** — Uses Breadth-First Search to find the shortest valid upgrade path between any two versions

#### Version Fields
| Field            | Description                              |
|------------------|------------------------------------------|
| versionCode      | e.g., "4.2"                             |
| versionName      | e.g., "Summer Security Release"         |
| releaseDate      | Date the version was published           |
| minOS / maxOS    | Supported OS range                       |
| customizationTag | "Global", "Chennai-Specific", "ClientA"  |
| mandatory        | true = force upgrade on all devices      |
| releaseNotes     | Description of changes                   |

#### Compatibility Matrix Example
```
v3.8  →  [4.0]
v4.0  →  [4.1, 4.3]
v4.1  →  [4.2, 4.3]
v4.2  →  [4.3]

Meaning:
- 3.8 → 4.3 is NOT allowed directly (no path exists in one hop)
- BFS finds: 3.8 → 4.0 → 4.3 (two steps required)
- 4.0 → 4.3 IS allowed directly
```

---

### Module 3: Update Scheduling

**File:** `service/UpdateScheduler.java`

#### What it does
The scheduling engine that allows admins to push app updates to targeted groups of devices. It enforces all business rules before creating any workflow.

#### Targeting Strategies
| Strategy          | Example Use Case                                        |
|-------------------|---------------------------------------------------------|
| By Region         | Upgrade all Chennai devices from 4.1 to 4.2            |
| By Version        | Upgrade all devices on 4.0 to 4.1 across all of India  |
| By Client Tag     | Push a custom build only to Client A devices            |

#### Rollout Types
| Type      | Behaviour                                              |
|-----------|--------------------------------------------------------|
| IMMEDIATE | All targeted devices receive the update simultaneously |
| PHASED    | Devices are batched (25% at a time) to reduce risk     |

#### Validations Enforced (in order)
1. Admin must have `RELEASE_MANAGER` or `SUPER_ADMIN` role
2. Target version must exist in the version repository
3. Target version must be **greater than** current version (downgrade blocked)
4. If the update is marked **mandatory**, only `SUPER_ADMIN` can approve it
5. The upgrade path must exist in the compatibility matrix

#### Downgrade Prevention
```
Admin tries: 4.5 → 4.3
System response: DowngradeNotAllowedException thrown immediately
Audit log: DOWNGRADE_BLOCKED event recorded with admin ID and timestamp
```

---

### Module 4: Workflow Simulator (State Machine)

**File:** `workflow/UpdateWorkflow.java`

#### What it does
Each device update goes through a defined sequence of states. This implements the **State Machine design pattern** where each transition is validated and every state change is logged to the device's personal timeline.

#### State Machine Diagram
```
  [SCHEDULED]
       |
       | notifyDevice()
       v
  [NOTIFIED]
       |
       | startDownload()
       v
  [DOWNLOAD_STARTED]
       |
       | completeDownload()
       v
  [DOWNLOAD_COMPLETED]
       |
       | startInstallation()
       v
  [INSTALL_STARTED]
       |
       | completeInstallation()
       v
  [INSTALL_COMPLETED]  ← SUCCESS

  Any state → markFailed(reason) → [FAILED]
  [FAILED]  → retry()            → [NOTIFIED]  (restarts from download)
```

#### Retry Mechanism
- Maximum **3 retry attempts** per workflow
- On retry, the workflow re-enters the `NOTIFIED` state so the device restarts the download
- After 3 failed retries, the workflow is marked `RETRY_EXHAUSTED` and requires manual intervention
- Every retry attempt is logged to the timeline

#### Timeline View (per device)
Every state change is recorded with a timestamp:
```
[2024-03-01 10:00:00] UPDATE_SCHEDULED   - Update from v4.1 to v4.3 scheduled
[2024-03-01 10:05:00] DEVICE_NOTIFIED    - Push notification sent
[2024-03-01 10:07:00] DOWNLOAD_STARTED   - Device started downloading v4.3
[2024-03-01 10:12:00] FAILED [FAILURE]   - Network Timeout during download
[2024-03-01 10:13:00] RETRY_ATTEMPT      - Retry attempt #1 of 3
[2024-03-01 10:13:05] DOWNLOAD_STARTED   - Device started downloading v4.3
[2024-03-01 10:18:00] DOWNLOAD_COMPLETED - Download finished
[2024-03-01 10:19:00] INSTALL_STARTED    - Installation initiated
[2024-03-01 10:21:00] INSTALL_COMPLETED  - Device successfully updated to v4.3
```

---

### Module 5: Audit Trail

**File:** `service/AuditService.java`, `models/AuditLog.java`

#### What it does
Every single action performed in the system — by an admin or the system itself — is recorded as an immutable `AuditLog` entry. This provides complete traceability and governance.

#### AuditLog Fields
| Field       | Description                                        |
|-------------|----------------------------------------------------|
| logId       | Auto-generated unique ID (e.g., LOG-0001)          |
| deviceImei  | Which device this log is associated with           |
| adminId     | Which admin triggered this action                  |
| action      | Event name (e.g., UPDATE_SCHEDULED, FAILED)        |
| details     | Free-text description of what happened             |
| timestamp   | Exact date and time of the event                   |
| isFailure   | true if this log represents an error or failure    |

#### Query Options
- View full audit trail for a specific device (IMEI)
- View all failure logs across all devices
- View all logs by a specific admin
- View the complete global audit log

#### Immutability
`AuditLog` has **no setters**. Once created, a log entry can never be modified. This ensures tamper-proof compliance records.

---

### Module 6: Real-Time Dashboard

**File:** `service/DashboardService.java`

#### What it does
Provides a live monitoring view of the entire MDM system. Operations teams use this to track rollout progress, version adoption, and device health.

#### Dashboard Sections

**Device Overview**
- Total registered devices
- All versions currently in use across the fleet

**Version Heatmap**
- Visual bar chart showing what percentage of devices are on each version
- Sorted by most-used version descending
```
v4.1       | ████████████         | 40 devices (40.0%)
v4.2       | ████████             | 30 devices (30.0%)
v4.3       | ████                 | 20 devices (20.0%)
```

**Region-wise Breakdown**
- Count of devices per city/region

**Rollout Progress**
- Total active workflows
- Count of completed, failed, and in-progress updates
- Overall success rate as a percentage

**Inactive Device Alert**
- Lists all devices that haven't opened the app beyond a configurable threshold (default: 30 days)

---

### Module 7: Admin Management (RBAC)

**File:** `models/Admin.java`

#### What it does
Implements **Role-Based Access Control (RBAC)**. Every admin has a role that determines what actions they can perform. This prevents unauthorized operations and supports the approval workflow.

#### Roles and Permissions

| Permission                     | SUPER_ADMIN | RELEASE_MANAGER | VIEWER |
|--------------------------------|:-----------:|:---------------:|:------:|
| View Dashboard & Audit Logs    | Yes         | Yes             | Yes    |
| Schedule Updates               | Yes         | Yes             | No     |
| Approve Mandatory Updates      | Yes         | No              | No     |
| Publish New App Versions       | Yes         | No              | No     |
| Modify Compatibility Matrix    | Yes         | No              | No     |

#### Predefined Admins
| Admin ID  | Name          | Role             |
|-----------|---------------|------------------|
| ADMIN-001 | Rajesh Kumar  | SUPER_ADMIN      |
| ADMIN-002 | Priya Sharma  | RELEASE_MANAGER  |
| ADMIN-003 | Amit Verma    | VIEWER           |

---

### Module 8: Upgrade Path Finder

**File:** `repository/VersionRepository.java` (findUpgradePath method)

#### What it does
Given any two version codes, finds the shortest valid upgrade path using **Breadth-First Search (BFS)** on the compatibility matrix graph.

#### Algorithm
```
Input:  fromVersion = "3.8", toVersion = "4.3"
Graph:  3.8→[4.0], 4.0→[4.1,4.3], 4.1→[4.2,4.3], 4.2→[4.3]

BFS traversal:
  Queue: [[3.8]]
  Pop [3.8], expand neighbors: [4.0]
  Queue: [[3.8, 4.0]]
  Pop [3.8, 4.0], expand neighbors: [4.1, 4.3]
  Path [3.8, 4.0, 4.3] reaches target!

Output: [3.8, 4.0, 4.3]  → 2 upgrade steps required
```

#### Use Cases
- Before scheduling any update, the system automatically validates the path
- Admins can manually query the path finder to understand intermediate steps required for any upgrade

---

## 7. OOP Concepts Used

### Encapsulation
Every model class (`Device`, `AppVersion`, `AuditLog`, `Admin`) has **private fields** with public getters. `AppVersion` and `AuditLog` have **no setters at all** — they are fully immutable by design.

### Inheritance
`Admin.Role` uses an enum hierarchy. The role system is designed so that `SUPER_ADMIN` is a superset of all other roles.

### Polymorphism
`RolloutType` enum (`IMMEDIATE` / `PHASED`) is used polymorphically in `UpdateScheduler` — the same `scheduleByRegion()` method behaves differently depending on the rollout type passed.

### Abstraction
Service classes (`UpdateScheduler`, `AuditService`, `DashboardService`) hide all internal logic. `Main.java` only calls high-level methods and never touches the internals of repositories directly.

### Enums
Used extensively for type safety:
- `Device.DeviceStatus` — ACTIVE, INACTIVE
- `Admin.Role` — SUPER_ADMIN, RELEASE_MANAGER, VIEWER
- `UpdateWorkflow.UpdateState` — 7 states of the update lifecycle
- `UpdateScheduler.RolloutType` — IMMEDIATE, PHASED

---

## 8. Exception Handling

All exceptions are **custom**, meaningful, and extend `RuntimeException` for clean handling.

| Exception                        | When Thrown                                              |
|----------------------------------|----------------------------------------------------------|
| `DowngradeNotAllowedException`   | Admin tries to schedule a version lower than current     |
| `DeviceNotFoundException`        | IMEI not found in the device registry                    |
| `UnauthorizedActionException`    | Admin's role doesn't permit the requested action         |
| `InvalidUpgradePathException`    | No valid path exists in the compatibility matrix         |

Every exception includes a descriptive message identifying exactly what went wrong. In `Main.java`, all exceptions are caught with `try-catch` blocks and displayed as clear error messages without crashing the application.

---

## 9. Design Patterns Used

| Pattern          | Where Used                          | Purpose                                              |
|------------------|-------------------------------------|------------------------------------------------------|
| State Machine    | `UpdateWorkflow.java`               | Controls valid update lifecycle transitions          |
| Repository       | `DeviceRegistry`, `VersionRepository` | Separates data access from business logic          |
| Service Layer    | `UpdateScheduler`, `AuditService`   | Centralizes business logic away from UI             |
| Facade           | `Main.java`                         | Single entry point that coordinates all modules      |
| BFS Algorithm    | `VersionRepository.findUpgradePath` | Finds shortest valid upgrade path in the version graph |

---

## 10. Sample Data (Preloaded)

The system automatically loads this data when it starts so you can use all features immediately without entering anything manually.

### Preloaded Versions
| Code | Name                  | Mandatory | Tag               |
|------|-----------------------|-----------|-------------------|
| 3.8  | Legacy Build          | No        | Global            |
| 4.0  | Spring Release        | No        | Global            |
| 4.1  | Bugfix Release        | No        | Global            |
| 4.2  | Chennai Pilot         | No        | Chennai-Specific  |
| 4.3  | Security Patch        | YES       | Global            |

### Preloaded Devices
| IMEI          | Version | Region    | Model          |
|---------------|---------|-----------|----------------|
| IMEI-BLR-001  | 4.1     | Bangalore | Pixel 7        |
| IMEI-BLR-002  | 4.1     | Bangalore | OnePlus 10     |
| IMEI-BLR-003  | 4.2     | Bangalore | Samsung S23    |
| IMEI-CHN-001  | 4.1     | Chennai   | Redmi Note 12  |
| IMEI-CHN-002  | 4.2     | Chennai   | Vivo V27       |
| IMEI-HYD-001  | 3.8     | Hyderabad | Realme GT      |
| IMEI-HYD-002  | 4.0     | Hyderabad | Moto G84       |
| IMEI-MUM-001  | 4.1     | Mumbai    | iPhone 14      |

### Preloaded Upgrade Paths
```
3.8 → 4.0
4.0 → 4.1
4.0 → 4.3
4.1 → 4.2
4.1 → 4.3
4.2 → 4.3
```

---

## 11. Trade-offs and Design Decisions

### In-Memory Storage vs Database
**Decision:** Used `HashMap` and `ArrayList` for all storage.
**Reason:** Keeps the project self-contained with no external dependencies, making it easy to compile and run anywhere. In a real production system, this would be replaced with a relational database (PostgreSQL) and an ORM like Hibernate.
**Trade-off:** Data is lost when the application exits.

### Immutable AuditLog and AppVersion
**Decision:** No setters on `AuditLog` and `AppVersion`.
**Reason:** Audit logs must be tamper-proof for compliance. App versions, once published, should never silently change to prevent version drift across devices.
**Trade-off:** If a version has a typo in its name, you must publish a new one; you cannot edit the old one.

### BFS for Upgrade Path Finding
**Decision:** Used BFS instead of DFS or simple lookup.
**Reason:** BFS guarantees the **shortest** upgrade path (fewest intermediate steps), which minimizes the number of updates a device needs to go through.
**Trade-off:** Slightly more complex than a direct lookup, but the correctness guarantee is worth it.

### Role-Based Access with Enum
**Decision:** Used a simple `enum Role` instead of a permission bit-mask or database-driven RBAC.
**Reason:** There are only 3 roles with well-defined, stable permissions. A simple enum is more readable and maintainable than an over-engineered permission system.
**Trade-off:** Adding a new role requires code changes.

### State Machine for Update Workflow
**Decision:** Enforced strict sequential state transitions with `assertState()`.
**Reason:** Prevents impossible transitions (e.g., jumping from SCHEDULED directly to INSTALL_COMPLETED), which would corrupt the audit trail and make root cause analysis impossible.
**Trade-off:** Less flexible — you cannot skip states even in testing without manually advancing through each one.

---

## 12. System Flow Diagram

```
ADMIN LOGS IN
     |
     v
MAIN MENU
     |
     |-----> [1] DEVICE MANAGEMENT
     |              |
     |              |--> Register Device (IMEI, version, region, OS, client)
     |              |--> Heartbeat (updates lastOpenTime + version)
     |              |--> Search by Region / Version / Region+Version
     |              +--> View Inactive Devices
     |
     |-----> [2] VERSION REPOSITORY
     |              |
     |              |--> Publish New Version (immutable once published)
     |              |--> View Compatibility Matrix
     |              +--> Add Upgrade Path (SUPER_ADMIN only)
     |
     |-----> [3] SCHEDULE UPDATE
     |              |
     |              |--> Validate Admin Role
     |              |--> Block Downgrade (throw exception if target < current)
     |              |--> Validate Upgrade Path (BFS)
     |              |--> Create UpdateWorkflow for each matching device
     |              +--> Log to AuditService
     |
     |-----> [4] WORKFLOW SIMULATOR
     |              |
     |              |--> Advance State (SCHEDULED→NOTIFIED→DOWNLOAD→INSTALL→DONE)
     |              |--> Mark Failed (capture failure reason + stage)
     |              |--> Retry (up to 3 times, re-enters NOTIFIED)
     |              +--> Auto-Complete (runs all steps instantly)
     |
     |-----> [5] AUDIT TRAIL
     |              |
     |              |--> View logs per device (full timeline)
     |              |--> View all failure logs
     |              +--> View logs by admin
     |
     |-----> [6] DASHBOARD
     |              |
     |              |--> Version Heatmap (% of devices per version)
     |              |--> Region Breakdown (devices per city)
     |              +--> Rollout Progress (% completed/failed)
     |
     |-----> [7] ADMIN MANAGEMENT
     |              +--> View all admins and their roles
     |
     |-----> [8] UPGRADE PATH FINDER
     |              +--> Enter any two versions, get BFS-computed path
     |
     +--> [0] EXIT
```

---

*Built for Moveinsync MDM System Design Assignment*
*Language: Java 21 | No external dependencies | Pure OOP*
