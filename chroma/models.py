from pydantic import BaseModel
from typing import List, Optional, Dict, Any

class ComponentData(BaseModel):
    id: str
    name: str
    type: str
    description: Optional[str] = None
    manufacturer: Optional[str] = None
    model: Optional[str] = None
    price: Optional[float] = None
    specifications: Dict[str, Any] = {}


class AdminKnowledge(BaseModel):
    content: str
    knowledge_type: str = "TRAINING"
    metadata: Dict[str, Any] = {}


class UserMessageData(BaseModel):
    id: str
    user_id: str
    user_message: str
    ai_response: str
    created_at: str


class SearchQuery(BaseModel):
    query: str
    collection: str = "components"
    n_results: int = 5
    where: Optional[Dict[str, Any]] = None


class SearchResponse(BaseModel):
    results: List[Dict[str, Any]]
    collection: str