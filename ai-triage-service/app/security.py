import os
import secrets

from fastapi import Header, HTTPException


def verify_api_key(x_api_key: str | None = Header(default=None, alias="X-API-Key")) -> None:
    """
    Require X-API-Key when AI_TRIAGE_API_KEY is configured.
    If the env var is empty, auth is skipped (local/dev convenience).
    """
    expected = (os.getenv("AI_TRIAGE_API_KEY") or "").strip()
    if not expected:
        return

    provided = (x_api_key or "").strip()
    if not provided or not secrets.compare_digest(provided, expected):
        raise HTTPException(
            status_code=401,
            detail="Invalid or missing API key",
        )
