from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import chromadb
from chromadb.config import Settings
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
import uuid
import json
from datetime import datetime

app = FastAPI(title="PC Builder ChromaDB Server")

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize ChromaDB
chroma_client = chromadb.PersistentClient(path="./chroma_db")

# Three separate collections:
components_collection = chroma_client.get_or_create_collection(
    name="pc_components",
    metadata={"description": "PC components from database"}
)

admin_knowledge_collection = chroma_client.get_or_create_collection(
    name="admin_knowledge",
    metadata={"description": "Admin training data and instructions"}
)

# NEW: Collection for user chat context (last 50 messages per user)
user_messages_collection = chroma_client.get_or_create_collection(
    name="user_messages",
    metadata={"description": "Last 50 chat messages per user for context"}
)


# Pydantic models
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
    # Removed user_id from metadata - admin knowledge is global
    metadata: Dict[str, Any] = {}


# NEW: Model for user messages
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


class SearchResponse(BaseModel):
    results: List[Dict[str, Any]]
    collection: str


def flatten_specifications(specs: Dict[str, Any]) -> Dict[str, Any]:
    """Flatten nested specifications for ChromaDB metadata"""
    flattened = {}

    for key, value in specs.items():
        if value is None:
            flattened[key] = None
        elif isinstance(value, (str, int, float, bool)):
            flattened[key] = value
        elif isinstance(value, dict):
            # Convert nested dict to JSON string
            flattened[key] = json.dumps(value)
        elif isinstance(value, list):
            # Convert list to JSON string
            flattened[key] = json.dumps(value)
        else:
            # Convert any other type to string
            flattened[key] = str(value)

    return flattened


@app.get("/")
async def root():
    return {"message": "PC Builder ChromaDB Server", "status": "running"}


