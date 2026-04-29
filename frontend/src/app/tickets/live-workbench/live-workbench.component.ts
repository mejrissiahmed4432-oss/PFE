import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WorkbenchEngineService } from '../workbench-engine.service';
import { Ticket } from '../ticket.model';
import { Equipment } from '../../equipment/equipment.model';
import {
  WorkbenchState, WorkbenchAction, WorkbenchResource,
  ValidationItem, WorkbenchTimelineEntry,
  WorkflowStep, ActionCategory, ActionStatus, CapabilityResponse,
  ACTION_TYPES_BY_CATEGORY, CATEGORY_ICONS
} from '../workbench.model';

@Component({
  selector: 'app-live-workbench',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './live-workbench.component.html',
  styleUrl: './live-workbench.component.css'
})
export class LiveWorkbenchComponent implements OnInit {
  @Input() ticket!: Ticket;
  @Input() equipment!: Equipment;
  @Input() userInventory: any[] = [];
  @Output() complete = new EventEmitter<{ workNote: string; repairTasks: any[]; partsUsed: any[] }>();
  @Output() suspendWorkbench = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  suspend(): void {
    this.saveState();
    this.suspendWorkbench.emit();
  }

  capabilities: CapabilityResponse | null = null;
  isLoading = true;
  stepError = '';
  animating = false;
  animDir: 'left' | 'right' = 'right';

  // Expose constants to template
  ACTION_TYPES_BY_CATEGORY = ACTION_TYPES_BY_CATEGORY;
  CATEGORY_ICONS = CATEGORY_ICONS;

  ALL_ACTION_CATEGORIES: ActionCategory[] = [
    'Hardware', 'Software', 'Configuration', 'Network', 'Maintenance', 
    'Inspection', 'Power', 'Security', 'Storage', 'Peripheral', 
    'Performance', 'Thermal', 'Consumables', 'Firmware', 'Cabling'
  ];

  state: WorkbenchState = {
    ticketId: '',
    currentStep: 'diagnosis',
    diagnosisResult: '',
    diagnosisCategories: [],
    diagnosisSymptoms: [],
    additionalNotes: '',
    actions: [],
    validationChecklist: [],
    validationNotes: '',
    summaryNote: '',
    globalNotes: '',
    timeline: [
      { title: 'Ticket created', time: this.formatDateTime(new Date()), icon: 'edit', color: '#64748b' },
      { title: 'Assigned to technician', time: this.formatDateTime(new Date()), icon: 'user', color: '#64748b' },
      { title: 'Diagnosis started', time: this.formatDateTime(new Date()), icon: 'search', color: '#64748b' }
    ],
    startedAt: new Date().toISOString()
  };

  steps: { key: WorkflowStep; label: string; icon: string }[] = [
    { key: 'diagnosis', label: 'Diagnosis', icon: 'search' },
    { key: 'plan', label: 'Plan', icon: 'list' },
    { key: 'resources', label: 'Resources', icon: 'package' },
    { key: 'execution', label: 'Execution', icon: 'zap' },
    { key: 'validation', label: 'Testing', icon: 'check-circle' },
    { key: 'summary', label: 'Summary', icon: 'bar-chart' }
  ];

  // Dialogs
  showCancelDialog = false;
  showCompleteDialog = false;
  customAlert: { title: string; message: string } | null = null;
  newCheckLabel = '';

  // Resource Selection
  selectedActionId: string | null = null;

  // Inventory filters for resources step
  inventorySearch = '';

  constructor(private engine: WorkbenchEngineService) {}

  ngOnInit(): void {
    this.state.ticketId = this.ticket.id || '';
    this.loadState();
    const eqType = this.equipment?.type || this.equipment?.category || 'unknown';
    this.engine.getCapabilities(eqType).subscribe(cap => {
      this.capabilities = cap;
      this.isLoading = false;
      this.cleanupTimeline();
      
      // Auto-select first action if already on Resources step
      if (this.state.currentStep === 'resources' && this.state.actions.length > 0) {
        this.selectedActionId = this.state.actions[0].id;
      }

      if (this.state.timeline.length === 0) {
        this.pushTimeline('Maintenance Started', '#6366f1', 'edit');
        this.pushTimeline('Diagnosis Phase Started', '#3b82f6', 'search');
      }
    });
  }

  // ── Navigation ──────────────────────────────────────────────────────────────

