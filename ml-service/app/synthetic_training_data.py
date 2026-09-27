"""Generates a synthetic AP invoice dataset for training the anomaly model.

The isolation forest is trained mostly on "normal" invoices (drawn from
distributions that mimic ordinary B2B invoicing behavior) plus a small
fraction of clearly anomalous invoices, so the model learns a realistic
decision boundary instead of flagging everything as normal (which is what
happens if you train only on clean data with zero contamination signal).
"""
from __future__ import annotations

import numpy as np

from app.features import to_vector

RANDOM_SEED = 7


def generate_training_set(n_normal: int = 4000, n_anomalous: int = 200) -> np.ndarray:
    rng = np.random.default_rng(RANDOM_SEED)
    rows = []

    for _ in range(n_normal):
        amount = float(np.clip(rng.normal(2500, 1200), 50, 15000))
        vendor_age_days = float(rng.uniform(180, 2500))
        invoices_last_30_days = float(rng.poisson(2))
        bank_changed = rng.random() < 0.03
        is_dup = rng.random() < 0.01
        amount_z = float(rng.normal(0, 1))
        rows.append(to_vector(amount, vendor_age_days, invoices_last_30_days, bank_changed, is_dup, amount_z))

    for _ in range(n_anomalous):
        pattern = rng.integers(0, 3)
        if pattern == 0:
            # Large invoice shortly after a bank-account change.
            amount = float(rng.uniform(8000, 40000))
            vendor_age_days = float(rng.uniform(180, 2500))
            invoices_last_30_days = float(rng.poisson(2))
            bank_changed = True
            is_dup = False
            amount_z = float(rng.uniform(2.5, 6))
        elif pattern == 1:
            # Brand-new vendor immediately billing a large amount.
            amount = float(rng.uniform(5000, 20000))
            vendor_age_days = float(rng.uniform(0, 14))
            invoices_last_30_days = float(rng.poisson(1))
            bank_changed = False
            is_dup = False
            amount_z = float(rng.uniform(1.5, 4))
        else:
            # Velocity spike + duplicate-like billing.
            amount = float(np.clip(rng.normal(2500, 1200), 50, 15000))
            vendor_age_days = float(rng.uniform(180, 2500))
            invoices_last_30_days = float(rng.uniform(9, 20))
            bank_changed = False
            is_dup = True
            amount_z = float(rng.normal(0.5, 1))

        rows.append(to_vector(amount, vendor_age_days, invoices_last_30_days, bank_changed, is_dup, amount_z))

    return np.vstack(rows)
