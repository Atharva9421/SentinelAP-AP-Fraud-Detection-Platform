"""Isolation-forest anomaly model for AP invoice fraud risk scoring.

Design notes:
- IsolationForest isolates outliers with fewer random partitions than normal
  points, so its `decision_function` output (higher = more normal) is
  min-max scaled and inverted into a 0-100 "risk score" that's easy for a
  non-technical reviewer to read.
- Isolation forests don't give native per-feature attribution. We approximate
  "top contributing factors" by z-scoring the incoming feature vector against
  the training set's mean/std and surfacing whichever raw features deviate
  the most - a lightweight, explainable stand-in for full SHAP-style
  attribution that's enough for an analyst-facing "why was this flagged".
"""
from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path
from typing import List

import joblib
import numpy as np
from sklearn.ensemble import IsolationForest

from app.features import FEATURE_LABELS, FEATURE_NAMES
from app.synthetic_training_data import generate_training_set

MODEL_PATH = Path(os.environ.get("MODEL_PATH", "model_store/isolation_forest.joblib"))
ANOMALY_RISK_FLOOR = 55.0  # below this, we don't bother surfacing "top factors"


@dataclass
class ScoreResult:
    risk_score: float
    is_anomaly: bool
    top_factors: List[str]


class AnomalyModel:
    def __init__(self) -> None:
        self._forest: IsolationForest | None = None
        self._feature_mean: np.ndarray | None = None
        self._feature_std: np.ndarray | None = None
        self._score_min: float = 0.0
        self._score_max: float = 1.0

    def load_or_train(self) -> None:
        if MODEL_PATH.exists():
            bundle = joblib.load(MODEL_PATH)
            self._forest = bundle["forest"]
            self._feature_mean = bundle["mean"]
            self._feature_std = bundle["std"]
            self._score_min = bundle["score_min"]
            self._score_max = bundle["score_max"]
            return
        self.train_and_persist()

    def train_and_persist(self) -> None:
        training_data = generate_training_set()
        self._feature_mean = training_data.mean(axis=0)
        self._feature_std = training_data.std(axis=0)
        self._feature_std[self._feature_std == 0] = 1.0

        forest = IsolationForest(
            n_estimators=200,
            contamination=0.05,
            random_state=7,
        )
        forest.fit(training_data)

        raw_scores = forest.decision_function(training_data)
        self._score_min = float(raw_scores.min())
        self._score_max = float(raw_scores.max())
        self._forest = forest

        MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
        joblib.dump(
            {
                "forest": forest,
                "mean": self._feature_mean,
                "std": self._feature_std,
                "score_min": self._score_min,
                "score_max": self._score_max,
            },
            MODEL_PATH,
        )

    def score(self, vector: np.ndarray) -> ScoreResult:
        if self._forest is None:
            raise RuntimeError("Model not loaded. Call load_or_train() first.")

        raw_score = self._forest.decision_function(vector.reshape(1, -1))[0]
        # Invert + min-max scale so LOWER raw isolation-forest score (more
        # anomalous) maps to a HIGHER risk score, clipped to [0, 100].
        span = max(self._score_max - self._score_min, 1e-9)
        normalized = (self._score_max - raw_score) / span
        risk_score = float(np.clip(normalized * 100, 0, 100))

        prediction = self._forest.predict(vector.reshape(1, -1))[0]
        is_anomaly = bool(prediction == -1)

        top_factors: List[str] = []
        if risk_score >= ANOMALY_RISK_FLOOR:
            z_scores = np.abs((vector - self._feature_mean) / self._feature_std)
            ranked = sorted(zip(FEATURE_NAMES, z_scores), key=lambda pair: pair[1], reverse=True)
            top_factors = [FEATURE_LABELS[name] for name, z in ranked[:2] if z > 1.0]
            if not top_factors:
                top_factors = ["combination of multiple moderately unusual factors"]

        return ScoreResult(risk_score=risk_score, is_anomaly=is_anomaly, top_factors=top_factors)
