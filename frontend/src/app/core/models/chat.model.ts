export interface ChatRequest {
  message: string;
  userId: number;
}

export interface ChatResponse {
  response: string;
  messageId?: number;
}

export interface ChatMessage {
  id: number;
  userId: number;
  userMessage: string;
  aiResponse: string;
  createdAt: string;
}