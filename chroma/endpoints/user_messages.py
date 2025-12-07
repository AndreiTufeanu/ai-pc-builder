from datetime import datetime

from fastapi import APIRouter, HTTPException
from typing import List
from models import UserMessageData
from chroma_client import user_messages_collection

router = APIRouter()

async def cleanup_user_messages() -> int:
    """Keep only the last 50 messages per user in user_messages collection"""
    try:
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
                    'created_at': metadata.get('created_at', '')
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


@router.post("/upsert")
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
                "source": "user_chat"
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


@router.post("/cleanup/startup")
async def cleanup_on_startup():
    """Clean up collections on application startup"""
    try:
        # Get all user message IDs
        all_data = user_messages_collection.get()
        if all_data['ids']:
            user_messages_collection.delete(ids=all_data['ids'])
            return {
                "message": f"Cleared {len(all_data['ids'])} user messages on startup",
                "user_messages_removed": len(all_data['ids'])
            }
        return {
            "message": "No user messages to clear on startup",
            "user_messages_removed": 0
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error during startup cleanup: {str(e)}")