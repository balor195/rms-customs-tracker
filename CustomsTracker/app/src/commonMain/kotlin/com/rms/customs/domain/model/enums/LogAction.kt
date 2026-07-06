package com.rms.customs.domain.model.enums

enum class LogAction {
    CREATED,
    STATUS_CHANGED,
    PHASE_ADVANCED,
    DOC_UPLOADED,
    DOC_DELETED,
    NOTE_ADDED,
    PHASE_BLOCKED,
    PHASE_UNBLOCKED,
    SLA_BREACHED,
    PRIORITY_CHANGED,
    USER_ASSIGNED,
    EXCEPTION_SET,
    EXCEPTION_CLEARED,
    CLOSED;
}
