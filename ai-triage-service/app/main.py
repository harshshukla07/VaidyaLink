from fastapi import FastAPI
from app.graph.builder import graph
from app.schemas.triage import TriageRequest, TriageResponse

app = FastAPI(
    title="VaidyaLink AI Triage Service",
    description="LangGraph-powered medical triage microservice",
    version="0.1.0",
)


@app.get("/health")
def health_check():
    
    return {
        "status": "UP",
        "service": "ai-triage-service",
        "version": "0.1.0",

    }

@app.post("/api/ai/triage")
def triage(request: TriageRequest) -> TriageResponse:
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
    result = graph.invoke(initial_state)
    return TriageResponse(
        ai_reply=result["ai_reply"],
        is_complete=result["is_complete"],
        recommended_specialty=result["recommended_specialty"],
    )