  get stepIndex(): number {
    return this.steps.findIndex(s => s.key === this.state.currentStep);
  }

  nextStep(): void {
    const result = this.engine.validateStep(this.state.currentStep, this.state);
    if (!result.valid) {
      this.customAlert = {
        title: 'Missing Information',
        message: result.error
      };
      return;
    }
    this.stepError = '';

    if (this.state.currentStep === 'plan') {
      this.buildValidationChecklist();
    }

    const idx = this.stepIndex;
    if (idx < this.steps.length - 1) {
      const currentStepLabel = this.steps[idx].label;
      const nextStepLabel = this.steps[idx + 1].label;
      
      this.animDir = 'right';
      this.animating = true;
      setTimeout(() => {
        this.state.currentStep = this.steps[idx + 1].key;
        
        // Auto-select first action if moving to Resources step
        if (this.state.currentStep === 'resources' && this.state.actions.length > 0) {
          this.selectedActionId = this.state.actions[0].id;
        }

        this.pushTimeline(`${currentStepLabel} Completed`, '#10b981', 'check-circle');
        this.pushTimeline(`${nextStepLabel} Phase Started`, '#3b82f6', 'search');
        this.saveState();
        this.animating = false;
      }, 200);
    }
  }

  getProgressPercentage(): number {
    return ((this.stepIndex + 1) / this.steps.length) * 100;
  }

  get currentStepLabel(): string {
    return this.steps[this.stepIndex]?.label || '';
  }

  prevStep(): void {
    this.stepError = '';
    const idx = this.stepIndex;
    if (idx > 0) {
      const prevStepLabel = this.steps[idx - 1].label;
      this.animDir = 'left';
      this.animating = true;
      setTimeout(() => {
        this.state.currentStep = this.steps[idx - 1].key;
        
        // Auto-select first action if returning to Resources step
        if (this.state.currentStep === 'resources' && this.state.actions.length > 0) {
          this.selectedActionId = this.state.actions[0].id;
        }

        this.pushTimeline(`Returned to ${prevStepLabel}`, '#f59e0b', 'info');
        this.animating = false;
      }, 200);
    }
  }

  goToStep(step: WorkflowStep): void {
    const targetIdx = this.steps.findIndex(s => s.key === step);
    const currentIdx = this.stepIndex;

    if (targetIdx === currentIdx) return;

    if (targetIdx < currentIdx) {
      // Moving backward is always allowed
      this.stepError = '';
      this.state.currentStep = step;

      // Auto-select first action if jumping to Resources step
      if (this.state.currentStep === 'resources' && this.state.actions.length > 0) {
        this.selectedActionId = this.state.actions[0].id;
      }

      this.pushTimeline(`Returned to ${this.steps[targetIdx].label}`, '#f59e0b', 'info');
    } else {
      // Moving forward: must validate current step first
      const result = this.engine.validateStep(this.state.currentStep, this.state);
      if (!result.valid) {
        this.customAlert = { title: 'Required Information', message: result.error };
        return;
      }
      
      // If moving more than one step forward, we only allow going to the immediate next one
      // or to steps that are already "accessible" (though usually you just go one by one)
      if (targetIdx === currentIdx + 1) {
        this.nextStep();
      }
    }
  }

  isStepCompleted(step: WorkflowStep): boolean {
    const idx = this.steps.findIndex(s => s.key === step);
    return idx < this.stepIndex;
  }

  // ── Diagnosis Step ──────────────────────────────────────────────────────────

  availableCategories = [
    { id: 'Hardware', label: 'Hardware', icon: '🔧' },
    { id: 'Software', label: 'Software/OS', icon: '💻' },
    { id: 'Configuration', label: 'Configuration', icon: '⚙️' },
    { id: 'Network', label: 'Network', icon: '🌐' },
    { id: 'Maintenance', label: 'Maintenance', icon: '🧹' },
    { id: 'Inspection', label: 'Inspection', icon: '🔍' },
    { id: 'Power', label: 'Power/Battery', icon: '⚡' },
    { id: 'Security', label: 'Security', icon: '🛡️' },
    { id: 'Storage', label: 'Storage', icon: '💾' },
    { id: 'Peripheral', label: 'Peripheral', icon: '🔌' },
    { id: 'Performance', label: 'Performance', icon: '🚀' },
    { id: 'Thermal', label: 'Thermal', icon: '🌡️' },
    { id: 'Consumables', label: 'Consumables', icon: '📦' },
    { id: 'Firmware', label: 'Firmware', icon: '💾' },
    { id: 'Cabling', label: 'Cabling/Wiring', icon: '🧵' }
  ];

