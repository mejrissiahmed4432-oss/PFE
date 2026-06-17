import { Component, OnInit, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ItEquipmentService, ItEquipment } from './it-equipment.service';
import { AuthService } from '../auth.service';
import { EquipmentFormComponent } from '../equipment/equipment-form/equipment-form.component';

@Component({
  selector: 'app-it-equipment-management',
  standalone: true,
  imports: [CommonModule, FormsModule, EquipmentFormComponent],
  templateUrl: './it-equipment-management.component.html',
  styleUrl: './it-equipment-management.component.css'
})
export class ItEquipmentManagementComponent implements OnInit {

  // ── Tabs ──────────────────────────────────────────────────────────
  @Input() activeTab: 'available' | 'in-use' | 'history' = 'available';

  // ── Data ──────────────────────────────────────────────────────────
  availableEquipment: ItEquipment[] = [];
  inUseEquipment: ItEquipment[] = [];
  historyEquipment: ItEquipment[] = [];
  allUsers: any[] = [];
  allShelves: any[] = [];
  allDepartments: any[] = [];   // from /api/departments

  // ── Filtered ──────────────────────────────────────────────────────
  filteredAvailable: ItEquipment[] = [];
  filteredInUse: ItEquipment[] = [];
  filteredHistory: any[] = [];

  // ── Search ────────────────────────────────────────────────────────
  searchAvailable = '';
  filterAvailable = 'All';
  searchInUse = '';
  filterInUse = 'All';
  searchHistory = '';

  // ── Loading ───────────────────────────────────────────────────────
  loadingAvailable = false;
  loadingInUse = false;
  loadingHistory = false;

  // ── Toast ─────────────────────────────────────────────────────────
  toast: { message: string; type: 'success' | 'error' } | null = null;

  // ── Current user ──────────────────────────────────────────────────
  currentUser: any;

  // ── Assign Modal ──────────────────────────────────────────────────
  showAssignModal = false;
  assignTarget: ItEquipment | null = null;
  isSharedAssignment = false;
  selectedDepartmentId = '';
  selectedDepartmentName = '';
  selectedUserIds: Set<string> = new Set();
  userSearchQuery = '';
  filteredUsers: any[] = [];
  submittingAssign = false;
  sameDeptError = '';   // validation: all selected users must be same dept

  // ── Details Modal ─────────────────────────────────────────────────
  // ── Details Modal ─────────────────────────────────────────────────
  viewMode: 'list' | 'details' = 'list';
  detailsTarget: ItEquipment | null = null;
  equipmentDetailsTarget: any = null;

  // ── QR Code Modal ─────────────────────────────────────────────────
  showQrModal = false;
  qrTarget: ItEquipment | null = null;

  // ── Deassign Confirm ──────────────────────────────────────────────
  showDeassignModal = false;
  deassignTarget: ItEquipment | null = null;
  submittingDeassign = false;

  // ── Return Modal ──────────────────────────────────────────────────
  showReturnModal = false;
  returnTarget: ItEquipment | null = null;
  returnNote = '';
  submittingReturn = false;

  constructor(
    private equipmentService: ItEquipmentService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.authService.user$.subscribe(u => this.currentUser = u);
    this.loadAll();
  }

  loadAll(): void {
    this.loadAvailable();
    this.loadInUse();
    this.loadHistory();
    this.loadUsersAndMeta();
  }

  loadAvailable(): void {
    this.loadingAvailable = true;
    this.equipmentService.getAvailableInStock().subscribe({
      next: (data) => {
        this.availableEquipment = data;
        this.applyFilterAvailable();
        this.loadingAvailable = false;
      },
      error: () => { this.showToast('Failed to load available equipment', 'error'); this.loadingAvailable = false; }
    });
  }

  loadInUse(): void {
    this.loadingInUse = true;
    this.equipmentService.getAllInUse().subscribe({
      next: (data) => {
        this.inUseEquipment = data;
        this.applyFilterInUse();
        this.loadingInUse = false;
      },
      error: () => { this.showToast('Failed to load in-use equipment', 'error'); this.loadingInUse = false; }
    });
  }

