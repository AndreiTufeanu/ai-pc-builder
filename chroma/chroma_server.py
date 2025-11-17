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

# Two separate collections:
components_collection = chroma_client.get_or_create_collection(
    name="pc_components",
    metadata={"description": "PC components from database"}
)

admin_knowledge_collection = chroma_client.get_or_create_collection(
    name="admin_knowledge",
    metadata={"description": "Admin training data and instructions"}
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
    metadata: Dict[str, Any] = {}


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
    """Add admin training/knowledge to separate collection"""
    try:
        knowledge_id = str(uuid.uuid4())

        # Flatten metadata for admin knowledge too
        flattened_metadata = flatten_specifications(knowledge.metadata)

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
    """Search in either components or admin knowledge collections"""
    try:
        if search_query.collection == "components":
            collection = components_collection
        elif search_query.collection == "admin_knowledge":
            collection = admin_knowledge_collection
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

        return {
            "components_collection": {
                "count": components_count,
                "description": "PC components from database"
            },
            "admin_knowledge_collection": {
                "count": admin_knowledge_count,
                "description": "Admin training data and instructions"
            }
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error getting collection info: {str(e)}")


@app.delete("/collections/reset")
async def reset_collections():
    """Reset both collections (for development)"""
    try:
        chroma_client.delete_collection(name="pc_components")
        chroma_client.delete_collection(name="admin_knowledge")

        # Recreate collections
        global components_collection, admin_knowledge_collection
        components_collection = chroma_client.get_or_create_collection(name="pc_components")
        admin_knowledge_collection = chroma_client.get_or_create_collection(name="admin_knowledge")

        return {"message": "Collections reset successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error resetting collections: {str(e)}")


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=8000)