import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PartRequest } from './part-request.model';

@Injectable({
  providedIn: 'root'
})
export class PartRequestService {
  private apiUrl = 'http://localhost:8000/api/part-requests';

  constructor(private http: HttpClient) {}

  createRequest(request: PartRequest): Observable<PartRequest> {
    return this.http.post<PartRequest>(this.apiUrl, request);
  }

  getMyRequests(requesterId: string): Observable<PartRequest[]> {
    return this.http.get<PartRequest[]>(`${this.apiUrl}/my/${requesterId}`);
  }

  getAllRequests(): Observable<PartRequest[]> {
    return this.http.get<PartRequest[]>(this.apiUrl);
  }

  updateStatus(requestId: string, status: string): Observable<PartRequest> {
    return this.http.put<PartRequest>(`${this.apiUrl}/${requestId}/status?status=${status}`, {});
  }

  updateRequest(requestId: string, updateDetails: Partial<PartRequest>): Observable<PartRequest> {
    return this.http.put<PartRequest>(`${this.apiUrl}/${requestId}`, updateDetails);
  }

  deleteRequest(requestId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${requestId}`);
  }
}
