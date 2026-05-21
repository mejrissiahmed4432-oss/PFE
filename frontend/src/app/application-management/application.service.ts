import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Application, EquipmentApplication, InstallApplicationRequest } from './application.model';

@Injectable({
  providedIn: 'root'
})
export class ApplicationService {
  private apiUrl = '/api/software';

  constructor(private http: HttpClient) { }

  getAllApplications(): Observable<Application[]> {
    return this.http.get<Application[]>(this.apiUrl);
  }

  getApplicationById(id: string): Observable<Application> {
    return this.http.get<Application>(`${this.apiUrl}/${id}`);
  }

  createApplication(app: Application): Observable<Application> {
    return this.http.post<Application>(this.apiUrl, app);
  }

  updateApplication(id: string, app: Application): Observable<Application> {
    return this.http.put<Application>(`${this.apiUrl}/${id}`, app);
  }

  deleteApplication(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  installApplication(request: InstallApplicationRequest): Observable<EquipmentApplication> {
    return this.http.post<EquipmentApplication>(`${this.apiUrl}/install`, request);
  }

  uninstallApplication(installationId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/uninstall/${installationId}`, {});
  }
}
