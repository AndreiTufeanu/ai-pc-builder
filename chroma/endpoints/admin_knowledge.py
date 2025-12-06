from fastapi import APIRouter, HTTPException
import uuid
from datetime import datetime
from models import AdminKnowledge
import utils
from chroma_client import admin_knowledge_collection

router = APIRouter()

@router.post("/knowledge")
async def add_admin_knowledge(knowledge: AdminKnowledge):
    """Add admin training/knowledge to separate collection (no user ID)"""
    try:
        knowledge_id = str(uuid.uuid4())

        admin_knowledge_collection.add(
            documents=[knowledge.content],
            metadatas=[{
                "knowledge_type": knowledge.knowledge_type,
                "source": "admin_training",
                **knowledge.metadata,
            }],
            ids=[knowledge_id]
        )

        return {
            "message": "Admin knowledge added successfully",
            "knowledge_id": knowledge_id
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error adding admin knowledge: {str(e)}")


@router.delete("/knowledge/clear")
async def clear_admin_knowledge():
    """Clear all admin knowledge from ChromaDB"""
    try:
        # Get all admin knowledge IDs
        all_data = admin_knowledge_collection.get()
        if all_data['ids']:
            admin_knowledge_collection.delete(ids=all_data['ids'])
            return {"message": f"Cleared {len(all_data['ids'])} admin knowledge entries"}
        return {"message": "No admin knowledge to clear"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error clearing admin knowledge: {str(e)}")