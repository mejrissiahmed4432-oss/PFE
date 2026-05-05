
import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AiService } from '../../ai-assistant/ai.service';
import { ApplicationService } from '../../application-management/application.service';
import { Application } from '../../application-management/application.model';
import { OsService } from '../../os-management/os.service';
import { OperatingSystem } from '../../os-management/os.model';

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

  showNoResourcesWarning = false;

  customAlert: { title: string; message: string } | null = null;
  newCheckLabel = '';

  // Resource Selection
  selectedActionId: string | null = null;


  // AI Assistant State
  activeAiTab: 'diagnosis' | 'predictive' | null = null;
  aiLoading = false;
  aiDiagnosisCauses: string[] = [];
  aiDiagnosisChecks: string[] = [];
  aiPredictiveMap: any[] = [];
  aiSimilarCase: any = null;

  // AI Plan State
  lastGeneratedDiagnosis: string | null = null;
  aiPlanLoading = false;

  // AI Resource Matcher State
  aiResourceLoading = false;
  aiResourceMatched = false;

  // Resource Data
  availableOs: OperatingSystem[] = [];
  availableApps: Application[] = [];

  // Inventory filters for resources step
  inventorySearch = '';
  cachedInventory: any[] = [];

  // Timers
  elapsedTimeDisplay = '00:00:00';
  timerInterval: any;
  liveActionTimers: Record<string, string> = {};

  constructor(
    private engine: WorkbenchEngineService,
    private aiService: AiService,
    private appService: ApplicationService,
    private osService: OsService
  ) { }

  ngOnInit(): void {
    this.state.ticketId = this.ticket.id || '';
    if (!this.state.actionStartTimes) {
      this.state.actionStartTimes = {};
    }
    this.loadState();

    // Start Live Timer
    this.startLiveTimer();

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

      }
    });

    // Fetch OS and Apps for Resource selection
    this.osService.getAllOperatingSystems().subscribe(os => this.availableOs = os);
    this.appService.getAllApplications().subscribe(apps => this.availableApps = apps);
    this.refreshInventory();
  }

  ngOnDestroy(): void {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  // ── Timers & Mini Stats ───────────────────────────────────────────────────

  startLiveTimer(): void {
    this.updateTimers();
    this.timerInterval = setInterval(() => {
      this.updateTimers();
    }, 1000);
  }

  updateTimers(): void {
    if (!this.state.startedAt) return;
    const start = new Date(this.state.startedAt).getTime();
    const now = Date.now();
    const diff = Math.max(0, Math.floor((now - start) / 1000));
    this.elapsedTimeDisplay = this.formatSeconds(diff);

    // Update individual action timers
    if (this.state.actionStartTimes) {
      Object.keys(this.state.actionStartTimes).forEach(actionId => {
        const action = this.state.actions.find(a => a.id === actionId);
        if (action) {
          const actionStart = new Date(this.state.actionStartTimes![actionId]).getTime();
          const activeDiff = Math.max(0, Math.floor((now - actionStart) / 1000));
          const totalTime = (action.timeSpent || 0) + activeDiff;
          this.liveActionTimers[actionId] = this.formatSeconds(totalTime);
        }
      });
    }
  }

  formatSeconds(seconds: number): string {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  }

  get ticketPriorityLabel(): string {
    return this.ticket.priority || 'Medium';
  }

  get totalResourcesAssigned(): number {
    let count = 0;
    this.state.actions.forEach(a => {
      if (a.resources) count += a.resources.length;
    });
    return count;

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


    if (this.state.currentStep === 'resources' && !this.showNoResourcesWarning) {
      const anyResources = this.state.actions.some(a => a.resources && a.resources.length > 0);
      if (!anyResources) {
        this.showNoResourcesWarning = true;
        return;
      }
    }

    this.actuallyProceedNext();
  }

  confirmProceedWithoutResources(): void {
    this.showNoResourcesWarning = false;
    this.actuallyProceedNext();
  }

  private actuallyProceedNext(): void {
    const idx = this.stepIndex;
    if (idx < this.steps.length - 1) {

      this.animDir = 'right';
      this.animating = true;
      setTimeout(() => {
        this.state.currentStep = this.steps[idx + 1].key;

        // Auto-select first action if moving to Resources step
        if (this.state.currentStep === 'resources' && this.state.actions.length > 0) {
          this.selectedActionId = this.state.actions[0].id;

          this.refreshInventory();
        }
        // Removed noisy timeline step transitions

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

          this.refreshInventory();
        }
        // Removed noisy return log

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

        this.refreshInventory();
      }
      // Removed noisy return log

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


  // ── AI Assistant Methods ───────────────────────────────────────────────────

  copyAiToDiagnosis() {
    if (this.aiDiagnosisCauses.length > 0) {
      const causesText = this.aiDiagnosisCauses.map(c => `- ${c}`).join('\n');
      const checksText = this.aiDiagnosisChecks.map(c => `- ${c}`).join('\n');
      this.state.diagnosisResult = `Likely Causes:\n${causesText}\n\nSuggested Checks:\n${checksText}`;
      this.saveState();
    }
  }

  generateDiagnosis() {
    this.activeAiTab = 'diagnosis';
    this.aiLoading = true;
    const prompt = `Analyze this problem description and provide possible causes and suggested checks: ${this.ticket.description}. Format exactly as follows: \nCauses:\n- cause 1\n- cause 2\nChecks:\n- check 1\n- check 2`;

    this.aiService.query(prompt).subscribe({
      next: (res) => {
        this.aiLoading = false;
        const text = res.answer || '';
        const causesPart = text.split(/Checks:/i)[0] || '';
        const checksPart = text.split(/Checks:/i)[1] || '';

        this.aiDiagnosisCauses = (causesPart.match(/- (.*)/g) || []).map(s => s.substring(2).trim());
        this.aiDiagnosisChecks = (checksPart.match(/- (.*)/g) || []).map(s => s.substring(2).trim());

        if (this.aiDiagnosisCauses.length === 0 && text.length > 0) {
          this.aiDiagnosisCauses = [text.substring(0, 100) + '...'];
          this.aiDiagnosisChecks = ["Check system logs", "Perform hardware diagnostic"];
        }

        this.pushTimeline('AI Diagnosis Generated', '#0ea5e9', 'search');
      },
      error: () => {
        this.aiLoading = false;
        this.aiDiagnosisCauses = ['Could not analyze problem at this time.'];
        this.aiDiagnosisChecks = [];
      }
    });
  }

  runPredictiveAnalysis() {
    this.activeAiTab = 'predictive';
    this.aiLoading = true;
    const prompt = `Analyze this problem description: ${this.ticket.description}. Provide a predictive failure map with 2-3 likely failures, their risk level (High/Medium/Low), probability percentage, and a recommended action. Also provide one similar past case. Use this format:
Failures:
- Cause: [name] | Risk: [High/Medium/Low] | Percentage: [number] | Action: [action]
Similar Case:
- ID: [CASE-1234] | Match: [number]% | Problem: [desc] | Solution: [desc] | Time: [time] | Success: [number]%`;

    this.aiService.query(prompt).subscribe({
      next: (res) => {
        this.aiLoading = false;
        const text = res.answer || '';

        this.aiPredictiveMap = [];
        const failuresMatch = text.match(/- Cause:(.*)/g);
        if (failuresMatch) {
          failuresMatch.forEach(f => {
            const parts = f.split('|');
            if (parts.length >= 4) {
              this.aiPredictiveMap.push({
                cause: parts[0].replace(/- Cause:/i, '').trim(),
                risk: parts[1].replace(/Risk:/i, '').trim().toUpperCase() || 'MEDIUM',
                percentage: parseInt(parts[2].replace(/Percentage:/i, '').trim()) || 50,
                recommendation: parts[3].replace(/Action:/i, '').trim()
              });
            }
          });
        } else {
          this.aiPredictiveMap = [
            { cause: 'Insufficient RAM capacity', risk: 'MEDIUM', percentage: 62, recommendation: 'Run memory diagnostic test' },
            { cause: 'Storage drive degradation or failure', risk: 'HIGH', percentage: 28, recommendation: 'Check drive SMART status' }
          ];
        }

        const simCaseMatch = text.match(/Similar Case:[\s\S]*- ID:(.*)\| Match:(.*)\| Problem:(.*)\| Solution:(.*)\| Time:(.*)\| Success:(.*)/i);
        if (simCaseMatch) {
          this.aiSimilarCase = {
            id: simCaseMatch[1].trim(),
            matchPercentage: parseInt(simCaseMatch[2].trim()) || 80,
            problem: simCaseMatch[3].trim(),
            solution: simCaseMatch[4].trim(),
            timeToRepair: simCaseMatch[5].trim(),
            successRate: parseInt(simCaseMatch[6].trim()) || 90
          };
        } else {
          this.aiSimilarCase = {
            id: 'CASE-2024-0892', matchPercentage: 94, problem: 'Laptop running slow, frequent freezing',
            solution: 'RAM upgrade from 8GB to 16GB + OS update', timeToRepair: '45 min', successRate: 92
          };
        }
      },
      error: () => {
        this.aiLoading = false;
        this.aiPredictiveMap = [{ cause: 'Analysis failed', risk: 'LOW', percentage: 10, recommendation: 'Check connection' }];
      }
    });
  }

  addCheckToNotes(check: string) {
    const current = this.state.globalNotes || '';
    this.state.globalNotes = current ? `${current}\n- Checked: ${check}` : `- Checked: ${check}`;
  }

  // ── Plan Step ───────────────────────────────────────────────────────────────

  generateAIPlan() {
    this.aiPlanLoading = true;
    const prompt = `Based on this problem: ${this.ticket.description} and this diagnosis: ${this.state.diagnosisResult || 'No diagnosis'}, generate a step-by-step maintenance action plan. Format each step exactly as: \n- Action: [Type] | Target: [Target] | Desc: [Description] | Category: [Hardware/Software/Maintenance]`;

    this.aiService.query(prompt).subscribe({
      next: (res) => {
        this.aiPlanLoading = false;
        this.lastGeneratedDiagnosis = this.state.diagnosisResult;

        const text = res.answer || '';
        const stepsMatch = text.match(/- Action:(.*)/g);

        if (stepsMatch) {
          stepsMatch.forEach(step => {
            const parts = step.split('|');
            if (parts.length >= 4) {
              const type = parts[0].replace(/- Action:/i, '').trim();
              const target = parts[1].replace(/Target:/i, '').trim();
              const desc = parts[2].replace(/Desc:/i, '').trim();
              const catRaw = parts[3].replace(/Category:/i, '').trim();

              let category = ['Hardware', 'Software', 'Maintenance', 'Network', 'Configuration', 'Storage', 'Power', 'Firmware', 'Peripheral', 'Performance'].includes(catRaw) ? (catRaw as ActionCategory) : 'Maintenance';

              // Ensure 'type' is one of the valid types for the category, otherwise fallback to first valid type
              const validTypes = ACTION_TYPES_BY_CATEGORY[category] || [];
              let finalType = validTypes.find(t => t.toLowerCase() === type.toLowerCase());
              if (!finalType) {
                finalType = validTypes[0] || 'Inspect';
              }

              // Deduplication
              const exists = this.state.actions.some(a => a.type.toLowerCase() === finalType!.toLowerCase() && a.target.toLowerCase() === target.toLowerCase());

              if (!exists && target !== 'None' && target !== '') {
                const newAction: WorkbenchAction = {
                  id: Date.now().toString() + Math.random().toString(36).substr(2, 5),
                  category,
                  type: finalType,
                  target,
                  description: desc !== 'None' ? desc : 'Perform ' + finalType + ' on ' + target,
                  status: 'Planned',
                  notes: '',
                  resources: [],
                  expandedInExecution: false
                };
                this.state.actions.push(newAction);
              }
            }
          });
          this.saveState();
        } else {
          // Fallback dummy action if format fails
          const exists = this.state.actions.some(a => a.target === 'System Diagnostics');
          if (!exists) {
            this.state.actions.push({
              id: Date.now().toString(),
              category: 'Maintenance',
              type: ACTION_TYPES_BY_CATEGORY['Maintenance'][0] || 'Inspect',
              target: 'System Diagnostics',
              description: 'Run standard system diagnostics based on AI recommendation.',
              status: 'Planned',
              notes: '',
              resources: [],
              expandedInExecution: false
            });
            this.saveState();
          }
        }
      },
      error: () => {
        this.aiPlanLoading = false;
      }
    });
  }


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

      expandedInExecution: false,
      priority: 'Medium',
      estimatedTime: '30m'
    };
    this.state.actions.push(newAction);
    this.aiResourceMatched = false;
    this.pushTimeline(`Action Added: ${newAction.type}`, '#6366f1', 'edit');

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

    this.aiResourceMatched = false;

    this.saveState();
  }

  removeAction(id: string): void {
    this.state.actions = this.state.actions.filter(a => a.id !== id);

    // Unblock the AI Generator button if an action is removed
    this.lastGeneratedDiagnosis = null;
    // Unblock AI Resource matcher if actions change
    this.aiResourceMatched = false;
    this.saveState();
  }

  // ── Resource UI Selection ───────────────────────────────────────────────────

  // key = part unique key, value = qty being entered before confirming
  pendingPartAdd: Record<string, number | null> = {};

  getPartKey(part: any): string {
    if (!part) return '';
    return `${part.name}||${part.specification || ''}`;
  }

  openPartQtyPicker(part: any): void {
    this.pendingPartAdd[this.getPartKey(part)] = 1;
  }

  cancelPartQtyPicker(part: any): void {
    delete this.pendingPartAdd[this.getPartKey(part)];
  }

  confirmPartAdd(part: any): void {
    const key = this.getPartKey(part);
    const qty = this.pendingPartAdd[key];
    const available = part.totalQty; // already deducted in filteredInventory

    if (qty === null || qty === undefined || isNaN(Number(qty))) {
      alert('Please enter a valid quantity.');
      return;
    }
    if (Number(qty) <= 0) {
      alert('Quantity must be at least 1.');
      return;
    }
    if (Number(qty) > available) {
      alert(`Only ${available} unit(s) available in stock. You cannot assign more than that.`);
      return;
    }

    this.addResourceToAction('part', part.name, Number(qty), part.specification);
    delete this.pendingPartAdd[key];
  }

  addResourceToAction(resourceType: 'part' | 'software' | 'config' | 'network' | 'firmware' | 'service', name: string, quantity: number = 1, specification?: string) {
    const action = this.selectedAction;
    if (!action) return;
    if (!action.resources) action.resources = [];

    const existing = action.resources.find(r =>
      r.name === name &&
      r.resourceType === resourceType &&
      r.specification === specification
    );

    if (existing) {
      existing.quantity = (existing.quantity || 1) + quantity;
    } else {
      action.resources.push({ resourceType, name, quantity, specification });
    }
    this.aiResourceMatched = false;
    this.saveState();
    this.refreshInventory();
  }

  removeResourceFromAction(index: number) {
    const action = this.selectedAction;
    if (!action || !action.resources) return;
    action.resources.splice(index, 1);
    this.aiResourceMatched = false;
    this.saveState();
    this.refreshInventory();
  }

  // ── Resource Matching (AI) ────────────────────────────────────────────────

  matchResources() {
    this.aiResourceLoading = true;

    // Use previously fetched apps and os if available
    const availableAppsStr = this.availableApps.map(a => `${a.name} (OS: ${a.supportedOs})`).join(', ');
    const availableOsStr = this.availableOs.map(o => `${o.name} ${o.version}`).join(', ');
    const availableHardwareStr = this.userInventory.map(h => `${h.name} (Stock: ${h.totalQty})`).join(', ');

    const actionsStr = this.state.actions.map(a => `ID: ${a.id} | Action: ${a.type} ${a.target} | Desc: ${a.description} | Category: ${a.category}`).join('\n');

    const prompt = `Analyze these planned maintenance actions:
${actionsStr}

Equipment Context:
- OS/Specs: ${JSON.stringify(this.equipment?.specifications || {})}
- Available Applications: ${availableAppsStr || 'None listed'}
- Available Operating Systems: ${availableOsStr || 'None listed'}
- Available Hardware Inventory: ${availableHardwareStr || 'None listed'}

For EACH action ID, identify the exact resource needed. 
- If it is a software action, YOU MUST pick exactly from the "Available Applications" or "Available Operating Systems" lists if it matches. 
- If it is a hardware action, YOU MUST pick exactly from the "Available Hardware Inventory" if it matches.
- If the required item is NOT available in any list, return "None" for resource and explain in the note.
- If no resource is needed, explain what to do instead in the note.

You MUST respond strictly with a valid JSON array of objects. Do not include any other text or markdown formatting.
Format:
[
  { "id": "action_id", "resource": "Name of part/software or 'None'", "note": "Explanation" }
]`;

    this.aiService.query(prompt).subscribe({
      next: (res) => {
        this.aiResourceLoading = false;
        this.aiResourceMatched = true;

        try {
          // Attempt to parse JSON safely, ignoring markdown backticks if the AI accidentally added them
          let jsonStr = res.answer || '[]';
          jsonStr = jsonStr.replace(/^```json/i, '').replace(/^```/i, '').replace(/```$/i, '').trim();

          const parsed = JSON.parse(jsonStr);

          if (Array.isArray(parsed)) {
            parsed.forEach((item: any) => {
              const id = item.id;
              const resourceName = item.resource;
              const note = item.note;

              const action = this.state.actions.find(a => a.id === id);
              if (action) {
                action.notes = `AI Note: ${note}`;
                if (resourceName && resourceName !== 'None' && resourceName.toLowerCase() !== 'none') {
                  if (!action.resources) action.resources = [];
                  const resExists = action.resources.some(r => r.name === resourceName);
                  if (!resExists) {
                    action.resources.push({
                      resourceType: action.category === 'Software' ? 'software' : 'part',
                      name: resourceName,
                      quantity: 1
                    });
                  }
                }
              }
            });
            this.saveState();
          }
        } catch (e) {
          console.error("Failed to parse AI Resource matching response:", e);
        }
      },
      error: () => {
        this.aiResourceLoading = false;
      }
    });
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

    this.refreshInventory();

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


  getAssignedQuantity(partName: string, specification?: string): number {
    let used = 0;
    for (const a of this.state.actions) {
      if (a.resources) {
        for (const r of a.resources) {
          if (r.resourceType === 'part' && r.name === partName && r.specification === specification) {
            used += (r.quantity || 1);
          }
        }
      }
    }
    return used;
  }

  refreshInventory(): void {
    // Merge items with same name + specification
    const merged: Record<string, any> = {};
    for (const p of this.userInventory) {
      const key = `${p.name}||${p.specification || ''}`;
      if (merged[key]) {
        merged[key].totalQty += p.totalQty;
      } else {
        merged[key] = { name: p.name, specification: p.specification, category: p.category, type: p.type, brand: p.brand, location: p.location, totalQty: p.totalQty };
      }
    }

    let result = Object.values(merged)
      .map(p => ({
        ...p,
        totalQty: p.totalQty - this.getAssignedQuantity(p.name, p.specification)
      }))
      .filter(p => p.totalQty > 0);

    if (this.inventorySearch) {
      const q = this.inventorySearch.toLowerCase();
      result = result.filter(p => p.name?.toLowerCase().includes(q) || p.specification?.toLowerCase().includes(q));
      this.cachedInventory = result;
      return;
    }

    // Auto-filter based on the action's target if no manual search is active
    if (this.selectedAction && this.selectedAction.target) {
      const targetStr = this.selectedAction.target.toLowerCase();
      const targetWords = targetStr.split(' ').filter((w: string) => w.length >= 3);

      if (targetStr.includes('hard drive') || targetStr.includes('storage') || targetStr.includes('disk')) {
        targetWords.push('hdd', 'ssd', 'nvme', 'drive');
      }
      if (targetStr.includes('ram') || targetStr.includes('memory')) {
        targetWords.push('ram', 'ddr', 'memory', 'dimm');
      }
      if (targetStr.includes('screen') || targetStr.includes('display')) {
        targetWords.push('monitor', 'lcd', 'panel', 'display');
      }
      if (targetStr.includes('board') || targetStr.includes('mother')) {
        targetWords.push('motherboard', 'mainboard', 'pcb');
      }

      if (targetWords.length > 0) {
        result = result.filter((p: any) => {
          const nameSpec = ((p.name || '') + ' ' + (p.specification || '') + ' ' + (p.category || '')).toLowerCase();
          return targetWords.some((w: string) => nameSpec.includes(w));
        });
      }
    }

    this.cachedInventory = result;
  }

  onInventorySearchChange(): void {
    this.refreshInventory();
  }

  get filteredInventory(): any[] {
    return this.cachedInventory;

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

    this.refreshInventory();

  }

  // ── Execution Step ──────────────────────────────────────────────────────────

  toggleExecutionExpand(action: WorkbenchAction): void {
    action.expandedInExecution = !action.expandedInExecution;
  }

  setActionStatus(action: WorkbenchAction, status: ActionStatus): void {
    action.status = status;

    if (status === 'Done') {
      this.stopActionTimer(action);
      this.pushTimeline(`Completed: ${action.type} ${action.target}`, '#10b981', 'check-circle');
    }
    this.saveState();
  }

  toggleActionTimer(action: WorkbenchAction): void {
    if (this.isActionTimerRunning(action.id)) {
      this.stopActionTimer(action);
    } else {
      this.startActionTimer(action.id);
    }
  }

  startActionTimer(id: string): void {
    if (!this.state.actionStartTimes) this.state.actionStartTimes = {};
    this.state.actionStartTimes[id] = new Date().toISOString();
    this.saveState();
    this.updateTimers();
  }

  stopActionTimer(action: WorkbenchAction): void {
    if (this.state.actionStartTimes && this.state.actionStartTimes[action.id]) {
      const start = new Date(this.state.actionStartTimes[action.id]).getTime();
      const diff = Math.max(0, Math.floor((Date.now() - start) / 1000));
      action.timeSpent = (action.timeSpent || 0) + diff;
      delete this.state.actionStartTimes[action.id];
      this.liveActionTimers[action.id] = this.formatSeconds(action.timeSpent);
      this.saveState();
    }
  }

  isActionTimerRunning(id: string): boolean {
    return !!(this.state.actionStartTimes && this.state.actionStartTimes[id]);
  }

  getActionTimerDisplay(action: WorkbenchAction): string {
    if (this.isActionTimerRunning(action.id)) {
      return this.liveActionTimers[action.id] || '00:00:00';
    }
    return this.formatSeconds(action.timeSpent || 0);
  }


  getInstructions(action: WorkbenchAction): string[] {
    const defaults: Record<string, string[]> = {
      'Hardware-Install': ['Power off device completely', 'Disconnect power cables', 'Open device case/panel', 'Install the component carefully', 'Close case and reconnect power', 'Boot system and verify functionality'],
      'Hardware-Replace': ['Backup all data from device', 'Power off device', 'Remove old component', 'Install new component', 'Reconnect all cables', 'Power on and verify'],
      'Software-Install': ['Download installer package', 'Verify system requirements', 'Run installer as administrator', 'Follow installation wizard', 'Configure initial settings', 'Restart if required', 'Verify installation success'],
      'Software-Update': ['Back up current configuration', 'Download update package', 'Stop affected services', 'Apply update', 'Restart services', 'Verify update applied correctly'],
      'Configuration-Modify': ['Access admin panel', 'Navigate to configuration section', 'Backup current configuration', 'Modify required settings', 'Validate changes', 'Save configuration', 'Restart service if needed'],
      'Network-Configure': ['Access router admin panel', 'Navigate to network settings', 'Enter IP configuration', 'Set DNS servers', 'Configure subnet mask', 'Save and apply settings', 'Test connectivity'],
      'Maintenance-Clean': ['Power off device', 'Disconnect all cables', 'Use compressed air for dust removal', 'Clean thermal paste if needed', 'Reconnect components', 'Power on and verify temperatures']
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


  get validationProgress(): { checked: number; total: number; allDone: boolean; percentage: number } {
    const total = this.state.validationChecklist.length;
    const checked = this.state.validationChecklist.filter(v => v.checked).length;
    const percentage = total === 0 ? 0 : Math.round((checked / total) * 100);
    return { checked, total, allDone: total > 0 && checked === total, percentage };
  }

  get validationRingOffset(): number {
    const circumference = 2 * Math.PI * 34; // r=34 from SVG
    const pct = this.validationProgress.percentage;
    return circumference - (pct / 100) * circumference;

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


  exportReport(): void {
    const lines = [
      `MAINTENANCE REPORT`,
      `===================`,
      `Ticket ID: ${this.ticket.id || 'N/A'}`,
      `Device: ${this.getEquipmentName()} (${this.getEquipmentType()})`,
      `Date: ${this.formatDateTime(new Date())}`,
      `Total Elapsed Time: ${this.elapsedTimeDisplay}`,
      ``,
      `-- Original Problem --`,
      `${this.ticket.description}`,
      ``,
      `-- Diagnosis Result --`,
      `${this.state.diagnosisResult}`,
      ``,
      `-- Actions Performed --`
    ];

    this.completedActions.forEach(a => {
      lines.push(`[${a.category}] ${a.type} ${a.target}`);
      lines.push(`  Description: ${a.description}`);
      if (a.timeSpent) lines.push(`  Time spent: ${this.formatSeconds(a.timeSpent)}`);
      if (a.resources && a.resources.length > 0) {
        lines.push(`  Resources used: ${a.resources.map(r => r.name).join(', ')}`);
      }
    });

    lines.push(``);
    lines.push(`-- Final Technician Report --`);
    lines.push(this.state.summaryNote || 'No final notes provided.');

    const blob = new Blob([lines.join('\n')], { type: 'text/plain' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `Report_${this.ticket.id || 'Workbench'}.txt`;
    a.click();
    window.URL.revokeObjectURL(url);
  }


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

      try {
        this.state = { ...this.state, ...JSON.parse(saved) };
        // Remove noisy logs from previous sessions
        if (this.state.timeline) {
          this.state.timeline = this.state.timeline.filter(t =>
            !t.title.includes('Phase Started') &&
            !t.title.includes('Returned to') &&
            (!t.title.includes('Completed') || t.title.includes('Maintenance completed ✓'))
          );
        }
      } catch { }

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
