import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RequestedItem {
  itemId: string;
  itemType: string;
  itemName: string;
  brand?: string;
  model?: string;
  type?: string;
  version?: string;
}

export interface PersonalRequest {
  id?: string;
  userId: string;
  userName: string;
  requestedItems: RequestedItem[];
  reason?: string;
  status?: string;
  itManagerNote?: string;
  createdAt?: string;
  updatedAt?: string;
  reviewedAt?: string;
}

@Injectable({
  providedIn: 'root'
})
export class PersonalRequestService {
  private apiUrl = '/api/personal-requests';

  constructor(private http: HttpClient) {}

  createRequest(request: PersonalRequest): Observable<PersonalRequest> {
    return this.http.post<PersonalRequest>(this.apiUrl, request);
  }

  getMyRequests(userId: string): Observable<PersonalRequest[]> {
    return this.http.get<PersonalRequest[]>(`${this.apiUrl}/user/${userId}`);
  }

  getPendingRequests(): Observable<PersonalRequest[]> {
    return this.http.get<PersonalRequest[]>(`${this.apiUrl}/pending`);
  }

  getHistory(): Observable<PersonalRequest[]> {
    return this.http.get<PersonalRequest[]>(`${this.apiUrl}/history`);
  }

  approveRequest(id: string, note: string): Observable<PersonalRequest> {
    return this.http.put<PersonalRequest>(`${this.apiUrl}/${id}/approve`, { note });
  }

  rejectRequest(id: string, note: string): Observable<PersonalRequest> {
    return this.http.put<PersonalRequest>(`${this.apiUrl}/${id}/reject`, { note });
  }

  deleteRequest(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getAvailableEquipment(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/available-equipment`);
  }

  getAvailableSoftware(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/available-software`);
  }
}
