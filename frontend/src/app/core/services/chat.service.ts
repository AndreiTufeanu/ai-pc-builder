import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ChatRequest {
  message: string;
  userId?: number;
}

export interface ChatResponse {
  response: string;
}

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private baseUrl = 'http://localhost:8080/api';

  constructor(private http: HttpClient) {}

  // User chat - for general PC building advice
  sendUserMessage(message: string): Observable<ChatResponse> {
    const request: ChatRequest = { message };
    return this.http.post<ChatResponse>(`${this.baseUrl}/chat`, request);
  }

  // Admin chat - for training (we'll use this later for admin dashboard)
  sendAdminMessage(message: string, userId?: number): Observable<ChatResponse> {
    const request: ChatRequest = { message, userId };
    return this.http.post<ChatResponse>(`${this.baseUrl}/admin/chat`, request);
  }
}