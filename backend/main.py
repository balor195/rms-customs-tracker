from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from database import engine, Base
from routers import sync
import models  # noqa: F401 — ensures models are registered with metadata

# Create all tables on startup
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="RMS Customs Sync API",
    description="مديرية الصيدلة والتجهيزات الطبية — مزامنة بيانات التخليص الجمركي",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(sync.router)


@app.get("/health")
def health():
    return {"status": "ok", "service": "rms-customs-sync"}
