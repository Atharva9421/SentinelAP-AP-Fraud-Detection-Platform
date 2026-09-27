"""Feature schema shared between training-data generation and the live /score endpoint.

Keeping this in one place means the synthetic training generator and the
request-time feature vector can never silently drift apart.
"""
from __future__ import annotations

import numpy as np

FEATURE_NAMES = [
    "amount",
    "vendor_age_days",
    "invoices_last_30_days",
    "bank_account_changed_recently",
    "is_duplicate_candidate",
    "amount_zscore",
]

# Human-readable explanation shown to the analyst when a feature is a top
# contributor to an anomaly score.
FEATURE_LABELS = {
    "amount": "unusually large invoice amount",
    "vendor_age_days": "vendor relationship is very new",
    "invoices_last_30_days": "abnormal invoice volume from this vendor recently",
    "bank_account_changed_recently": "vendor's bank account changed recently",
    "is_duplicate_candidate": "resembles a recent duplicate invoice",
    "amount_zscore": "amount is a statistical outlier vs. this vendor's history",
}


def to_vector(
    amount: float,
    vendor_age_days: float,
    invoices_last_30_days: float,
    bank_account_changed_recently: bool,
    is_duplicate_candidate: bool,
    amount_zscore: float,
) -> np.ndarray:
    return np.array(
        [
            amount,
            vendor_age_days,
            invoices_last_30_days,
            1.0 if bank_account_changed_recently else 0.0,
            1.0 if is_duplicate_candidate else 0.0,
            amount_zscore,
        ],
        dtype=float,
    )
