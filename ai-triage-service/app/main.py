from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.errors import LlmServiceError, TriageValidationError
from app.graph.builder import graph
from app.schemas.triage import TriageRequest, TriageResponse

app = FastAPI(
    title="VaidyaLink AI Triage Service",
    description="LangGraph-powered medical triage microservice",
    version="0.1.0",
)


def _validate_triage_request(request: TriageRequest) -> None:
    if not request.messages:
        raise TriageValidationError("messages must not be empty")

    patient_texts = [
        (m.message_text or "").strip()
        for m in request.messages
        if m.sender_type == "PATIENT"
    ]
    if not any(patient_texts):
        raise TriageValidationError(
            "at least one non-blank PATIENT message is required"
        )


@app.exception_handler(TriageValidationError)
async def triage_validation_handler(
    _request: Request, exc: TriageValidationError
) -> JSONResponse:
    return JSONResponse(status_code=400, content={"detail": str(exc)})


@app.exception_handler(LlmServiceError)
async def llm_service_handler(
    _request: Request, exc: LlmServiceError
) -> JSONResponse:
    return JSONResponse(status_code=503, content={"detail": str(exc)})


@app.get("/health")
def health_check():
    return {
        "status": "UP",
        "service": "ai-triage-service",
        "version": "0.1.0",
    }


@app.post("/api/ai/triage")
def triage(request: TriageRequest) -> TriageResponse:
    _validate_triage_request(request)

    initial_state = {
        "session_id": request.session_id,
        "messages": [m.model_dump() for m in request.messages],
        "ai_reply": "",
        "is_complete": False,
        "recommended_specialty": None,
        "is_emergency": False,
        "has_enough_info": False,
        "allowed_specialties": request.allowed_specialties,
    }

    try:
        result = graph.invoke(initial_state)
    except LlmServiceError:
        raise
    except Exception as exc:
        raise LlmServiceError(f"Triage graph failed: {exc}") from exc

    return TriageResponse(
        ai_reply=result["ai_reply"],
        is_complete=result["is_complete"],
        recommended_specialty=result["recommended_specialty"],
    )
