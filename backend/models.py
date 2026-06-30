from sqlalchemy import Column, String, Integer, Float, BigInteger, Boolean, ForeignKey
from sqlalchemy.orm import relationship
from database import Base


class Transaction(Base):
    __tablename__ = "transactions"

    id                = Column(String, primary_key=True, index=True)
    transaction_ref   = Column(String, unique=True, index=True)
    title             = Column(String, nullable=False)
    tender_ref        = Column(String, nullable=True)
    contract_ref      = Column(String, nullable=True)
    supplier_name     = Column(String, nullable=False)
    total_value       = Column(Float, nullable=True)
    currency          = Column(String, default="JOD")
    current_phase     = Column(String, nullable=False)
    current_status    = Column(String, nullable=False)
    exception_state   = Column(String, nullable=True)
    priority          = Column(String, nullable=False)
    created_at        = Column(BigInteger, nullable=False)
    created_by_user_id= Column(String, nullable=False)
    updated_at        = Column(BigInteger, nullable=False)
    closed_at         = Column(BigInteger, nullable=True)
    notes             = Column(String, nullable=True)
    server_updated_at = Column(BigInteger, nullable=False, default=0, index=True)

    phase_records = relationship("PhaseRecord", back_populates="transaction",
                                 cascade="all, delete-orphan")


class PhaseRecord(Base):
    __tablename__ = "phase_records"

    id                 = Column(String, primary_key=True, index=True)
    transaction_id     = Column(String, ForeignKey("transactions.id", ondelete="CASCADE"), nullable=False)
    phase_number       = Column(Integer, nullable=False)
    sub_phase          = Column(String, nullable=False)
    status             = Column(String, nullable=False)
    assigned_to_entity = Column(String, nullable=False)
    started_at         = Column(BigInteger, nullable=True)
    completed_at       = Column(BigInteger, nullable=True)
    sla_target_days    = Column(Integer, nullable=True)
    blocker_reason     = Column(String, nullable=True)
    completed_by_user_id = Column(String, nullable=True)
    notes              = Column(String, nullable=True)

    transaction = relationship("Transaction", back_populates="phase_records")
