# مشروع متتبع التخليص الجمركي — خطة التطوير متعددة المراحل
## Customs Clearance Transaction Tracker — Android App

> **الوثيقة المرجعية:** `Customs.docx` — دليل إجراءات مشروع تتبع معاملات التخليص الجمركي لعطاءات الخدمات الطبية الملكية
> **الجهة المستفيدة:** مديرية الصيدلة والتجهيزات الطبية — الخدمات الطبية الملكية الأردنية
> **المنصة المستهدفة:** Android (Kotlin + Jetpack Compose)
> **تاريخ إعداد الخطة:** 2026-06-29

---

## سياق المشروع (Context)

### المشكلة
توثيق معاملات التخليص الجمركي يُنجز حالياً بملفات ورقية وتنسيق هاتفي مباشر بلا سجل مركزي. أعلى نقطة اختناق هي مصادقة القيادة العامة للقوات المسلحة الأردنية على الإعفاء الجمركي (إلزامية في **كل** معاملة). لا توجد KPIs موثقة ولا تنبيهات للتأخر.

### المنهجية (من تجربة Wasfa)
استُخلصت الدروس التالية من بناء Wasfa v6 وWasfa X لتطبيقها هنا:
1. **مسار حالة واحد (Single Exit)** — مثل `_hakeem_confirm_one` في Wasfa، كل انتقال بين مراحل المعاملة يمر عبر دالة واحدة موثوقة
2. **فلترة الصلاحيات من اليوم صفر** — درس `_get_scope` في Wasfa كان مكلفاً حين أُضيف لاحقاً
3. **مخطط إضافي فقط (Additive Schema)** — لا حذف أو إعادة بناء للجداول، فقط `ALTER TABLE ADD COLUMN`
4. **سجل تدقيق كامل** — كل تغيير في حالة المعاملة يُسجَّل مع الطابع الزمني والمستخدم المنفِّذ
5. **عربي أولاً** — اللغة العربية هي اللغة الأساسية، تُبنى i18n والـ RTL من اليوم الأول
6. **SLA كبيانات أساسية** — أهداف الوقت تُخزَّن في قاعدة البيانات ولا تُحسَّب تبعياً
7. **Offline-first** — الاتصال في البيئة العسكرية غير مضمون، يُبنى الـ outbox pattern من البداية

---

## المكدس التقني (Stack)

### Android Client (Primary)
| المكوِّن | التقنية |
|---|---|
| اللغة | Kotlin 2.x |
| واجهة المستخدم | Jetpack Compose + Material Design 3 |
| معمارية | Clean Architecture · MVVM · UDF |
| قاعدة البيانات المحلية | Room (SQLite) |
| شبكة | Retrofit 2 + OkHttp + Kotlinx Serialization |
| حقن التبعيات | Hilt |
| التنقل | Navigation Compose |
| التزامن | Kotlin Coroutines + Flow |
| العمل الخلفي | WorkManager (sync, alerts) |
| الصور والمستندات | CameraX + Android MediaStore |
| التنبيهات | Local Notifications + FCM (Phase 6) |

### Backend (Companion API — Phase 3+)
| المكوِّن | التقنية |
|---|---|
| إطار العمل | FastAPI (Python) — نفس قاعدة Wasfa X |
| قاعدة البيانات | SQLite عبر SQLAlchemy (ترقية لـ PostgreSQL لاحقاً) |
| مهاجرات | Alembic (additive only) |
| المصادقة | JWT Bearer (HS256) · scrypt passwords |
| التغليف | PyInstaller أو Docker (TBD) |

> **ملاحظة تشغيلية:** في المرحلة الأولى يمكن تشغيل البيانات محلياً (Room only) وإضافة البيانات الخلفية في المرحلة 9.

---

## مخطط المراحل (Overview)

```
Phase 0  │ Foundation & Architecture
Phase 1  │ Data Layer (Room + State Machine)
Phase 2  │ Auth & RBAC
Phase 3  │ Core Transaction CRUD
Phase 4  │ Seven-Phase Workflow Engine
Phase 5  │ Document Management
Phase 6  │ Alerts & SLA Engine
Phase 7  │ Dashboard & KPIs
Phase 8  │ Reporting & Export
Phase 9  │ Backend API + Offline Sync
Phase 10 │ Admin, Polish & Packaging
```

---

## المرحلة 0 — الأسس والهيكل (Foundation & Architecture)

**الهدف:** إعداد المشروع، تعريف النموذج المعلوماتي الكامل، والاتفاق على البنية قبل كتابة كود تشغيلي.

