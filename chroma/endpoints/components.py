from fastapi import APIRouter, HTTPException
from typing import List
from models import ComponentData
import utils
from chroma_client import components_collection

router = APIRouter()

@router.post("/upsert")
async def upsert_components(components: List[ComponentData]):
    """Add or update components in ChromaDB with type-specific fields"""
    try:
        documents = []
        metadatas = []
        ids = []

        for component in components:
            # Skip if component type not supported
            if component.type.upper() not in utils.constants.COMPONENT_SPECS_MAP:
                continue

            # Create type-specific document and metadata
            doc_text = utils.create_component_document(component)
            metadata = utils.create_component_metadata(component)

            documents.append(doc_text)
            metadatas.append(metadata)
            ids.append(component.id)

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


@router.delete("/{component_id}")
async def delete_component(component_id: str):
    """Delete a component from ChromaDB"""
    try:
        components_collection.delete(ids=[component_id])
        return {"message": f"Component {component_id} deleted successfully"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error deleting component: {str(e)}")