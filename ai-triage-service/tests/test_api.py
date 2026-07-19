from unittest.mock import patch

from fastapi.testclient import TestClient

from app.errors import LlmServiceError
from app.main import app
from app.schemas.llm import CompletenessAssessment, SpecialtyChoice
from tests.conftest import FakeLLM

client = TestClient(app)


def test_health():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "UP"


def test_triage_rejects_empty_messages():
    response = client.post(
        "/api/ai/triage",
        json={"sessionId": 1, "messages": [], "allowedSpecialties": []},
    )
    assert response.status_code == 400
    assert "empty" in response.json()["detail"].lower()


def test_triage_rejects_blank_patient_message():
    response = client.post(
        "/api/ai/triage",
        json={
            "sessionId": 1,
            "messages": [
                {"id": 1, "senderType": "PATIENT", "messageText": "   "},
            ],
            "allowedSpecialties": ["General Physician"],
        },
    )
    assert response.status_code == 400


def test_triage_returns_503_on_llm_failure():
    with patch(
        "app.graph.nodes.get_llm",
        side_effect=LlmServiceError("OPENAI_API_KEY is not set"),
    ):
        response = client.post(
            "/api/ai/triage",
            json={
                "sessionId": 1,
                "messages": [
                    {
                        "id": 1,
                        "senderType": "PATIENT",
                        "messageText": "I have a mild rash on my arm",
                    }
                ],
                "allowedSpecialties": ["Dermatology", "General Physician"],
            },
        )
    assert response.status_code == 503
    assert "OPENAI_API_KEY" in response.json()["detail"]


def test_triage_success_path():
    with patch(
        "app.graph.nodes.get_llm",
        side_effect=[
            FakeLLM(CompletenessAssessment(has_enough_info=True)),
            FakeLLM(SpecialtyChoice(specialty="Dermatology")),
        ],
    ):
        response = client.post(
            "/api/ai/triage",
            json={
                "sessionId": 1,
                "messages": [
                    {
                        "id": 1,
                        "senderType": "PATIENT",
                        "messageText": "I have a mild rash on my arm for a week",
                    }
                ],
                "allowedSpecialties": ["Dermatology", "General Physician"],
            },
        )

    assert response.status_code == 200
    body = response.json()
    assert body["is_complete"] is True
    assert body["recommended_specialty"] == "Dermatology"


def test_triage_emergency_without_llm():
    response = client.post(
        "/api/ai/triage",
        json={
            "sessionId": 1,
            "messages": [
                {
                    "id": 1,
                    "senderType": "PATIENT",
                    "messageText": "Sudden chest pain and cannot breathe",
                }
            ],
            "allowedSpecialties": ["General Physician"],
        },
    )
    assert response.status_code == 200
    body = response.json()
    assert body["is_complete"] is True
    assert body["recommended_specialty"] == "Emergency"
