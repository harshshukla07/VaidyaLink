from pydantic import BaseModel, Field


class CompletenessAssessment(BaseModel):
    """Whether patient info is enough to recommend a specialty."""

    has_enough_info: bool = Field(
        description=(
            "True if the main symptom plus duration, severity, pattern, or related "
            "symptoms is enough to recommend a specialty (prefer True / General "
            "Physician when unsure but non-emergency). False only if one critical "
            "detail is still missing."
        )
    )


class SpecialtyChoice(BaseModel):
    """Single specialty selected from the hospital allowlist."""

    specialty: str = Field(
        description="Exact specialty name from the provided allowlist, or General Physician if unsure."
    )


class FollowUpQuestion(BaseModel):
    """One focused clarifying question for the patient."""

    question: str = Field(
        description=(
            "One short follow-up (about 20 words) for the single most important "
            "missing detail: severity, duration, or one key associated symptom."
        )
    )
