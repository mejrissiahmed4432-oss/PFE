import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OperatingSystem, InstallOSRequest, EquipmentSoftware } from './os.model';

@Injectable({
  providedIn: 'root'
})
export class OsService {
  private apiUrl = '/api/os'; 

  constructor(private http: HttpClient) {}

  getAllOperatingSystems(): Observable<OperatingSystem[]> {
    return this.http.get<OperatingSystem[]>(this.apiUrl);
  }

  addOperatingSystem(os: OperatingSystem): Observable<OperatingSystem> {
    return this.http.post<OperatingSystem>(this.apiUrl, os);
  }

  updateOperatingSystem(id: string, os: OperatingSystem): Observable<OperatingSystem> {
    return this.http.put<OperatingSystem>(`${this.apiUrl}/${id}`, os);
  }

  installOS(request: InstallOSRequest): Observable<EquipmentSoftware> {
    return this.http.post<EquipmentSoftware>(`${this.apiUrl}/install`, request);
  }

  uninstallOS(softwareId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/uninstall/${softwareId}`, {});
  }

  deleteOperatingSystem(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}


