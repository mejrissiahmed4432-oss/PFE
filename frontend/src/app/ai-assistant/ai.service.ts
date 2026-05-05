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
  private readonly BASE_STORAGE_KEY = 'ai_conversations';

  constructor(
    private http: HttpClient,
    private authService: AuthService
  ) { }

  private getStorageKey(): string {
    const user = this.authService.getCurrentUser();
    const userId = user?.id || user?._id || 'guest';
    return `${this.BASE_STORAGE_KEY}_${userId}`;
  }

  query(message: string, history: ConversationTurn[] = []): Observable<AiResponse> {
    const user = this.authService.getCurrentUser();
    const role = user?.role || 'technician';
    const userId = user?.id || user?._id || 'unknown';

    const payload: AiRequest = {
      userId,
      role,
      message,
      conversationHistory: history
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

  // ── Conversation Persistence (localStorage) ───────────────────────────────

  getAllConversations(): Conversation[] {
    try {
      const raw = localStorage.getItem(this.getStorageKey());
      if (!raw) return [];
      const parsed: Conversation[] = JSON.parse(raw);
      return parsed.map(conv => ({
        ...conv,
        messages: conv.messages.map(m => ({ ...m, timestamp: new Date(m.timestamp) }))
      }));
    } catch {
      return [];
    }
  }

  saveConversation(conv: Conversation): void {
    try {
      const key = this.getStorageKey();
      const all = this.getAllConversations();
      const idx = all.findIndex(c => c.id === conv.id);
      if (idx >= 0) {
        all[idx] = { ...conv, updatedAt: new Date().toISOString() };
      } else {
        all.unshift({ ...conv, updatedAt: new Date().toISOString() });
      }
      localStorage.setItem(key, JSON.stringify(all));
    } catch { }
  }

  deleteConversation(id: string): void {
    try {
      const all = this.getAllConversations().filter(c => c.id !== id);
      localStorage.setItem(this.getStorageKey(), JSON.stringify(all));
    } catch { }
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
}