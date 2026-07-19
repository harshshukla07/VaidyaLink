from langgraph.graph import END, START, StateGraph

from app.graph.nodes import (
    assess_completeness,
    emergency_response,
    generate_followup,
    off_topic_response,
    route_specialty,
    safety_check,
    topic_guard,
)
from app.graph.state import TriageState


def route_after_safety(state: TriageState) -> str:
    """Emergency overrides everything; otherwise run topic guard."""
    if state.get("is_emergency"):
        return "emergency_response"
    return "topic_guard"


def route_after_topic_guard(state: TriageState) -> str:
    """Jailbreak / off-topic short-circuit before any LLM call."""
    if state.get("is_off_topic"):
        return "off_topic_response"
    return "assess_completeness"


def route_after_assess(state: TriageState) -> str:
    """Use the flag set by assess_completeness (LLM)."""
    if state.get("has_enough_info"):
        return "route_specialty"
    return "generate_followup"


workflow = StateGraph(TriageState)

workflow.add_node("safety_check", safety_check)
workflow.add_node("emergency_response", emergency_response)
workflow.add_node("topic_guard", topic_guard)
workflow.add_node("off_topic_response", off_topic_response)
workflow.add_node("assess_completeness", assess_completeness)
workflow.add_node("generate_followup", generate_followup)
workflow.add_node("route_specialty", route_specialty)

workflow.add_edge(START, "safety_check")

workflow.add_conditional_edges(
    "safety_check",
    route_after_safety,
    {
        "emergency_response": "emergency_response",
        "topic_guard": "topic_guard",
    },
)

workflow.add_conditional_edges(
    "topic_guard",
    route_after_topic_guard,
    {
        "off_topic_response": "off_topic_response",
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
workflow.add_edge("off_topic_response", END)
workflow.add_edge("generate_followup", END)
workflow.add_edge("route_specialty", END)

graph = workflow.compile()
