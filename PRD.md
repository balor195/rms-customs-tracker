# Product Requirements Document
## RMS Customs Clearance Transaction Tracker

**Organization:** Royal Medical Services — Directorate of Pharmacy & Medical Equipment  
**الجهة:** الخدمات الطبية الملكية — مديرية الصيدلة والتجهيزات الطبية  
**Document version:** 1.0  
**Date:** 2026-06-29 (last updated 2026-07-05 — workflow/roles/field overhaul, see inline `2026-07-05` annotations and `PROGRESS.md`)  
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

*(Updated 2026-07-05 — role model and division scoping overhauled; see below.)*

*(Updated again 2026-07-05 — added `TENDER_OFFICER`; `ADMIN`/`CLEARANCE`/`WAREHOUSE` accounts no longer belong to any division at all.)*

| Role | Arabic | Permissions | Division scope |
|---|---|---|---|
| **ADMIN** | مسؤول النظام | Full access: create/manage users, configure SLAs, view all data, export | No division (belongs to none) |
| **CLEARANCE** | التخليص | The only role (besides ADMIN) that can mark a transaction "تم التخليص" (clearance issued) | No division (belongs to none) |
| **WAREHOUSE** | المستودعات | The only role (besides ADMIN) that can confirm "تم النقل الى المستودعات" (transferred to warehouses) | No division (belongs to none), but only sees transactions that have already been cleared |
| **SUPERVISOR** | مشرف | Create/edit transactions, approve phase transitions, export reports | Own division (شعبة) only |
| **TENDER_OFFICER** | ضابط العطاء | Create/edit transactions and upload documents — no approval or export rights | Own division (شعبة) only |
| **VIEWER** | مستعرض | Read-only access to transaction list and detail | Own division (شعبة) only |

A Supervisor, Tender Officer, or Viewer can only see and create transactions within their own division; a Supervisor in the Pharmacy division (شعبة الدواء), for example, cannot see or create transactions belonging to the Medical Consumables or Medical Devices divisions. Admin, Clearance, and Warehouse accounts are not assigned to any division at account-creation time.

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

*(Updated 2026-07-05, twice — first simplified from 7 to 5 phases; later that day, the old Phase 3 "Transit & Warehouse Receipt" and Phase 5 "Transferred to Warehouses" were recognized as describing the same event and merged, leaving 4 phases.)*

Each transaction follows a strict 4-phase linear workflow enforced by a state machine. Unauthorized out-of-order transitions are rejected at the domain layer.

**Phases:**

| # | Arabic | English |
|---|---|---|
| 1 | تحضير المناقصة وإصدارها | Tender Preparation & Publication |
| 2 | طلب تخليص | Clearance Request |
| 3 | إغلاق المعاملة والتسوية المالية | Transaction Closing & Financial Settlement |
| 4 | تم النقل الى المستودعات | Transferred to Warehouses — final confirmation checkbox; closes the transaction and displays it in red |

**Hard gates enforced by the system:**
- Cannot confirm Phase 4 ("تم النقل الى المستودعات") before financial closing (Phase 3) is complete
- Only the `WAREHOUSE` role (or `ADMIN`) may perform the Phase 4 confirmation; only `CLEARANCE` (or `ADMIN`) may issue Phase 2 clearance

**Exception overlays:** A transaction can be flagged BLOCKED, ON_HOLD, or DISPUTED at any point without losing its position in the workflow. Resolving the exception restores the transaction to its last phase.

### 4.2 Phase 4 Parallel Tracks *(removed 2026-07-05)*

> The former Phase 4 (Gov-Agency Processing) — which required simultaneous parallel approvals from Armed Forces Command, Jordan Customs, and Jordan FDA — was removed from the workflow entirely, along with its supporting sub-approval tracking. It is no longer part of the product.

### 4.3 SLA Monitoring & Alerts *(automated checker removed 2026-07-05)*

> The background SLA-breach checker (6-hourly scan + Android/in-app notifications) was tied entirely to the removed Phase-4 approval tracks and was removed along with them rather than left non-functional. SLA targets remain configurable by ADMIN through the SLA Configuration screen as reference data, but there is currently no automated breach/escalation alerting. A future SLA engine would need a new per-phase timestamp mechanism, since the sub-phase tracking it previously relied on no longer exists.

### 4.4 Document Management

Each transaction has an attached documents section. Supported actions:
- Upload documents from device storage or camera capture (CameraX)
- Categorize by document type (e.g., customs declaration, release order, contract, inspection minutes)
- Each document is stored locally with the transaction; not synced to the backend (local-only)

### 4.5 Dashboard & Reporting

**Dashboard** shows at-a-glance KPIs, scoped to the viewer's division (see §2):
- Transactions by status (active, closed this month)
- Transactions per phase distribution
- Shipment status breakdown and value-by-division

> *(2026-07-05)* SLA compliance rate and overdue-transaction indicators were removed from the dashboard along with the Phase-4 subsystem they depended on (§4.2/§4.3).

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

### 4.9 Tender Intake Fields & Business Rules *(added 2026-07-05)*

When creating a new transaction, officers can additionally record:
- **Weight (كغم)** — numeric shipment weight
- **Refrigerated (مبرّدة / غير مبرّدة)** — whether the shipment requires refrigeration
- **Default shelf life (العمر الافتراضي)** — free-text shelf-life note, shown only for the Medical Consumables division (شعبة المستهلكات الطبية)

**Business rule:** The "عاجل" (Urgent) priority can only be selected when the beneficiary is الخدمات الطبية الملكية (RMS) **and** the shipment is refrigerated. This is enforced both in the create form (the option is disabled otherwise) and as a hard validation rule.

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
| **Phase** | One of 5 major workflow stages a transaction must pass through (reduced from 7 on 2026-07-05) |
| **Division (شعبة)** | The medical division a transaction belongs to (Pharmacy, Medical Consumables, or Medical Devices); Supervisor/Viewer accounts are confined to their own division |
| **SLA** | Service Level Agreement — target number of days to complete a phase; configurable as reference data, though the automated breach-checker has been removed (see §4.3) |
| **Hard Gate** | A system rule that absolutely prevents a transition if a prerequisite is unmet |
| **Exception State** | An overlay status (BLOCKED/ON_HOLD/DISPUTED) that pauses a transaction without losing its position |
| **Cursor** | The `updatedAt` timestamp used as a sync watermark to pull only changed records |
