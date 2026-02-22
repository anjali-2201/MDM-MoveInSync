# MOVEINSYNC  
## Mobile Device Management (MDM) System  

### Trade-offs in System Design  
### Architectural & Design Decision Analysis (Interview-Aligned)

**Trade-offs Analysed:** 5  
**Design Dimensions:** Performance, Scalability, Maintainability, Security  
**Modules Covered:** Devices, Versions, RBAC, Workflow, Audit  
**Patterns Used:** Repository, State Machine, Immutability, RBAC, Graph Traversal  

**System Design Assignment | Java | February 2026**

---

## 1. Introduction

Every non-trivial system design involves deliberate trade-offs. This document captures **five key design trade-offs** made while designing the **Moveinsync Mobile Device Management (MDM) System**.

Each trade-off is analysed by:
- The approach chosen
- Alternatives considered
- Rationale behind the decision
- Impact on performance, scalability, maintainability, and security

The guiding philosophy of this design is to **prioritise correctness, auditability, and clarity** over premature optimisation or unnecessary flexibility. All decisions are reversible if system requirements evolve toward a production-scale deployment.

---

## 2. Trade-off 1: In-Memory Storage vs Persistent Database

### Decision

The system stores all data — devices, app versions, workflows, and audit logs — **in-memory** using Java collections instead of a persistent relational database.

### Chosen Approach: In-Memory Storage

- Devices stored in `HashMap<String, Device>` using **IMEI as primary key**
- O(1) average lookup time
- No external dependencies (DB server, JDBC, schema)
- Entire system runs using a single `javac` command
- Audit logs stored in `ArrayList` with O(1) append

### Alternative: Relational Database (PostgreSQL)

- Persistent storage across restarts
- Supports indexing, joins, and concurrency
- Requires schema management, migrations, JDBC drivers
- Higher operational and setup overhead

### Rationale

The objective of this system is to **demonstrate domain logic, OOP principles, and architectural reasoning**, not production infrastructure. In-memory storage removes environmental complexity while still mirroring real-world indexing logic.

Persistence is intentionally isolated behind repository boundaries. In a production system, the in-memory layer can be replaced with a database-backed implementation **without modifying the service layer**.

### Impact Analysis

| Aspect | In-Memory | Database | Rationale |
|-----|----------|---------|-----------|
| Performance | O(1) access | Indexed queries | In-memory faster at small scale |
| Scalability | JVM-bound | Horizontally scalable | DB needed at massive scale |
| Maintainability | No migrations | Schema evolution needed | Lower maintenance for scope |
| Security | Transient data | Persistent encrypted data | Reduced attack surface |

---

## 3. Trade-off 2: Immutability of AppVersion and AuditLog

### Decision

`AppVersion` and `AuditLog` are designed as **fully immutable objects**.

### Chosen Approach: Full Immutability

- All fields declared `final`
- No setter methods
- App versions are locked at publish time
- Audit logs are permanent, append-only records
- Updates require publishing a new version

### Alternative: Mutable Models

- Allows post-publish edits
- Requires version history tracking
- Increases risk of silent or malicious data changes

### Rationale

Version management and audit logs are **compliance-critical components**.  
Immutability guarantees that once data is recorded, it becomes a **historical fact** that cannot be altered. This provides strong trust guarantees and simplifies reasoning in concurrent environments.

### Impact Analysis

| Aspect | Immutable | Mutable | Rationale |
|-----|----------|---------|-----------|
| Performance | No locking | Synchronisation needed | Immutable is thread-safe |
| Scalability | Safe sharing | Defensive copying | Better concurrency |
| Maintainability | Simple reasoning | Complex state tracking | Fewer bugs |
| Security | Tamper-proof | Logs editable | Compliance requirement |

---

## 4. Trade-off 3: BFS for Upgrade Path vs Direct Lookup

### Decision

Upgrade path validation uses **Breadth-First Search (BFS)** on a version compatibility graph.

### Chosen Approach: BFS