  loadHistory(): void {
    this.loadingHistory = true;
    this.equipmentService.getAssignmentHistory().subscribe({
      next: (data) => {
        this.historyEquipment = data;
        this.buildHistoryEntries();
        this.loadingHistory = false;
      },
      error: () => { this.showToast('Failed to load history', 'error'); this.loadingHistory = false; }
    });
  }

  /** Load users (enriched with employee department), departments list, and shelves */
  loadUsersAndMeta(): void {
    forkJoin({
      users: this.equipmentService.getAllUsers(),
      employees: this.equipmentService.getAllEmployees(),
      departments: this.equipmentService.getAllDepartments(),
      shelves: this.equipmentService.getAllShelves()
    }).subscribe({
      next: ({ users, employees, departments, shelves }) => {
        // Build employee lookup: employeeId → employee
        const empMap = new Map<string, any>();
        employees.forEach((emp: any) => empMap.set(emp.id, emp));

        // Enrich each user with department from employee table
        this.allUsers = users.map((u: any) => ({
          ...u,
          department: (u.employeeId && empMap.has(u.employeeId))
            ? empMap.get(u.employeeId).department
            : ''
        }));

        // Departments from departments table (not extracted from users)
        this.allDepartments = departments;

        // Shelves lookup map: id → nb
        this.allShelves = shelves;

        this.filterUsers();
      },
      error: () => console.warn('[ItEquipmentMgmt] Failed to load meta data')
    });
  }

  // ── Shelf helpers ─────────────────────────────────────────────────
  /** Returns a human-readable shelf label. */
  getShelfLabel(shelfId: string | null | undefined): string {
    if (!shelfId) return 'Unassigned';
    if (shelfId === 'IN_USE') return 'In Use (Not on shelf)';
    if (shelfId === 'MAINTENANCE_AREA') return 'Maintenance Area';
    if (shelfId === 'SCRAP_YARD') return 'Scrap Yard';
    if (shelfId === 'OUT_OF_STOCK') return 'Out of Stock';
    const shelf = this.allShelves.find(s => String(s.id) === String(shelfId));
    return shelf ? `Shelf ${shelf.nb}` : `Shelf ${shelfId}`;
  }

  // ── Filtering ─────────────────────────────────────────────────────
  applyFilterAvailable(): void {
    const q = this.searchAvailable.toLowerCase().trim();
    this.filteredAvailable = this.availableEquipment.filter(e => {
      const matchSearch = !q ||
        e.equipmentName?.toLowerCase().includes(q) ||
        e.serialNumber?.toLowerCase().includes(q) ||
        e.brand?.toLowerCase().includes(q) ||
        e.type?.toLowerCase().includes(q) ||
        e.category?.toLowerCase().includes(q);
      const matchFilter = this.filterAvailable === 'All' || e.category === this.filterAvailable;
      return matchSearch && matchFilter;
    });
  }

  applyFilterInUse(): void {
    const q = this.searchInUse.toLowerCase().trim();
    this.filteredInUse = this.inUseEquipment.filter(e => {
      const matchSearch = !q ||
        e.equipmentName?.toLowerCase().includes(q) ||
        e.serialNumber?.toLowerCase().includes(q) ||
        e.itAssignedUserNames?.some(n => n.toLowerCase().includes(q)) ||
        e.itAssignedDepartmentName?.toLowerCase().includes(q);
      const matchFilter = this.filterInUse === 'All' || e.category === this.filterInUse;
      return matchSearch && matchFilter;
    });
  }

  // ── History ───────────────────────────────────────────────────────
  historyRows: Array<{
    equipmentName: string; equipmentId: string; serialNumber: string; icon: string;
    action: string; description: string; actor: string; timestamp: string;
    note?: string;
  }> = [];

  filterHistory = 'All';

