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
  duration?: string;
  cost?: string;
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
  workbenchTicket: Ticket | null = null;
  workbenchEquipment: EquipmentWithHistory | null = null;

  // Timer
  timerSeconds: number = 0;
  timerRunning: boolean = false;
  private timerInterval: any = null;

  // Kanban Checklist Tasks (session-only)
  repairChecklist: { id: string; label: string; status: 'todo' | 'in-progress' | 'waiting' | 'testing' | 'done'; editing: boolean }[] = [];
  newTaskLabel: string = '';
  draggedTaskId: string | null = null;

  // Notes
  workNotes: string[] = [];
  newNoteText: string = '';

  // Parts Used
  partsUsed: { name: string; qty: number }[] = [];
  newPartName: string = '';
  newPartQty: number = 1;
  selectedPartCategory: string = '';
  selectedPartFromInventory: any = null;

  // Timelines
  workbenchTimeline: { event: string; status?: string; color: string; time: Date }[] = [];

  // Technician Inventory
  userInventory: { name: string; totalQty: number; category: string }[] = [];

  get inventoryCategories(): string[] {
    const cats = this.userInventory.map(i => i.category).filter(c => !!c);
    return Array.from(new Set(cats)).sort();
  }

  get partsInCategory(): any[] {
    if (!this.selectedPartCategory) return [];
    return this.userInventory
      .filter(i => i.category === this.selectedPartCategory)
      .sort((a, b) => a.name.localeCompare(b.name));
  }

  onPartSelect(partName: string): void {
    const part = this.userInventory.find(i => i.name === partName);
    this.selectedPartFromInventory = part || null;
    this.newPartName = partName;
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
    this.stopTimer();
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
          (req.items || []).forEach(item => {
            const existing = itemsList.find(i => i.name.toLowerCase() === item.partName.toLowerCase());
            if (existing) {
              existing.totalQty += item.quantity;
            } else {
              itemsList.push({
                name: item.partName,
                totalQty: item.quantity,
                category: item.category
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

  applyTicketFilters(): void {
    // Only show tickets that are NOT 'Resolved' or 'Closed' (Still Open)
    this.filteredTickets = this.ticketsList.filter(t =>
      t.status !== 'Resolved' && t.status !== 'Closed'
    ).sort((a, b) => {
      const dateA = a.createdAt ? new Date(a.createdAt).getTime() : 0;
      const dateB = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      return dateB - dateA; // Newest first
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
    // Filter tickets that belong to this equipment
    // We match by name since that's what's currently in the Ticket model
    const relatedTickets = this.ticketsList.filter(t => t.equipmentName === eq.name || t.equipmentName === eq.equipmentName);

    // Map to RepairHistoryEntry (Short History)
    eq.repairHistory = relatedTickets.map(t => ({
      issue: t.title,
      date: t.createdAt ? new Date(t.createdAt).toISOString().split('T')[0] : 'N/A',
      technician: 'Technician' // Placeholder until Ticket has assigned technician info
    })).sort((a, b) => b.date.localeCompare(a.date));

    // Map to ExtendedHistoryEntry (Rich Timeline)
    eq.extendedHistory = relatedTickets.map(t => ({
      type: 'ticket' as 'ticket',
      priority: t.priority?.toUpperCase(),
      referenceId: t.id || 'N/A',
      dateStr: t.createdAt ? new Date(t.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) : 'N/A',
      timeStr: t.createdAt ? new Date(t.createdAt).toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' }) : 'N/A',
      title: t.title,
      description: t.description,
      user: 'Requested by User'
    })).sort((a, b) => b.referenceId.localeCompare(a.referenceId));

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
    this.selectedEquipment = eq || null;
    if (this.selectedEquipment) {
      this.calculateEquipmentHistory(this.selectedEquipment);
    }
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
    const inactiveStatuses = ['Resolved', 'Closed', 'Completed', 'Done'];
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
    this.timerSeconds = 0;
    this.timerRunning = false;
    this.repairChecklist = [];
    this.newTaskLabel = '';
    this.draggedTaskId = null;
    this.workNotes = [];
    this.newNoteText = '';
    this.partsUsed = [];
    this.newPartName = '';
    this.newPartQty = 1;
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
      workNotes: this.workNotes,
      partsUsed: this.partsUsed,
      timerSeconds: this.timerSeconds,
      timeline: this.workbenchTimeline
    };
    localStorage.setItem(`wb_state_${this.workbenchTicket.id}`, JSON.stringify(state));
  }

  private loadWorkbenchState(ticketId: string): void {
    const saved = localStorage.getItem(`wb_state_${ticketId}`);
    if (saved) {
      try {
        const state = JSON.parse(saved);
        this.repairChecklist = state.repairChecklist || [];
        this.workNotes = state.workNotes || [];
        this.partsUsed = state.partsUsed || [];
        this.timerSeconds = state.timerSeconds || 0;
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
    this.stopTimer();
    this.saveWorkbenchState();
    this.showWorkbench = false;
    this.workbenchTicket = null;
  }

  startTimer(): void {
    if (this.timerRunning) return;
    this.timerRunning = true;
    this.timerInterval = setInterval(() => { this.timerSeconds++; }, 1000);
  }

  stopTimer(): void {
    this.timerRunning = false;
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  formatTimer(): string {
    const h = Math.floor(this.timerSeconds / 3600);
    const m = Math.floor((this.timerSeconds % 3600) / 60);
    const s = this.timerSeconds % 60;
    return `${this.pad(h)}:${this.pad(m)}:${this.pad(s)}`;
  }

  private pad(n: number): string {
    return n.toString().padStart(2, '0');
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

  addNote(): void {
    const note = this.newNoteText.trim();
    if (!note) return;
    this.workNotes.push(note);
    this.workbenchTimeline.push({ event: `Note added: "${note}"`, color: '#8b5cf6', time: new Date() });
    this.newNoteText = '';
    this.saveWorkbenchState();
  }

  addPart(): void {
    const name = this.newPartName.trim();
    if (!name) return;

    // Validation against inventory if selected from list
    if (this.selectedPartFromInventory && name === this.selectedPartFromInventory.name) {
      if (this.newPartQty > this.selectedPartFromInventory.totalQty) {
        alert(`⚠️ Insufficient stock! You only have ${this.selectedPartFromInventory.totalQty} units of "${name}" available.`);
        return;
      }
    }

    const existing = this.partsUsed.find((p: { name: string; qty: number }) => p.name.toLowerCase() === name.toLowerCase());
    if (existing) {
      existing.qty += this.newPartQty;
    } else {
      this.partsUsed.push({ name, qty: this.newPartQty });
    }

    this.workbenchTimeline.push({ event: `Part added: ${name} x${this.newPartQty}`, color: '#f59e0b', time: new Date() });

    // Reset selection
    this.newPartName = '';
    this.newPartQty = 1;
    this.selectedPartFromInventory = null;
    this.saveWorkbenchState();
  }

  removePart(index: number): void {
    this.partsUsed.splice(index, 1);
    this.saveWorkbenchState();
  }

  completeRepair(): void {
    if (!this.workbenchTicket?.id) return;
    const confirmed = confirm('Mark this ticket as Completed? This will close the repair session.');
    if (!confirmed) return;
    const updated: Ticket = { ...this.workbenchTicket, status: 'Completed' };
    this.ticketService.updateTicket(this.workbenchTicket.id, updated).subscribe({
      next: (res) => {
        const idx = this.ticketsList.findIndex(t => t.id === res.id);
        if (idx !== -1) this.ticketsList[idx] = res;
        // Remove from filtered (completed tickets hidden from active list)
        this.applyTicketFilters();
        this.clearWorkbenchState(this.workbenchTicket!.id!);
        this.showWorkbench = false;
        this.selectedTicket = null;
        this.selectedEquipment = null;
        this.workbenchTicket = null;
      },
      error: () => {
        alert('Failed to update ticket status. Please try again.');
      }
    });
  }
}
