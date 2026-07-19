class TriageValidationError(ValueError):
    """Invalid triage request (empty history, blank patient text, etc.)."""


class LlmServiceError(RuntimeError):
    """LLM provider failure or misconfiguration."""
