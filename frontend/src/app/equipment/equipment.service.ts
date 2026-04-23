import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Equipment } from './equipment.model';

@Injectable({
  providedIn: 'root'
})
export class EquipmentService {
  // Using the proxy or direct backend URL as per environment config
  // The 'environment' file is usually used, but we'll use a direct path or relative path
  private apiUrl = '/api/equipment';

  private filterShelfId: string | null = null;
  private filterShelfNb: string | null = null;

  constructor(private http: HttpClient) {}

  setShelfFilter(id: string | null, nb: string | null) {
    this.filterShelfId = id;
    this.filterShelfNb = nb;
  }

  getShelfFilter() {
    return { id: this.filterShelfId, nb: this.filterShelfNb };
  }

  getAllEquipment(): Observable<Equipment[]> {
    return this.http.get<Equipment[]>(this.apiUrl);
  }

  getEquipmentById(id: string): Observable<Equipment> {
    return this.http.get<Equipment>(`${this.apiUrl}/${id}`);
  }

  checkSerialNumberUnique(serial: string, excludeId?: string): Observable<boolean> {
    let url = `${this.apiUrl}/check-serial/${serial}`;
    if (excludeId) {
      url += `?excludeId=${excludeId}`;
    }
    return this.http.get<boolean>(url);
  }

  getEquipmentByShelfId(shelfId: string): Observable<Equipment[]> {
    return this.http.get<Equipment[]>(`${this.apiUrl}/shelf/${shelfId}`);
  }

  createEquipment(equipment: Equipment): Observable<Equipment> {
    return this.http.post<Equipment>(this.apiUrl, equipment);
  }

  updateEquipment(id: string, equipment: Equipment): Observable<Equipment> {
    return this.http.put<Equipment>(`${this.apiUrl}/${id}`, equipment);
  }

  deleteEquipment(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  deleteBulkEquipment(ids: string[]): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/bulk-delete`, ids);
  }

  updateBulkBasicInfo(ids: string[], name: string, brand: string, model: string): Observable<Equipment[]> {
    return this.http.put<Equipment[]>(`${this.apiUrl}/bulk-update-basic`, { ids, name, brand, model });
  }

  getInvoiceFile(id: string): Observable<string> {
    return this.http.get(`${this.apiUrl}/${id}/invoice-file`, { responseType: 'text' });
  }

  getWarrantyFile(id: string): Observable<string> {
    return this.http.get(`${this.apiUrl}/${id}/warranty-file`, { responseType: 'text' });
  }

  consumeParts(parts: any[]): Observable<any> {
    // This is the legacy method that was causing 405 because it didn't match the new backend structure
    // We'll keep it for compatibility if needed but prefer allocateParts
    return this.http.post(`${this.apiUrl}/allocate-parts`, { parts });
  }

  allocateParts(technicianId: string, technicianName: string, parts: any[]): Observable<any> {
    return this.http.post(`${this.apiUrl}/allocate-parts`, {
      requesterId: technicianId,
      requesterName: technicianName,
      parts: parts
    });
  }

  returnPart(id: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${id}/return`, {});
  }
}
