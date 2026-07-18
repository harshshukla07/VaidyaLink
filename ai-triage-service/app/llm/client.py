import os
from dotenv import load_dotenv
from langchain_openai import ChatOpenAI

load_dotenv()  

def get_llm()->ChatOpenAI:
    OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
    if not OPENAI_API_KEY:
        raise RuntimeError("OPENAI_API_KEY is not set in environment variables.")

    model = os.getenv("OPENAI_MODEL")
    if not model:
        raise RuntimeError("OPENAI_MODEL is not set in environment variables.")
    return ChatOpenAI(api_key=OPENAI_API_KEY, temperature=0.2, model=model)
