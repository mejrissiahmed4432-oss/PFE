import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Notification {
  id: string;
  title: string;
  message: string;
  type: 'SUCCESS' | 'INFO' | 'ERROR';
  category: string;
  read: boolean;
  relatedId?: string;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private apiUrl = '/api/notifications';

  constructor(private http: HttpClient) { }

  getNotifications(userId?: string, role?: string): Observable<Notification[]> {
    let params: any = {};
    if (userId) params.userId = userId;
    if (role) params.role = role;
    return this.http.get<Notification[]>(this.apiUrl, { params });
  }

  getUnreadNotifications(userId?: string, role?: string): Observable<Notification[]> {
    let params: any = {};
    if (userId) params.userId = userId;
    if (role) params.role = role;
    return this.http.get<Notification[]>(`${this.apiUrl}/unread`, { params });
  }

  markAsRead(id: string): Observable<Notification> {
    return this.http.put<Notification>(`${this.apiUrl}/${id}/read`, {});
  }

  markAllAsRead(userId?: string, role?: string): Observable<void> {
    let params: any = {};
    if (userId) params.userId = userId;
    if (role) params.role = role;
    return this.http.put<void>(`${this.apiUrl}/read-all`, {}, { params });
  }

  deleteNotification(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  deleteAllNotifications(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/all`);
  }
}