# NEW: Cleanup endpoints for startup
@app.post("/cleanup/startup")
async def cleanup_on_startup():
    """Clean up collections on application startup"""
    try:
        # Clean user_messages: keep only last 50 per user
        user_messages_cleanup_count = await cleanup_user_messages()

        # Clean admin_knowledge: no user-specific cleanup needed, but we can remove any user_id metadata if present
        admin_cleanup_count = await cleanup_admin_knowledge()

        return {
            "message": "Startup cleanup completed",
            "user_messages_removed": user_messages_cleanup_count,
            "admin_knowledge_cleaned": admin_cleanup_count
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error during startup cleanup: {str(e)}")


async def cleanup_user_messages() -> int:
    """Keep only the last 50 messages per user in user_messages collection"""
    try:
        # Get all messages
        all_messages = user_messages_collection.get()
        if not all_messages['ids']:
            return 0

        # Group messages by user_id
        messages_by_user = {}
        for i, message_id in enumerate(all_messages['ids']):
            metadata = all_messages['metadatas'][i]
            user_id = metadata.get('user_id')
            if user_id:
                if user_id not in messages_by_user:
                    messages_by_user[user_id] = []
                messages_by_user[user_id].append({
                    'id': message_id,
                    'created_at': metadata.get('created_at', ''),
                    'metadata': metadata
                })

        # Identify messages to delete (beyond 50 per user)
        messages_to_delete = []
        for user_id, messages in messages_by_user.items():
            # Sort by created_at descending (newest first)
            messages.sort(key=lambda x: x['created_at'], reverse=True)

            # Keep only first 50, delete the rest
            if len(messages) > 50:
                excess_messages = messages[50:]
                messages_to_delete.extend([msg['id'] for msg in excess_messages])

        # Delete excess messages
        if messages_to_delete:
            user_messages_collection.delete(ids=messages_to_delete)
            return len(messages_to_delete)

        return 0

    except Exception as e:
        print(f"Error cleaning user messages: {e}")
        return 0


async def cleanup_admin_knowledge() -> int:
    """Remove any user_id metadata from admin knowledge (make it global)"""
    try:
        all_knowledge = admin_knowledge_collection.get()
        if not all_knowledge['ids']:
            return 0

        updated_count = 0
        for i, knowledge_id in enumerate(all_knowledge['ids']):
            metadata = all_knowledge['metadatas'][i]
            # Remove user_id if present to make knowledge global
            if 'user_id' in metadata:
                new_metadata = metadata.copy()
                del new_metadata['user_id']
                # Update the metadata
                admin_knowledge_collection.update(
                    ids=[knowledge_id],
                    metadatas=[new_metadata]
                )
                updated_count += 1

        return updated_count

    except Exception as e:
        print(f"Error cleaning admin knowledge: {e}")
        return 0


# NEW: User messages endpoints
@app.post("/user_messages/upsert")
async def upsert_user_messages(messages: List[UserMessageData]):
    """Add or update user messages in ChromaDB (automatically limits to 50 per user)"""
    try:
        documents = []
        metadatas = []
        ids = []

        for message in messages:
            # Create document combining user message and AI response for better context
            doc_text = f"""
            User: {message.user_message}
            Assistant: {message.ai_response}
            """

            documents.append(doc_text)

            # Create metadata
            metadata = {
                "message_id": message.id,
                "user_id": message.user_id,
                "user_message": message.user_message,
                "ai_response": message.ai_response,
                "created_at": message.created_at,
                "source": "user_chat",
                "updated_at": datetime.now().isoformat()
            }

            metadatas.append(metadata)
            ids.append(message.id)

        # Upsert to ChromaDB
        user_messages_collection.upsert(
            documents=documents,
            metadatas=metadatas,
            ids=ids
        )

        # After upsert, cleanup to ensure we only keep last 50 per user
        await cleanup_user_messages()

        return {
            "message": f"Successfully upserted {len(messages)} user messages",
            "upserted_ids": ids
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error upserting user messages: {str(e)}")


@app.delete("/user_messages/{user_id}")
async def delete_user_messages(user_id: str):
    """Delete all messages for a specific user"""
    try:
        # Get all messages for this user
        user_messages = user_messages_collection.get(
            where={"user_id": user_id}
        )

        if user_messages['ids']:
            user_messages_collection.delete(ids=user_messages['ids'])
            return {"message": f"Deleted {len(user_messages['ids'])} messages for user {user_id}"}
        else:
            return {"message": f"No messages found for user {user_id}"}

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error deleting user messages: {str(e)}")


@app.get("/user_messages/user/{user_id}")
async def get_user_messages(user_id: str, limit: int = 50):
    """Get messages for a specific user (limited to 50)"""
    try:
        results = user_messages_collection.get(
            where={"user_id": user_id},
            limit=limit,
            include=["documents", "metadatas"]
        )

        # Sort by created_at descending (newest first)
        if results['metadatas']:
            sorted_indices = sorted(
                range(len(results['metadatas'])),
                key=lambda i: results['metadatas'][i].get('created_at', ''),
                reverse=True
            )

            sorted_results = {
                'ids': [results['ids'][i] for i in sorted_indices],
                'documents': [results['documents'][i] for i in sorted_indices],
                'metadatas': [results['metadatas'][i] for i in sorted_indices]
            }
            return sorted_results
        else:
            return results

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error getting user messages: {str(e)}")


# Existing endpoints (updated to include user_messages in search)
@app.post("/components/upsert")
async def upsert_components(components: List[ComponentData]):
    """Add or update components in ChromaDB"""
    try:
        documents = []
        metadatas = []
        ids = []

        for component in components:
            # Create a document string that includes all relevant information
            doc_text = f"""
            Component: {component.name}
            Type: {component.type}
            Manufacturer: {component.manufacturer or 'Unknown'}
            Model: {component.model or 'Unknown'}
            Description: {component.description or 'No description'}
            Price: ${component.price or 'N/A'}
            Specifications: {json.dumps(component.specifications, indent=2)}
            """

            documents.append(doc_text)

            # Create metadata for filtering - flatten nested objects
            metadata = {
                "component_id": component.id,
                "name": component.name,
                "type": component.type,
                "manufacturer": component.manufacturer or "",
                "model": component.model or "",
                "description": component.description or "",
                "source": "database",
                "updated_at": datetime.now().isoformat()
            }

            # Add price if available
            if component.price is not None:
                metadata["price"] = float(component.price)

            # Flatten and add specifications
            flattened_specs = flatten_specifications(component.specifications)
            metadata.update(flattened_specs)

            metadatas.append(metadata)
            ids.append(component.id)

        # Upsert to ChromaDB
        components_collection.upsert(
            documents=documents,
            metadatas=metadatas,
            ids=ids
        )

        return {
            "message": f"Successfully upserted {len(components)} components",
            "upserted_ids": ids
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error upserting components: {str(e)}")


@app.delete("/components/{component_id}")
async def delete_component(component_id: str):
    """Delete a component from ChromaDB"""
    try:
        components_collection.delete(ids=[component_id])
        return {"message": f"Component {component_id} deleted successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error deleting component: {str(e)}")


@app.post("/admin/knowledge")
async def add_admin_knowledge(knowledge: AdminKnowledge):
    """Add admin training/knowledge to separate collection (no user ID)"""
    try:
        knowledge_id = str(uuid.uuid4())

        # Flatten metadata for admin knowledge (no user_id)
        flattened_metadata = flatten_specifications(knowledge.metadata)

        # Ensure no user_id in admin knowledge (make it global)
        if 'user_id' in flattened_metadata:
            del flattened_metadata['user_id']

        admin_knowledge_collection.add(
            documents=[knowledge.content],
            metadatas=[{
                "knowledge_type": knowledge.knowledge_type,
                "source": "admin_training",
                "added_at": datetime.now().isoformat(),
                **flattened_metadata
            }],
            ids=[knowledge_id]
        )

        return {
            "message": "Admin knowledge added successfully",
            "knowledge_id": knowledge_id
        }

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error adding admin knowledge: {str(e)}")


@app.post("/search")
async def search_knowledge(search_query: SearchQuery):
    """Search in components, admin knowledge, or user messages collections"""
    try:
        if search_query.collection == "components":
            collection = components_collection
        elif search_query.collection == "admin_knowledge":
            collection = admin_knowledge_collection
        elif search_query.collection == "user_messages":
            collection = user_messages_collection
        else:
            raise HTTPException(status_code=400, detail="Invalid collection type")

        results = collection.query(
            query_texts=[search_query.query],
            n_results=search_query.n_results
        )

        # Format results
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


@app.get("/collections/info")
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


@app.delete("/collections/reset")
async def reset_collections():
    """Reset all collections (for development)"""
    try:
        chroma_client.delete_collection(name="pc_components")
        chroma_client.delete_collection(name="admin_knowledge")
        chroma_client.delete_collection(name="user_messages")

        # Recreate collections
        global components_collection, admin_knowledge_collection, user_messages_collection
        components_collection = chroma_client.get_or_create_collection(name="pc_components")
        admin_knowledge_collection = chroma_client.get_or_create_collection(name="admin_knowledge")
        user_messages_collection = chroma_client.get_or_create_collection(name="user_messages")

        return {"message": "All collections reset successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error resetting collections: {str(e)}")


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)