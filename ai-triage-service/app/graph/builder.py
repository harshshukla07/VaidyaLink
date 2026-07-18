from langgraph.graph import StateGraph, START, END

from app.graph.state import TriageState
from app.graph.nodes import (
    safety_check,
    emergency_response,
    assess_completeness,
    generate_followup,
    route_specialty,
)


def route_after_safety(state: TriageState) -> str:
    """Emergency overrides everything; otherwise ask the LLM assess node."""
    if state.get("is_emergency"):
        return "emergency_response"
    return "assess_completeness"


def route_after_assess(state: TriageState) -> str:
    """Use the flag set by assess_completeness (LLM)."""
    if state.get("has_enough_info"):
        return "route_specialty"
    return "generate_followup"


workflow = StateGraph(TriageState)

workflow.add_node("safety_check", safety_check)
workflow.add_node("emergency_response", emergency_response)
workflow.add_node("assess_completeness", assess_completeness)
workflow.add_node("generate_followup", generate_followup)
workflow.add_node("route_specialty", route_specialty)

workflow.add_edge(START, "safety_check")

workflow.add_conditional_edges(
    "safety_check",
    route_after_safety,
    {
        "emergency_response": "emergency_response",
        "assess_completeness": "assess_completeness",
    },
)

workflow.add_conditional_edges(
    "assess_completeness",
    route_after_assess,
    {
        "route_specialty": "route_specialty",
        "generate_followup": "generate_followup",
    },
)

workflow.add_edge("emergency_response", END)
workflow.add_edge("generate_followup", END)
workflow.add_edge("route_specialty", END)

graph = workflow.compile()
