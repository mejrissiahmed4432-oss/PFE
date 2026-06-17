import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface AuditLog {
  id: string;
  userId: string;
  userName: string;
  userRole: string;
  action: string;
  details: string;
  timestamp: string;
  ipAddress: string;
  blockchainTxHash: string;
  blockchainLogId: string;
  contentHash: string;
}

export interface BlockchainStatus {
  online: boolean;
  network: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuditService {

  private apiUrl = '/api/employees/audit/logs';
  private statusUrl = '/api/employees/audit/status';

  constructor(private http: HttpClient) { }

  getLogs(): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(this.apiUrl);
  }

  getStatus(): Observable<BlockchainStatus> {
    return this.http.get<BlockchainStatus>(this.statusUrl);
  }
}
