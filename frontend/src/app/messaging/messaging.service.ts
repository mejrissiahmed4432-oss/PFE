import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class MessagingService {
  private apiUrl = '/api/messages';
  private usersUrl = '/api/users';

  constructor(private http: HttpClient) {}

  getUsers(): Observable<any[]> {
    return this.http.get<any[]>(this.usersUrl);
  }

  ping(): Observable<any> {
    return this.http.post<any>(`${this.usersUrl}/ping`, {});
  }

  getHistory(otherUserId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/history/${otherUserId}`);
  }

  sendMessage(message: any): Observable<any> {
    return this.http.post<any>(this.apiUrl, message);
  }

  getConversationSummaries(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/conversations`);
  }

  getUnreadCount(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/unread-count`);
  }

  markAsRead(senderId: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/read/${senderId}`, {});
  }

  uploadAttachment(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<any>(`${this.apiUrl}/upload`, formData);
  }

  getAttachmentUrl(attachmentId: string): string {
    return `${this.apiUrl}/attachment/${attachmentId}`;
  }

  editMessage(id: string, content: string): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/${id}`, { content });
  }

  deleteMessage(id: string, forEveryone: boolean = false): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/${id}?forEveryone=${forEveryone}`);
  }
}
