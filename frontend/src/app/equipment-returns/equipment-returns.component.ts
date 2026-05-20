import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ItEquipmentService, ItEquipment } from '../it-equipment-management/it-equipment.service';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-equipment-returns',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './equipment-returns.component.html',
  styleUrl: './equipment-returns.component.css'
})
export class EquipmentReturnsComponent implements OnInit {

  activeTab: 'pending' | 'history' = 'pending';
  
  returnRequests: ItEquipment[] = [];
  filteredRequests: ItEquipment[] = [];
  
  returnsHistory: any[] = [];
  filteredHistory: any[] = [];

  searchQuery = '';
  searchHistoryQuery = '';
  loading = false;
  loadingHistory = false;
  
  toast: { message: string; type: 'success' | 'error' } | null = null;
  currentUser: any;
  allShelves: any[] = [];
  availableShelves: any[] = []; // Shelves with space

  // Process Modal
  showProcessModal = false;
  processTarget: ItEquipment | null = null;
  selectedStatus = 'Available';
  selectedShelfId = '';
  submitting = false;

  constructor(
    private equipmentService: ItEquipmentService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.authService.user$.subscribe(u => this.currentUser = u);
    this.refreshAll();
    this.loadShelves();
  }

  refreshAll(): void {
    this.loadReturns();
    this.loadHistory();
  }

  loadReturns(): void {
    this.loading = true;
    this.equipmentService.getReturnRequests().subscribe({
      next: (data) => {
        this.returnRequests = data;
        this.applyFilter();
        this.loading = false;
      },
      error: () => {
        this.showToast('Failed to load return requests', 'error');
        this.loading = false;
      }
    });
  }

  loadHistory(): void {
    this.loadingHistory = true;
    this.equipmentService.getAllEquipment().subscribe({
      next: (data: ItEquipment[]) => {
        const history: any[] = [];
        data.forEach((eq: ItEquipment) => {
          (eq.lifecycle || []).forEach((entry: any) => {
            if (entry.status === 'Returned to Stock') {
              // Try to extract the return note from the description
              // Description format is usually like: "Returned to stock. Note: <note>" or similar
              const desc: string = entry.description || '';
              // Extract the return note (after 'Note:' or 'Original return note:')
              const noteMatch = desc.match(/[Nn]ote:\s*(.+)/s);
              const note = noteMatch ? noteMatch[1].trim() : '';
              // Remove the note portion from the description to get a clean 'details' string
              const details = noteMatch 
                ? desc.replace(noteMatch[0], '').replace(/\.?\s*$/, '').trim() 
                : desc;
              history.push({
                equipmentName: eq.equipmentName,
                serialNumber: eq.serialNumber,
                icon: eq.icon,
                type: eq.type,
                timestamp: entry.timestamp,
                actor: entry.actor,
                description: desc,
                note: note,
                details: details
              });
            }
          });
        });
        history.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
        this.returnsHistory = history;
        this.applyHistoryFilter();
        this.loadingHistory = false;
      },
      error: () => {
        this.loadingHistory = false;
      }
    });
  }

  loadShelves(): void {
    this.equipmentService.getAllShelves().subscribe({
      next: (shelves) => {
        this.allShelves = shelves;
        this.updateAvailableShelves();
      },
      error: () => {}
    });
  }

  updateAvailableShelves(): void {
    const eqType = this.processTarget ? (this.processTarget.type || '').toLowerCase().trim() : '';
    // A shelf is available if it's a real shelf (not MAINTENANCE_AREA), has space, and matches equipment type (if specified)
    this.availableShelves = this.allShelves.filter(s => {
      const isRealShelf = !['MAINTENANCE_AREA', 'SCRAP_YARD'].includes(s.id);
      const hasSpace = (s.currentQte || 0) < (s.maxQte || 9999);
      const typeMatches = !eqType || (s.equipmentType && s.equipmentType.toLowerCase().trim() === eqType);
      return isRealShelf && hasSpace && typeMatches;
    });
  }

  applyFilter(): void {
    const q = this.searchQuery.toLowerCase().trim();
    this.filteredRequests = this.returnRequests.filter(eq => 
      !q ||
      eq.equipmentName?.toLowerCase().includes(q) ||
      eq.serialNumber?.toLowerCase().includes(q) ||
      eq.returnNote?.toLowerCase().includes(q)
    );
  }

  applyHistoryFilter(): void {
    const q = this.searchHistoryQuery.toLowerCase().trim();
    this.filteredHistory = this.returnsHistory.filter(entry => 
      !q ||
      entry.equipmentName?.toLowerCase().includes(q) ||
      entry.serialNumber?.toLowerCase().includes(q) ||
      entry.actor?.toLowerCase().includes(q)
    );
  }

  openProcessModal(eq: ItEquipment): void {
    this.processTarget = eq;
    this.selectedStatus = 'Available';
    this.selectedShelfId = '';
    this.updateAvailableShelves();
    this.showProcessModal = true;
  }

  closeProcessModal(): void {
    this.showProcessModal = false;
    this.processTarget = null;
  }

  canConfirm(): boolean {
    if (this.selectedStatus === 'Available') {
      return !!this.selectedShelfId;
    }
    return true; // For Maintenance/Broken, shelf is auto-assigned
  }

  confirmProcess(): void {
    if (!this.processTarget || !this.canConfirm() || this.submitting) return;
    this.submitting = true;
    
    const actor = `${this.currentUser?.firstName || ''} ${this.currentUser?.lastName || ''}`.trim() || 'Stock Manager';
    
    this.equipmentService.processReturn(
      this.processTarget.id,
      this.selectedStatus,
      this.selectedStatus === 'Available' ? this.selectedShelfId : null,
      actor
    ).subscribe({
      next: () => {
        this.showToast('Return processed successfully!', 'success');
        this.closeProcessModal();
        this.loadReturns(); // Refresh list
        this.loadHistory(); // Refresh history
        this.loadShelves(); // Refresh shelves info
        this.submitting = false;
      },
      error: (err) => {
        this.showToast(err.error || 'Failed to process return', 'error');
        this.submitting = false;
      }
    });
  }

  // Helpers
  getEquipmentIcon(eq: ItEquipment): string {
    if (eq.icon) return eq.icon;
    const type = (eq.type || '').toLowerCase();
    if (type.includes('laptop')) return '💻';
    if (type.includes('desktop')) return '🖥️';
    if (type.includes('printer')) return '🖨️';
    if (type.includes('phone')) return '📱';
    return '📦';
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleString('fr-FR', { 
      year: 'numeric', month: 'short', day: '2-digit', 
      hour: '2-digit', minute: '2-digit' 
    });
  }

  showToast(message: string, type: 'success' | 'error'): void {
    this.toast = { message, type };
    setTimeout(() => this.toast = null, 4000);
  }
}
