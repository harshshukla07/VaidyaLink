from pydantic import BaseModel, Field


class CompletenessAssessment(BaseModel):
    """Whether patient info is enough to recommend a specialty."""

    has_enough_info: bool = Field(
        description="True if symptoms are sufficient to recommend one specialty; False if one clarifying question is still needed."
    )


class SpecialtyChoice(BaseModel):
    """Single specialty selected from the hospital allowlist."""

    specialty: str = Field(
        description="Exact specialty name from the provided allowlist, or General Physician if unsure."
    )


class FollowUpQuestion(BaseModel):
    """One focused clarifying question for the patient."""

    question: str = Field(
        description="A specific follow-up question about severity, onset, progression, or related factors."
    )
