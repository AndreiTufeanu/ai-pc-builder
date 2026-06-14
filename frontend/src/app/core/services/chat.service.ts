import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChatRequest, ChatResponse, ChatMessage } from '../models/chat.model';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ChatService {
  private baseUrl = `${environment.apiUrl}/api`;

  constructor(private http: HttpClient) {}

  sendUserMessage(message: string, userId: number): Observable<ChatResponse> {
    const request: ChatRequest = { message, userId };
    return this.http.post<ChatResponse>(`${this.baseUrl}/chat`, request);
  }

  sendAdminMessage(message: string, userId: number): Observable<ChatResponse> {
    const request: ChatRequest = { message, userId };
    return this.http.post<ChatResponse>(`${this.baseUrl}/admin/chat`, request);
  }

  getChatHistory(userId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.baseUrl}/chat/history/${userId}`);
  }
}