  buildHistoryEntries(): void {
    const ASSIGNMENT_ACTIONS = ['Assigned', 'Deassigned', 'Return Requested', 'Returned to Stock'];
    const rows: any[] = [];
    this.historyEquipment.forEach(eq => {
      (eq.lifecycle || []).forEach(entry => {
        if (ASSIGNMENT_ACTIONS.includes(entry.status)) {
          rows.push({
            equipmentName: eq.equipmentName,
            equipmentId: eq.id,
            serialNumber: eq.serialNumber || '',
            icon: eq.type || eq.category || '',
            action: entry.status,
            description: entry.description,
            actor: entry.actor,
            timestamp: entry.timestamp,
            note: entry.status === 'Return Requested' || entry.status === 'Returned to Stock'
              ? this.extractNoteFromDescription(entry.description)
              : undefined
          });
        }
      });
    });
    rows.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
    this.historyRows = rows;
    this.applyFilterHistory();
  }

  applyFilterHistory(): void {
    const q = this.searchHistory.toLowerCase().trim();
    this.filteredHistory = this.historyRows.filter(r => {
      const matchSearch = !q ||
        r.equipmentName?.toLowerCase().includes(q) ||
        r.action?.toLowerCase().includes(q) ||
        r.description?.toLowerCase().includes(q) ||
        r.actor?.toLowerCase().includes(q);
      const matchFilter = this.filterHistory === 'All' || r.action === this.filterHistory;
      return matchSearch && matchFilter;
    }) as any;
  }

  extractNoteFromDescription(desc: string): string {
    if (!desc) return '';
    const noteMatch = desc.match(/[Nn]ote:\s*(.+)/);
    return noteMatch ? noteMatch[1].trim() : '';
  }

  // ── Assign Modal ──────────────────────────────────────────────────
  openAssignModal(equipment: ItEquipment): void {
    this.assignTarget = equipment;
    this.isSharedAssignment = false;
    this.selectedDepartmentId = '';
    this.selectedDepartmentName = '';
    this.selectedUserIds = new Set();
    this.userSearchQuery = '';
    this.sameDeptError = '';
    this.filterUsers();
    this.showAssignModal = true;
  }

  closeAssignModal(): void {
    this.showAssignModal = false;
    this.assignTarget = null;
    this.sameDeptError = '';
  }

  filterUsers(): void {
    const q = this.userSearchQuery.toLowerCase().trim();
    this.filteredUsers = this.allUsers.filter(u =>
      !q ||
      (u.firstName + ' ' + u.lastName).toLowerCase().includes(q) ||
      u.email?.toLowerCase().includes(q) ||
      u.department?.toLowerCase().includes(q)
    );
  }

  toggleUser(userId: string): void {
    if (this.selectedUserIds.has(userId)) {
      this.selectedUserIds.delete(userId);
    } else {
      this.selectedUserIds.add(userId);
    }
    // Re-validate department consistency
    this.validateSameDept();
  }

  isUserSelected(userId: string): boolean {
    return this.selectedUserIds.has(userId);
  }

  getUserEquipmentCount(userId: string): number {
    return this.inUseEquipment.filter(e =>
      e.itAssignedUserIds && e.itAssignedUserIds.includes(userId)
    ).length;
  }

  /** Validates that all selected users belong to the same department. */
  validateSameDept(): void {
    const ids = Array.from(this.selectedUserIds);
    if (ids.length <= 1) { this.sameDeptError = ''; return; }
    const depts = ids.map(id => {
      const u = this.allUsers.find(u => u.id === id);
      return u?.department || '';
    });
    const unique = new Set(depts);
    if (unique.size > 1) {
      this.sameDeptError = `All selected users must belong to the same department. Found: ${Array.from(unique).join(', ')}`;
    } else {
      this.sameDeptError = '';
    }
  }

  canConfirmAssign(): boolean {
    if (this.isSharedAssignment) return !!this.selectedDepartmentId;
    return this.selectedUserIds.size > 0 && !this.sameDeptError;
  }

  onDepartmentChange(): void {
    const dept = this.allDepartments.find(d => d.id === this.selectedDepartmentId);
    this.selectedDepartmentName = dept ? dept.name : '';
  }

