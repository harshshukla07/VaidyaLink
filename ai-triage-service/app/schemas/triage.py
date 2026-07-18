from typing import Optional

from pydantic import BaseModel, ConfigDict, Field


class MessageDTO(BaseModel):

    model_config = ConfigDict(populate_by_name=True)

    id: int
    sender_type: str = Field(alias="senderType")
    message_text: str = Field(alias="messageText")


class TriageRequest(BaseModel):

    model_config = ConfigDict(populate_by_name=True)

    session_id: int = Field(alias="sessionId")
    messages: list[MessageDTO]
    allowed_specialties: list[str] = Field(default_factory=list, alias="allowedSpecialties")


class TriageResponse(BaseModel):

    ai_reply: str
    is_complete: bool = False
    recommended_specialty: Optional[str] = None