### 0.1 إعداد مشروع Android
- إنشاء مشروع Android Studio (Kotlin, minSdk 26, Compose BOM)
- إعداد Hilt · Navigation · Room · Retrofit في `build.gradle`
- إعداد مجلدات Clean Architecture:
  ```
  app/
    data/
      local/         ← Room DAOs, Entities, TypeConverters
      remote/        ← Retrofit API interfaces, DTOs
      repository/    ← Repository implementations
    domain/
      model/         ← Domain entities (Transaction, Phase, etc.)
      repository/    ← Repository interfaces
      usecase/       ← Business logic use cases
    presentation/
      ui/            ← Compose screens
      viewmodel/     ← ViewModels
    di/              ← Hilt modules
  ```

### 0.2 تعريف النموذج المعلوماتي

#### الكيانات الأساسية (Domain Entities)

**`Transaction`** — المعاملة الواحدة
```
id                 UUID (Primary Key)
transactionRef     String (UNIQUE, e.g. "RMS-2026-0042")
title              String (وصف مختصر للعطاء)
tenderRef          String? (رقم العطاء)
contractRef        String? (رقم العقد)
supplierName       String
totalValue         Double?
currency           String (JOD default)
currentPhase       Enum(7 phases) 
currentStatus      Enum (see state machine)
priority           Enum (NORMAL, HIGH, URGENT)
createdAt          Long (epoch ms)
createdByUserId    String (FK → User)
updatedAt          Long
closedAt           Long?
notes              String?
```

**`PhaseRecord`** — سجل مرحلة واحدة من مراحل المعاملة
```
id                 UUID
transactionId      UUID (FK → Transaction)
phaseNumber        Int (1-7)
subPhase           String (e.g. "3.3.1", "3.4.1")
status             Enum (PENDING, IN_PROGRESS, BLOCKED, DONE, SKIPPED)
startedAt          Long?
completedAt        Long?
slaTargetDays      Int? (من إعداد المسؤول)
assignedToEntity   Enum (PHARMACY, TENDERS_COMMITTEE, CUSTOMS, JFDA, MILITARY_COMMAND, etc.)
blockerReason      String?
completedByUserId  String?
```

**`TransactionDocument`** — وثيقة مرفقة
```
id                 UUID
transactionId      UUID (FK → Transaction)
phaseRef           String (e.g. "3.3.1")
documentType       Enum (see document registry in Section 4 of Customs.docx)
filename           String
filePath           String (local URI)
remoteUrl          String? (بعد الرفع للسيرفر)
uploadedAt         Long
uploadedByUserId   String
isVerified         Boolean (default false)
notes              String?
```

**`ActivityLog`** — سجل تدقيق كامل
```
id                 UUID
transactionId      UUID
userId             String
action             Enum (CREATED, STATUS_CHANGED, DOC_UPLOADED, NOTE_ADDED, PHASE_ADVANCED, etc.)
fromStatus         String?
toStatus           String?
payload            String (JSON)
occurredAt         Long
```

**`SlaConfig`** — إعدادات أهداف الوقت (Admin)
```
id                 UUID
phaseNumber        Int
subPhase           String
targetDays         Int
escalationAfterDays Int
isActive           Boolean
```

**`User`**
```
id                 UUID
username           String (UNIQUE)
displayName        String
role               Enum (ADMIN, COORDINATOR, VIEWER, SUPERVISOR)
department         Enum (PHARMACY, TENDERS, MANAGEMENT)
passwordHash       String
isActive           Boolean
lastLoginAt        Long?
```

**`Notification`** (local alerts)
```
id                 UUID
transactionId      UUID
type               Enum (SLA_BREACH, PHASE_BLOCKED, DOC_MISSING, MANUAL)
message            String
isRead             Boolean
createdAt          Long
```

### 0.3 مخطط آلة الحالة (State Machine)

كل معاملة تمر بالحالات التالية بالترتيب:

