from __future__ import annotations

from contextlib import asynccontextmanager

from fastapi import FastAPI
from pydantic import BaseModel, Field

from app.features import to_vector
from app.model import AnomalyModel

model = AnomalyModel()


@asynccontextmanager
async def lifespan(app: FastAPI):
    model.load_or_train()
    yield


app = FastAPI(
    title="SentinelAP ML Scoring Service",
    description="Isolation-forest anomaly scoring for AP invoice fraud risk.",
    version="0.1.0",
    lifespan=lifespan,
)


class RiskScoreRequest(BaseModel):
    amount: float = Field(..., ge=0)
    vendorAgeDays: float = Field(..., ge=0)
    invoicesLast30Days: float = Field(..., ge=0)
    bankAccountChangedRecently: bool
    duplicateCandidate: bool
    amountZScore: float


class RiskScoreResponse(BaseModel):
    riskScore: float
    anomaly: bool
    topFactors: list[str]


@app.get("/health")
def health() -> dict:
    return {"status": "ok"}


@app.post("/score", response_model=RiskScoreResponse)
def score(request: RiskScoreRequest) -> RiskScoreResponse:
    vector = to_vector(
        amount=request.amount,
        vendor_age_days=request.vendorAgeDays,
        invoices_last_30_days=request.invoicesLast30Days,
        bank_account_changed_recently=request.bankAccountChangedRecently,
        is_duplicate_candidate=request.duplicateCandidate,
        amount_zscore=request.amountZScore,
    )
    result = model.score(vector)
    return RiskScoreResponse(
        riskScore=round(result.risk_score, 2),
        anomaly=result.is_anomaly,
        topFactors=result.top_factors,
    )


@app.post("/admin/retrain")
def retrain() -> dict:
    """Retrains the model from freshly-generated synthetic data. Exposed mainly
    for the demo/CI pipeline; in a real deployment this would instead retrain
    on real historical invoice outcomes on a schedule."""
    model.train_and_persist()
    return {"status": "retrained"}
