from app.errors import LlmServiceError
from app.graph.constants import DEFAULT_SPECIALTY, EMERGENCY_KEYWORDS
from app.graph.state import TriageState
from app.llm.client import get_llm
from app.schemas.llm import CompletenessAssessment, FollowUpQuestion, SpecialtyChoice


def _patient_messages(state: TriageState) -> list[dict]:
    return [m for m in state["messages"] if m.get("sender_type") == "PATIENT"]


def _all_patient_text(state: TriageState) -> str:
    return " ".join(
        m["message_text"] for m in _patient_messages(state) if m.get("message_text")
    ).lower()


def _invoke_structured(schema: type, prompt: str):
    """Call the LLM with a Pydantic schema; wrap provider failures."""
    try:
        llm = get_llm().with_structured_output(schema)
        return llm.invoke(prompt)
    except LlmServiceError:
        raise
    except Exception as exc:
        raise LlmServiceError(f"LLM call failed: {exc}") from exc


def _match_allowed_specialty(raw: str, allowed: list[str]) -> str | None:
    """Return the allowlist entry that matches raw (case-insensitive), or None."""
    cleaned = raw.strip().strip('"').strip("'")
    for specialty in allowed:
        if cleaned.lower() == specialty.lower():
            return specialty
    return None


def safety_check(state: TriageState) -> dict:
    text = _all_patient_text(state)

    is_emergency = False
    for keyword in EMERGENCY_KEYWORDS:
        if keyword in text:
            is_emergency = True
            break
    return {
        "is_emergency": is_emergency,
    }


def emergency_response(state: TriageState) -> dict:
    return {
        "ai_reply": (
            "Your symptoms may need urgent care. Please seek emergency medical help "
            "or call local emergency services immediately."
        ),
        "is_complete": True,
        "recommended_specialty": "Emergency",
        "is_emergency": True,
    }


def assess_completeness(state: TriageState) -> dict:
    text = _all_patient_text(state)
    allowed = state.get("allowed_specialties") or []
    specialties_text = ", ".join(allowed) if allowed else "the hospital specialty list"

    prompt = (
        "You are an AI triage assistant for a medical microservice. "
        f'Given the following patient-provided information: "{text}" '
        f"decide if this is enough to recommend one specialty from: {specialties_text}, "
        "or if you need one more clarifying question."
    )
    result: CompletenessAssessment = _invoke_structured(CompletenessAssessment, prompt)
    return {
        "has_enough_info": bool(result.has_enough_info),
    }


def generate_followup(state: TriageState) -> dict:
    messages = _all_patient_text(state)
    prompt = (
        "You are a triage assistant. The patient has said: "
        f'"{messages}". '
        "Generate a specific follow-up question about the symptoms described. "
        "Do not ask general or vague questions. Focus on clarifying details such as "
        "severity, onset, progression, or related factors about the mentioned symptom(s)."
    )
    result: FollowUpQuestion = _invoke_structured(FollowUpQuestion, prompt)
    return {
        "ai_reply": result.question.strip(),
        "is_complete": False,
        "recommended_specialty": None,
    }


def route_specialty(state: TriageState) -> dict:
    """
    LLM chooses one specialty from Java's allowlist.
    If unsure / invalid → General Physician (DEFAULT_SPECIALTY).
    """
    text = _all_patient_text(state)
    allowed = list(state.get("allowed_specialties") or [])

    if DEFAULT_SPECIALTY not in allowed:
        allowed.append(DEFAULT_SPECIALTY)

    specialties_text = ", ".join(allowed)
    prompt = (
        "You are a medical triage assistant (not a doctor). "
        "Given the patient information below, choose the single best specialty "
        "from this hospital allowlist ONLY:\n"
        f"{specialties_text}\n\n"
        f'Patient information: "{text}"\n\n'
        "Rules:\n"
        "- The specialty must match one item from the allowlist exactly.\n"
        f"- If you are not sure, use exactly: {DEFAULT_SPECIALTY}\n"
    )

    result: SpecialtyChoice = _invoke_structured(SpecialtyChoice, prompt)
    suggested = _match_allowed_specialty(result.specialty, allowed)
    if suggested is None:
        suggested = DEFAULT_SPECIALTY

    return {
        "ai_reply": (
            f"Based on your symptoms, I recommend seeing a {suggested}. "
            "You can book an appointment now."
        ),
        "is_complete": True,
        "recommended_specialty": suggested,
    }
