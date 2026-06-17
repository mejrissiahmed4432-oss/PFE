import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../auth.service';

export interface ConversationTurn {
  role: 'user' | 'assistant';
  content: string;
}

export interface AiRequest {
  userId: string;
  role: string;
  message: string;
  conversationHistory?: ConversationTurn[];
  imageBase64?: string;
}

export interface AiResponse {
  intent: string;
  answer: string;
  data: any[];
  suggestions: string[];
  role: string;
  success: boolean;
  errorMessage?: string;
  actionPending?: boolean;
  actionType?: string;
  actionPayload?: any;
}

export interface ChatMessage {
  id: number;
  text: string;
  sender: 'user' | 'assistant';
  timestamp: Date;
  suggestions?: string[];
  data?: any[];
  isError?: boolean;
  actionPending?: boolean;
  actionType?: string;
  actionPayload?: any;
  imageUrl?: string;   // base64 data URL for images attached to messages
}

export interface Conversation {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messages: ChatMessage[];
}

@Injectable({
  providedIn: 'root'
})
export class AiService {
  private apiUrl = '/api/ai';
  private conversationsUrl = '/api/aiconversations';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) { }

  query(message: string, history: ConversationTurn[] = [], imageBase64?: string): Observable<AiResponse> {
    const user = this.authService.getCurrentUser();
    const role = user?.role || 'technician';
    const userId = user?.id || user?._id || 'unknown';

    const payload: AiRequest = {
      userId,
      role,
      message,
      conversationHistory: history,
      ...(imageBase64 ? { imageBase64 } : {})
    };

    return this.http.post<AiResponse>(`${this.apiUrl}/query`, payload);
  }

  executeAction(actionType: string, payload: any): Observable<AiResponse> {
    const user = this.authService.getCurrentUser();
    const role = user?.role || 'technician';
    const userId = user?.id || user?._id || 'unknown';

    return this.http.post<AiResponse>(`${this.apiUrl}/action/execute`, {
      actionType,
      payload,
      role,
      userId
    });
  }

  // ── Conversation Persistence (Backend) ───────────────────────────────

  getAllConversations(): Observable<Conversation[]> {
    return this.http.get<Conversation[]>(this.conversationsUrl);
  }

  saveConversation(conv: Conversation): Observable<Conversation> {
    return this.http.post<Conversation>(this.conversationsUrl, conv);
  }

  deleteConversation(id: string): Observable<void> {
    return this.http.delete<void>(`${this.conversationsUrl}/${id}`);
  }

  createConversation(): Conversation {
    return {
      id: `conv_${Date.now()}_${Math.random().toString(36).slice(2, 7)}`,
      title: 'New Conversation',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      messages: []
    };
  }

  compareQuotations(request: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/compare-quotations`, request);
  }
}