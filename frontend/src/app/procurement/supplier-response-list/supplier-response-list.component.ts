import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProcurementService } from '../procurement.service';
import { SupplierResponse, EquipmentRequest, RESPONSE_STATUS_META, SupplierResponseStatus } from '../procurement.models';
import { AiService } from '../../ai-assistant/ai.service';
import { ToastService } from '../../shared/toast.service';
import { forkJoin, from, of, Observable } from 'rxjs';
import { map, catchError, mergeMap } from 'rxjs/operators';

interface GroupedResponses {
  requestId: string;
  requestTitle: string;
  requestStatus: string;
  responses: SupplierResponse[];
}

@Component({
  selector: 'app-supplier-response-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './supplier-response-list.component.html',
  styleUrls: ['./supplier-response-list.component.css']
})
export class SupplierResponseListComponent implements OnInit {
  @Output() createOrder = new EventEmitter<SupplierResponse>();

  responses: SupplierResponse[] = [];
  requests: EquipmentRequest[] = [];
  groupedResponses: GroupedResponses[] = [];
  isLoading = true;
  showHistory = false;
  expandedGroups: Set<string> = new Set();
  aiAnalysis: Record<string, any> = {};
  isAnalyzing: Record<string, boolean> = {};

  constructor(
    private procService: ProcurementService,
    private aiService: AiService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.isLoading = true;
    this.procService.getAllRequests().subscribe(requests => {
      this.requests = requests;
      this.procService.getAllResponses().subscribe(responses => {
        this.responses = responses;
        this.groupData();
        this.isLoading = false;
      });
    });
  }

  groupData(): void {
    const groupsMap = new Map<string, GroupedResponses>();
    this.responses.forEach(resp => {
      const rid = resp.requestId || 'Unknown';
      if (!groupsMap.has(rid)) {
        const req = this.requests.find(r => r.id === rid);
        groupsMap.set(rid, {
          requestId: rid,
          requestTitle: req ? (req.notes || 'Hardware Batch #' + rid.substring(0,6).toUpperCase()) : 'Manual Audit Node',
          requestStatus: req?.status || 'UNKNOWN',
          responses: []
        });
      }
      groupsMap.get(rid)?.responses.push(resp);
    });
    this.groupedResponses = Array.from(groupsMap.values());
    if (this.displayedGroups.length > 0 && this.expandedGroups.size === 0) {
      this.expandedGroups.add(this.displayedGroups[0].requestId);
    }
  }

  get displayedGroups(): GroupedResponses[] {
    return this.groupedResponses.filter(g => {
      const isHistory = g.requestStatus === 'ORDER_CONFIRMED' || g.requestStatus === 'RECEIVED';
      return this.showHistory ? isHistory : !isHistory;
    });
  }

  toggleGroup(requestId: string): void {
    if (this.expandedGroups.has(requestId)) this.expandedGroups.delete(requestId);
    else this.expandedGroups.add(requestId);
  }

  approve(id: string): void {
    this.procService.approveResponse(id).subscribe({ 
      next: () => { this.toastService.success('Vendor Node Selected. PO Authorization Required.'); this.load(); },
      error: () => this.toastService.error('Action protocol failed.')
    });
  }

  downloadUrl(id: string): string { return this.procService.getDownloadUrl(id); }
  viewUrl(id: string): string { return this.procService.getViewUrl(id); }

  runAIAnalysis(group: GroupedResponses): void {
    const requestId = group.requestId;
    this.isAnalyzing[requestId] = true;
    const pdfFetchObservables = group.responses.map(resp => {
      const url = this.viewUrl(resp.id!);
      return from(fetch(url).then(res => res.blob())).pipe(
        mergeMap(blob => this.blobToBase64(blob)),
        map(base64 => ({ ...resp, pdfBase64: base64 })),
        catchError(() => of({ ...resp, pdfBase64: null }))
      );
    });
    forkJoin(pdfFetchObservables).subscribe(responsesWithPdf => {
      const request = this.requests.find(r => r.id === requestId);
      const payload = {
        requestNotes: request?.notes || '',
        items: request?.items || [],
        quotes: responsesWithPdf.map(resp => ({
          supplierName: resp.supplierName,
          totalPrice: resp.totalPrice,
          deliveryDays: resp.deliveryDays,
          currency: resp.currency,
          pdfBase64: resp.pdfBase64
        }))
      };
      this.aiService.compareQuotations(payload).subscribe({
        next: (res) => { this.aiAnalysis[requestId] = res; this.isAnalyzing[requestId] = false; this.toastService.success('Neural Audit Node Complete.'); },
        error: () => { this.isAnalyzing[requestId] = false; this.toastService.error('AI Intelligence Node Unavailable.'); }
      });
    });
  }

  private blobToBase64(blob: Blob): Observable<string> {
    return new Observable<string>(observer => {
      const reader = new FileReader();
      reader.onloadend = () => {
        let result = reader.result as string;
        if (result.includes(',')) result = result.split(',')[1];
        observer.next(result);
        observer.complete();
      };
      reader.onerror = (err) => observer.error(err);
      reader.readAsDataURL(blob);
    });
  }

  getRespMeta(status?: SupplierResponseStatus) { 
    const meta = RESPONSE_STATUS_META[status || 'PENDING'];
    let colorClass = 'bg-slate-100 text-slate-600 border-slate-200 dark:bg-slate-800 dark:text-slate-300 dark:border-white/10';
    if (status === 'APPROVED_SUPPLIER') colorClass = 'bg-teal-50 text-teal-600 border-teal-100 dark:bg-teal-500/10 dark:text-teal-400 dark:border-teal-500/20';
    if (status === 'REJECTED_SUPPLIER') colorClass = 'bg-rose-50 text-rose-600 border-rose-100 dark:bg-rose-500/10 dark:text-rose-400 dark:border-rose-500/20';
    if (status === 'PENDING') colorClass = 'bg-amber-50 text-amber-600 border-amber-100 dark:bg-amber-500/10 dark:text-amber-400 dark:border-amber-500/20';
    return { ...meta, colorClass };
  }
}