  hasCategory(cId: string): boolean {
    return this.state.diagnosisCategories?.includes(cId) || false;
  }

  toggleCategory(cId: string): void {
    if (!this.state.diagnosisCategories) this.state.diagnosisCategories = [];
    const i = this.state.diagnosisCategories.indexOf(cId);
    if (i === -1) this.state.diagnosisCategories.push(cId);
    else this.state.diagnosisCategories.splice(i, 1);
    this.saveState();
  }

  // ── Plan Step ───────────────────────────────────────────────────────────────

  addAction(): void {
    const defaultCat: ActionCategory = 'Hardware';
    const newAction: WorkbenchAction = {
      id: Date.now().toString(),
      category: defaultCat,
      type: ACTION_TYPES_BY_CATEGORY[defaultCat][0],
      target: '',
      description: '',
      status: 'Planned',
      notes: '',
      resources: [],
      expandedInExecution: false
    };
    this.state.actions.push(newAction);
    this.saveState();
  }

  updateAction(id: string, field: keyof WorkbenchAction, value: any): void {
    const action = this.state.actions.find(a => a.id === id);
    if (!action) return;
    (action as any)[field] = value;
    // Reset action type when category changes
    if (field === 'category') {
      action.type = ACTION_TYPES_BY_CATEGORY[value as ActionCategory]?.[0] || '';
    }
    this.saveState();
  }

  removeAction(id: string): void {
    this.state.actions = this.state.actions.filter(a => a.id !== id);
    this.saveState();
  }

  getActionTypesForCategory(category: ActionCategory): string[] {
    return ACTION_TYPES_BY_CATEGORY[category] || [];
  }

  getCategoryIcon(category: ActionCategory): string {
    return CATEGORY_ICONS[category] || '⚙️';
  }

  getStatusClass(status: ActionStatus): string {
    const map: Record<ActionStatus, string> = {
      'Planned': 'status-planned',
      'In Progress': 'status-in-progress',
      'Done': 'status-done'
    };
    return map[status] || 'status-planned';
  }

  // ── Resources Step ──────────────────────────────────────────────────────────

  selectActionForResources(id: string): void {
    this.selectedActionId = id;
    this.saveState();
  }

  get selectedAction(): WorkbenchAction | undefined {
    return this.state.actions.find(a => a.id === this.selectedActionId);
  }

  get hardwareActions(): WorkbenchAction[] {
    return this.state.actions.filter(a => a.category === 'Hardware' || a.category === 'Consumables');
  }

  get softwareActions(): WorkbenchAction[] {
    return this.state.actions.filter(a => a.category === 'Software');
  }

  get configActions(): WorkbenchAction[] {
    return this.state.actions.filter(a => a.category === 'Configuration');
  }

  get networkActions(): WorkbenchAction[] {
    return this.state.actions.filter(a => a.category === 'Network');
  }

  get filteredInventory(): any[] {
    return this.userInventory.filter(p => {
      if (p.totalQty <= 0) return false;
      if (this.inventorySearch) {
        const q = this.inventorySearch.toLowerCase();
        if (!p.name?.toLowerCase().includes(q) && !p.specification?.toLowerCase().includes(q)) return false;
      }
      return true;
    });
  }

  isPartLinkedToAction(part: any, action: WorkbenchAction): boolean {
    return action.resources.some(r => r.name === part.name && r.specification === part.specification);
  }

  togglePartForAction(part: any, action: WorkbenchAction): void {
    const idx = action.resources.findIndex(r => r.name === part.name && r.specification === part.specification);
    if (idx === -1) {
      action.resources.push({ resourceType: 'part', name: part.name, quantity: 1, specification: part.specification });
    } else {
      action.resources.splice(idx, 1);
    }
    this.saveState();
  }

  // ── Execution Step ──────────────────────────────────────────────────────────

  toggleExecutionExpand(action: WorkbenchAction): void {
    action.expandedInExecution = !action.expandedInExecution;
  }

  setActionStatus(action: WorkbenchAction, status: ActionStatus): void {
    action.status = status;
    this.saveState();
  }

