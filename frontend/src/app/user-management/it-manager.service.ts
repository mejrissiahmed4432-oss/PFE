import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SystemUser, ProvisionRequest } from './user.service';
import { Employee } from '../employee/employee.model';

@Injectable({ providedIn: 'root' })
export class ItManagerService {
  private apiUrl = '/api/it-manager';

  constructor(private http: HttpClient) {}

  getEmployees(): Observable<Employee[]> {
    return this.http.get<Employee[]>(`${this.apiUrl}/employees`);
  }

  getUsers(): Observable<SystemUser[]> {
    return this.http.get<SystemUser[]>(`${this.apiUrl}/users`);
  }

  provisionUser(data: ProvisionRequest): Observable<any> {
    return this.http.post(`${this.apiUrl}/provision`, data);
  }

  updateUserStatus(id: string, status: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/users/${id}/status`, { status });
  }

  updateUserRole(id: string, role: string): Observable<any> {
    return this.http.put(`${this.apiUrl}/users/${id}/role`, { role });
  }

  resendInvitation(id: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/users/${id}/resend-invitation`, {});
  }

  deleteUser(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/users/${id}`);
  }
}