  confirmAssign(): void {
    if (!this.assignTarget || this.submittingAssign) return;
    this.submittingAssign = true;

    let payload: any = { actor: `${this.currentUser?.firstName || ''} ${this.currentUser?.lastName || ''}`.trim() || 'IT Manager' };

    if (this.isSharedAssignment) {
      payload.departmentId = this.selectedDepartmentId;
      payload.departmentName = this.selectedDepartmentName;
    } else {
      const ids = Array.from(this.selectedUserIds);
      const names = ids.map(id => {
        const u = this.allUsers.find(u => u.id === id);
        return u ? `${u.firstName} ${u.lastName}` : id;
      });
      const firstUser = this.allUsers.find(u => u.id === ids[0]);
      payload.userIds = ids;
      payload.userNames = names;
      payload.targetDepartment = firstUser?.department || '';
    }

    this.equipmentService.assignEquipment(this.assignTarget.id, payload).subscribe({
      next: () => {
        this.showToast('Equipment assigned successfully!', 'success');
        this.closeAssignModal();
        this.loadAvailable();
        this.loadInUse();
        this.loadHistory();
        this.submittingAssign = false;
      },
      error: (err) => {
        this.showToast(err.error || 'Failed to assign equipment', 'error');
        this.submittingAssign = false;
      }
    });
  }

  // ── Details Modal ─────────────────────────────────────────────────
  openDetails(equipment: ItEquipment): void {
    this.detailsTarget = equipment;
    // We create the object once here instead of mapping it in the template
    // to avoid infinite change detection loops.
    this.equipmentDetailsTarget = { id: equipment.id };
    this.viewMode = 'details';
  }

  closeDetailsModal(): void {
    this.viewMode = 'list';
    this.detailsTarget = null;
    this.equipmentDetailsTarget = null;
  }

  // ── QR Code Modal ─────────────────────────────────────────────────
  openQrModal(equipment: ItEquipment): void {
    this.qrTarget = equipment;
    this.showQrModal = true;
  }

  closeQrModal(): void {
    this.showQrModal = false;
    this.qrTarget = null;
  }

  printQrCode(): void {
    const content = document.getElementById('qr-print-area');
    if (!content) return;
    const win = window.open('', '_blank', 'width=400,height=400');
    if (!win) return;
    win.document.write(`<html><head><title>QR Code</title></head><body style="display:flex;justify-content:center;align-items:center;height:100vh;">${content.innerHTML}</body></html>`);
    win.document.close();
    win.print();
  }

  // ── Deassign Modal ────────────────────────────────────────────────
  openDeassignModal(equipment: ItEquipment): void {
    this.deassignTarget = equipment;
    this.showDeassignModal = true;
  }

  closeDeassignModal(): void {
    this.showDeassignModal = false;
    this.deassignTarget = null;
  }

  confirmDeassign(): void {
    if (!this.deassignTarget || this.submittingDeassign) return;
    this.submittingDeassign = true;
    const actor = `${this.currentUser?.firstName || ''} ${this.currentUser?.lastName || ''}`.trim() || 'IT Manager';
    this.equipmentService.deassignEquipment(this.deassignTarget.id, actor).subscribe({
      next: () => {
        this.showToast('Equipment deassigned.', 'success');
        this.closeDeassignModal();
        this.loadInUse();
        this.submittingDeassign = false;
      },
      error: () => { this.showToast('Failed to deassign', 'error'); this.submittingDeassign = false; }
    });
  }

  // ── Return Modal ──────────────────────────────────────────────────
  openReturnModal(equipment: ItEquipment): void {
    this.returnTarget = equipment;
    this.returnNote = '';
    this.showReturnModal = true;
  }

  closeReturnModal(): void {
    this.showReturnModal = false;
    this.returnTarget = null;
    this.returnNote = '';
  }

