from typing import Optional, TypedDict


class TriageState(TypedDict):
    session_id: int
    messages: list[dict]
    ai_reply: str
    is_complete: bool
    recommended_specialty: Optional[str]
    is_emergency: bool
    is_off_topic: bool
    has_enough_info: bool
    allowed_specialties: list[str]
