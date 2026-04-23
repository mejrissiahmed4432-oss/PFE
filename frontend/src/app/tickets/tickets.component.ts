import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TicketService } from './ticket.service';
import { EquipmentService } from '../equipment/equipment.service';
import { CategoryService } from '../category-manager/category.service';
import { AuthService } from '../auth.service';
import { Ticket } from './ticket.model';
import { Equipment } from '../equipment/equipment.model';
import { EquipmentCategory } from '../category-manager/category.model';
import { DragDropModule, CdkDragDrop, moveItemInArray, transferArrayItem } from '@angular/cdk/drag-drop';
import { PartRequestService } from '../parts-management/part-request.service';
import { PartRequest } from '../parts-management/part-request.model';

export interface RepairHistoryEntry {
  issue: string;
  date: string;
  technician: string;
}

export interface ExtendedHistoryEntry {
  type: 'repair' | 'ticket';
  priority?: string;
  referenceId: string;
  dateStr: string;
  timeStr: string;
  title: string;
  description: string;
  user: string;
  userRole?: string;
  duration?: string;
  cost?: string;
  workNote?: string;
  repairTasks?: any[];
  partsUsed?: any[];
}

export interface EquipmentWithHistory extends Equipment {
  repairHistory?: RepairHistoryEntry[];
  extendedHistory?: ExtendedHistoryEntry[];
  assignedUser?: string;
  lastRepair?: string;
}

@Component({
  selector: 'app-tickets',
  standalone: true,
  imports: [CommonModule, FormsModule, DragDropModule],
  templateUrl: './tickets.component.html',
  styleUrl: './tickets.component.css'
})
export class TicketsComponent implements OnInit, OnDestroy {
  // View Mode: 'equipment' or 'tickets'
  viewMode: 'equipment' | 'tickets' = 'equipment';

  // Search & Filters state
  showFilters: boolean = false;
  showFullHistory: boolean = false;
  searchQuery: string = '';
  filterCategory: string = 'All Categories';
  filterType: string = 'All Types';
  filterSerial: string = '';

  // Data
  categories: EquipmentCategory[] = [];
  availableTypes: string[] = [];
  equipments: EquipmentWithHistory[] = [];
  filteredEquipments: EquipmentWithHistory[] = [];
  selectedEquipment: EquipmentWithHistory | null = null;
  selectedTicket: Ticket | null = null;

  ticketsList: Ticket[] = [];
  filteredTickets: Ticket[] = [];

  // Stats
  stats = {
    open: 0,
    inProgress: 0,
    waiting: 0,
    testing: 0
  };

  // New Ticket mapping
  showAddModal: boolean = false;
  isEditMode: boolean = false;
  isSubmitting: boolean = false;
  currentUser: any;
  attachmentFiles: { name: string; size: string; base64?: string }[] = [];

  newTicket: Ticket = {
    title: '',
    description: '',
    category: 'Maintenance',
    priority: 'Medium',
    status: 'Open',
    deadline: ''
  };

  // ── Live Workbench State ──
  showWorkbench: boolean = false;
  showWorkbenchHistory: boolean = false;
  workbenchTicket: Ticket | null = null;
  workbenchEquipment: EquipmentWithHistory | null = null;



  // Kanban Checklist Tasks (session-only)
  repairChecklist: { id: string; label: string; status: 'todo' | 'in-progress' | 'waiting' | 'testing' | 'done'; editing: boolean }[] = [];
  newTaskLabel: string = '';
  draggedTaskId: string | null = null;

  // Notes
  workNote: string = '';
  showCancelConfirmation: boolean = false;
  showCompleteConfirmation: boolean = false;
  validationAlert: { title: string; message: string } | null = null;

  // Parts Used (Workbench)
  partsUsed: { name: string; qty: number; type?: string; specification?: string }[] = [];
  inventorySearchQuery: string = '';
  inventoryFilterCategory: string = '';
  inventoryFilterType: string = '';
  inventoryAvailableTypes: string[] = [];
  showInventoryTable: boolean = true;
  showSelectedParts: boolean = true;

  // Timelines
  workbenchTimeline: { event: string; status?: string; color: string; time: Date }[] = [];
  showWorkbenchTimeline: boolean = true;

  // Technician Inventory
  userInventory: { 
    name: string; 
    totalQty: number; 
    category: string;
    type: string;
    specification: string;
    brand?: string;
  }[] = [];

  get inventoryCategories(): string[] {
    const cats = this.userInventory.map(i => i.category).filter(c => !!c);
    return Array.from(new Set(cats)).sort();
  }