  confirmReturn(): void {
    if (!this.returnTarget || this.submittingReturn) return;
    this.submittingReturn = true;
    const actor = `${this.currentUser?.firstName || ''} ${this.currentUser?.lastName || ''}`.trim() || 'IT Manager';
    this.equipmentService.requestReturn(this.returnTarget.id, this.returnNote, actor).subscribe({
      next: () => {
        this.showToast('Return request submitted to Stock Manager.', 'success');
        this.closeReturnModal();
        this.loadInUse();
        this.loadHistory();
        this.submittingReturn = false;
      },
      error: () => { this.showToast('Failed to submit return request', 'error'); this.submittingReturn = false; }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────
  getAssignedToLabel(eq: ItEquipment): string {
    if (eq.itAssignedDepartmentName) return eq.itAssignedDepartmentName + ' (Dept.)';
    if (eq.itAssignedUserNames && eq.itAssignedUserNames.length > 0) return eq.itAssignedUserNames.join(', ');
    return 'Not assigned';
  }

  isAssigned(eq: ItEquipment): boolean {
    return !!(eq.itAssignedDepartmentName || (eq.itAssignedUserIds && eq.itAssignedUserIds.length > 0));
  }

  getDaysInUse(eq: ItEquipment): number {
    if (!eq.itAssignedAt) return 0;
    const diff = new Date().getTime() - new Date(eq.itAssignedAt).getTime();
    return Math.floor(diff / (1000 * 60 * 60 * 24));
  }

  getActionBadgeClass(action: string): string {
    switch (action) {
      case 'Assigned': return 'badge-assigned';
      case 'Deassigned': return 'badge-deassigned';
      case 'Return Requested': return 'badge-return-req';
      case 'Returned to Stock': return 'badge-returned';
      case 'Installed': return 'badge-installed';
      case 'Allocated': return 'badge-allocated';
      default: return 'badge-default';
    }
  }

  getTypeKey(type: string | undefined): string {
    if (!type) return 'default';
    const t = type.toLowerCase();
    if (t.includes('laptop')) return 'laptop';
    if (t.includes('desktop') || t.includes('pc')) return 'pc';
    if (t.includes('monitor') || t.includes('screen')) return 'monitor';
    if (t.includes('server')) return 'server';
    if (t.includes('printer')) return 'printer';
    if (t.includes('scanner')) return 'scanner';
    if (t.includes('projector')) return 'projector';
    if (t.includes('router') || t.includes('switch') || t.includes('network')) return 'router';
    if (t.includes('ups') || t.includes('battery')) return 'ups';
    if (t.includes('tablet')) return 'tablet';
    if (t.includes('phone') || t.includes('mobile')) return 'phone';
    if (t.includes('keyboard')) return 'keyboard';
    if (t.includes('mouse')) return 'mouse';
    if (t.includes('headset') || t.includes('headphone')) return 'headset';
    if (t.includes('ram') || t.includes('memory')) return 'ram';
    if (t.includes('hdd') || t.includes('ssd') || t.includes('drive')) return 'hdd';
    if (t.includes('cpu') || t.includes('processor')) return 'cpu';
    if (t.includes('gpu') || t.includes('graphic')) return 'gpu';
    if (t.includes('motherboard')) return 'motherboard';
    if (t.includes('power') || t.includes('psu')) return 'psu';
    if (t.includes('cable') || t.includes('wire')) return 'cable';
    if (t.includes('software') || t.includes('license')) return 'software';
    if (t.includes('paper')) return 'paper';
    if (t.includes('ink') || t.includes('toner')) return 'ink';
    return 'default';
  }

  getCategories(): string[] {
    const cats = new Set<string>();
    this.availableEquipment.forEach(e => { if (e.category) cats.add(e.category); });
    this.inUseEquipment.forEach(e => { if (e.category) cats.add(e.category); });
    return Array.from(cats).sort();
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('fr-FR', { year: 'numeric', month: 'short', day: '2-digit' });
  }

  formatDateTime(dateStr: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleString('fr-FR', { year: 'numeric', month: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit' });
  }

  showToast(message: string, type: 'success' | 'error'): void {
    this.toast = { message, type };
    setTimeout(() => this.toast = null, 4000);
  }

  getSpecKeys(eq: ItEquipment): string[] {
    return eq.specifications ? Object.keys(eq.specifications) : [];
  }
}
