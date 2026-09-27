import pytest
from fastapi.testclient import TestClient

from app.main import app


@pytest.fixture()
def client():
    with TestClient(app) as c:
        yield c


def test_health(client):
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_normal_invoice_scores_low(client):
    payload = {
        "amount": 2400,
        "vendorAgeDays": 900,
        "invoicesLast30Days": 2,
        "bankAccountChangedRecently": False,
        "duplicateCandidate": False,
        "amountZScore": 0.1,
    }
    response = client.post("/score", json=payload)
    assert response.status_code == 200
    body = response.json()
    assert 0 <= body["riskScore"] <= 100
    assert body["riskScore"] < 60


def test_bank_change_plus_large_amount_scores_high(client):
    payload = {
        "amount": 25000,
        "vendorAgeDays": 900,
        "invoicesLast30Days": 2,
        "bankAccountChangedRecently": True,
        "duplicateCandidate": False,
        "amountZScore": 5.2,
    }
    response = client.post("/score", json=payload)
    assert response.status_code == 200
    body = response.json()
    assert body["riskScore"] > 60
    assert body["anomaly"] is True
    assert len(body["topFactors"]) > 0


def test_retrain_endpoint(client):
    response = client.post("/admin/retrain")
    assert response.status_code == 200
