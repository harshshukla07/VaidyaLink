from unittest.mock import patch

import pytest

from app.errors import LlmServiceError
from app.graph.nodes import (
    assess_completeness,
    emergency_response,
    generate_followup,
    route_specialty,
    safety_check,
)
from app.schemas.llm import SpecialtyChoice
from tests.conftest import FakeLLM


def test_safety_check_detects_emergency(patient_state):
    patient_state["messages"][0]["message_text"] = "I have severe chest pain"
    assert safety_check(patient_state)["is_emergency"] is True


def test_safety_check_clears_non_emergency(patient_state):
    assert safety_check(patient_state)["is_emergency"] is False


def test_emergency_response_completes_session(patient_state):
    result = emergency_response(patient_state)
    assert result["is_complete"] is True
    assert result["recommended_specialty"] == "Emergency"
    assert "urgent" in result["ai_reply"].lower()


def test_assess_completeness_uses_structured_bool(patient_state, completeness_true):
    with patch("app.graph.nodes.get_llm", return_value=FakeLLM(completeness_true)):
        result = assess_completeness(patient_state)
    assert result["has_enough_info"] is True


def test_generate_followup_uses_structured_question(patient_state, followup_question):
    with patch("app.graph.nodes.get_llm", return_value=FakeLLM(followup_question)):
        result = generate_followup(patient_state)
    assert result["is_complete"] is False
    assert result["recommended_specialty"] is None
    assert "severe" in result["ai_reply"].lower()


def test_route_specialty_accepts_allowlist_match(patient_state):
    llm = FakeLLM(SpecialtyChoice(specialty="Dermatology"))
    with patch("app.graph.nodes.get_llm", return_value=llm):
        result = route_specialty(patient_state)
    assert result["recommended_specialty"] == "Dermatology"
    assert result["is_complete"] is True


def test_route_specialty_falls_back_when_not_on_allowlist(patient_state):
    llm = FakeLLM(SpecialtyChoice(specialty="Neurology"))
    with patch("app.graph.nodes.get_llm", return_value=llm):
        result = route_specialty(patient_state)
    assert result["recommended_specialty"] == "General Physician"


def test_llm_failure_becomes_llm_service_error(patient_state):
    llm = FakeLLM(RuntimeError("provider down"))
    with patch("app.graph.nodes.get_llm", return_value=llm):
        with pytest.raises(LlmServiceError, match="LLM call failed"):
            assess_completeness(patient_state)