  get filteredInventory(): any[] {
    let result = [...this.userInventory];
    
    // Only show parts with quantity > 0
    result = result.filter(p => p.totalQty > 0);
    
    if (this.inventoryFilterCategory) {
      result = result.filter(p => p.category === this.inventoryFilterCategory);
    }
    
    if (this.inventoryFilterType) {
      result = result.filter(p => p.type === this.inventoryFilterType);
    }
    
    if (this.inventorySearchQuery) {
      const q = this.inventorySearchQuery.toLowerCase();
      result = result.filter(p => 
        p.name.toLowerCase().includes(q) || 
        p.specification.toLowerCase().includes(q) ||
        (p.brand || '').toLowerCase().includes(q)
      );
    }
    
    return result;
  }

  onInventoryCategoryChange(): void {
    this.inventoryFilterType = '';
    const cat = this.categories.find(c => c.name === this.inventoryFilterCategory);
    this.inventoryAvailableTypes = cat && cat.types ? cat.types.map(t => typeof t === 'string' ? t : t.name) : [];
  }

  isPartSelected(part: any): boolean {
    return this.partsUsed.some(p => p.name === part.name && p.specification === part.specification);
  }

  togglePartUsed(part: any, event: any): void {
    const checked = event.target.checked;
    if (checked) {
      const max = this.getPartMaxQty(part);
      if (max < 1 || (part.status && part.status.toLowerCase() === 'in use')) {
        this.validationAlert = {
          title: max < 1 ? 'Out of Stock' : 'Part In Use',
          message: max < 1 
            ? `"${part.name}" is currently out of stock and cannot be used.`
            : `"${part.name}" is already assigned to another machine (${part.assignedToEquipmentName || 'Unknown'}).`
        };
        if (event.target) event.target.checked = false;
        return;
      }
      if (!this.isPartSelected(part)) {
        this.partsUsed.push({ 
          name: part.name, 
          qty: 1, 
          type: part.type, 
          specification: part.specification 
        });
      }
    } else {
      this.partsUsed = this.partsUsed.filter(p => !(p.name === part.name && p.specification === part.specification));
    }
    this.workbenchTimeline.push({ 
      event: `Part ${checked ? 'added' : 'removed'}: ${part.name}`, 
      color: checked ? '#f59e0b' : '#ef4444', 
      time: new Date() 
    });
    this.saveWorkbenchState();
  }

  updatePartUsedQty(part: any, newQty: number | string): void {
    const qty = parseInt(newQty.toString(), 10) || 0;
    const max = this.getPartMaxQty(part);
    
    if (qty > max) {
      this.validationAlert = {
        title: 'Stock Limit Exceeded',
        message: `You requested ${qty} units of "${part.name}", but only ${max} are available. The quantity has been reset to the maximum available.`
      };
    }

    const found = this.partsUsed.find(p => p.name === part.name && p.specification === part.specification);
    if (found) {
      const targetQty = Math.min(Math.max(1, qty), max);
      // If we are setting it to the same value it already had (clamping), 
      // Angular might not refresh the input field. We force it by toggling.
      if (found.qty === targetQty && qty > max) {
        found.qty = 0;
        setTimeout(() => found.qty = targetQty, 0);
      } else {
        found.qty = targetQty;
      }
    }
    this.saveWorkbenchState();
  }

  getPartMaxQty(part: any): number {
    const invPart = this.userInventory.find(p => p.name === part.name && p.specification === part.specification);
    return invPart ? invPart.totalQty : 999;
  }

  getPartUsedQty(part: any): number {
    const found = this.partsUsed.find(p => p.name === part.name && p.specification === part.specification);
    return found ? found.qty : 1;
  }

  // Kanban Columns Definition (Stable reference for CDK)
  readonly checklistColumns = [
    { key: 'todo', label: 'To Do', color: '#64748b', dotColor: '#94a3b8' },
    { key: 'in-progress', label: 'In Progress', color: '#3b82f6', dotColor: '#3b82f6' },
    { key: 'waiting', label: 'Waiting for Parts', color: '#f59e0b', dotColor: '#f59e0b' },
    { key: 'testing', label: 'Testing', color: '#8b5cf6', dotColor: '#8b5cf6' },
    { key: 'done', label: 'Done', color: '#10b981', dotColor: '#10b981' }
  ];

  constructor(
    private ticketService: TicketService,
    private equipmentService: EquipmentService,
    private categoryService: CategoryService,
    private authService: AuthService,
    private partRequestService: PartRequestService
  ) { }

  ngOnInit(): void {
    this.authService.user$.subscribe(user => {
      this.currentUser = user;
      if (this.currentUser) {
        this.loadCategories();
        this.loadEquipments();
        this.loadTickets();
        this.loadUserInventory();
      }
    });
  }

  switchView(mode: 'equipment' | 'tickets'): void {
    this.viewMode = mode;
    this.selectedEquipment = null;
    this.selectedTicket = null;
    this.showFullHistory = false;
    this.showWorkbench = false;
  }

