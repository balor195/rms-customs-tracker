import time
from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from database import get_db
from models import Transaction, PhaseRecord
from schemas import SyncPushRequest, SyncPushResponse, SyncPullResponse, TransactionSchema

router = APIRouter(prefix="/api/v1/sync", tags=["sync"])


@router.post("/push", response_model=SyncPushResponse)
def push(request: SyncPushRequest, db: Session = Depends(get_db)):
    now_ms = int(time.time() * 1000)
    accepted = 0

    for dto in request.transactions:
        tx = db.get(Transaction, dto.id)
        if tx is None:
            tx = Transaction(id=dto.id)
            db.add(tx)

        # last-write-wins: only update if incoming is newer
        if tx.updated_at is None or dto.updated_at >= tx.updated_at:
            tx.transaction_ref    = dto.transaction_ref
            tx.title              = dto.title
            tx.tender_ref         = dto.tender_ref
            tx.contract_ref       = dto.contract_ref
            tx.supplier_name      = dto.supplier_name
            tx.total_value        = dto.total_value
            tx.currency           = dto.currency
            tx.current_phase      = dto.current_phase
            tx.current_status     = dto.current_status
            tx.exception_state    = dto.exception_state
            tx.priority           = dto.priority
            tx.created_at         = dto.created_at
            tx.created_by_user_id = dto.created_by_user_id
            tx.updated_at         = dto.updated_at
            tx.closed_at          = dto.closed_at
            tx.notes              = dto.notes
            tx.server_updated_at  = now_ms

            # Upsert phase records
            for pdto in dto.phase_records:
                pr = db.get(PhaseRecord, pdto.id)
                if pr is None:
                    pr = PhaseRecord(id=pdto.id)
                    db.add(pr)
                pr.transaction_id       = pdto.transaction_id
                pr.phase_number         = pdto.phase_number
                pr.sub_phase            = pdto.sub_phase
                pr.status               = pdto.status
                pr.assigned_to_entity   = pdto.assigned_to_entity
                pr.started_at           = pdto.started_at
                pr.completed_at         = pdto.completed_at
                pr.sla_target_days      = pdto.sla_target_days
                pr.blocker_reason       = pdto.blocker_reason
                pr.completed_by_user_id = pdto.completed_by_user_id
                pr.notes                = pdto.notes

            accepted += 1

    db.commit()
    return SyncPushResponse(accepted=accepted)


@router.get("/pull", response_model=SyncPullResponse)
def pull(since: int = 0, device_id: str = "", db: Session = Depends(get_db)):
    now_ms = int(time.time() * 1000)
    txs = (
        db.query(Transaction)
        .filter(Transaction.server_updated_at > since)
        .all()
    )
    result = []
    for tx in txs:
        phases = [
            TransactionSchema.model_validate({"id": pr.id, "transaction_id": pr.transaction_id,
                "phase_number": pr.phase_number, "sub_phase": pr.sub_phase,
                "status": pr.status, "assigned_to_entity": pr.assigned_to_entity,
                "started_at": pr.started_at, "completed_at": pr.completed_at,
                "sla_target_days": pr.sla_target_days, "blocker_reason": pr.blocker_reason,
                "completed_by_user_id": pr.completed_by_user_id, "notes": pr.notes})
            for pr in tx.phase_records
        ]
        # build from ORM manually to include phase records
        tx_schema = TransactionSchema(
            id=tx.id, transaction_ref=tx.transaction_ref, title=tx.title,
            tender_ref=tx.tender_ref, contract_ref=tx.contract_ref,
            supplier_name=tx.supplier_name, total_value=tx.total_value,
            currency=tx.currency, current_phase=tx.current_phase,
            current_status=tx.current_status, exception_state=tx.exception_state,
            priority=tx.priority, created_at=tx.created_at,
            created_by_user_id=tx.created_by_user_id, updated_at=tx.updated_at,
            closed_at=tx.closed_at, notes=tx.notes,
            phase_records=[],
        )
        for pr in tx.phase_records:
            from schemas import PhaseRecordSchema
            tx_schema.phase_records.append(PhaseRecordSchema(
                id=pr.id, transaction_id=pr.transaction_id,
                phase_number=pr.phase_number, sub_phase=pr.sub_phase,
                status=pr.status, assigned_to_entity=pr.assigned_to_entity,
                started_at=pr.started_at, completed_at=pr.completed_at,
                sla_target_days=pr.sla_target_days, blocker_reason=pr.blocker_reason,
                completed_by_user_id=pr.completed_by_user_id, notes=pr.notes,
            ))
        result.append(tx_schema)

    return SyncPullResponse(transactions=result, server_time_ms=now_ms)