```
DRAFT
  ↓
TENDER_PREPARATION         (Phase 1 — الصيدلة + لجنة العطاءات)
  ↓
TENDER_PUBLISHED
  ↓
EVALUATION_IN_PROGRESS     (Phase 2 — لجنة العطاءات)
  ↓
CONTRACT_PENDING_SIGNATURE
  ↓
CONTRACT_SIGNED            ← نقطة تحقق إلزامية
  ↓
CLEARANCE_DOCS_PREPARATION (Phase 3 — الصيدلة)
  ↓
DECLARATION_SUBMITTED      (Phase 3.3 — تقديم التصريح الجمركي)
  ↓
GOV_PROCESSING             (Phase 4 — متوازٍ: جمارك + JFDA + قيادة عامة)
  │
  ├── CUSTOMS_INSPECTION_PENDING
  ├── MILITARY_EXEMPTION_PENDING   ← أعلى نقطة اختناق
  ├── JFDA_APPROVAL_PENDING
  └── GOV_APPROVED                 ← كل المصادقات مكتملة
  ↓
FINAL_RELEASE_ISSUED       (Phase 5 — أمر الإفراج النهائي)
  ↓
IN_TRANSIT                 (Phase 6 — النقل)
  ↓
RECEIVED_AT_WAREHOUSE
  ↓
INSPECTION_COMPLETE        (Phase 6.3 — محضر الاستلام)
  ↓
FINANCIAL_SETTLEMENT_PENDING (Phase 7)
  ↓
CLOSED                     ← الحالة النهائية
```

حالات استثنائية موازية: `BLOCKED` · `ON_HOLD` · `DISPUTED`

**قاعدة مشتقة من Wasfa:** كل انتقال حالة يمر عبر `TransactionStateMachine.advance()` — دالة واحدة تُدقق صحة الانتقال وتكتب `ActivityLog` وتُحدِّث `updatedAt` أتومياً (Room transaction).

### 0.4 نقاط إلزامية (Hard Gates)
مشتقة مباشرة من الوثيقة — لا يسمح النظام بتجاوزها:
1. لا تقديم للتصريح الجمركي قبل `CONTRACT_SIGNED`
2. لا إفراج جمركي بدون مصادقة القيادة العامة
3. لا تحريك شحنة قبل `FINAL_RELEASE_ISSUED`
4. لا إغلاق مالي بدون محضر استلام موقَّع من لجنة الاستلام

**الحجم التقديري:** 3–5 أيام عمل

---

## المرحلة 1 — طبقة البيانات (Data Layer)

**الهدف:** بناء Room schema، DAOs، وRepository layer مع tests.

### 1.1 Room Database
- تعريف `@Entity` لكل كيان (Transaction, PhaseRecord, TransactionDocument, ActivityLog, SlaConfig, User, Notification)
- `TypeConverters` للـ Enums والـ UUID
- `@Database` مع version=1، مهاجرات إضافية فقط لاحقاً

### 1.2 DAOs
```kotlin
// المبدأ: استخدم Flow<> لكل استعلام مقروء (reactive)
// استخدم suspend fun لكل كتابة (coroutine-safe)
// لا SQL خارج DAOs (درس Wasfa Rule 2)

@Dao interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<TransactionEntity>>
    
    @Query("SELECT * FROM transactions WHERE currentStatus IN (:statuses)")
    fun observeByStatus(statuses: List<String>): Flow<List<TransactionEntity>>
    
    @Transaction
    suspend fun advanceState(transactionId: UUID, newStatus: String, log: ActivityLogEntity)
    
    // ... CRUD + KPI queries
}
```

### 1.3 Repository Implementations
- `TransactionRepository` — حالات المعاملات + state machine
- `DocumentRepository` — رفع/حذف المستندات
- `SlaRepository` — تحميل الإعدادات + حساب التأخر
- `UserRepository` — المصادقة + الأدوار

### 1.4 Unit Tests
- اختبار كل DAO مع in-memory Room database
- اختبار كل انتقال حالة في `TransactionStateMachine`
- اختبار الـ Hard Gates (تأكيد رفض الانتقالات غير المسموحة)

**الحجم التقديري:** 4–6 أيام عمل

---

## المرحلة 2 — المصادقة والصلاحيات (Auth & RBAC)

**الهدف:** نظام تسجيل دخول محلي مع RBAC كامل (مشتق من مصفوفة RACI في الوثيقة).

### 2.1 نموذج الأدوار (Roles)

| الدور | المقابل في RACI | الصلاحيات |
|---|---|---|
| `ADMIN` | المدير العام | كل العمليات + إدارة المستخدمين + SLA config |
| `COORDINATOR` | منسّق الصيدلة (R في أغلب الأنشطة) | إنشاء/تعديل/تقديم/تتبع المعاملات |
| `SUPERVISOR` | مدير مديرية الصيدلة | قراءة كاملة + اعتماد الإغلاق |
| `VIEWER` | الجهات الأخرى (I في RACI) | قراءة فقط + تقارير |

