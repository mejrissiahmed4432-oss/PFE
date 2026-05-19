import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Software, LicensePool, SoftwareAssignment } from './software.model';

@Injectable({
  providedIn: 'root'
})
export class SoftwareService {
  private baseUrl = 'http://localhost:8081/api/software';

  constructor(private http: HttpClient) {}

  // --- SOFTWARE ENDPOINTS ---

  getAllSoftware(): Observable<Software[]> {
    return this.http.get<Software[]>(this.baseUrl);
  }

  getSoftwareById(id: string): Observable<Software> {
    return this.http.get<Software>(`${this.baseUrl}/${id}`);
  }

  createSoftware(software: Software): Observable<Software> {
    return this.http.post<Software>(this.baseUrl, software);
  }

  updateSoftware(id: string, software: Software): Observable<Software> {
    return this.http.put<Software>(`${this.baseUrl}/${id}`, software);
  }

  deleteSoftware(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  // --- LICENSE POOL ENDPOINTS ---

  getPoolsBySoftware(softwareId: string): Observable<LicensePool[]> {
    return this.http.get<LicensePool[]>(`${this.baseUrl}/${softwareId}/pools`);
  }

  createLicensePool(softwareId: string, pool: LicensePool): Observable<LicensePool> {
    return this.http.post<LicensePool>(`${this.baseUrl}/${softwareId}/pools`, pool);
  }

  revealKeys(poolId: string, password: string): Observable<string[]> {
    return this.http.post<string[]>(`${this.baseUrl}/pools/${poolId}/reveal-keys`, { password });
  }

  // --- ASSIGNMENT ENDPOINTS ---

  getAssignmentsBySoftware(softwareId: string): Observable<SoftwareAssignment[]> {
    return this.http.get<SoftwareAssignment[]>(`${this.baseUrl}/${softwareId}/assignments`);
  }

  assignLicense(assignment: SoftwareAssignment): Observable<SoftwareAssignment> {
    return this.http.post<SoftwareAssignment>(`${this.baseUrl}/assignments`, assignment);
  }

  revokeAssignment(assignmentId: string): Observable<SoftwareAssignment> {
    return this.http.post<SoftwareAssignment>(`${this.baseUrl}/assignments/${assignmentId}/revoke`, {});
  }
}