  ngOnDestroy(): void {
  }

  loadCategories(): void {
    this.categoryService.getAllCategories().subscribe({
      next: (data) => {
        this.categories = data;
      },
      error: (err) => console.error('Error loading categories', err)
    });
  }

  loadUserInventory(): void {
    if (!this.currentUser?.id) return;
    this.partRequestService.getMyRequests(this.currentUser.id).subscribe({
      next: (requests) => {
        const approved = requests.filter(r => r.status === 'APPROVED');
        const itemsList: any[] = [];

        approved.forEach(req => {
          (req.items || [])
            .filter(item => !item.returned)
            .forEach(item => {
              const isMatched = !!item.equipmentId && !!item.matchedEquipmentName;
              const displayName = (isMatched ? item.matchedEquipmentName : item.partName) || 'Unknown Part';
              const displaySpec = (isMatched ? (item.matchedSpecification || item.specification) : (item.specification || '')) || '';

              const existing = itemsList.find(i => 
                i.name.toLowerCase() === displayName.toLowerCase() &&
                (i.specification || '').toLowerCase() === (displaySpec || '').toLowerCase()
              );
              if (existing) {
                existing.totalQty += item.quantity;
              } else {
                itemsList.push({
                  name: displayName,
                  totalQty: item.quantity,
                  category: item.category,
                  type: item.type,
                  specification: displaySpec,
                  brand: item.brand || '',
                  isMatched: isMatched
                });
              }
            });
        });

        this.userInventory = itemsList;
      },
      error: (err) => console.error('Failed to load user inventory', err)
    });
  }

  loadTickets(): void {
    // Determine whether to load all tickets or just the current technician's
    const fetchCall = this.currentUser?.role === 'TECHNICIAN'
      ? this.ticketService.getTicketsByUser(this.currentUser.id)
      : this.ticketService.getTickets();

    fetchCall.subscribe({
      next: (tickets) => {
        this.ticketsList = tickets;
        this.applyTicketFilters();
        // Update history for all equipments now that we have the tickets
        this.equipments.forEach(eq => this.calculateEquipmentHistory(eq));
        this.applyFilters();
      },
      error: (err) => {
        console.error('Failed to load real tickets', err);
        this.ticketsList = [];
      }
    });
  }

  loadEquipments(): void {
    this.equipmentService.getAllEquipment().subscribe({
      next: (data) => {
        // Map backend equipment to UI model
        this.equipments = data.map(eq => ({
          ...eq,
          assignedUser: eq.department || 'Unassigned',
          // History will be calculated dynamically when selected or during filtering
          repairHistory: [],
          extendedHistory: [],
          lastRepair: undefined
        }));
        this.applyFilters();
      },
      error: (err) => {
        console.error('Failed to load real equipments', err);
        this.equipments = [];
        this.applyFilters();
      }
    });
  }

  activeTickets: Ticket[] = [];
  completedTickets: Ticket[] = [];

  applyTicketFilters(): void {
    // Active tickets (everything except Completed, Resolved, Closed, Cancelled)
    this.activeTickets = this.ticketsList.filter(t =>
      t.status !== 'Completed' && t.status !== 'Resolved' && t.status !== 'Closed' && t.status !== 'Cancelled'
    ).sort((a, b) => {
      const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
      const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      return dateB - dateA; // Newest first
    });

    // Completed tickets
    let comps = this.ticketsList.filter(t => {
      if (t.status !== 'Completed') return false;
      // Do not display the completed ticket if this equipment has an active ticket
      const hasActive = this.activeTickets.some(active => active.equipmentName === t.equipmentName);
      return !hasActive;
    }).sort((a, b) => {
      const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
      const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      return dateB - dateA; // Newest first
    });

    // Deduplicate by equipmentName to only show the LATEST completed ticket per equipment
    const seenEq = new Set<string>();
    this.completedTickets = comps.filter(t => {
      if (!t.equipmentName) return true;
      if (seenEq.has(t.equipmentName)) return false;
      seenEq.add(t.equipmentName);
      return true;
    });

    this.calculateStats();
  }

  calculateStats(): void {
    // Update stats based on the FULL ticketsList
    this.stats = {
      open: this.ticketsList.filter(t => t.status === 'Open').length,
      inProgress: this.ticketsList.filter(t => t.status === 'In Progress').length,
      waiting: this.ticketsList.filter(t => t.status === 'Waiting' || t.status === 'Waiting for Parts').length,
      testing: this.ticketsList.filter(t => t.status === 'Testing').length
    };
  }