**القاعدة المشتقة من Wasfa:** كل Composable screen تتحقق من الدور عبر `LocalUserRole.current` قبل عرض أزرار التعديل.

### 2.2 المصادقة
- تسجيل دخول بكلمة مرور محلية (scrypt hashing عبر Bouncy Castle)
- رمز جلسة JWT في `EncryptedSharedPreferences`
- تسجيل خروج تلقائي بعد 8 ساعات (بيئة عمل مكتبي عسكري)
- نقطة تحقق: أول تشغيل يطلب من المسؤول إنشاء حساب admin بكلمة مرور عشوائية معروضة مرة واحدة (درس Wasfa SEC-03)

### 2.3 Guard Composables
```kotlin
@Composable fun RequireRole(
    requiredRole: UserRole,
    content: @Composable () -> Unit
) {
    val currentRole = LocalUserSession.current?.role
    if (currentRole != null && currentRole.level >= requiredRole.level) {
        content()
    }
}
```

**الحجم التقديري:** 3–4 أيام عمل

---

## المرحلة 3 — إدارة المعاملات الأساسية (Core Transaction CRUD)

**الهدف:** شاشات إنشاء، قائمة، وتفاصيل المعاملة.

### 3.1 شاشة القائمة (Transaction List)
- قائمة lazy مع Card لكل معاملة
- بطاقة المعاملة تعرض: رقم المرجع، اسم المورّد، المرحلة الحالية، أيام منذ آخر تحديث، مؤشر تأخر ملوَّن
- فلترة: حسب المرحلة / الحالة / الأولوية / الجهة المعنية
- بحث بالرقم المرجعي أو اسم المورّد
- Floating Action Button لإنشاء معاملة جديدة (COORDINATOR فقط)

### 3.2 شاشة إنشاء معاملة (Create Transaction)
- حقول: عنوان، رقم العطاء، اسم المورّد، القيمة، ملاحظات
- رقم المرجع يُولَّد تلقائياً: `RMS-YYYY-NNNN`
- تحقق من صحة المدخلات قبل الحفظ
- يبدأ في حالة `TENDER_PREPARATION`

### 3.3 شاشة تفاصيل المعاملة (Transaction Detail)
- Header: الرقم المرجعي + الحالة + مؤشر SLA
- Timeline للمراحل المكتملة والجارية والقادمة
- شريط الإجراءات: "تقديم للمرحلة التالية" / "تعليق" / "تبليغ عن عائق"
- تبويبات: التفاصيل | المستندات | السجل (Activity Log) | الملاحظات

### 3.4 انتقال المراحل (Phase Transition)
- Dialog تأكيد لكل انتقال مع التحقق من Hard Gates
- إذا كانت وثائق مطلوبة غير مرفوعة → تحذير مع إمكانية المتابعة (لكن يُسجَّل ناقص)
- كل انتقال ينشئ `ActivityLog` record

**الحجم التقديري:** 5–7 أيام عمل

---

## المرحلة 4 — محرك سير العمل متعدد المراحل (Seven-Phase Workflow Engine)

**الهدف:** تمثيل المراحل السبع وتتبع الإجراءات الفرعية بما فيها المسارات المتوازية في المرحلة 4.

### 4.1 عرض المراحل (Phase Timeline UI)
Compose component يعرض:
```
◉ المرحلة 1 — تحضير المناقصة ✅  (مكتملة - 12 يوم)
◉ المرحلة 2 — الإحالة والعقد  ✅  (مكتملة - 8 أيام)
◉ المرحلة 3 — ملف التخليص    ✅  (مكتملة - 4 أيام)
⟳ المرحلة 4 — الجهات الحكومية 🔴  (جارية - 18 يوم / SLA: 15)
  ├── ✅ دائرة الجمارك — الفحص (5 أيام)
  ├── ⟳ القيادة العامة — الإعفاء (18 يوم - متأخر 3 أيام)
  └── ✅ مؤسسة الغذاء والدواء (6 أيام)
○ المرحلة 5 — أمر الإفراج      ⟳ (معلقة)
○ المرحلة 6 — النقل والاستلام  ○ (لم تبدأ)
○ المرحلة 7 — الإغلاق المالي   ○ (لم تبدأ)
```

### 4.2 المرحلة الرابعة (أكثر مرحلة تعقيداً)
تُشغَّل 3 مسارات **متوازية**:

**مسار 1: دائرة الجمارك**
- تقديم التصريح → تسجيل رقم التصريح
- الفحص الجمركي → تاريخ البدء/الانتهاء
- تحديد الإعفاء أو الرسوم
- تقرير المعاينة (إن وجد)

