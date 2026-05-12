import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProcurementService } from '../procurement.service';
import { SupplierResponse, EquipmentRequest, RESPONSE_STATUS_META, SupplierResponseStatus } from '../procurement.models';
import { AiService } from '../../ai-assistant/ai.service';
import { ToastService } from '../../shared/toast.service';
import { forkJoin, from, of, Observable } from 'rxjs';
import { map, catchError, mergeMap, toArray } from 'rxjs/operators';

interface GroupedResponses {
  requestId: string;
  requestTitle: string;
  responses: SupplierResponse[];
}

@Component({
  selector: 'app-supplier-response-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="flex flex-col gap-6">
      
      <!-- Header -->
      <div class="bg-white/70 backdrop-blur-md border border-slate-200/60 rounded-2xl p-6 shadow-sm">
        <div class="flex items-center gap-4 mb-2">
          <div class="w-12 h-12 rounded-xl flex items-center justify-center bg-teal-50 text-teal-600 shrink-0">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
          </div>
          <div>
            <h2 class="text-lg font-bold text-slate-800 m-0">Quotation Management</h2>
            <p class="text-sm text-slate-500 mt-1">Grouped by Request. Compare and select the best offers.</p>
          </div>
        </div>
      </div>

      <!-- Loading -->
      <div *ngIf="isLoading" class="flex flex-col items-center justify-center py-20 bg-white/50 rounded-3xl border border-slate-100">
        <div class="w-10 h-10 border-4 border-slate-200 border-t-teal-600 rounded-full animate-spin mb-4"></div>
        <p class="text-slate-500 font-medium">Synchronizing quotations...</p>
      </div>

      <!-- Grouped List -->
      <div *ngIf="!isLoading && groupedResponses.length > 0" class="flex flex-col gap-6">
        <div *ngFor="let group of groupedResponses" class="bg-white border border-slate-200 rounded-3xl overflow-hidden shadow-sm hover:shadow-md transition-shadow">
          
          <!-- Group Header -->
          <div class="p-5 bg-slate-50/80 border-b border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div class="flex items-center gap-4">
              <div class="w-10 h-10 rounded-full bg-white border border-slate-200 flex items-center justify-center text-teal-600 shadow-sm">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/></svg>
              </div>
              <div>
                <h3 class="font-bold text-slate-800">{{ group.requestTitle || 'Unknown Request' }}</h3>
                <div class="flex items-center gap-2 mt-1">
                  <span class="text-[10px] font-mono text-slate-400 bg-white px-1.5 py-0.5 rounded border border-slate-100">ID: {{ group.requestId }}</span>
                  <span class="text-[10px] font-bold text-teal-600 uppercase tracking-wider">{{ group.responses.length }} Response(s)</span>
                </div>
              </div>
            </div>
            <div class="flex items-center gap-2">
              <button class="px-4 py-2 bg-white border border-slate-200 rounded-xl text-xs font-bold text-slate-600 hover:bg-slate-50 transition-colors" (click)="toggleGroup(group.requestId)">
                {{ expandedGroups.has(group.requestId) ? 'Collapse' : 'Show Responses' }}
              </button>
            </div>
          </div>

          <!-- Group Body (Responses Table) -->
          <div *ngIf="expandedGroups.has(group.requestId)" class="p-6 bg-slate-50/50 border-t border-slate-200 animate-in fade-in duration-300">
            
            <!-- AI QUOTATION ANALYST CARD (Photo 2 Style) -->
            <div class="mb-6 group/ai relative overflow-hidden">
              <div class="absolute inset-0 bg-gradient-to-r from-indigo-500/10 via-purple-500/10 to-blue-500/10 opacity-0 group-hover/ai:opacity-100 transition-opacity"></div>
              <div class="bg-white/80 backdrop-blur-sm border border-indigo-100 rounded-2xl p-5 shadow-sm relative z-10">
                <div class="flex items-center justify-between mb-4">
                  <div class="flex items-center gap-4">
                    <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-600 to-blue-600 text-white flex items-center justify-center shadow-lg shadow-indigo-200">
                      <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path d="m12 3-1.912 5.813a2 2 0 0 1-1.275 1.275L3 12l5.813 1.912a2 2 0 0 1 1.275 1.275L12 21l1.912-5.813a2 2 0 0 1 1.275-1.275L21 12l-5.813-1.912a2 2 0 0 1-1.275-1.275L12 3Z"/></svg>
                    </div>
                    <div>
                      <h3 class="text-sm font-black text-slate-800 uppercase tracking-tight">AI Quotation Analyst</h3>
                      <span class="px-2 py-0.5 bg-indigo-50 text-indigo-600 text-[9px] font-black uppercase rounded-full border border-indigo-100">SMART-MATCH</span>
                    </div>
                  </div>
                  <button (click)="runAIAnalysis(group)" 
                          [disabled]="isAnalyzing[group.requestId]"
                          class="px-4 py-2 bg-slate-900 text-white text-[11px] font-bold rounded-xl hover:bg-indigo-600 transition-all flex items-center gap-2 shadow-xl shadow-slate-200 disabled:opacity-50">
                    <span *ngIf="isAnalyzing[group.requestId]" class="w-3 h-3 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                    {{ aiAnalysis[group.requestId] ? 'Refresh Analysis' : 'Run Smart Comparison' }}
                  </button>
                </div>

                <!-- AI Results -->
                <div *ngIf="aiAnalysis[group.requestId]" class="animate-in fade-in slide-in-from-top-2 duration-500">
                  <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <div class="md:col-span-2">
                      <p class="text-xs font-black text-indigo-600 uppercase tracking-widest mb-2">Recommendation</p>
                      <p class="text-sm text-slate-700 leading-relaxed font-medium italic">
                        "{{ aiAnalysis[group.requestId].reasoning }}"
                      </p>
                      <div class="mt-4 flex flex-wrap gap-4">
                        <div class="flex-1 min-w-[150px]">
                          <p class="text-[10px] font-bold text-emerald-600 uppercase mb-2 flex items-center gap-1"><svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" fill="none" stroke="currentColor" stroke-width="3" viewBox="0 0 24 24"><polyline points="20 6 9 17 4 12"/></svg> Key Pros</p>
                          <ul class="space-y-1">
                            <li *ngFor="let pro of aiAnalysis[group.requestId].keyPros" class="text-xs text-slate-600 flex items-start gap-2">
                              <span class="w-1 h-1 rounded-full bg-emerald-400 mt-1.5 shrink-0"></span> {{ pro }}
                            </li>
                          </ul>
                        </div>
                        <div class="flex-1 min-w-[150px]">
                          <p class="text-[10px] font-bold text-rose-500 uppercase mb-2 flex items-center gap-1"><svg xmlns="http://www.w3.org/2000/svg" width="12" height="12" fill="none" stroke="currentColor" stroke-width="3" viewBox="0 0 24 24"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg> Key Cons</p>
                          <ul class="space-y-1">
                            <li *ngFor="let con of aiAnalysis[group.requestId].keyCons" class="text-xs text-slate-600 flex items-start gap-2">
                              <span class="w-1 h-1 rounded-full bg-rose-400 mt-1.5 shrink-0"></span> {{ con }}
                            </li>
                          </ul>
                        </div>
                      </div>
                    </div>
                    <div class="bg-indigo-50/50 rounded-xl p-4 border border-indigo-50 flex flex-col justify-center text-center">
                      <p class="text-[9px] font-black text-indigo-400 uppercase tracking-widest mb-1">Top Pick</p>
                      <div class="text-lg font-black text-indigo-600 leading-tight mb-2">{{ aiAnalysis[group.requestId].recommendedSupplier }}</div>
                      <div class="text-[10px] text-indigo-500 font-bold italic">"{{ aiAnalysis[group.requestId].summary }}"</div>
                    </div>
                  </div>
                </div>

                <div *ngIf="!aiAnalysis[group.requestId] && !isAnalyzing[group.requestId]" class="text-center py-4">
                  <p class="text-xs text-slate-400 italic">Click "Run Smart Comparison" to let AI analyze quotes based on price, delivery, and item specifications.</p>
                </div>
              </div>
            </div>

            <div class="overflow-x-auto rounded-2xl border border-slate-200 bg-white">
              <table class="w-full text-left border-collapse whitespace-nowrap">
              <thead>
                <tr class="bg-white">
                  <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest border-b border-slate-100">Supplier</th>
                  <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest border-b border-slate-100">Price</th>
                  <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest border-b border-slate-100">Delivery</th>
                  <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest border-b border-slate-100">Status</th>
                  <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest border-b border-slate-100 text-right">Actions</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-50">
                <tr *ngFor="let resp of group.responses" class="hover:bg-teal-50/30 transition-colors" [ngClass]="{'bg-emerald-50/50': resp.status === 'APPROVED_SUPPLIER'}">
                  <td class="px-6 py-4">
                    <div class="flex items-center gap-3">
                      <div class="w-8 h-8 rounded-lg bg-slate-100 flex items-center justify-center text-slate-500 font-bold text-[10px]">
                        {{ resp.supplierName?.substring(0,2).toUpperCase() }}
                      </div>
                      <div>
                        <div class="text-sm font-bold text-slate-700">{{ resp.supplierName }}</div>
                        <div class="text-[10px] text-slate-400 font-mono">{{ resp.supplierId }}</div>
                      </div>
                    </div>
                  </td>
                  <td class="px-6 py-4">
                    <div class="text-sm font-bold text-slate-800">{{ resp.totalPrice | number:'1.2-2' }} <span class="text-[10px] text-slate-400">{{ resp.currency }}</span></div>
                  </td>
                  <td class="px-6 py-4">
                    <div class="text-sm text-slate-600">{{ resp.deliveryDays }} days</div>
                  </td>
                  <td class="px-6 py-4">
                    <span class="px-2.5 py-1 rounded-full text-[9px] font-black uppercase tracking-wider" [ngClass]="getRespMeta(resp.status).colorClass">
                      {{ getRespMeta(resp.status).label }}
                    </span>
                  </td>
                  <td class="px-6 py-4 text-right">
                    <div class="flex items-center justify-end gap-2">
                      <a [href]="viewUrl(resp.id!)" target="_blank" class="p-2 text-slate-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-all" title="View PDF in Browser">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                      </a>
                      <a [href]="downloadUrl(resp.id!)" target="_blank" class="p-2 text-slate-400 hover:text-teal-600 hover:bg-teal-50 rounded-lg transition-all" title="Download PDF">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                      </a>
                      
                      <button class="px-3 py-1.5 bg-teal-600 text-white text-[10px] font-bold rounded-lg hover:bg-teal-700 transition-colors shadow-sm" *ngIf="resp.status === 'PENDING'" (click)="approve(resp.id!)">SELECT</button>
                      <button class="px-3 py-1.5 bg-indigo-600 text-white text-[10px] font-bold rounded-lg hover:bg-indigo-700 transition-colors shadow-sm" *ngIf="resp.status === 'APPROVED_SUPPLIER'" (click)="createOrder.emit(resp)">CONFIRM ORDER</button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div *ngIf="!isLoading && groupedResponses.length === 0" class="flex flex-col items-center justify-center py-20 bg-white rounded-3xl border border-dashed border-slate-200">
        <div class="w-16 h-16 bg-slate-50 text-slate-300 rounded-full flex items-center justify-center mb-4">
          <svg xmlns="http://www.w3.org/2000/svg" width="32" height="32" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
        </div>
        <p class="text-slate-500 font-medium">No quotations received yet.</p>
        <p class="text-xs text-slate-400 mt-1">Quotations will appear here once suppliers respond to your RFQs.</p>
      </div>

    </div>
  `
})
export class SupplierResponseListComponent implements OnInit {
  @Output() createOrder = new EventEmitter<SupplierResponse>();

  responses: SupplierResponse[] = [];
  requests: EquipmentRequest[] = [];
  groupedResponses: any[] = [];
  isLoading = true;
  expandedGroups: Set<string> = new Set();
  aiAnalysis: Record<string, any> = {};
  isAnalyzing: Record<string, boolean> = {};

  constructor(
    private procService: ProcurementService,
    private aiService: AiService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.isLoading = true;
    // Fetch both requests and responses to group them effectively
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
    const groupsMap = new Map<string, any>();

    this.responses.forEach(resp => {
      const rid = resp.requestId || 'Unknown';
      if (!groupsMap.has(rid)) {
        const req = this.requests.find(r => r.id === rid);
        
        // Skip confirmed requests
        if (req && req.status === 'ORDER_CONFIRMED') return;

        groupsMap.set(rid, {
          requestId: rid,
          requestTitle: req ? (req.notes || 'Request ' + rid.substring(0,8)) : 'Order for ' + (resp.supplierName || 'Items'),
          responses: []
        });
        // Auto-expand first group
        if (this.expandedGroups.size === 0) this.expandedGroups.add(rid);
      }
      groupsMap.get(rid).responses.push(resp);
    });

    this.groupedResponses = Array.from(groupsMap.values());
  }

  toggleGroup(requestId: string): void {
    if (this.expandedGroups.has(requestId)) {
      this.expandedGroups.delete(requestId);
    } else {
      this.expandedGroups.add(requestId);
    }
  }

  approve(id: string): void {
    this.procService.approveResponse(id).subscribe({ next: () => this.load() });
  }

  downloadUrl(id: string): string {
    return this.procService.getDownloadUrl(id);
  }

  viewUrl(id: string): string {
    return this.procService.getViewUrl(id);
  }

  runAIAnalysis(group: GroupedResponses): void {
    const requestId = group.requestId;
    this.isAnalyzing[requestId] = true;

    // We need to fetch all PDFs for this group
    const pdfFetchObservables = group.responses.map(resp => {
      const url = this.viewUrl(resp.id!);
      return from(fetch(url).then(res => res.blob())).pipe(
        mergeMap(blob => this.blobToBase64(blob)),
        map(base64 => ({ ...resp, pdfBase64: base64 })),
        catchError(() => of({ ...resp, pdfBase64: null })) // fallback if PDF fails to load
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
        next: (res) => {
          this.aiAnalysis[requestId] = res;
          this.isAnalyzing[requestId] = false;
          this.toastService.success('AI Deep Analysis complete (Invoices audited)');
        },
        error: () => {
          this.isAnalyzing[requestId] = false;
          this.toastService.error('AI Service is currently busy. Please try again.');
        }
      });
    });
  }

  private blobToBase64(blob: Blob): Observable<string> {
    return new Observable<string>(observer => {
      const reader = new FileReader();
      reader.onloadend = () => {
        let result = reader.result as string;
        // Strip data:application/pdf;base64, if present
        if (result.includes(',')) result = result.split(',')[1];
        observer.next(result);
        observer.complete();
      };
      reader.onerror = (err) => observer.error(err);
      reader.readAsDataURL(blob);
    });
  }

  getRespMeta(status?: SupplierResponseStatus) {
    return RESPONSE_STATUS_META[status || 'PENDING'];
  }
}
