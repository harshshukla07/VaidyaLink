from typing import TypedDict, Optional

class TriageState(TypedDict):
    session_id: int
    messages: list[dict]
    ai_reply: str
    is_complete: bool
    recommended_specialty: Optional[str]
    is_emergency: bool
    has_enough_info: bool
    allowed_specialties: list[str]
    