  onCategoryChange(): void {
    if (this.filterCategory === 'All Categories' || !this.filterCategory) {
      this.availableTypes = [];
      this.filterType = 'All Types';
    } else {
      const cat = this.categories.find(c => c.name === this.filterCategory);
      this.availableTypes = cat && cat.types ? cat.types.map(t => t.name) : [];
      this.filterType = 'All Types';
    }
    this.applyFilters();
  }

  applyFilters(): void {
    let result = [...this.equipments];

    // 1. General Search query (Name, Brand, Model, Specification)
    if (this.searchQuery) {
      const q = this.searchQuery.toLowerCase();
      result = result.filter(e =>
        (e.equipmentName || '').toLowerCase().includes(q) ||
        (e.brand || '').toLowerCase().includes(q) ||
        (e.model || '').toLowerCase().includes(q) ||
        (e.specification || '').toLowerCase().includes(q)
      );
    }

    // 2. Serial Number filter (Exact/Partial)
    if (this.filterSerial) {
      const q = this.filterSerial.toLowerCase();
      result = result.filter(e => (e.serialNumber || '').toLowerCase().includes(q));
    }

    // 3. Category filter
    if (this.filterCategory !== 'All Categories' && this.filterCategory) {
      result = result.filter(e => e.category === this.filterCategory);
    }

    // 4. Type filter
    if (this.filterType !== 'All Types' && this.filterType) {
      result = result.filter(e => e.type === this.filterType);
    }

    this.filteredEquipments = result;
  }

  selectEquipment(equipment: EquipmentWithHistory): void {
    this.calculateEquipmentHistory(equipment);
    this.selectedEquipment = equipment;
    this.selectedTicket = null;
    this.showFullHistory = false;
  }

  private calculateEquipmentHistory(eq: EquipmentWithHistory): void {
    // Filter tickets that belong to this equipment and are NOT cancelled
    const relatedTickets = this.ticketsList.filter(t => 
      (t.equipmentName === eq.name || t.equipmentName === eq.equipmentName) && 
      (t.status || '').toLowerCase() !== 'cancelled'
    );

    // Map to RepairHistoryEntry (Short History)
    eq.repairHistory = relatedTickets.map(t => ({
      issue: t.title,
      date: t.createdAt ? new Date(t.createdAt).toISOString().split('T')[0] : 'N/A',
      technician: 'Technician' // Placeholder until Ticket has assigned technician info
    })).sort((a, b) => b.date.localeCompare(a.date));

    // Map to ExtendedHistoryEntry (Rich Timeline)
    eq.extendedHistory = relatedTickets.map(t => ({
      type: (t.status === 'Completed' || t.status === 'Closed' ? 'repair' : 'ticket') as 'repair' | 'ticket',
      priority: t.priority?.toUpperCase(),
      referenceId: t.id || 'N/A',
      dateStr: t.createdAt ? new Date(t.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : 'N/A',
      timeStr: t.createdAt ? new Date(t.createdAt).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }) : 'N/A',
      title: t.title,
      description: t.description,
      user: t.userName || (t.userId && t.userId.length !== 24 ? t.userId : 'Technician (Legacy Record)'),
      userRole: t.userRole || 'Technician',
      workNote: t.workNote,
      repairTasks: t.repairTasks,
      partsUsed: t.partsUsed
    })).sort((a, b) => {
      const dateA = new Date(a.dateStr + ' ' + a.timeStr).getTime();
      const dateB = new Date(b.dateStr + ' ' + b.timeStr).getTime();
      return dateB - dateA; // Newest first
    });