**مسار 2: القيادة العامة للقوات المسلحة (الأكثر حساسية)**
- إرسال طلب الإعفاء → تاريخ الإرسال
- انتظار المصادقة → عداد أيام + تنبيه عند تجاوز الحد
- استلام المصادقة → تاريخ الاستلام
- نقطة تنسيق (Focal Point) محفوظة في جهات الاتصال

**مسار 3: مؤسسة الغذاء والدواء (JFDA)**
- تقديم طلب إذن الاستيراد → تاريخ التقديم
- مراجعة المؤسسة → تسجيل الملاحظات
- إصدار الإذن → تاريخ الإصدار

**المرحلة 4 لا تُعتبر منتهية حتى تكتمل جميع المسارات الثلاثة.**

### 4.3 عداد الأيام المباشر
كل `PhaseRecord` نشط يعرض:
- "بدأت منذ X أيام"
- إذا تجاوز `slaTargetDays`: "متأخر X أيام عن الهدف" بلون أحمر
- شريط تقدم مرئي

**الحجم التقديري:** 6–8 أيام عمل

---

## المرحلة 5 — إدارة المستندات (Document Management)

**الهدف:** رفع، عرض، وإدارة الوثائق السبعة عشر المعرَّفة في الوثيقة (القسم 4).

### 5.1 قائمة الوثائق المطلوبة (Document Registry)
تُبنى من القسم 4 في `Customs.docx`:
```kotlin
enum class DocumentType(
    val nameAr: String,
    val nameEn: String,
    val phaseRef: String,
    val issuingEntity: String,
    val isMandatory: Boolean
) {
    NEEDS_ASSESSMENT("كشف الاحتياج الدوائي", "Needs Assessment", "1", "PHARMACY", true),
    APPROVED_TENDER_DOC("وثيقة المناقصة المعتمدة", "Approved Tender Doc", "1", "TENDERS", true),
    ENVELOPE_OPENING_MINUTES("محضر فتح المظاريف", "Opening Minutes", "2", "TENDERS", true),
    SIGNED_CONTRACT("العقد الموقَّع", "Signed Contract", "2", "TENDERS+SUPPLIER", true),
    COMMERCIAL_INVOICE("الفاتورة التجارية", "Commercial Invoice", "3", "SUPPLIER", true),
    PACKING_LIST("قائمة البيان", "Packing List", "3", "SUPPLIER", true),
    CERTIFICATE_OF_ORIGIN("شهادة المنشأ", "Certificate of Origin", "3", "SUPPLIER", true),
    BILL_OF_LADING("بوليصة الشحن", "Bill of Lading / AWB", "3", "SHIPPING_CO", true),
    CUSTOMS_DECLARATION("التصريح الجمركي", "Customs Declaration", "3-4", "PHARMACY+CUSTOMS", true),
    EXEMPTION_REQUEST("طلب الإعفاء الجمركي", "Exemption Request", "4", "PHARMACY", true),
    EXEMPTION_APPROVAL("مصادقة الإعفاء الجمركي", "Exemption Approval", "4", "MILITARY_COMMAND", true),
    JFDA_IMPORT_PERMIT("إذن الاستيراد الدوائي", "Drug Import Permit", "4", "JFDA", true),
    CUSTOMS_INSPECTION_REPORT("تقرير المعاينة الجمركية", "Inspection Report", "4", "CUSTOMS", false),
    FINAL_RELEASE_ORDER("أمر الإفراج الجمركي النهائي", "Final Release Order", "5", "CUSTOMS", true),
    DUTIES_RECEIPT("إيصال سداد الرسوم", "Duties Receipt", "5", "CUSTOMS", false),
    RECEIPT_MINUTES("محضر الاستلام والفحص النهائي", "Receipt Minutes", "6", "RECEPTION_COMMITTEE", true),
    PAYMENT_REQUEST_DOCS("مستندات طلب الصرف المالي", "Payment Docs", "7", "PHARMACY+FINANCE", true),
}
```

### 5.2 قائمة تحقق وثائق المرحلة (Checklist per Phase)
شاشة تعرض لكل مرحلة:
- الوثائق المطلوبة مع حالة كل وثيقة (✅ مرفوعة | ⚠️ ناقصة | — غير منطبقة)
- زر "رفع وثيقة" لكل بند ناقص

### 5.3 رفع المستندات (Document Upload)
**مصادر الرفع:**
1. الكاميرا (CameraX) — التقاط الوثيقة الورقية
2. معرض الصور
3. منتقي الملفات (PDF, JPEG, PNG)

