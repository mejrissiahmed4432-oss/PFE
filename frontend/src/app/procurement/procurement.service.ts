import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  EquipmentRequest,
  RFQ,
  SupplierResponse,
  PurchaseOrder,
  CatalogItem,
  EquipmentParsingResponse,
  EquipmentSpecification
} from './procurement.models';
import { Subject } from 'rxjs';
import { tap } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class ProcurementService {

  private base = '/api/procurement';
  private requestCreatedSource = new Subject<void>();
  requestCreated$ = this.requestCreatedSource.asObservable();

  constructor(private http: HttpClient) {}

  // ─── Catalog & AI ─────────────────────────────────────────────────────────

  getCatalog(query?: string): Observable<CatalogItem[]> {
    const params: any = query ? { query } : {};
    return this.http.get<CatalogItem[]>(`${this.base}/catalog`, { params });
  }

  autocompleteCatalog(query: string): Observable<CatalogItem[]> {
    return this.http.get<CatalogItem[]>(`${this.base}/catalog/autocomplete`, { params: { query } });
  }

  getSpecifications(catalogItemId: string): Observable<EquipmentSpecification[]> {
    return this.http.get<EquipmentSpecification[]>(`${this.base}/catalog/${catalogItemId}/specifications`);
  }

  parseRequestWithAI(text: string): Observable<EquipmentParsingResponse> {
    return this.http.post<EquipmentParsingResponse>('/api/ai/parse-equipment-request', { text });
  }

  suggestRelatedEquipment(cartItems: any[]): Observable<string[]> {
    return this.http.post<string[]>('/api/ai/suggest-equipment', { cartItems });
  }

  autocompleteSpecsWithAI(text: string): Observable<string[]> {
    return this.http.post<string[]>('/api/ai/autocomplete-specs', { text });
  }

  // ─── Equipment Requests ───────────────────────────────────────────────────

  createRequest(req: EquipmentRequest): Observable<EquipmentRequest> {
    return this.http.post<EquipmentRequest>(`${this.base}/requests`, req).pipe(
      tap(() => this.requestCreatedSource.next())
    );
  }

  getAllRequests(): Observable<EquipmentRequest[]> {
    return this.http.get<EquipmentRequest[]>(`${this.base}/requests`);
  }

  getRequestsByUser(userId: string): Observable<EquipmentRequest[]> {
    return this.http.get<EquipmentRequest[]>(`${this.base}/requests/user/${userId}`);
  }

  getRequestById(id: string): Observable<EquipmentRequest> {
    return this.http.get<EquipmentRequest>(`${this.base}/requests/${id}`);
  }

  approveRequest(id: string): Observable<EquipmentRequest> {
    return this.http.put<EquipmentRequest>(`${this.base}/requests/${id}/approve`, {}).pipe(
      tap(() => this.requestCreatedSource.next())
    );
  }

  rejectRequest(id: string, reason: string): Observable<EquipmentRequest> {
    return this.http.put<EquipmentRequest>(`${this.base}/requests/${id}/reject`, { reason }).pipe(
      tap(() => this.requestCreatedSource.next())
    );
  }

  // ─── RFQ ──────────────────────────────────────────────────────────────────

  createRFQ(payload: { requestId: string; supplierIds: string[]; supplierEmails: string[]; selectedItemIndices?: number[] }): Observable<RFQ> {
    return this.http.post<RFQ>(`${this.base}/rfq`, payload);
  }

  getAllRFQs(): Observable<RFQ[]> {
    return this.http.get<RFQ[]>(`${this.base}/rfq`);
  }

  getRFQById(id: string): Observable<RFQ> {
    return this.http.get<RFQ>(`${this.base}/rfq/${id}`);
  }

  getRFQByRequestId(requestId: string): Observable<RFQ> {
    return this.http.get<RFQ>(`${this.base}/rfq/request/${requestId}`);
  }

  // ─── Supplier Responses ───────────────────────────────────────────────────

  uploadResponse(formData: FormData): Observable<SupplierResponse> {
    return this.http.post<SupplierResponse>(`${this.base}/responses/upload`, formData);
  }

  getResponsesByRfq(rfqId: string): Observable<SupplierResponse[]> {
    return this.http.get<SupplierResponse[]>(`${this.base}/responses/rfq/${rfqId}`);
  }

  getResponsesByRequest(requestId: string): Observable<SupplierResponse[]> {
    return this.http.get<SupplierResponse[]>(`${this.base}/responses/request/${requestId}`);
  }

  getAllResponses(): Observable<SupplierResponse[]> {
    return this.http.get<SupplierResponse[]>(`${this.base}/responses`);
  }

  approveResponse(id: string): Observable<SupplierResponse> {
    return this.http.put<SupplierResponse>(`${this.base}/responses/${id}/approve`, {});
  }

  rejectResponse(id: string): Observable<SupplierResponse> {
    return this.http.put<SupplierResponse>(`${this.base}/responses/${id}/reject`, {});
  }

  getDownloadUrl(id: string): string {
    return `${this.base}/responses/${id}/download`;
  }

  getViewUrl(id: string): string {
    return `${this.base}/responses/${id}/view`;
  }

  // ─── Public Supplier Portal ───────────────────────────────────────────────

  getPublicRequestByToken(token: string): Observable<any> {
    return this.http.get<any>(`/api/public/supplier-response/token/${token}`);
  }

  submitPublicResponse(formData: FormData): Observable<any> {
    return this.http.post<any>(`/api/public/supplier-response/submit`, formData);
  }

  // ─── Purchase Orders ──────────────────────────────────────────────────────

  createOrder(payload: { requestId: string; rfqId: string; responseId: string }): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${this.base}/orders`, payload);
  }

  getAllOrders(): Observable<PurchaseOrder[]> {
    return this.http.get<PurchaseOrder[]>(`${this.base}/orders`);
  }

  getOrderById(id: string): Observable<PurchaseOrder> {
    return this.http.get<PurchaseOrder>(`${this.base}/orders/${id}`);
  }

  getOrderByRequest(requestId: string): Observable<PurchaseOrder> {
    return this.http.get<PurchaseOrder>(`${this.base}/orders/request/${requestId}`);
  }

  confirmReceipt(orderId: string, notes: string, rating: number, postToStock: boolean = false): Observable<PurchaseOrder> {
    return this.http.post<PurchaseOrder>(`${this.base}/orders/${orderId}/confirm-receipt`, { notes, rating, postToStock }).pipe(
      tap(() => this.requestCreatedSource.next())
    );
  }
}
