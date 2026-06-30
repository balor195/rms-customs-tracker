# Product Requirements Document
## RMS Customs Clearance Transaction Tracker

**Organization:** Royal Medical Services — Directorate of Pharmacy & Medical Equipment  
**الجهة:** الخدمات الطبية الملكية — مديرية الصيدلة والتجهيزات الطبية  
**Document version:** 1.0  
**Date:** 2026-06-29  
**Status:** Implemented (v1.0.0)

---

## 1. Background & Problem Statement

The Directorate of Pharmacy & Medical Equipment at Royal Medical Services (RMS) Jordan is responsible for procuring and importing medical supplies through a complex multi-agency customs clearance process. Each import transaction passes through up to 7 sequential phases and must be co-approved by three separate government bodies: the Jordan Armed Forces Command, Jordan Customs, and the Jordan Food and Drug Administration (JFDA).

Prior to this system, tracking was done through paper logs and shared spreadsheets, which caused:

- Missed SLA deadlines with no automated alerts
- No audit trail of who advanced which phase and when
- No visibility into which transactions were blocked or on hold
- Manual generation of reports for senior management
- Data siloed on individual staff devices with no synchronization

---

## 2. Users & Roles

| Role | Arabic | Permissions |
|---|---|---|
| **ADMIN** | مسؤول النظام | Full access: create/manage users, configure SLAs, view all data, export |
| **COORDINATOR** | منسق التخليص | Create transactions, advance phases, upload documents |
| **SUPERVISOR** | مشرف | Approve phase transitions, view all data, export reports |
| **VIEWER** | مستعرض | Read-only access to transaction list and detail |

All users authenticate with a username and password (PBKDF2-SHA256 hashed). Sessions persist across app restarts via encrypted SharedPreferences (EncryptedSharedPreferences).

---

## 3. Goals & Success Metrics

| Goal | Metric |
|---|---|
| Eliminate missed deadlines | Zero SLA breaches go unnotified within 6 hours of occurrence |
| Full audit trail | Every phase change has a timestamped actor and reason |
| Multi-device access | Data consistent across all staff tablets within 15 minutes of any change |
| Paperless reporting | Weekly/monthly/executive PDF and CSV reports generated in < 5 seconds |
| Offline resilience | All core functions (view, phase advance, document attach) work without network |

---

## 4. Core Features

### 4.1 Transaction Lifecycle Management

Each transaction follows a strict 7-phase linear workflow enforced by a state machine. Unauthorized out-of-order transitions are rejected at the domain layer.

**Phases:**

| # | Arabic | English |
|---|---|---|
| 1 | إعداد العطاء | Tender Preparation |
| 2 | التقييم والعقد | Evaluation & Contract |
| 3 | إعداد وثائق التخليص | Clearance Documentation |
| 4 | الجهات الحكومية | Gov-Agency Processing (parallel) |
| 5 | أمر الإفراج | Release Order |
| 6 | النقل والاستلام | Transit & Receipt |
| 7 | التسوية المالية | Financial Settlement |

**Hard gates enforced by the system:**
- Cannot submit customs declaration (Phase 3→4) without a signed contract
- Cannot move shipment to transit (Phase 5→6) without a final release order
- Cannot issue GOV_APPROVED until all three Phase 4 parallel tracks are complete

**Exception overlays:** A transaction can be flagged BLOCKED, ON_HOLD, or DISPUTED at any point without losing its position in the workflow. Resolving the exception restores the transaction to its last phase.

### 4.2 Phase 4 Parallel Tracks

Phase 4 (Gov-Agency Processing) requires simultaneous approvals from three entities:

| Track | Entity | Arabic |
|---|---|---|
| 4.1 | Armed Forces Command | القيادة العامة للقوات المسلحة |
| 4.2 | Jordan Customs | الجمارك الأردنية |
| 4.3 | Jordan FDA | هيئة الغذاء والدواء الأردنية |

All three tracks must reach DONE status before the transaction can advance to Phase 5.

