from fastapi import APIRouter, HTTPException
from models import SearchQuery, SearchResponse
from chroma_client import components_collection, admin_knowledge_collection, user_messages_collection

router = APIRouter()

@router.post("/search")
async def search_knowledge(search_query: SearchQuery):
    """Search with optional filtering"""
    try:
        collection_map = {
            "components": components_collection,
            "admin_knowledge": admin_knowledge_collection,
            "user_messages": user_messages_collection
        }

        if search_query.collection not in collection_map:
            raise HTTPException(status_code=400, detail="Invalid collection type")

        collection = collection_map[search_query.collection]

        # Use where filter if provided
        results = collection.query(
            query_texts=[search_query.query],
            n_results=search_query.n_results,
            where=search_query.where
        )

        formatted_results = []
        if results['documents']:
            for i, doc in enumerate(results['documents'][0]):
                formatted_results.append({
                    "document": doc,
                    "metadata": results['metadatas'][0][i],
                    "distance": results['distances'][0][i] if results['distances'] else None,
                    "id": results['ids'][0][i]
                })

        return SearchResponse(
            results=formatted_results,
            collection=search_query.collection
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error searching: {str(e)}")