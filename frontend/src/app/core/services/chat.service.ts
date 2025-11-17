import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ChatRequest {
  message: string;
  userId: number; // Now required for both
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

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  // User chat - for general PC building advice
  sendUserMessage(message: string, userId: number): Observable<ChatResponse> {
    const request: ChatRequest = { message, userId };
    return this.http.post<ChatResponse>(`${this.baseUrl}/chat`, request);
  }

  // Admin chat - for training
  sendAdminMessage(message: string, userId: number): Observable<ChatResponse> {
    const request: ChatRequest = { message, userId };
    return this.http.post<ChatResponse>(`${this.baseUrl}/admin/chat`, request);
  }

  // Get chat history for any user (works for both normal users and admins)
  getChatHistory(userId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.baseUrl}/chat/history/${userId}`);
  }
}