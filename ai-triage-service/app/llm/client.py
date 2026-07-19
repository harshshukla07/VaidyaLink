import os

from dotenv import load_dotenv
from langchain_openai import ChatOpenAI

from app.errors import LlmServiceError

load_dotenv()


def get_llm() -> ChatOpenAI:
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        raise LlmServiceError("OPENAI_API_KEY is not set")

    model = os.getenv("OPENAI_MODEL", "gpt-4o-mini")
    return ChatOpenAI(api_key=api_key, temperature=0.2, model=model)
