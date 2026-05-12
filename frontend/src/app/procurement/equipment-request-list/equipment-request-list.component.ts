import { Component, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProcurementService } from '../procurement.service';
import { ToastService } from '../../shared/toast.service';
import { EquipmentRequest, STATUS_META, RequestStatus, PurchaseOrder } from '../procurement.models';

@Component({
  selector: 'app-equipment-request-list',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="flex flex-col gap-8 pb-12">
      
      <!-- PROFESSIONAL GOODS RECEIPT PANEL (Centered Modal) -->
      <div *ngIf="showReceiptModal" class="fixed inset-0 z-[100] flex justify-center items-start pt-12 overflow-y-auto pb-12">
        <!-- Transparent Blocking Layer -->
        <div class="absolute inset-0 bg-transparent" (click)="showReceiptModal = false"></div>
        
        <div class="bg-white rounded-[2rem] w-full max-w-4xl shadow-[0_20px_60px_rgba(0,0,0,0.12)] relative z-10 overflow-hidden border border-slate-200/60 flex flex-col max-h-[90vh]">
          <!-- Header -->
          <div class="bg-slate-900 p-8 text-white shrink-0">
            <div class="flex justify-between items-start">
              <div class="flex items-center gap-5">
                <div class="w-14 h-14 rounded-2xl bg-indigo-600 flex items-center justify-center text-white shadow-lg shadow-indigo-500/20">
                  <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><path d="M20 7h-9m9 4H9m11 4H9m-5-8v12M4 7l3 3m-3 0 3-3"/></svg>
                </div>
                <div>
                  <span class="text-[10px] font-black uppercase tracking-[0.2em] text-indigo-400 mb-1 block">Warehouse Operations</span>
                  <h3 class="text-2xl font-black tracking-tight">Goods Receipt Note (GRN)</h3>
                </div>
              </div>
              <div class="text-right">
                <span class="text-[10px] font-black uppercase tracking-[0.2em] text-slate-400 mb-1 block">Order Reference</span>
                <p class="font-mono text-sm text-indigo-300">#{{ selectedOrderForReceipt?.id?.substring(0,8) }}</p>
              </div>
            </div>

            <!-- SUPPLIER INFO & REPUTATION -->
            <div class="mt-8 flex items-center justify-between bg-white/5 backdrop-blur-md rounded-2xl p-5 border border-white/10">
              <div class="flex items-center gap-6">
                <div class="w-12 h-12 rounded-full bg-gradient-to-tr from-indigo-500 to-purple-500 flex items-center justify-center text-white text-sm font-black shadow-inner">
                  {{ selectedOrderForReceipt?.supplierName?.substring(0,2)?.toUpperCase() }}
                </div>
                <div class="border-r border-white/10 pr-6">
                  <p class="text-[9px] font-black text-slate-400 uppercase tracking-widest">Selected Supplier</p>
                  <p class="text-base font-bold text-white">{{ selectedOrderForReceipt?.supplierName }}</p>
                </div>
                <div class="flex flex-col gap-1">
                  <p class="text-[9px] font-black text-slate-400 uppercase tracking-widest">Supplier Reputation</p>
                  <div class="flex items-center gap-2">
                    <div class="flex text-amber-400 text-xs">⭐⭐⭐⭐⭐</div>
                    <span class="text-xs font-black text-emerald-400">4.9/5.0</span>
                  </div>
                </div>
              </div>
              
              <a [href]="getInvoiceUrl()" target="_blank" class="flex items-center gap-3 px-5 py-3 bg-indigo-600 hover:bg-white hover:text-indigo-600 text-white text-[11px] font-black rounded-xl transition-all shadow-lg shadow-indigo-900/20 group">
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5" class="group-hover:scale-110 transition-transform" viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
                VIEW ORIGINAL INVOICE (PDF)
              </a>
            </div>
          </div>

          <!-- Scrollable Content -->
          <div class="p-8 overflow-y-auto custom-scrollbar flex-1 bg-slate-50/30">
            <div class="flex flex-col gap-10">
              
              <!-- STEP 1: ITEM INSPECTION -->
              <div>
                <div class="flex items-center justify-between mb-4">
                  <div class="flex items-center gap-3">
                    <div class="w-8 h-8 rounded-lg bg-indigo-100 text-indigo-600 flex items-center justify-center text-xs font-black shadow-sm">01</div>
                    <h4 class="text-sm font-black text-slate-800 uppercase tracking-widest">Itemized Inspection Checklist</h4>
                  </div>
                  <div class="flex gap-4">
                    <span class="text-[10px] font-black text-indigo-500 uppercase px-3 py-1 bg-indigo-50 rounded-full border border-indigo-100">
                      {{ checkedCount }} / {{ selectedOrderForReceipt?.items?.length }} VERIFIED
                    </span>
                  </div>
                </div>
                
                <div class="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-sm">
                  <table class="w-full text-left border-collapse">
                    <thead>
                      <tr class="bg-slate-50">
                        <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">Inventory Item & Specifications</th>
                        <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest text-center">Expected Qty</th>
                        <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest text-right">Verification</th>
                      </tr>
                    </thead>
                    <tbody class="divide-y divide-slate-100">
                      <tr *ngFor="let item of selectedOrderForReceipt?.items; let i = index" 
                          [ngClass]="{'bg-emerald-50/20': checkedItems[i]}"
                          class="hover:bg-slate-50 transition-colors">
                        <td class="px-6 py-5">
                          <div class="flex flex-col gap-2">
                            <div>
                              <p class="text-sm font-bold" [ngClass]="checkedItems[i] ? 'text-emerald-700' : 'text-slate-700'">{{ item.name }}</p>
                              <p class="text-[10px] text-slate-400 mt-0.5">{{ item.description || 'Verified specs in invoice' }}</p>
                            </div>
                            <!-- SPECIFICATIONS DISPLAY -->
                            <div *ngIf="item.selectedSpecs && objectKeys(item.selectedSpecs).length > 0" class="flex flex-wrap gap-2 mt-1">
                              <div *ngFor="let key of objectKeys(item.selectedSpecs)" class="flex items-center bg-slate-100/80 border border-slate-200 rounded-lg px-2 py-1">
                                <span class="text-[9px] font-black text-slate-400 uppercase mr-1.5">{{ key }}:</span>
                                <span class="text-[10px] font-bold text-slate-700">{{ item.selectedSpecs[key] }}</span>
                              </div>
                            </div>
                            <div *ngIf="!item.selectedSpecs || objectKeys(item.selectedSpecs).length === 0" class="text-[10px] text-slate-400 italic">
                              Standard configuration as per catalog.
                            </div>
                          </div>
                        </td>
                        <td class="px-6 py-5 text-center">
                          <span class="px-3 py-1.5 bg-slate-100 text-slate-600 rounded-xl text-xs font-black border border-slate-200 shadow-sm">×{{ item.quantity }}</span>
                        </td>
                        <td class="px-6 py-5 text-right">
                          <label class="relative inline-flex items-center cursor-pointer group">
                            <input type="checkbox" class="sr-only peer" [(ngModel)]="checkedItems[i]">
                            <div class="w-11 h-6 bg-slate-200 peer-focus:outline-none rounded-full peer peer-checked:bg-emerald-500 peer-checked:after:translate-x-full rtl:peer-checked:after:-translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:start-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all shadow-inner group-hover:after:scale-110"></div>
                          </label>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>

              <!-- STEP 2 & 3 -->
              <div class="grid grid-cols-1 md:grid-cols-2 gap-10">
                <div>
                  <div class="flex items-center gap-3 mb-4">
                    <div class="w-8 h-8 rounded-lg bg-indigo-100 text-indigo-600 flex items-center justify-center text-xs font-black shadow-sm">02</div>
                    <h4 class="text-sm font-black text-slate-800 uppercase tracking-widest">Your Experience Audit</h4>
                  </div>
                  <div class="flex flex-col gap-4 bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
                    <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest text-center">How was the delivery quality?</p>
                    <div class="flex gap-3 justify-center">
                      <button *ngFor="let star of [1,2,3,4,5]" 
                              (click)="receiptRating = star"
                              type="button"
                              class="text-4xl transition-all hover:scale-125 focus:outline-none filter drop-shadow-sm"
                              [class.grayscale]="receiptRating < star">
                        {{ receiptRating >= star ? '⭐' : '☆' }}
                      </button>
                    </div>
                  </div>
                </div>

                <div>
                  <div class="flex items-center gap-3 mb-4">
                    <div class="w-8 h-8 rounded-lg bg-indigo-100 text-indigo-600 flex items-center justify-center text-xs font-black shadow-sm">03</div>
                    <h4 class="text-sm font-black text-slate-800 uppercase tracking-widest">Inspection Notes & Log</h4>
                  </div>
                  <textarea [(ngModel)]="receiptNotes" 
                            placeholder="Describe any damage to packaging, serial number mismatches, or perfect condition..."
                            class="w-full bg-white border border-slate-200 rounded-2xl p-5 text-sm focus:ring-2 focus:ring-indigo-500 outline-none transition-all min-h-[120px] shadow-sm"></textarea>
                </div>
              </div>
            </div>
          </div>

          <!-- Footer Actions -->
          <div class="p-8 bg-white border-t border-slate-100 flex gap-4 shrink-0 shadow-[0_-10px_20px_rgba(0,0,0,0.02)]">
            <button (click)="showReceiptModal = false" type="button" class="px-6 py-4 bg-slate-50 text-slate-500 font-black rounded-2xl hover:bg-slate-100 transition-all uppercase text-[10px] tracking-widest border border-slate-200">Cancel Audit</button>
            
            <button (click)="submitReceipt(false)" 
                    type="button"
                    [disabled]="submittingReceipt || !allChecked"
                    class="flex-1 px-6 py-4 bg-slate-200 text-slate-600 font-black rounded-2xl hover:bg-slate-300 transition-all uppercase text-[10px] tracking-widest disabled:opacity-30">
              Confirm Receipt Only
            </button>

            <button (click)="submitReceipt(true)" 
                    type="button"
                    [disabled]="submittingReceipt || !allChecked"
                    class="flex-[1.5] px-6 py-4 bg-indigo-600 text-white font-black rounded-2xl hover:bg-slate-900 transition-all uppercase text-[10px] tracking-widest shadow-2xl shadow-indigo-200 disabled:opacity-20 disabled:cursor-not-allowed flex items-center justify-center gap-3 active:scale-[0.98] border border-indigo-500/20">
              <span *ngIf="submittingReceipt" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
              <div *ngIf="!submittingReceipt" class="flex flex-col items-center">
                <span class="flex items-center gap-2">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" stroke="currentColor" stroke-width="3" viewBox="0 0 24 24"><path d="M12 2L2 7l10 5 10-5-10-5zM2 17l10 5 10-5M2 12l10 5 10-5"/></svg>
                  Verify & Add to Inventory (AI)
                </span>
                <span class="text-[8px] opacity-70 mt-0.5">Auto-generates Serials & QR</span>
              </div>
            </button>
          </div>
        </div>
      </div>

      <!-- HEADER & FILTERS -->
      <div class="flex flex-col md:flex-row justify-between items-center bg-white/80 backdrop-blur-md p-5 rounded-2xl border border-slate-200/60 shadow-sm gap-4">
        <div class="flex items-center gap-4">
          <div class="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center shadow-sm">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
          </div>
          <div>
            <h2 class="text-xl font-black text-slate-800 tracking-tight">My Equipment Requests</h2>
            <p class="text-xs text-slate-400 font-bold uppercase tracking-widest">Stock Manager Procurement</p>
          </div>
        </div>

        <div class="flex items-center gap-3 bg-slate-50 p-1.5 rounded-2xl border border-slate-200/50">
          <div class="flex bg-white rounded-xl shadow-sm border border-slate-200 p-0.5 mr-2">
            <button (click)="viewMode = 'grid'" 
                    [class.bg-indigo-600]="viewMode === 'grid'" 
                    [class.text-white]="viewMode === 'grid'"
                    class="p-2 rounded-lg transition-all duration-200">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><rect width="7" height="7" x="3" y="3" rx="1"/><rect width="7" height="7" x="14" y="3" rx="1"/><rect width="7" height="7" x="14" y="14" rx="1"/><rect width="7" height="7" x="3" y="14" rx="1"/></svg>
            </button>
            <button (click)="viewMode = 'list'" 
                    [class.bg-indigo-600]="viewMode === 'list'" 
                    [class.text-white]="viewMode === 'list'"
                    class="p-2 rounded-lg transition-all duration-200">
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>
            </button>
          </div>

          <div class="flex gap-1">
            <button *ngFor="let s of ['ALL', 'PENDING_IT_APPROVAL', 'APPROVED', 'SENT_TO_SUPPLIERS']"
                    (click)="filterStatus = s"
                    class="px-3 py-2 rounded-xl text-[10px] font-black uppercase tracking-wider transition-all"
                    [ngClass]="filterStatus === s ? 'bg-slate-800 text-white shadow-lg shadow-slate-200' : 'text-slate-500 hover:bg-white hover:text-indigo-600'">
              {{ s === 'ALL' ? 'All' : s.replace('_IT_APPROVAL', '').replace('_TO_SUPPLIERS', '').replace('_', ' ') }}
            </button>
          </div>
        </div>
      </div>

      <div *ngIf="loading" class="flex items-center justify-center p-20 text-slate-500 gap-3">
        <div class="w-8 h-8 border-3 border-indigo-100 border-t-indigo-600 rounded-full animate-spin"></div>
        <span class="font-black uppercase tracking-widest text-xs">Syncing with Central Stock...</span>
      </div>
      
      <div *ngIf="!loading">
        <div *ngIf="activeRequests.length === 0" class="flex flex-col items-center justify-center p-20 bg-white/50 backdrop-blur-sm border border-dashed border-slate-300 rounded-3xl text-slate-400">
          <p class="font-bold uppercase tracking-widest text-xs">No active procurement requests</p>
        </div>

        <!-- GRID VIEW -->
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6" *ngIf="viewMode === 'grid'">
          <div *ngFor="let req of activeRequests" 
               class="bg-white border border-slate-200 rounded-2xl p-6 hover:border-indigo-400 hover:shadow-2xl hover:shadow-indigo-500/10 transition-all group flex flex-col h-full cursor-pointer relative overflow-hidden" 
               (click)="selectRequest(req)">
            
            <div class="absolute top-0 right-0 p-3">
              <span class="px-3 py-1 rounded-full text-[10px] font-black uppercase tracking-tighter" [ngClass]="getStatusMeta(req.status).colorClass">
                {{ getStatusMeta(req.status).label }}
              </span>
            </div>

            <div class="flex-1 pt-4">
              <div class="flex flex-col gap-3 mb-4">
                <div *ngFor="let item of req.items.slice(0,3)" class="flex flex-col gap-1 bg-slate-50 p-3 rounded-xl border border-slate-100">
                  <div class="flex items-center justify-between">
                    <span class="text-sm font-black text-slate-800">{{ item.name }}</span>
                    <span class="text-xs font-bold px-2 py-0.5 bg-slate-200 rounded-md text-slate-600">×{{ item.quantity }}</span>
                  </div>
                  <div *ngIf="item.selectedSpecs && objectKeys(item.selectedSpecs).length > 0" class="flex flex-wrap gap-1 mt-1">
                    <span *ngFor="let key of objectKeys(item.selectedSpecs)" class="px-1.5 py-0.5 bg-indigo-100 text-indigo-700 text-[9px] rounded font-black uppercase tracking-tighter">
                      {{ key }}: {{ item.selectedSpecs[key] }}
                    </span>
                  </div>
                </div>
                <div *ngIf="(req.items.length || 0) > 3" class="text-center">
                  <span class="text-[10px] font-black text-slate-300 uppercase">+{{ (req.items.length || 0) - 3 }} more items</span>
                </div>
              </div>
              <div *ngIf="req.status === 'ORDER_CONFIRMED' || req.status === 'RECEIVED'" class="mt-4 p-3 bg-emerald-50 border border-emerald-100 rounded-xl">
                <p class="text-[9px] font-black text-emerald-600 uppercase mb-1">Confirmed Supplier</p>
                <p class="text-xs font-bold text-slate-800">{{ req.supplierName || 'System Confirmed' }}</p>
              </div>
              <p *ngIf="req.notes" class="text-xs text-slate-500 italic line-clamp-2 bg-indigo-50/30 p-3 rounded-xl border border-indigo-50 mt-3">"{{ req.notes }}"</p>
            </div>

            <div class="mt-6 pt-4 border-t border-slate-100 flex justify-between items-center">
              <div class="flex items-center gap-2">
                <button *ngIf="req.status === 'ORDER_CONFIRMED'" 
                        (click)="$event.stopPropagation(); openReceiptModal(req)"
                        class="px-4 py-2 bg-indigo-600 text-white text-[10px] font-black rounded-xl hover:bg-slate-900 transition-all shadow-lg shadow-indigo-100">
                  CONFIRM RECEIPT
                </button>
                <div *ngIf="req.status !== 'ORDER_CONFIRMED'" class="flex items-center gap-2 text-[10px] text-slate-400 font-black uppercase tracking-widest">
                  <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                  {{ formatDate(req.createdAt) }}
                </div>
              </div>
              <div class="w-8 h-8 rounded-full bg-slate-50 flex items-center justify-center text-slate-400 group-hover:bg-indigo-600 group-hover:text-white transition-all shadow-sm">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" stroke="currentColor" stroke-width="3" viewBox="0 0 24 24"><line x1="5" y1="12" x2="19" y2="12"/><polyline points="12 5 19 12 12 19"/></svg>
              </div>
            </div>
          </div>
        </div>

        <!-- LIST VIEW -->
        <div class="bg-white border border-slate-200 rounded-3xl overflow-hidden shadow-sm max-h-[500px] overflow-y-auto custom-scrollbar" *ngIf="viewMode === 'list' && activeRequests.length > 0">
          <table class="w-full text-left border-collapse sticky-header">
            <thead>
              <tr class="bg-slate-50/80 backdrop-blur-sm sticky top-0 z-10">
                <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">Date</th>
                <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">Items</th>
                <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">Supplier</th>
                <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">Status</th>
                <th class="px-6 py-4 text-[10px] font-black text-slate-400 uppercase tracking-widest">Action</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-100">
              <tr *ngFor="let req of activeRequests" (click)="selectRequest(req)" class="hover:bg-slate-50 cursor-pointer transition-colors group">
                <td class="px-6 py-4 text-xs font-bold text-slate-500">{{ formatDate(req.createdAt) }}</td>
                <td class="px-6 py-4">
                  <div class="flex gap-2">
                    <span *ngFor="let item of req.items.slice(0,2)" class="px-2 py-1 bg-slate-800 text-white text-[10px] font-black rounded-lg">
                      {{ item.name }} ({{ item.quantity }})
                    </span>
                    <span *ngIf="req.items.length > 2" class="text-[10px] text-slate-300 font-bold self-center">+{{ req.items.length - 2 }}</span>
                  </div>
                </td>
                <td class="px-6 py-4 text-xs font-bold text-slate-700">
                  {{ req.supplierName || '—' }}
                </td>
                <td class="px-6 py-4">
                  <span class="px-2.5 py-1 rounded-full text-[9px] font-black uppercase" [ngClass]="getStatusMeta(req.status).colorClass">
                    {{ getStatusMeta(req.status).label }}
                  </span>
                </td>
                <td class="px-6 py-4 text-right">
                  <button *ngIf="req.status === 'ORDER_CONFIRMED'" 
                          (click)="$event.stopPropagation(); openReceiptModal(req)"
                          class="px-3 py-1.5 bg-indigo-600 text-white text-[9px] font-black rounded-lg hover:bg-slate-900 transition-all shadow-lg shadow-indigo-100">
                    RECEIVE
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- REJECTED HISTORY SECTION -->
      <div class="mt-12" *ngIf="!loading && rejectedRequests.length > 0">
        <div class="flex items-center gap-4 mb-6">
          <div class="w-10 h-10 rounded-xl bg-rose-50 text-rose-500 flex items-center justify-center border border-rose-100 shadow-sm">
            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
          </div>
          <h3 class="text-lg font-black text-slate-800 uppercase tracking-tight">Rejected Requests History</h3>
        </div>

        <div class="bg-white border border-rose-100 rounded-3xl overflow-hidden shadow-xl shadow-rose-500/5">
          <table class="w-full text-left border-collapse">
            <thead class="bg-rose-50/50">
              <tr>
                <th class="px-6 py-4 text-[10px] font-black text-rose-400 uppercase tracking-widest">Rejection Date</th>
                <th class="px-6 py-4 text-[10px] font-black text-rose-400 uppercase tracking-widest">Requested Items</th>
                <th class="px-6 py-4 text-[10px] font-black text-rose-400 uppercase tracking-widest">Rejection Reason</th>
                <th class="px-6 py-4 text-[10px] font-black text-rose-400 uppercase tracking-widest">Action</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-rose-50">
              <tr *ngFor="let req of rejectedRequests" (click)="selectRequest(req)" class="hover:bg-rose-50/30 transition-colors cursor-pointer">
                <td class="px-6 py-4 text-xs font-bold text-slate-500">{{ formatDate(req.updatedAt || req.createdAt) }}</td>
                <td class="px-6 py-4">
                  <div class="flex flex-wrap gap-1">
                    <span *ngFor="let item of req.items" class="px-2 py-0.5 bg-slate-100 text-slate-600 text-[10px] font-bold rounded border border-slate-200">
                      {{ item.name }}
                    </span>
                  </div>
                </td>
                <td class="px-6 py-4">
                  <div class="flex items-start gap-2 text-rose-700 bg-rose-100/50 p-2 rounded-xl border border-rose-100 text-xs">
                    <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2.5" class="mt-0.5 shrink-0" viewBox="0 0 24 24"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><line x1="12" y1="9" x2="12" y2="13"/><line x1="12" y1="17" x2="12.01" y2="17"/></svg>
                    <span class="font-medium italic">"{{ req.rejectionReason || 'No reason specified' }}"</span>
                  </div>
                </td>
                <td class="px-6 py-4">
                  <button (click)="$event.stopPropagation(); selectRequest(req)" class="text-[10px] font-black text-indigo-600 uppercase hover:underline">View Details</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `,
})
export class EquipmentRequestListComponent implements OnInit {
  @Output() requestSelected = new EventEmitter<EquipmentRequest>();

  requests: EquipmentRequest[] = [];
  loading = true;
  filterStatus = 'ALL';
  viewMode: 'grid' | 'list' = 'grid';
  objectKeys = Object.keys;

  // Receipt Modal State
  showReceiptModal = false;
  submittingReceipt = false;
  selectedOrderForReceipt: PurchaseOrder | null = null;
  receiptNotes = '';
  receiptRating = 5;
  checkedItems: boolean[] = [];

  constructor(
    private procService: ProcurementService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.procService.getAllRequests().subscribe({
      next: (data) => { this.requests = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  get activeRequests(): EquipmentRequest[] {
    const active = this.requests.filter(r => r.status !== 'REJECTED');
    return this.filterStatus === 'ALL'
      ? active
      : active.filter(r => r.status === this.filterStatus);
  }

  get rejectedRequests(): EquipmentRequest[] {
    return this.requests.filter(r => r.status === 'REJECTED');
  }

  get checkedCount(): number {
    return this.checkedItems.filter(v => v).length;
  }

  get allChecked(): boolean {
    if (!this.selectedOrderForReceipt?.items) return false;
    return this.checkedItems.length === this.selectedOrderForReceipt.items.length && 
           this.checkedItems.every(v => v === true);
  }

  selectRequest(req: EquipmentRequest): void {
    this.requestSelected.emit(req);
  }

  openReceiptModal(req: EquipmentRequest): void {
    const container = document.getElementById('procurement-scroll-container');
    if (container) {
      container.scrollTop = 0;
    } else {
      window.scrollTo(0, 0);
    }
    
    this.procService.getOrderByRequest(req.id!).subscribe({
      next: (po) => {
        this.selectedOrderForReceipt = po;
        this.receiptNotes = '';
        this.receiptRating = 5;
        this.checkedItems = new Array(po.items?.length || 0).fill(false);
        this.showReceiptModal = true;
      },
      error: () => this.toastService.error('Could not find order details for this request.')
    });
  }

  getInvoiceUrl(): string {
    if (!this.selectedOrderForReceipt?.selectedResponseId) return '#';
    return this.procService.getViewUrl(this.selectedOrderForReceipt.selectedResponseId);
  }

  submitReceipt(postToStock: boolean): void {
    if (!this.selectedOrderForReceipt) return;
    if (!this.allChecked) {
      this.toastService.error('Please verify all items against the invoice before confirmation.');
      return;
    }
    
    this.submittingReceipt = true;
    this.procService.confirmReceipt(
      this.selectedOrderForReceipt.id!, 
      this.receiptNotes, 
      this.receiptRating,
      postToStock
    ).subscribe({
      next: () => {
        this.submittingReceipt = false;
        this.showReceiptModal = false;
        this.toastService.success(
          postToStock 
            ? 'Success! Items verified and automatically added to inventory.' 
            : 'Audit complete. Request marked as received.'
        );
        this.load();
      },
      error: () => {
        this.submittingReceipt = false;
        this.toastService.error('Failed to post receipt. Database rejected the audit.');
      }
    });
  }

  getStatusMeta(status?: RequestStatus) {
    return STATUS_META[status || 'PENDING_IT_APPROVAL'];
  }

  formatDate(d?: string): string {
    if (!d) return '';
    return new Date(d).toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric' });
  }
}
