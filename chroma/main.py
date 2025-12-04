from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from endpoints import components, admin_knowledge, user_messages, search, info

app = FastAPI(title="PC Builder ChromaDB Server")

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(components.router, prefix="/components", tags=["components"])
app.include_router(admin_knowledge.router, prefix="/admin", tags=["admin"])
app.include_router(user_messages.router, prefix="/user_messages", tags=["user_messages"])
app.include_router(search.router, prefix="", tags=["search"])
app.include_router(info.router, prefix="", tags=["info"])

@app.get("/")
async def root():
    return {"message": "PC Builder ChromaDB Server", "status": "running"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)