**المعالجة:**
- ضغط تلقائي للصور (max 2MB)
- حفظ في `Context.getFilesDir()` (لا MediaStore — بيانات حساسة)
- تسجيل في `TransactionDocument` مع `uploadedAt` و`uploadedByUserId`
- سجل activity: `DOC_UPLOADED`

### 5.4 عارض المستندات
- عرض PDF في WebView داخلي أو PdfRenderer
- عرض الصور مع تكبير/تصغير (Zoomable Compose)
- زر مشاركة (محجوب افتراضياً للـ VIEWER)

**الحجم التقديري:** 5–7 أيام عمل

---

## المرحلة 6 — محرك التنبيهات وSLA (Alerts & SLA Engine)

**الهدف:** تنبيهات تلقائية عند تجاوز أهداف الوقت — الأداة الأساسية لمحاربة التأخر.

### 6.1 إعداد SLA (Admin only)
شاشة إعداد تتيح للمسؤول تحديد:
- عدد أيام الهدف لكل مرحلة/مرحلة فرعية
- عدد أيام التصعيد (escalation) — بعدها تُرسَل تنبيهات أكثر إلحاحاً
- تشغيل/إيقاف تنبيهات كل مرحلة

مخزَّنة في جدول `SlaConfig` ← درس Wasfa: السياسات في قاعدة البيانات لا في الكود.

### 6.2 WorkManager Job
`SlaCheckerWorker` يُشغَّل كل 6 ساعات:
```kotlin
class SlaCheckerWorker(...) : CoroutineWorker(...) {
    override suspend fun doWork(): Result {
        val activePhases = phaseRepository.getActivePhases()
        activePhases.forEach { phase ->
            val sla = slaRepository.getConfig(phase.phaseNumber, phase.subPhase)
            val daysSinceStart = daysBetween(phase.startedAt, now())
            if (sla != null && daysSinceStart > sla.targetDays) {
                notificationRepository.createAlert(
                    transactionId = phase.transactionId,
                    type = if (daysSinceStart > sla.escalationAfterDays) 
                           NotificationType.SLA_ESCALATED 
                           else NotificationType.SLA_BREACH,
                    message = buildSlaMessage(phase, daysSinceStart, sla)
                )
            }
        }
        return Result.success()
    }
}
```

### 6.3 مركز التنبيهات (Notification Center)
- شاشة قائمة التنبيهات (Bell icon في Topbar مع Badge)
- تصفية: غير مقروء | SLA | موثق | كل
- الضغط على تنبيه → ينتقل لتفاصيل المعاملة
- أرشفة / تحديد كمقروء

### 6.4 Android Notifications
- `NotificationManager` + Channel لكل نوع تنبيه
- Deep link → شاشة التفاصيل عند الضغط
- تنبيهات إلزامية (HIGH priority) لمعاملات متأخرة +7 أيام عن السلك

**الحجم التقديري:** 4–6 أيام عمل

---

## المرحلة 7 — لوحة المتابعة والمؤشرات (Dashboard & KPIs)

**الهدف:** الشاشة الرئيسية التي تعرض حالة النظام بالكامل دفعة واحدة.

### 7.1 بطاقات ملخص المعاملات
```
┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐
│  إجمالي نشطة   │  │  متأخرة (SLA)   │  │  مغلقة هذا شهر │
│       14        │  │    🔴  3         │  │       7         │
└─────────────────┘  └─────────────────┘  └─────────────────┘
```

### 7.2 توزيع المعاملات حسب المرحلة
رسم بياني شريطي أفقي يعرض:
- تحضير العطاء: N
- قيد التخليص الجمركي: N
- قيد مصادقة الإعفاء (القيادة العامة): N 🔴
- قيد موافقة JFDA: N
- قيد النقل: N
- مستلمة — قيد الإغلاق المالي: N

### 7.3 KPIs (من القسم 7.1 في الوثيقة)
| المؤشر | طريقة الحساب |
|---|---|
| متوسط إجمالي زمن المعاملة | `avg(closedAt - createdAt)` على المعاملات المغلقة |
| متوسط زمن مصادقة القيادة العامة | `avg(PhaseRecord.completedAt - startedAt)` للمسار العسكري |
| متوسط زمن التخليص الجمركي | نفس المنطق لمسار الجمارك |
| نسبة المعاملات المتأخرة | `count(delayed) / count(total) * 100` |
| نسبة معاملات معفاة | `count(EXEMPTED) / count(total) * 100` |
| حالات نقص/تلف عند الاستلام | `count(ReceiptMinutes.hasDefects = true)` |