  getInstructions(action: WorkbenchAction): string[] {
    const defaults: Record<string, string[]> = {
      'Hardware-Install':     ['Power off device completely', 'Disconnect power cables', 'Open device case/panel', 'Install the component carefully', 'Close case and reconnect power', 'Boot system and verify functionality'],
      'Hardware-Replace':     ['Backup all data from device', 'Power off device', 'Remove old component', 'Install new component', 'Reconnect all cables', 'Power on and verify'],
      'Software-Install':     ['Download installer package', 'Verify system requirements', 'Run installer as administrator', 'Follow installation wizard', 'Configure initial settings', 'Restart if required', 'Verify installation success'],
      'Software-Update':      ['Back up current configuration', 'Download update package', 'Stop affected services', 'Apply update', 'Restart services', 'Verify update applied correctly'],
      'Configuration-Modify': ['Access admin panel', 'Navigate to configuration section', 'Backup current configuration', 'Modify required settings', 'Validate changes', 'Save configuration', 'Restart service if needed'],
      'Network-Configure':    ['Access router admin panel', 'Navigate to network settings', 'Enter IP configuration', 'Set DNS servers', 'Configure subnet mask', 'Save and apply settings', 'Test connectivity'],
      'Maintenance-Clean':    ['Power off device', 'Disconnect all cables', 'Use compressed air for dust removal', 'Clean thermal paste if needed', 'Reconnect components', 'Power on and verify temperatures']
    };
    const key = `${action.category}-${action.type}`;
    return defaults[key] || ['Review action requirements', 'Prepare necessary tools and resources', 'Execute the planned action carefully', 'Verify completion and test results', 'Document any issues encountered'];
  }

  get executionProgress(): number {
    if (!this.state.actions.length) return 0;
    return Math.round((this.state.actions.filter(a => a.status === 'Done').length / this.state.actions.length) * 100);
  }

  // ── Validation Step ─────────────────────────────────────────────────────────

  buildValidationChecklist(): void {
    if (this.state.validationChecklist.length > 0) return;

    const items: ValidationItem[] = [];
    const eqType = (this.equipment?.type || '').toLowerCase();

    // Truly Universal checks (Applicable to all: Hardware, Software, Network)
    items.push({ label: 'All planned maintenance actions completed', checked: false, autoGenerated: true });
    items.push({ label: 'System stability and performance verified', checked: false, autoGenerated: true });
    items.push({ label: 'No warning lights or error codes present', checked: false, autoGenerated: true });
    items.push({ label: 'Cleanliness and cable management inspected', checked: false, autoGenerated: true });

    if (eqType.includes('laptop') || eqType.includes('computer')) {
      items.push({ label: 'Operating system loads correctly', checked: false, autoGenerated: true });
      items.push({ label: 'Display/Screen shows no artifacts', checked: false, autoGenerated: true });
      items.push({ label: 'Keyboard and Touchpad functional', checked: false, autoGenerated: true });
      items.push({ label: 'Battery charging status verified', checked: false, autoGenerated: true });
    } else if (eqType.includes('router') || eqType.includes('switch') || eqType.includes('network')) {
      items.push({ label: 'Management console accessible', checked: false, autoGenerated: true });
      items.push({ label: 'All LAN/WAN ports link up', checked: false, autoGenerated: true });
      items.push({ label: 'DHCP/Routing services functional', checked: false, autoGenerated: true });
      items.push({ label: 'Firmware version verified', checked: false, autoGenerated: true });
    } else if (eqType.includes('server')) {
      items.push({ label: 'RAID status optimal', checked: false, autoGenerated: true });
      items.push({ label: 'Remote management (IPMI/iDRAC) accessible', checked: false, autoGenerated: true });
      items.push({ label: 'Service/Daemons running correctly', checked: false, autoGenerated: true });
      items.push({ label: 'Network throughput verified', checked: false, autoGenerated: true });
    } else {
      items.push({ label: 'Primary functionality verified', checked: false, autoGenerated: true });
    }

    this.state.validationChecklist = items;
    this.saveState();
  }

  toggleValidation(item: ValidationItem): void {
    item.checked = !item.checked;
    this.saveState();
  }

  addValidationItem(): void {
    if (!this.newCheckLabel.trim()) return;
    this.state.validationChecklist.push({
      label: this.newCheckLabel.trim(),
      checked: false,
      autoGenerated: false
    });
    this.newCheckLabel = '';
    this.saveState();
  }

