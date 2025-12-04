from fastapi import APIRouter, HTTPException
from chroma_client import components_collection, admin_knowledge_collection, user_messages_collection

router = APIRouter()

@router.get("/collections/info")
async def get_collections_info():
    """Get information about collections"""
    try:
        components_count = components_collection.count()
        admin_knowledge_count = admin_knowledge_collection.count()
        user_messages_count = user_messages_collection.count()

        return {
            "components_collection": {
                "count": components_count,
                "description": "PC components from database"
            },
            "admin_knowledge_collection": {
                "count": admin_knowledge_count,
                "description": "Admin training data and instructions (global)"
            },
            "user_messages_collection": {
                "count": user_messages_count,
                "description": "Last 50 chat messages per user for context"
            }
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error getting collection info: {str(e)}")