### 7.4 قائمة المعاملات المتأخرة (Priority View)
قائمة مرتبة تنازلياً حسب عدد أيام التأخر:
```
🔴 RMS-2026-0031 — شركة الدواء العربية — مصادقة القيادة العامة — متأخرة 12 يوم
🔴 RMS-2026-0028 — مورّد المستلزمات — فحص جمركي — متأخرة 8 أيام
🟡 RMS-2026-0035 — الشركة الدولية — إذن JFDA — متأخرة 2 يوم
```

**الحجم التقديري:** 4–5 أيام عمل

---

## المرحلة 8 — التقارير والتصدير (Reporting & Export)

**الهدف:** التقارير الدورية المنصوص عليها في القسم 7.3 من الوثيقة.

### 8.1 أنواع التقارير

**تقرير أسبوعي تشغيلي**
- جدول كل معاملة نشطة مع: رقم المرجع، المرحلة الحالية، أيام منذ آخر تحديث، حالة SLA
- مصدر: `TransactionRepository.getActiveForWeeklyReport()`

**تقرير شهري تحليلي**
- KPIs للشهر مقارنةً بالشهر السابق
- متوسطات زمن كل مرحلة
- نسب التأخر حسب الجهة
- رسوم بيانية

**تقرير للمدير العام (Executive)**
- ملخص تنفيذي: عدد المعاملات، متوسط الوقت، نسبة التأخر
- أكبر 3 مسببات للتأخر
- توصيات تلقائية (مثلاً: "القيادة العامة تسبّبت في 67% من التأخيرات هذا الشهر")

### 8.2 تصدير PDF
- استخدام `iTextG` (Android PDF library) أو `PdfDocument` API
- نموذج PDF ثابت مع هيدر الخدمات الطبية الملكية
- تصدير إلى `Downloads/` أو مشاركة مباشرة

### 8.3 تصدير Excel/CSV
- `Apache POI` أو CSV builder
- مناسب للتسليم لجهات خارجية

**الحجم التقديري:** 4–5 أيام عمل

---

## المرحلة 9 — الخلفية وMزامنة المشترين (Backend API + Offline Sync)

**الهدف:** بناء REST API (FastAPI) لدعم تعدد المستخدمين والمزامنة بين أجهزة مختلفة.

> **ملاحظة:** هذه المرحلة اختيارية في الإصدار الأول. يمكن الإطلاق بشكل محلي (Room only) أولاً.

### 9.1 FastAPI Backend
بناءً على خبرة Wasfa X:
- `app/models/` — SQLAlchemy models (مطابقة للـ Room entities)
- `app/api/` — FastAPI routers (transactions, phases, documents, users, reports)
- `app/services/` — business logic (StateMachine, SlaChecker, ReportGenerator)
- `migrations/` — Alembic (additive only)
- مصادقة JWT bearer + scrypt passwords

### 9.2 Outbox Pattern (من Wasfa X P8)
الأندرويد يُخزِّن التغييرات محلياً في جدول `OutboxEvent`:
```
id          UUID
type        Enum (CREATE_TX, ADVANCE_PHASE, UPLOAD_DOC, etc.)
payload     String (JSON)
createdAt   Long
syncedAt    Long?
attempts    Int
```

`SyncWorker` يُشغَّل عند عودة الاتصال:
1. يقرأ الأحداث غير المتزامنة
2. يرفعها للـ API بالترتيب
3. يحدِّث `syncedAt` عند النجاح
4. يزيد `attempts` عند الفشل (max 5 → يُبلَّغ المستخدم)

### 9.3 Conflict Resolution
- السيرفر دائماً هو مصدر الحقيقة (Server wins)
- على الأندرويد: إذا تعارض سجل محلي مع سجل من السيرفر → يُعرض تنبيه للمستخدم

### 9.4 مؤشر الاتصال
شريط دقيق في أعلى التطبيق:
- 🟢 متصل (بيانات محدَّثة)
- 🟡 غير متصل (عمل محلي - X حدث قيد الانتظار)
- 🔴 خطأ في المزامنة (تفاصيل عند الضغط)

**الحجم التقديري:** 8–12 يوم عمل

---

## المرحلة 10 — الإدارة والتلميع والتغليف (Admin, Polish & Packaging)

**الهدف:** إكمال الميزات الإدارية، تلميع الواجهة، وإعداد ملف APK/AAB للتوزيع.

