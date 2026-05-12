import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SystemUser {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  photo?: string;
  phoneNumber?: string;
  employeeId?: string;
  status: 'PENDING' | 'ACTIVE' | 'INACTIVE';
  createdAt?: string;
  lastLogin?: string;
  online?: boolean;
  lastActive?: string;
  resetToken?: string;
  resetTokenExpiry?: string;
}

export interface ProvisionRequest {
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  employeeId?: string;
}

@Injectable({ providedIn: 'root' })
export class UserService {
  private apiUrl = '/api/users';

  constructor(private http: HttpClient) {}

  getAllUsers(): Observable<SystemUser[]> {
    return this.http.get<SystemUser[]>(this.apiUrl);
  }

  provisionUser(data: ProvisionRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/provision`, data);
  }

  deleteUser(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  updateUserRole(id: string, role: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}/role`, { role });
  }

  updateUserStatus(id: string, status: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/${id}/status`, { status });
  }

  resendInvitation(id: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/${id}/resend-invitation`, {});
  }
}