- Versions modelled as a directed graph
- Guarantees **shortest upgrade path**
- Automatically handles multi-step upgrades
- Prevents cycles using a visited set

### Alternatives

#### Direct Lookup Table
- O(1) lookup
- Cannot derive intermediate upgrade paths
- Requires manual enumeration

#### Depth-First Search (DFS)
- May return longer, suboptimal paths
- Less predictable operational behaviour

### Rationale

App upgrades represent an **unweighted shortest-path problem**.  
BFS is the correct algorithm both theoretically and operationally, ensuring devices go through the **fewest possible upgrade steps**, reducing risk and downtime.

### Impact Analysis

| Aspect | BFS | Alternative | Rationale |
|-----|-----|-----------|-----------|
| Performance | O(V+E) | O(1) lookup | Cost negligible |
| Scalability | Linear growth | Manual paths fail | Self-scaling |
| Maintainability | Auto-derived paths | Manual upkeep | Lower admin burden |
| Security | Enforces mandatory steps | Steps may be skipped | Safer upgrades |

---

## 5. Trade-off 4: Enum-Based RBAC vs Permission Bitmask

### Decision

Role-Based Access Control (RBAC) is implemented using **Java Enums**.

### Chosen Approach: Enum-Based RBAC

- Roles: `SUPER_ADMIN`, `RELEASE_MANAGER`, `VIEWER`
- Permissions defined via boolean methods
- Compile-time safety
- Self-documenting access rules

### Alternative: Bitmask or DB-Driven Permissions

- Highly flexible
- Harder to audit and debug
- Over-engineered for stable roles

### Rationale

The system has a **small, stable set of roles**.  
Enum-based RBAC optimises for **auditability and clarity over flexibility**, aligning with enterprise governance needs and the YAGNI principle.

### Impact Analysis

| Aspect | Enum RBAC | Bitmask | Rationale |
|-----|----------|---------|-----------|
| Performance | O(1) | O(1) | Equal |
| Scalability | Code changes needed | Dynamic roles | Enum sufficient |
| Maintainability | Readable & safe | Needs decoding | Easier audits |
| Security | Type-safe | Misconfig risk | Safer by design |

---

## 6. Trade-off 5: Strict State Machine vs Flexible Workflow

### Decision

Device updates are enforced via a **strict sequential state machine**.

### Chosen Approach: Strict State Machine

**States:**

- Every transition validated
- Invalid transitions throw exceptions
- All transitions logged
- Retry re-enters `NOTIFIED`

### Alternative: Flexible Workflow Engine

- Allows skipping or reordering states
- More adaptable but harder to audit
- Risk of masking failures

### Rationale

Each state represents an **observability checkpoint**.  
Strict sequencing ensures deterministic root-cause analysis and prevents silent failure masking — critical in large-scale device deployments.

### Impact Analysis

| Aspect | Strict Machine | Flexible Engine | Rationale |
|-----|---------------|----------------|-----------|
| Performance | Minimal overhead | Higher complexity | Faster |
| Scalability | Independent workflows | Needs orchestration | Easier to scale |
| Maintainability | Explicit transitions | External configs | Self-contained |
| Security | No bypass possible | Skip risk | Strong guarantees |

---

## 7. Overall Impact Summary

| Trade-off | Performance | Scalability | Maintainability | Security |
|---------|------------|------------|----------------|----------|
| In-Memory Storage | High | Medium | High | Medium |
| Immutability | High | High | High | High |
| BFS Upgrade Path | Medium | High | High | High |
| Enum RBAC | High | Medium | High | High |
| Strict State Machine | High | High | High | High |

---

## 8. Conclusion

The Moveinsync MDM system is designed with a clear bias toward **correctness, auditability, and operational clarity**.

- In-memory storage removes infrastructure noise
- Immutability guarantees trust and compliance
- BFS ensures safest upgrade paths
- Enum-based RBAC improves audit transparency
- Strict state machines provide deterministic observability

Each trade-off is **intentional, documented, and reversible**, making the system both fit for current requirements and ready for production evolution.
