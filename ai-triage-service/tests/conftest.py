import pytest

from app.schemas.llm import CompletenessAssessment, FollowUpQuestion, SpecialtyChoice


class FakeStructuredLLM:
    def __init__(self, result):
        self._result = result
        self.prompts: list[str] = []

    def invoke(self, prompt: str):
        self.prompts.append(prompt)
        if isinstance(self._result, Exception):
            raise self._result
        return self._result


class FakeLLM:
    def __init__(self, result):
        self._result = result
        self.structured = FakeStructuredLLM(result)

    def with_structured_output(self, _schema):
        return self.structured


@pytest.fixture
def patient_state():
    return {
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
        "allowed_specialties": [
            "Cardiologist",
            "Dermatology",
            "General Physician",
        ],
    }


@pytest.fixture
def completeness_true():
    return CompletenessAssessment(has_enough_info=True)


@pytest.fixture
def completeness_false():
    return CompletenessAssessment(has_enough_info=False)


@pytest.fixture
def specialty_gp():
    return SpecialtyChoice(specialty="General Physician")


@pytest.fixture
def followup_question():
    return FollowUpQuestion(question="How severe is the headache on a scale of 1–10?")
