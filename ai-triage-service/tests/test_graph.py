from unittest.mock import patch

from app.graph.builder import graph
from app.schemas.llm import CompletenessAssessment, FollowUpQuestion, SpecialtyChoice
from tests.conftest import FakeLLM


def _base_state(**overrides):
    state = {
        "session_id": 1,
        "messages": [
            {
                "id": 1,
                "sender_type": "PATIENT",
                "message_text": "Mild headache for two days, no fever.",
            }
        ],
        "ai_reply": "",
        "is_complete": False,
        "recommended_specialty": None,
        "is_emergency": False,
        "has_enough_info": False,
        "allowed_specialties": ["Dermatology", "General Physician"],
    }
    state.update(overrides)
    return state


def test_graph_emergency_path_skips_llm():
    state = _base_state(
        messages=[
            {
                "id": 1,
                "sender_type": "PATIENT",
                "message_text": "I can't breathe and feel faint",
            }
        ]
    )
    result = graph.invoke(state)
    assert result["is_complete"] is True
    assert result["recommended_specialty"] == "Emergency"
    assert result["is_emergency"] is True


def test_graph_followup_when_incomplete():
    # assess and followup need different structured schemas — patch per call.
    with patch(
        "app.graph.nodes.get_llm",
        side_effect=[
            FakeLLM(CompletenessAssessment(has_enough_info=False)),
            FakeLLM(FollowUpQuestion(question="When did the headache start?")),
        ],
    ):
        result = graph.invoke(_base_state())

    assert result["is_complete"] is False
    assert result["recommended_specialty"] is None
    assert "start" in result["ai_reply"].lower()


def test_graph_routes_specialty_when_complete():
    with patch(
        "app.graph.nodes.get_llm",
        side_effect=[
            FakeLLM(CompletenessAssessment(has_enough_info=True)),
            FakeLLM(SpecialtyChoice(specialty="General Physician")),
        ],
    ):
        result = graph.invoke(_base_state())

    assert result["is_complete"] is True
    assert result["recommended_specialty"] == "General Physician"
    assert "General Physician" in result["ai_reply"]