    // Set Last Repair Date
    if (eq.repairHistory.length > 0) {
      eq.lastRepair = eq.repairHistory[0].date;
    } else {
      eq.lastRepair = 'Never';
    }
  }

  selectTicket(ticket: Ticket): void {
    this.selectedTicket = ticket;
    // Map ticket to equipment for the detail pane
    const eq = this.equipments.find(e => e.name === ticket.equipmentName || e.equipmentName === ticket.equipmentName);
    
    if (eq) {
      this.selectedEquipment = eq;
    } else {
      // Create a fallback so the equipment details section still renders using ticket info
      this.selectedEquipment = {
        name: ticket.equipmentName || 'Unknown Equipment',
        equipmentName: ticket.equipmentName || 'Unknown Equipment',
        type: 'Unknown Type',
        serialNumber: 'N/A',
        status: 'Under Repair',
        assignedUser: 'N/A',
        category: ticket.category || 'Unknown',
        repairHistory: [],
        extendedHistory: [],
        lastRepair: 'Unknown'
      } as any;
    }
    
    // Always calculate history, as we have the tickets locally to build the timeline
    this.calculateEquipmentHistory(this.selectedEquipment!);
    this.showFullHistory = false;
  }

  setViewMode(mode: 'equipment' | 'tickets'): void {
    this.viewMode = mode;
    this.selectedEquipment = null;
    this.selectedTicket = null;
    this.showFullHistory = false;
  }

  handleQRUpload(event: any): void {
    const file = event.target.files[0];
    if (file) {
      // Simulate QR reading finding EQ-001
      this.filterSerial = 'EQ-001';
      this.applyFilters();

      const found = this.filteredEquipments.find(e => e.serialNumber === 'EQ-001');
      if (found) {
        this.selectEquipment(found);
      }
    }
    // Reset file input
    event.target.value = '';
  }

  triggerQRUpload(): void {
    const fileInput = document.getElementById('qr-upload-input');
    if (fileInput) {
      fileInput.click();
    }
  }

  hasActiveTicket(equipmentName: string | undefined): boolean {
    if (!equipmentName) return false;
    // Block if there is any ticket that is NOT Resolved, Closed, or Completed
    const inactiveStatuses = ['Resolved', 'Closed', 'Completed', 'Cancelled'];
    return this.ticketsList.some(t =>
      (t.equipmentName === equipmentName) &&
      !inactiveStatuses.includes(t.status || '')
    );
  }

  // Ticket Creation Flow
  openTicketModal(): void {
    if (!this.selectedEquipment) return;

    const eqName = this.selectedEquipment.name || this.selectedEquipment.equipmentName;
    if (this.hasActiveTicket(eqName)) {
      alert(`Cannot create a new ticket. There is already an active ticket for ${eqName}. Please complete or delete the existing ticket first.`);
      return;
    }

    this.isEditMode = false;
    this.newTicket = {
      title: '',
      description: '',
      category: 'Maintenance',
      priority: 'Medium',
      status: 'Open',
      equipmentName: eqName,
      deadline: ''
    };
    this.attachmentFiles = [];
    this.showAddModal = true;
  }

  openEditModal(ticket: Ticket): void {
    this.isEditMode = true;
    this.newTicket = { ...ticket };
    // Map existing attachments if strings to the UI model
    this.attachmentFiles = (ticket.attachments || []).map(att => ({
      name: 'Attachment',
      size: 'N/A',
      base64: att
    }));
    this.showAddModal = true;
  }

  setPriority(p: 'High' | 'Medium' | 'Low'): void {
    this.newTicket.priority = p;
  }

  handleAttachmentUpload(event: any): void {
    const files: FileList = event.target.files;
    if (!files || files.length === 0) return;
    Array.from(files).forEach(file => {
      const reader = new FileReader();
      reader.onload = (e: any) => {
        this.attachmentFiles.push({
          name: file.name,
          size: this.formatFileSize(file.size),
          base64: e.target.result
        });
      };
      reader.readAsDataURL(file);
    });
    event.target.value = '';
  }

  removeAttachment(index: number): void {
    this.attachmentFiles.splice(index, 1);
  }

  private formatFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  }

  closeModal(): void {
    this.showAddModal = false;
    this.isEditMode = false;
  }

  createTicket(): void {
    if (!this.newTicket.title || !this.newTicket.description) return;
    this.isSubmitting = true;

    const ticketData: Ticket = {
      ...this.newTicket,
      userId: this.currentUser?.id,
      userName: (this.currentUser?.firstName ? this.currentUser.firstName + ' ' + (this.currentUser.lastName || '') : '') || this.currentUser?.email || 'Unknown User',
      userRole: this.currentUser?.role || 'Technician',
      attachments: this.attachmentFiles.map(f => f.base64 || f.name)
    };

    const request = this.isEditMode && this.newTicket.id
      ? this.ticketService.updateTicket(this.newTicket.id, ticketData)
      : this.ticketService.createTicket(ticketData);

    request.subscribe({
      next: (result) => {
        console.log(this.isEditMode ? "Ticket updated" : "Ticket created", result);

        if (this.isEditMode) {
          const idx = this.ticketsList.findIndex(t => t.id === result.id);
          if (idx !== -1) this.ticketsList[idx] = result;
          this.selectedTicket = result;
        } else {
          this.ticketsList.unshift(result);
        }

        this.closeModal();
        this.isSubmitting = false;
        this.applyTicketFilters();

        if (!this.isEditMode && this.selectedEquipment) {
          // Update status locally
          this.selectedEquipment.status = 'In Maintenance';
          this.calculateEquipmentHistory(this.selectedEquipment);

          // Persist status to backend
          const eqId = this.selectedEquipment.id || '';
          if (eqId) {
            this.equipmentService.updateEquipment(eqId, this.selectedEquipment).subscribe({
              next: () => console.log('Equipment status updated to In Maintenance'),
              error: (err) => console.error('Failed to update equipment status', err)
            });
          }
        }

        this.viewMode = 'tickets';
      },
      error: (err) => {
        console.error('Error saving ticket', err);
        this.isSubmitting = false;
        // Fallback for demo
        if (this.isEditMode) {
          const idx = this.ticketsList.findIndex(t => t.id === ticketData.id);
          if (idx !== -1) this.ticketsList[idx] = ticketData;
          this.selectedTicket = ticketData;
        } else {
          this.ticketsList.unshift(ticketData);
        }
        this.closeModal();
      }
    });
  }

  deleteTicket(id: string | undefined): void {
    if (!id || !window.confirm('Are you sure you want to delete this ticket?')) return;

    this.ticketService.deleteTicket(id).subscribe({
      next: () => {
        // Remove from memory
        this.ticketsList = this.ticketsList.filter(t => t.id !== id);

        // Refresh detail state
        this.selectedTicket = null;
        if (this.selectedEquipment) {
          this.calculateEquipmentHistory(this.selectedEquipment);
        }

        // Refresh lists and stats
        this.calculateStats();
        this.applyTicketFilters();

        console.log('Ticket deleted successfully');
      },
      error: (err) => {
        console.error('Error deleting ticket', err);
        // Fallback for demo
        this.ticketsList = this.ticketsList.filter(t => t.id !== id);
        this.selectedTicket = null;
        if (this.selectedEquipment) {
          this.calculateEquipmentHistory(this.selectedEquipment);
        }
        this.applyTicketFilters();
      }
    });
  }

  getPriorityClass(priority: string | undefined): string {
    switch (priority) {
      case 'High': return 'prio-high';
      case 'Medium': return 'prio-medium';
      case 'Low': return 'prio-low';
      default: return '';
    }
  }

  getStatusClass(status: string | undefined): string {
    const s = (status || '').toLowerCase();
    if (s.includes('repair')) return 'status-warning';
    if (s.includes('active')) return 'status-success';
    if (s.includes('maintenance')) return 'status-progress';
    if (s.includes('progress')) return 'status-progress';
    if (s.includes('open')) return 'status-open';
    if (s.includes('resolved') || s.includes('closed')) return 'status-success';
    if (s.includes('cancelled')) return 'status-error';
    return 'status-open';
  }

  getIconForEquipment(type: string | undefined): string {
    const t = (type || '').toLowerCase();
    if (t.includes('laptop') || t.includes('pc')) return 'laptop';
    if (t.includes('printer')) return 'printer';
    if (t.includes('switch') || t.includes('server')) return 'server';
    return 'monitor';
  }

  // \u2500\u2500 Live Workbench Methods \u2500\u2500

  startWorkbench(ticket: Ticket): void {
    this.workbenchTicket = { ...ticket };
    this.workbenchEquipment = this.selectedEquipment;
    // Reset session state
    this.repairChecklist = [];
    this.newTaskLabel = '';
    this.draggedTaskId = null;
    this.workNote = '';
    this.partsUsed = [];
    this.inventorySearchQuery = '';
    this.inventoryFilterCategory = '';
    this.inventoryFilterType = '';
    this.workbenchTimeline = [
      { event: 'Ticket created', status: 'Open', color: '#3b82f6', time: ticket.createdAt ? new Date(ticket.createdAt) : new Date() },
      { event: 'Workbench started', status: 'In Progress', color: '#10b981', time: new Date() }
    ];
    // Update ticket status to In Progress
    const updated: Ticket = { ...ticket, status: 'In Progress' };
    this.ticketService.updateTicket(ticket.id!, updated).subscribe({
      next: (res) => {
        const idx = this.ticketsList.findIndex(t => t.id === res.id);
        if (idx !== -1) this.ticketsList[idx] = res;
        this.applyTicketFilters();
      },
      error: () => { }
    });
    this.showWorkbench = true;
    this.loadWorkbenchState(ticket.id!);
  }

  // ── Workbench Persistence ──
  private saveWorkbenchState(): void {
    if (!this.workbenchTicket?.id) return;
    const state = {
      repairChecklist: this.repairChecklist,
      workNote: this.workNote,
      partsUsed: this.partsUsed,
      timeline: this.workbenchTimeline
    };
    if (this.workbenchTicket?.id) {
      localStorage.setItem(`wb_state_${this.workbenchTicket.id}`, JSON.stringify(state));
    }
  }

  private loadWorkbenchState(ticketId: string): void {
    const saved = localStorage.getItem(`wb_state_${ticketId}`);
    if (saved) {
      try {
        const state = JSON.parse(saved);
        this.repairChecklist = state.repairChecklist || [];
        this.workNote = state.workNote || '';
        this.partsUsed = state.partsUsed || [];
        this.workbenchTimeline = state.timeline || this.workbenchTimeline;
      } catch (e) {
        console.error('Failed to load workbench state', e);
      }
    }
  }

  private clearWorkbenchState(ticketId: string): void {
    localStorage.removeItem(`wb_state_${ticketId}`);
  }

  exitWorkbench(): void {
    this.saveWorkbenchState();
    this.showWorkbench = false;
    this.workbenchTicket = null;
  }

  // ── Kanban Checklist Methods ──

  getTasksForColumn(col: string): typeof this.repairChecklist {
    return this.repairChecklist.filter(t => t.status === col);
  }

  get checklistProgress(): number {
    if (!this.repairChecklist.length) return 0;
    return Math.round((this.repairChecklist.filter(c => c.status === 'done').length / this.repairChecklist.length) * 100);
  }

  get completedChecklistCount(): number {
    return this.repairChecklist.filter(c => c.status === 'done').length;
  }

  addChecklistTask(): void {
    const label = this.newTaskLabel.trim();
    if (!label) return;
    const newTask = { id: Date.now().toString(), label, status: 'todo' as const, editing: false };
    this.repairChecklist.push(newTask);
    this.workbenchTimeline.push({ event: `Task added: "${label}"`, color: '#3b82f6', time: new Date() });
    this.newTaskLabel = '';
    this.saveWorkbenchState();
  }

  deleteChecklistTask(id: string): void {
    const task = this.repairChecklist.find(t => t.id === id);
    this.repairChecklist = this.repairChecklist.filter(t => t.id !== id);
    if (task) {
      this.workbenchTimeline.push({ event: `Task removed: "${task.label}"`, color: '#ef4444', time: new Date() });
      this.saveWorkbenchState();
    }
  }

  startEditTask(id: string): void {
    this.repairChecklist.forEach(t => t.editing = false);
    const task = this.repairChecklist.find(t => t.id === id);
    if (task) task.editing = true;
  }

  saveEditTask(id: string, newLabel: string): void {
    const task = this.repairChecklist.find(t => t.id === id);
    if (task) {
      task.label = newLabel.trim() || task.label;
      task.editing = false;
      this.saveWorkbenchState();
    }
  }

  moveTaskToColumn(id: string, status: 'todo' | 'in-progress' | 'waiting' | 'testing' | 'done'): void {
    const task = this.repairChecklist.find(t => t.id === id);
    if (!task || task.status === status) return;
    task.status = status;
    if (status === 'done') {
      this.workbenchTimeline.push({ event: `Completed: "${task.label}"`, color: '#10b981', time: new Date() });
    }
    this.saveWorkbenchState();
  }

  // ── Angular CDK Drag & Drop handlers ──
  onTaskDrop(event: CdkDragDrop<any[]>): void {
    if (event.previousContainer === event.container) {
      // Reordering in the same column
      moveItemInArray(event.container.data, event.previousIndex, event.currentIndex);
    } else {
      // Moving between columns
      const task = event.item.data;
      const newStatus = event.container.id as any; // We'll set column ID to the status key

      this.moveTaskToColumn(task.id, newStatus);
    }
  }

  onNoteChange(): void {
    this.saveWorkbenchState();
  }

  // addPart was replaced by togglePartUsed in the inventory table.
  
  removePart(index: number): void {
    this.partsUsed.splice(index, 1);
    this.saveWorkbenchState();
  }

  completeRepair(): void {
    if (!this.workbenchTicket?.id) return;
    
    // 1. Validation: Must have at least one task
    if (this.repairChecklist.length === 0) {
      this.validationAlert = {
        title: 'Empty Checklist',
        message: 'Your workbench checklist is empty. Please add at least one task or diagnostic step before completing the repair.'
      };
      return;
    }

    // 2. Validation: All tasks must be 'done'
    const pendingTasks = this.repairChecklist.filter(t => t.status !== 'done');
    if (pendingTasks.length > 0) {
      this.validationAlert = {
        title: 'Pending Tasks',
        message: `There are ${pendingTasks.length} task(s) that are not yet completed. Please move them to the "Done" column or delete them to proceed.`
      };
      return;
    }

    this.showCompleteConfirmation = true;
  }

  closeValidationAlert(): void {
    this.validationAlert = null;
  }

  confirmCompleteRepair(): void {
    if (!this.workbenchTicket?.id) return;
    
    // Bundled session data
    const updated: Ticket = { 
      ...this.workbenchTicket, 
      status: 'Completed',
      workNote: this.workNote,
      repairTasks: this.repairChecklist.map(t => ({ label: t.label, status: t.status })),
      partsUsed: this.partsUsed
    };

    this.ticketService.updateTicket(this.workbenchTicket.id, updated).subscribe({
      next: (res) => {
        const idx = this.ticketsList.findIndex(t => t.id === res.id);
        if (idx !== -1) this.ticketsList[idx] = res;

        // ── UPDATE EQUIPMENT STATUS TO AVAILABLE ──
        const eqName = res.equipmentName;
        const eq = this.equipments.find(e => e.name === eqName || e.equipmentName === eqName);
        if (eq) {
          eq.status = 'Available';
          this.equipmentService.updateEquipment(eq.id!, eq).subscribe({
            next: () => {
              console.log('Equipment status reset to Available');
              this.calculateEquipmentHistory(eq); // Refresh history
            },
            error: (err) => console.error('Failed to update equipment status', err)
          });
        }

        this.applyTicketFilters();
        this.clearWorkbenchState(this.workbenchTicket!.id!);
        this.showWorkbench = false;
        this.selectedTicket = null;
        this.selectedEquipment = null;
        this.workbenchTicket = null;
        this.showCompleteConfirmation = false;
        
        // ── CONSUME PARTS ──
        if (this.partsUsed && this.partsUsed.length > 0) {
          const consumed = this.partsUsed.map(p => ({
            name: p.name,
            type: p.type,
            specification: p.specification,
            qty: p.qty,
            assignedToEquipmentName: res.equipmentName,
            assignedToEquipmentId: this.workbenchEquipment?.id
          }));
          
          if (this.currentUser?.id) {
            this.partRequestService.consumeParts(this.currentUser.id, consumed).subscribe({
              next: () => {
                console.log('Parts consumed from technician inventory');
                this.loadUserInventory(); // Refresh technician inventory
              },
              error: (err) => console.error('Error consuming parts from technician inventory', err)
            });
          }
        }

        // Return to my tickets view
        this.viewMode = 'tickets';
      },
      error: () => {
        alert('Failed to update ticket status. Please try again.');
        this.showCompleteConfirmation = false;
      }
    });
  }

  closeCompleteConfirmation(): void {
    this.showCompleteConfirmation = false;
  }

  cancelRepair(): void {
    if (!this.workbenchTicket?.id) return;
    this.showCancelConfirmation = true;
  }

  confirmCancelTicket(): void {
    if (!this.workbenchTicket?.id) return;
    
    // Instead of updating status to Cancelled, we delete the ticket as requested
    const eqName = this.workbenchTicket.equipmentName;
    const ticketId = this.workbenchTicket.id;

    this.ticketService.deleteTicket(ticketId).subscribe({
      next: () => {
        // Remove from local list
        this.ticketsList = this.ticketsList.filter(t => t.id !== ticketId);
        
        // ── RESET EQUIPMENT STATUS ON CANCEL (DELETE) ──
        const eq = this.equipments.find(e => e.name === eqName || e.equipmentName === eqName);
        if (eq) {
          eq.status = 'Available';
          this.equipmentService.updateEquipment(eq.id!, eq).subscribe({
            next: () => {
              console.log('Equipment status reset to Available after cancellation');
              this.calculateEquipmentHistory(eq);
            },
            error: (err) => console.error('Failed to reset equipment status', err)
          });
        }

        this.applyTicketFilters();
        this.calculateStats();
        this.clearWorkbenchState(ticketId);
        this.showWorkbench = false;
        this.selectedTicket = null;
        this.selectedEquipment = null;
        this.workbenchTicket = null;
        this.showCancelConfirmation = false;
        
        this.viewMode = 'tickets';
      },
      error: () => {
        alert('Failed to cancel (delete) ticket. Please try again.');
        this.showCancelConfirmation = false;
      }
    });
  }

  closeCancelConfirmation(): void {
    this.showCancelConfirmation = false;
  }

  reopenTicket(ticket: Ticket): void {
    if (!ticket.id) return;
    
    const updated: Ticket = { ...ticket, status: 'Open' };
    this.ticketService.updateTicket(ticket.id, updated).subscribe({
      next: (res) => {
        const idx = this.ticketsList.findIndex(t => t.id === res.id);
        if (idx !== -1) this.ticketsList[idx] = res;
        this.selectedTicket = res;

        // Reset equipment status to 'In Maintenance'
        const eqName = res.equipmentName;
        const eq = this.equipments.find(e => e.name === eqName || e.equipmentName === eqName);
        if (eq) {
          eq.status = 'In Maintenance';
          this.equipmentService.updateEquipment(eq.id!, eq).subscribe({
            next: () => {
              console.log('Equipment status set to In Maintenance after re-opening');
              this.calculateEquipmentHistory(eq);
            },
            error: (err) => console.error('Failed to update equipment status', err)
          });
        }

        this.applyTicketFilters();
        this.calculateStats();
      },
      error: (err) => {
        console.error('Error re-opening ticket', err);
        alert('Failed to re-open ticket. Please try again.');
      }
    });
  }
}
