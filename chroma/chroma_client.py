import chromadb
import os
from chromadb.config import Settings

# Initialize ChromaDB
PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(PROJECT_ROOT, "chroma_db")
chroma_client = chromadb.PersistentClient(path=DB_PATH)

# Three separate collections:
components_collection = chroma_client.get_or_create_collection(
    name="pc_components",
    metadata={"description": "PC components from database"}
)

admin_knowledge_collection = chroma_client.get_or_create_collection(
    name="admin_knowledge",
    metadata={"description": "Admin training data and instructions"}
)

user_messages_collection = chroma_client.get_or_create_collection(
    name="user_messages",
    metadata={"description": "Last 50 chat messages per user for context"}
)