### 10.1 لوحة الإدارة (Admin)
- إدارة المستخدمين (إنشاء، تعطيل، تغيير كلمة المرور، الأدوار)
- إدارة أهداف SLA
- دليل جهات الاتصال (الملحق ج) — CRUD لنقاط تنسيق الجهات
- ضبط إعدادات التطبيق (مدة الجلسة، تردد المزامنة)
- نسخ احتياطي/استعادة لقاعدة البيانات المحلية

### 10.2 دليل الإجراءات داخل التطبيق
- وصول لنص الوثيقة (PDF viewer) من داخل التطبيق
- "مساعدة سريعة" في كل شاشة توضّح ما يجب فعله في هذه المرحلة

### 10.3 التلميع البصري
- Dark mode + Light mode عبر Material 3 Dynamic Color
- RTL كامل (العربية افتراضياً، English متاح)
- Density المضغوطة للشاشات الصغيرة
- Animations (crossfade بين الشاشات، transition للمراحل)
- Accessible: contentDescription على كل Icon، minimum touch target 48dp

### 10.4 التوزيع (Distribution)
**للاستخدام الداخلي (Internal Distribution):**
- APK موقَّع (Debug keystore أولاً، Production keystore للتسليم)
- يُوزَّع عبر USB أو شبكة LAN الداخلية
- `minSdk 26` (Android 8.0+) لتغطية الغالبية

**لمتطلبات موسَّعة (إذا طُلب لاحقاً):**
- Google Play Internal Testing Track
- MDM (Mobile Device Management) deployment

**الحجم التقديري:** 4–6 أيام عمل

---

## جدول زمني إجمالي (Timeline Summary)

| المرحلة | الوصف | الحجم التقديري |
|---|---|---|
| 0 | Foundation & Architecture | 3–5 أيام |
| 1 | Data Layer (Room + State Machine) | 4–6 أيام |
| 2 | Auth & RBAC | 3–4 أيام |
| 3 | Core Transaction CRUD | 5–7 أيام |
| 4 | Seven-Phase Workflow Engine | 6–8 أيام |
| 5 | Document Management | 5–7 أيام |
| 6 | Alerts & SLA Engine | 4–6 أيام |
| 7 | Dashboard & KPIs | 4–5 أيام |
| 8 | Reporting & Export | 4–5 أيام |
| 9 | Backend API + Offline Sync | 8–12 أيام |
| 10 | Admin, Polish & Packaging | 4–6 أيام |
| **المجموع** | | **50–71 يوم عمل** |

> **MVP (Phases 0–8):** تطبيق محلي كامل بدون سيرفر — حوالي 38–53 يوم عمل
> **Full (Phases 0–10):** تطبيق متعدد المستخدمين مع سيرفر ومزامنة — حوالي 50–71 يوم

---

## قرارات مفتوحة (Open Decisions)

هذه النقاط تحتاج قرارك قبل البدء:

| # | القرار | الخيارات |
|---|---|---|
| D1 | هل يعمل التطبيق على جهاز واحد أم متعدد الأجهزة؟ | محلي فقط (أسهل) ← سيرفر + مزامنة (أقوى) |
| D2 | من يستخدم التطبيق؟ | موظفو الصيدلة فقط، أم الجهات الأخرى (جمارك/JFDA) أيضاً؟ |
| D3 | هل تتبع الشحنة أثناء النقل فعلي (GPS)؟ | لا (اليدوي كافٍ) ← نعم (يضيف تعقيداً) |
| D4 | ما حجم الأرشيف المتوقع؟ | عشرات المعاملات/سنة؟ مئات؟ |
| D5 | هل اللغة الإنجليزية مطلوبة؟ | عربي فقط ← ثنائي (AR/EN) |

---

## ملاحظة ختامية

هذه الخطة مبنية على مبادئ Wasfa ومُكيَّفة للبيئة العسكرية اللوجستية. أهم الفوارق عن Wasfa:
- **الجمهور**: موظفون حكوميون عسكريون — التصميم يجب أن يكون صريحاً ومنظَّماً لا إبداعياً
- **البيانات**: حساسة — لا تحميل للسحابة العامة بدون موافقة صريحة
- **الاتصال**: غير مضمون — offline-first ليس ميزة بل ضرورة
- **المساءلة**: كل إجراء موثَّق بالمستخدم والوقت — audit trail غير قابل للتعديل

---

*الخطة جاهزة للمراجعة والتأكيد. بعد الموافقة، نبدأ بالمرحلة 0.*
