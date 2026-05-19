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
import { LiveWorkbenchComponent } from './live-workbench/live-workbench.component';
import { RefreshService } from '../shared/refresh.service';
import { Subscription } from 'rxjs';
import { ToastService } from '../shared/toast.service';
import { ConfirmDialogService } from '../shared/components/confirm-dialog/confirm-dialog.service';
import { TranslatePipe } from '../shared/translate.pipe';

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
  imports: [CommonModule, FormsModule, DragDropModule, LiveWorkbenchComponent, TranslatePipe],
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

  // Real-time Activity Badges
  newEquipments = new Set<string>();
  updatedEquipments = new Set<string>();
  private previousIds = new Set<string>();

  newTickets = new Set<string>();
  updatedTickets = new Set<string>();
  private previousTicketIds = new Set<string>();

  newTicket: Ticket = {
    title: '',
    description: '',
    category: 'Maintenance',
    priority: 'Medium',
    status: 'Open',
    deadline: '',
    technicianName: ''
  };

  // ── Live Workbench State ──
  showWorkbench: boolean = false;
  showWorkbenchHistory: boolean = false;
  workbenchTicket: Ticket | null = null;
  workbenchEquipment: EquipmentWithHistory | null = null;

  // Technician Inventory
  userInventory: { 
    name: string; 
    totalQty: number; 
    category: string;
    type: string;
    specification: string;
    brand?: string;
  }[] = [];
  private refreshSubscription?: Subscription;



  constructor(
    private authService: AuthService,
    private partRequestService: PartRequestService,
    private refreshService: RefreshService,
    private ticketService: TicketService,
    private equipmentService: EquipmentService,
    private categoryService: CategoryService,
    private toastService: ToastService,
    private confirmDialogService: ConfirmDialogService
  ) { }

  ngOnInit(): void {
    this.authService.user$.subscribe(user => {
      this.currentUser = user;
      if (this.currentUser) {
        this.loadAllData();
      }
    });

    // Listen for global refresh events (e.g., from AI Assistant)
    this.refreshSubscription = this.refreshService.refresh$.subscribe(actionType => {
      console.log('TicketsComponent: Refreshing all data due to action:', actionType);
      this.loadAllData();
    });

    // Feature 3: Auto-polling every 30 seconds
    setInterval(() => {
      if (this.currentUser) {
        this.loadAllData();
      }
    }, 30000);
  }

  private loadAllData(): void {
    if (!this.currentUser) return;
    this.loadCategories();
    this.loadEquipments();
    this.loadTickets();
    this.loadUserInventory();
  }

  switchView(mode: 'equipment' | 'tickets'): void {
    this.viewMode = mode;
    this.selectedEquipment = null;
    this.selectedTicket = null;
    this.showFullHistory = false;
    this.showWorkbench = false;
  }

  ngOnDestroy(): void {
    if (this.refreshSubscription) {
      this.refreshSubscription.unsubscribe();
    }
  }

  loadCategories(): void {
    this.categoryService.getAllCategories().subscribe({
      next: (data: EquipmentCategory[]) => {
        this.categories = data;
        this.applyFilters();
      },
      error: (err: any) => console.error('Error loading categories', err)
    });
  }

  loadUserInventory(): void {
    if (!this.currentUser?.id) return;
    this.partRequestService.getMyRequests(this.currentUser.id).subscribe({
      next: (requests: PartRequest[]) => {
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
                  id: item.equipmentId,
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
      error: (err: any) => console.error('Failed to load user inventory', err)
    });
  }

  loadTickets(): void {
    // Technicians see only their assigned/created tickets; others see only their own requests
    const fetchCall = this.currentUser?.role === 'TECHNICIAN'
      ? this.ticketService.getTicketsForTechnician(this.currentUser.id)
      : this.ticketService.getTicketsByUser(this.currentUser.id);

    fetchCall.subscribe({
      next: (tickets: Ticket[]) => {
        const isFirstLoad = this.previousTicketIds.size === 0;
        const now = new Date().getTime();

        this.ticketsList = tickets.map(t => {
          if (t.id && !isFirstLoad) {
            // Check if New
            if (!this.previousTicketIds.has(t.id)) {
              this.newTickets.add(t.id);
              setTimeout(() => this.newTickets.delete(t.id!), 8000);
            }
            // Check if Updated (we'll use updatedAt if available, otherwise fallback to finding it in the array, but backend ticket model might not have updatedAt exposed yet. Let's assume t.updatedAt exists)
            else if ((t as any).updatedAt) {
              const updatedTime = new Date((t as any).updatedAt).getTime();
              if (now - updatedTime < 60000) {
                this.updatedTickets.add(t.id);
                setTimeout(() => this.updatedTickets.delete(t.id!), 8000);
              }
            }
          }
          if (t.id) this.previousTicketIds.add(t.id);
          return t;
        });

        this.applyTicketFilters();
        // Update history for all equipments now that we have the tickets
        this.equipments.forEach(eq => this.calculateEquipmentHistory(eq));
        this.applyFilters();
      },
      error: (err: any) => {
        console.error('Failed to load real tickets', err);
        this.ticketsList = [];
      }
    });
  }

  loadEquipments(): void {
    this.equipmentService.getAllEquipment().subscribe({
      next: (data) => {
        const isFirstLoad = this.previousIds.size === 0;
        const now = new Date().getTime();

        this.equipments = data.map((eq: Equipment) => {
          if (eq.id && !isFirstLoad) {
            // Check if New
            if (!this.previousIds.has(eq.id)) {
              this.newEquipments.add(eq.id);
              setTimeout(() => this.newEquipments.delete(eq.id!), 8000);
            } 
            // Check if Updated (within last 60 seconds)
            else if (eq.updatedAt) {
              const updatedTime = new Date(eq.updatedAt).getTime();
              if (now - updatedTime < 60000) {
                this.updatedEquipments.add(eq.id);
                setTimeout(() => this.updatedEquipments.delete(eq.id!), 8000);
              }
            }
          }
          if (eq.id) this.previousIds.add(eq.id);

          return {
            ...eq,
            assignedUser: eq.department || 'Unassigned',
            repairHistory: [],
            extendedHistory: [],
            lastRepair: undefined
          };
        });
        
        this.applyFilters();
        this.calculateStats();
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
        Object.values(e.specifications || {}).join(' ').toLowerCase().includes(q)
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

    // 5. Exclude Consumables (unless they have status Installed, Broken, or Maintenance)
    if (this.categories && this.categories.length > 0) {
      result = result.filter(e => {
        const cat = this.categories.find(c => c.name === e.category);
        if (cat && cat.types) {
          const tInfo = cat.types.find(t => t.name === e.type);
          if (tInfo && tInfo.nature === 'Consumable') {
            return e.status === 'Installed' || e.status === 'Broken' || e.status === 'Maintenance';
          }
        }
        return true;
      });
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
      const reader = new FileReader();
      reader.onload = (e: any) => {
        const image = new Image();
        image.onload = () => {
          const canvas = document.createElement('canvas');
          canvas.width = image.width;
          canvas.height = image.height;
          const ctx = canvas.getContext('2d');
          if (!ctx) return;
          ctx.drawImage(image, 0, 0);
          const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
          
          // Use the jsQR library we added to index.html
          const code = (window as any).jsQR(imageData.data, imageData.width, imageData.height);
          
          if (code && code.data) {
            let extractedSerial = code.data;
            try {
              // If it's a JSON string (our app's standard format)
              const data = JSON.parse(code.data);
              extractedSerial = data.serial || data.id || code.data;
            } catch (e) {
              // Not JSON, use raw string
            }

            console.log('Decoded QR Serial:', extractedSerial);
            this.filterSerial = extractedSerial;
            this.applyFilters();

            // If we found matching equipment, select it automatically for a better UX
            if (this.filteredEquipments.length > 0) {
              // Try to find an exact match first
              const exactMatch = this.filteredEquipments.find(eq => 
                (eq.serialNumber || '').toLowerCase() === extractedSerial.toLowerCase() ||
                eq.id === extractedSerial
              );
              this.selectEquipment(exactMatch || this.filteredEquipments[0]);
            }
          } else {
            alert('QR Code not detected. Please ensure the image is clear and contains a valid code.');
          }
        };
        image.src = e.target.result;
      };
      reader.readAsDataURL(file);
    }
    // Reset file input so same file can be uploaded again if needed
    event.target.value = '';
  }

  triggerQRUpload(): void {
    const fileInput = document.getElementById('qr-upload-input');
    if (fileInput) {
      fileInput.click();
    }
  }

  hasActiveTicket(equipmentName: string | undefined): boolean {
    return !!this.getActiveTicketForEquipment(equipmentName);
  }

  getActiveTicketForEquipment(equipmentName: string | undefined): Ticket | null {
    if (!equipmentName) return null;
    // Block if there is any ticket that is NOT Resolved, Closed, or Completed
    const inactiveStatuses = ['Resolved', 'Closed', 'Completed', 'Cancelled'];
    return this.ticketsList.find(t =>
      (t.equipmentName === equipmentName) &&
      !inactiveStatuses.includes(t.status || '')
    ) || null;
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
      equipmentId: this.selectedEquipment.id,
      equipmentName: eqName,
      deadline: ''
    };

    // TECHNICIAN: self-assign and mark In Progress (will open workbench immediately)
    if (this.currentUser?.role === 'TECHNICIAN') {
      this.newTicket.assignedTo = this.currentUser.id;
      this.newTicket.technicianName = ((this.currentUser.firstName || '') + ' ' + (this.currentUser.lastName || '')).trim();
      this.newTicket.status = 'In Progress';
    }
    // STOCK_MANAGER: leave assignedTo blank — backend's findBestTechnician() handles it

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
    if (!this.newTicket.title) return;
    this.isSubmitting = true;

    const ticketData: Ticket = {
      ...this.newTicket,
      userId: this.currentUser?.id,
      userName: (this.currentUser?.firstName ? this.currentUser.firstName + ' ' + (this.currentUser.lastName || '') : '') || this.currentUser?.email || 'Unknown User',
      userRole: this.currentUser?.role || 'Technician',
      equipmentId: this.newTicket.equipmentId || this.selectedEquipment?.id,
      attachments: this.attachmentFiles.map(f => f.base64 || f.name)
    };

    const request = this.isEditMode && this.newTicket.id
      ? this.ticketService.updateTicket(this.newTicket.id, ticketData)
      : this.ticketService.createTicket(ticketData);

    request.subscribe({
      next: (result: Ticket) => {
        console.log(this.isEditMode ? "Ticket updated" : "Ticket created", result);

        if (this.isEditMode) {
          const idx = this.ticketsList.findIndex(t => t.id === result.id);
          if (idx !== -1) this.ticketsList[idx] = result;
          this.selectedTicket = result;
          this.toastService.update(`Ticket "${result.title}" updated successfully.`);
        } else {
          this.ticketsList.unshift(result);
          if (this.currentUser?.role === 'TECHNICIAN') {
            this.toastService.success(`Ticket "${result.title}" created. Opening workbench...`);
          } else {
            const assignedMsg = result.technicianName
              ? ` Assigned to ${result.technicianName}.`
              : ' A technician will be assigned shortly.';
            this.toastService.success(`Ticket "${result.title}" created.${assignedMsg}`);
          }

          // If technician, auto-open workbench immediately
          if (this.currentUser?.role === 'TECHNICIAN' && result.status !== 'Completed') {
            this.startWorkbench(result);
          }
        }

        this.closeModal();
        this.isSubmitting = false;
        this.applyTicketFilters();

        if (!this.isEditMode && this.selectedEquipment) {
          // New ticket: Update status to Maintenance
          this.selectedEquipment.status = 'Maintenance';
          this.calculateEquipmentHistory(this.selectedEquipment);

          const eqId = this.selectedEquipment.id || '';
          if (eqId) {
            this.equipmentService.updateEquipment(eqId, this.selectedEquipment).subscribe({
              next: () => {
                console.log('Equipment status updated to Maintenance');
                this.toastService.info(`Equipment status updated to Maintenance`);
              },
              error: (err: any) => console.error('Failed to update equipment status', err)
            });
          }
        } else if (this.isEditMode && result.status && ['Closed', 'Resolved', 'Cancelled', 'Completed'].includes(result.status)) {
          // Update ticket to finished status: Reset equipment to Available
          const eqId = result.equipmentId;
          const eq = this.equipments.find(e => e.id === eqId || e.name === result.equipmentName || e.equipmentName === result.equipmentName);
          if (eq) {
            eq.status = 'Available';
            this.equipmentService.updateEquipment(eq.id!, eq).subscribe({
              next: () => {
                console.log('Equipment status reset to Available after ticket resolution/cancellation');
                this.calculateEquipmentHistory(eq);
              },
              error: (err) => console.error('Failed to reset equipment status', err)
            });
          }
        }

        this.viewMode = 'tickets';
        // Auto-refresh data to pull real-time changes
        this.loadAllData();
        this.refreshService.triggerRefresh('TICKET');
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

  async deleteTicket(id: string | undefined): Promise<void> {
    if (!id) return;

    const confirmed = await this.confirmDialogService.confirm({
      title: 'Delete Ticket',
      message: 'Are you sure you want to delete this ticket? This action cannot be undone.',
      confirmText: 'Delete',
      isDanger: true
    });

    if (!confirmed) return;

    // Find ticket first to get equipment name for status reset
    const ticketToDelete = this.ticketsList.find(t => t.id === id);
    const eqName = ticketToDelete?.equipmentName;

    this.ticketService.deleteTicket(id).subscribe({
      next: () => {
        // Remove from memory
        this.ticketsList = this.ticketsList.filter(t => t.id !== id);

        // ── RESET EQUIPMENT STATUS TO AVAILABLE ──
        if (eqName) {
          const eq = this.equipments.find(e => e.name === eqName || e.equipmentName === eqName);
          if (eq) {
            eq.status = 'Available';
            this.equipmentService.updateEquipment(eq.id!, eq).subscribe({
              next: () => {
                console.log('Equipment status reset to Available after ticket deletion');
                this.calculateEquipmentHistory(eq);
              },
              error: (err: any) => console.error('Failed to reset equipment status', err)
            });
          }
        }

        // Refresh detail state
        this.selectedTicket = null;
        if (this.selectedEquipment) {
          this.calculateEquipmentHistory(this.selectedEquipment);
        }

        // Refresh lists and stats
        this.calculateStats();
        this.applyTicketFilters();

        this.toastService.delete(`Ticket deleted successfully.`);
        console.log('Ticket deleted successfully');
        
        // Auto-refresh data to pull real-time changes
        this.loadAllData();
        this.refreshService.triggerRefresh('TICKET');
      },
      error: (err: any) => {
        console.error('Error deleting ticket', err);
        // Fallback for demo: still remove from list
        this.ticketsList = this.ticketsList.filter(t => t.id !== id);
        this.selectedTicket = null;
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
    if (this.currentUser?.role !== 'TECHNICIAN') {
      alert('Only technicians can access the Live Workbench.');
      return;
    }
    this.workbenchTicket = { ...ticket };
    // Resolve workbenchEquipment from selectedEquipment or equipment list (needed for part lifecycle)
    this.workbenchEquipment = this.selectedEquipment
      || this.equipments.find(e => e.name === ticket.equipmentName || e.equipmentName === ticket.equipmentName)
      || null;
    
    // Only update status if not already In Progress (avoids redundant API call for self-created tickets)
    if (ticket.status !== 'In Progress') {
      const updated: Ticket = { ...ticket, status: 'In Progress' };
      this.ticketService.updateTicket(ticket.id!, updated).subscribe({
        next: (res: Ticket) => {
          const idx = this.ticketsList.findIndex(t => t.id === res.id);
          if (idx !== -1) this.ticketsList[idx] = res;
          this.applyTicketFilters();
        },
        error: () => { }
      });
    }
    this.showWorkbench = true;
  }

  onWorkbenchCancel(): void {
    if (!this.workbenchTicket?.id) return;
    
    const eqName = this.workbenchTicket.equipmentName;
    const ticketId = this.workbenchTicket.id;

    this.ticketService.deleteTicket(ticketId).subscribe({
      next: () => {
        this.ticketsList = this.ticketsList.filter(t => t.id !== ticketId);
        const eq = this.equipments.find(e => e.name === eqName || e.equipmentName === eqName);
        if (eq) {
          eq.status = 'Available';
          this.equipmentService.updateEquipment(eq.id!, eq).subscribe({
            next: () => {
              this.calculateEquipmentHistory(eq);
            }
          });
        }
        this.applyTicketFilters();
        this.calculateStats();
        this.showWorkbench = false;
        this.workbenchTicket = null;
        this.viewMode = 'tickets';
      }
    });
  }

  onWorkbenchComplete(event: { 
    workNote: string; 
    repairTasks: any[]; 
    partsUsed: any[]; 
    isBroken?: boolean;
    diagnosisResult?: string;
    validationSummary?: string;
    partsInstalled?: any[];
  }): void {
    if (!this.workbenchTicket?.id) return;
    
    const updated: Ticket = { 
      ...this.workbenchTicket, 
      status: 'Completed',
      workNote: event.workNote,
      repairTasks: event.repairTasks,
      partsUsed: event.partsUsed,
      diagnosisResult: event.diagnosisResult,
      validationSummary: event.validationSummary,
      partsInstalled: event.partsInstalled
    };

    this.ticketService.updateTicket(this.workbenchTicket.id, updated).subscribe({
      next: (res: Ticket) => {
        this.toastService.success(`Maintenance on ${res.equipmentName} completed.`);
        const idx = this.ticketsList.findIndex(t => t.id === res.id);
        if (idx !== -1) this.ticketsList[idx] = res;

        // Capture BEFORE nulling any references — used in async closures below
        const completedEquipmentId: string | undefined = this.workbenchEquipment?.id || res.equipmentId;
        const completedEquipmentName: string | undefined = res.equipmentName || undefined;
        const completedTicketId: string | undefined = res.id;

        const processEquipmentUpdate = (eq: any) => {
          const wasInstalled = eq.assignedToEquipmentId || eq.assignedToEquipmentName;
          if (wasInstalled) {
            if (event.isBroken) {
              eq.status = 'Unrepairable';
              const oldPcId = eq.assignedToEquipmentId;
              const oldPcName = eq.assignedToEquipmentName;
              eq.assignedToEquipmentId = undefined;
              eq.assignedToEquipmentName = undefined;
              
              if (!eq.lifecycle) eq.lifecycle = [];
              eq.lifecycle.push({
                status: 'Unrepairable',
                timestamp: new Date().toISOString(),
                description: `Maintenance failed: marked as unrepairable. Uninstalled from PC ${oldPcName || ''}. Reason: ${event.workNote}`,
                actor: this.currentUser?.firstName || 'Technician'
              });

              // Update the parent PC's specifications to clear this part type
              if (oldPcId) {
                this.equipmentService.getEquipmentById(oldPcId).subscribe({
                  next: (pc) => {
                    if (pc && pc.specifications) {
                      const partType = (eq.type || '').toLowerCase().trim();
                      const specKey = Object.keys(pc.specifications).find(
                        k => k.toLowerCase().trim() === partType
                      );
                      if (specKey) {
                        delete pc.specifications[specKey];
                        this.equipmentService.updateEquipment(pc.id!, pc).subscribe({
                          next: () => {
                            console.log(`Cleared ${specKey} spec from PC ${pc.equipmentName}`);
                            this.loadAllData();
                          }
                        });
                      }
                    }
                  }
                });
              }
            } else {
              eq.status = 'Installed';
              if (!eq.lifecycle) eq.lifecycle = [];
              eq.lifecycle.push({
                status: 'Installed',
                timestamp: new Date().toISOString(),
                description: `Maintenance Completed: ${res.title}. Status returned to Installed in ${eq.assignedToEquipmentName || ''}.`,
                actor: this.currentUser?.firstName || 'Technician'
              });
            }
          } else {
            eq.status = event.isBroken ? 'Unrepairable' : 'Available';
            
            // ── AUTOMATED LIFECYCLE UPDATE ──
            if (!eq.lifecycle) eq.lifecycle = [];
            eq.lifecycle.push({
              status: eq.status,
              timestamp: new Date().toISOString(),
              description: `Maintenance Completed: ${res.title}. ${event.workNote}`,
              actor: this.currentUser?.firstName || 'Technician'
            });
          }

          // ── AUTOMATED SPECIFICATION SYNC ──
          if (event.partsInstalled && event.partsInstalled.length > 0) {
            if (!eq.specifications) eq.specifications = {};
            event.partsInstalled.forEach((part: any) => {
              if (part.replacesSpecKey) {
                const newValue = [part.name, part.specification].filter(Boolean).join(' ').trim();
                eq.specifications![part.replacesSpecKey] = newValue;
                console.log(`Spec synced: ${part.replacesSpecKey} -> ${newValue}`);
              }
            });
          }

          this.equipmentService.updateEquipment(eq.id!, eq).subscribe({
            next: () => {
              this.calculateEquipmentHistory(eq);
              this.loadAllData();
              this.refreshService.triggerRefresh('TICKET');
            }
          });
        };

        const eq = this.equipments.find(e => e.id === completedEquipmentId);
        if (eq) {
          processEquipmentUpdate(eq);
        } else if (completedEquipmentId) {
          this.equipmentService.getEquipmentById(completedEquipmentId).subscribe(fetchedEq => processEquipmentUpdate(fetchedEq));
        }

        this.applyTicketFilters();
        this.showWorkbench = false;
        this.workbenchTicket = null;
        this.selectedTicket = null;
        this.selectedEquipment = null;
        
        // ── CONSUME PARTS & UPDATE PART RECORDS ──
        if (event.partsInstalled && event.partsInstalled.length > 0) {
          const consumed = event.partsInstalled.map((p: any) => ({
            name: p.name,
            qty: p.qty,
            specification: p.specification,
            assignedToEquipmentName: completedEquipmentName,
            assignedToEquipmentId: completedEquipmentId
          }));
          
          if (this.currentUser?.id) {
            this.partRequestService.consumeParts(this.currentUser.id, consumed).subscribe({
              next: () => this.loadUserInventory()
            });

            // Update individual part records to "Installed" with full lifecycle entry
            event.partsInstalled.forEach((part: any) => {
              if (part.equipmentId) {
                this.equipmentService.getEquipmentById(part.equipmentId).subscribe(partRecord => {
                  partRecord.status = 'Installed';
                  partRecord.assignedToEquipmentName = completedEquipmentName;
                  partRecord.assignedToEquipmentId = completedEquipmentId;
                  if (!partRecord.lifecycle) partRecord.lifecycle = [];
                  partRecord.lifecycle.push({
                    status: 'Installed',
                    timestamp: new Date().toISOString(),
                    description: `Installed in ${completedEquipmentName} during ticket #${completedTicketId}`,
                    actor: this.currentUser?.firstName || 'Technician'
                  });
                  this.equipmentService.updateEquipment(partRecord.id!, partRecord).subscribe();
                });
              }
            });
          }
        }
        this.viewMode = 'tickets';
        // Note: loadAllData() and triggerRefresh() are called inside processEquipmentUpdate's
        // subscribe callback to ensure the DB write completes first.
        // If no equipment was found to update, do a simple refresh.
        if (!completedEquipmentId && !this.equipments.find(e => e.id === completedEquipmentId)) {
          this.loadAllData();
          this.refreshService.triggerRefresh('TICKET');
        }
      }
    });
  }



  reopenTicket(ticket: Ticket): void {
    if (!ticket.id) return;
    
    const updated: Ticket = { ...ticket, status: 'Open' };
    this.ticketService.updateTicket(ticket.id, updated).subscribe({
      next: (res: Ticket) => {
        const idx = this.ticketsList.findIndex(t => t.id === res.id);
        if (idx !== -1) this.ticketsList[idx] = res;
        this.selectedTicket = res;

        // Reset equipment status to 'Maintenance'
        const eqName = res.equipmentName;
        const eq = this.equipments.find(e => e.name === eqName || e.equipmentName === eqName);
        if (eq) {
          eq.status = 'Maintenance';
          this.equipmentService.updateEquipment(eq.id!, eq).subscribe({
            next: () => {
              console.log('Equipment status set to Maintenance after re-opening');
              this.calculateEquipmentHistory(eq);
              this.loadAllData();
              this.refreshService.triggerRefresh('TICKET');
            },
            error: (err) => console.error('Failed to update equipment status', err)
          });
        }

        this.applyTicketFilters();
        this.calculateStats();
      },
      error: (err: any) => {
        console.error('Error re-opening ticket', err);
        alert('Failed to re-open ticket. Please try again.');
      }
    });
  }
}