### 4.3 SLA Monitoring & Alerts

- Each sub-phase (including all three Phase 4 tracks) has a configurable SLA target in working days
- The system checks SLA compliance every 6 hours in the background (even when the app is closed)
- Two alert levels: **SLA Breach** (target exceeded) and **SLA Escalated** (escalation threshold exceeded)
- At most one notification per transaction per alert type per 24 hours (deduplication)
- Notifications appear in the in-app Notification Center and as Android system notifications

SLA targets are configurable by ADMIN through the SLA Configuration screen.

### 4.4 Document Management

Each transaction has an attached documents section. Supported actions:
- Upload documents from device storage or camera capture (CameraX)
- Categorize by document type (e.g., customs declaration, release order, contract, inspection minutes)
- Each document is stored locally with the transaction; not synced to the backend (local-only)

### 4.5 Dashboard & Reporting

**Dashboard** shows at-a-glance KPIs:
- Transactions by status (active, blocked, closed)
- SLA compliance rate
- Transactions per phase distribution

**Reports** (ADMIN and SUPERVISOR only) are generated as PDF or CSV:
- **Weekly** — all transactions updated in the last 7 days
- **Monthly** — full transaction list with phase breakdown
- **Executive** — summary KPIs for management

PDFs are generated natively using `android.graphics.pdf.PdfDocument` (no external library) and can be shared via Android's share sheet.

### 4.6 Backend Sync

All transaction and phase data syncs bi-directionally with a central FastAPI server:

- Sync runs automatically every 15 minutes when on any network
- Manual sync can be triggered from the top bar or Settings screen
- Uses an incremental cursor-based strategy: only records changed since the last sync are transferred
- Conflict resolution: last-write-wins based on `updatedAt` timestamp
- Offline use is fully supported — changes accumulate and sync when connectivity is restored

### 4.7 User Management (ADMIN only)

Admins can:
- Create new user accounts (all roles and departments supported)
- Change a user's role at any time
- Deactivate users (self-deactivation is prevented)

Passwords are hashed with PBKDF2-SHA256 (10,000 iterations) and never stored in plaintext.

### 4.8 Settings

- **Server URL** — configurable at runtime; changes take effect immediately without restart
- **Manual Sync** — trigger a full push+pull cycle on demand
- **Last Sync Time** — visible to all users
- **User Management** — ADMIN only
- **SLA Configuration** — ADMIN only

---

## 5. Non-Functional Requirements

| Requirement | Specification |
|---|---|
| Platform | Android 8.0+ (API 26+) |
| Language | Arabic (RTL primary), English secondary |
| Offline capability | All read and write operations work without network |
| Security | PBKDF2-SHA256 passwords; encrypted session store; RBAC enforced in UI and domain layers |
| Performance | Transaction list renders < 16ms per frame; PDF generation < 5 seconds for 100 transactions |
| APK size | < 25 MB debug build |
| Minimum device | Any Android tablet or phone running Android 8+ |
| Sync latency | Changes visible on other devices within 15 minutes (next scheduled sync cycle) |

---

## 6. Out of Scope (v1.0)

- Web or iOS client
- Email or SMS notifications
- Payment integration
- Document OCR or automated data extraction
- Full-text document search
- Multi-organization support
- Biometric authentication

---

## 7. Glossary

| Term | Meaning |
|---|---|
| **Transaction** | A single import/procurement clearance case, uniquely identified by a transaction reference (e.g., RMS-2026-0042) |
| **Phase** | One of 7 major workflow stages a transaction must pass through |
| **PhaseRecord** | A sub-step within a phase, assigned to a specific entity, with its own SLA |
| **SLA** | Service Level Agreement — target number of days to complete a phase/sub-phase |
| **Hard Gate** | A system rule that absolutely prevents a transition if a prerequisite is unmet |
| **Exception State** | An overlay status (BLOCKED/ON_HOLD/DISPUTED) that pauses a transaction without losing its position |
| **Cursor** | The `updatedAt` timestamp used as a sync watermark to pull only changed records |
