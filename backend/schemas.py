from __future__ import annotations
from typing import Optional, List
from pydantic import BaseModel


class PhaseRecordSchema(BaseModel):
    id: str
    transaction_id: str
    phase_number: int
    sub_phase: str
    status: str
    assigned_to_entity: str
    started_at: Optional[int] = None
    completed_at: Optional[int] = None
    sla_target_days: Optional[int] = None
    blocker_reason: Optional[str] = None
    completed_by_user_id: Optional[str] = None
    notes: Optional[str] = None

    class Config:
        from_attributes = True


class TransactionSchema(BaseModel):
    id: str
    transaction_ref: str
    title: str
    tender_ref: Optional[str] = None
    contract_ref: Optional[str] = None
    supplier_name: str
    total_value: Optional[float] = None
    currency: str = "JOD"
    current_phase: str
    current_status: str
    exception_state: Optional[str] = None
    priority: str
    created_at: int
    created_by_user_id: str
    updated_at: int
    closed_at: Optional[int] = None
    notes: Optional[str] = None
    phase_records: List[PhaseRecordSchema] = []

    class Config:
        from_attributes = True


class SyncPushRequest(BaseModel):
    device_id: str
    pushed_at: int
    transactions: List[TransactionSchema]


class SyncPushResponse(BaseModel):
    accepted: int


class SyncPullResponse(BaseModel):
    transactions: List[TransactionSchema]
    server_time_ms: int