  removeValidationItem(index: number): void {
    this.state.validationChecklist.splice(index, 1);
    this.saveState();
  }

  get validationProgress(): { checked: number; total: number; allDone: boolean } {
    const total = this.state.validationChecklist.length;
    const checked = this.state.validationChecklist.filter(v => v.checked).length;
    return { checked, total, allDone: total > 0 && checked === total };
  }

  // ── Summary Step ─────────────────────────────────────────────────────────────

  get completedActions(): WorkbenchAction[] {
    return this.state.actions.filter(a => a.status === 'Done');
  }

  get changesByCategory(): Record<string, string[]> {
    return this.completedActions.reduce((acc, action) => {
      if (!acc[action.category]) acc[action.category] = [];
      acc[action.category].push(`${action.type} ${action.target}`);
      return acc;
    }, {} as Record<string, string[]>);
  }

  get changesByCategoryKeys(): string[] {
    return Object.keys(this.changesByCategory);
  }

  // ── Complete / Cancel ────────────────────────────────────────────────────────

  confirmComplete(): void {
    const parts = this.state.actions
      .flatMap(a => a.resources.filter(r => r.resourceType === 'part'))
      .map(r => ({ name: r.name, qty: r.quantity || 1, specification: r.specification }));

    const tasks = this.state.actions.map(a => ({
      label: `[${a.category.toUpperCase()}] ${a.type} ${a.target}`,
      status: a.status,
      notes: a.notes
    }));

    this.pushTimeline('Maintenance completed ✓', '#10b981', 'check-circle');
    this.saveState();

    this.complete.emit({
      workNote: this.state.summaryNote,
      repairTasks: tasks,
      partsUsed: parts
    });
    this.showCompleteDialog = false;
    this.clearState();
  }

  confirmCancel(): void {
    this.showCancelDialog = false;
    this.clearState();
    this.cancel.emit();
  }

  closeAlert(event?: Event): void {
    if (event) {
      event.preventDefault();
      event.stopPropagation();
    }
    this.customAlert = null;
  }

  // ── Timeline ────────────────────────────────────────────────────────────────

  pushTimeline(title: string, color: string, icon: string = 'info'): void {
    // Unique Phase Transition Logic: If we are adding a step transition, remove previous instances of it
    // to keep the timeline concise as requested.
    if (title.includes('Started') || title.includes('Completed') || title.includes('Returned to')) {
      this.state.timeline = this.state.timeline.filter(t => t.title !== title);
    }

    this.state.timeline.push({ title, color, icon, time: this.formatDateTime(new Date()) });
    this.saveState();
  }

  cleanupTimeline(): void {
    // One-time cleanup for existing duplicates in the whole array
    const seen = new Set<string>();
    const unique: WorkbenchTimelineEntry[] = [];
    
    // We iterate backwards to keep the latest instance of each event
    for (let i = this.state.timeline.length - 1; i >= 0; i--) {
      const entry = this.state.timeline[i];
      if (!seen.has(entry.title)) {
        unique.unshift(entry);
        seen.add(entry.title);
      }
    }
    this.state.timeline = unique;
    this.saveState();
  }

  formatDateTime(date: Date): string {
    return date.toLocaleDateString('fr-FR') + ' ' + date.toLocaleTimeString('fr-FR');
  }

  // ── Persistence ──────────────────────────────────────────────────────────────

  saveState(): void {
    if (!this.state.ticketId) return;
    localStorage.setItem(`wb_v3_${this.state.ticketId}`, JSON.stringify(this.state));
  }

  loadState(): void {
    if (!this.ticket?.id) return;
    const saved = localStorage.getItem(`wb_v3_${this.ticket.id}`);
    if (saved) {
      try { this.state = { ...this.state, ...JSON.parse(saved) }; } catch {}
    }
  }

  clearState(): void {
    localStorage.removeItem(`wb_v3_${this.state.ticketId}`);
  }

  // ── Helpers ──────────────────────────────────────────────────────────────────

  getCategoryColor(cat: string): string {
    return this.engine.getCategoryColor(cat);
  }

  getEquipmentName(): string {
    return this.equipment?.equipmentName || this.equipment?.name || 'Unknown Equipment';
  }

  getEquipmentType(): string {
    return this.equipment?.type || this.equipment?.category || '–';
  }

  trackById(_: number, item: WorkbenchAction): string { return item.id; }
}
