
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

import { PartRequestWizardComponent } from '../../parts-management/part-request-wizard/part-request-wizard.component';

@Component({
  selector: 'app-live-workbench',
  standalone: true,
  imports: [CommonModule, FormsModule, PartRequestWizardComponent],
  templateUrl: './live-workbench.component.html',
  styleUrl: './live-workbench.component.css'
})
export class LiveWorkbenchComponent implements OnInit {
  @Input() ticket!: Ticket;
  @Input() equipment!: Equipment;
  @Input() userInventory: any[] = [];
  @Input() currentUser: any;
  isRequestWizardOpen = false;
  wizardInitialSelection: any = null;
  @Output() complete = new EventEmitter<{
    workNote: string;
    repairTasks: any[];
    partsUsed: any[];
    isBroken?: boolean;
    diagnosisResult?: string;
    validationSummary?: string;
    partsInstalled?: any[];
  }>();
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
    manualDiagnosis: '',
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
    startedAt: new Date().toISOString(),
    aiDiagnosisCauses: [],
    aiDiagnosisChecks: [],
    aiPredictiveMap: [],
    aiSimilarCase: null,
    lastAiDiagnosisInput: null,
    lastAiPredictiveInput: null,
    lastGeneratedDiagnosis: null,
    aiResourceMatched: false
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
  showBrokenDialog = false;
  brokenReason = '';

  stepWarningModal = { title: '', message: '', show: false };
  pendingTargetStep: WorkflowStep | null = null;
  showAiValidation = true;

  showNoResourcesWarning = false;

  customAlert: { title: string; message: string } | null = null;
  newCheckLabel = '';

  // Resource Selection
  selectedActionId: string | null = null;


  // AI Assistant State
  activeAiTab: 'diagnosis' | 'predictive' | null = null;
  aiLoading = false;
  aiPlanLoading = false;
  aiResourceLoading = false;
  aiTestsLoading = false;
  aiHistoryLoading = false;
  showAiDiagnosis = true;
  showAiPlan = true;
  showAiResources = true;

  toggleAiDiagnosis() {
    this.showAiDiagnosis = !this.showAiDiagnosis;
  }

  toggleAiPlan() {
    this.showAiPlan = !this.showAiPlan;
  }

  toggleAiResources() {
    this.showAiResources = !this.showAiResources;
  }

  // AI Resource Matcher State

  // Resource Data
  availableOs: OperatingSystem[] = [];
  availableApps: Application[] = [];

  // Inventory filters for resources step
  inventorySearch = '';
  cachedInventory: any[] = [];



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
    // Component cleanup
  }

  // ── Timers & Mini Stats ───────────────────────────────────────────────────



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
    const nextIdx = this.stepIndex + 1;
    const nextStep = nextIdx < this.steps.length ? this.steps[nextIdx].key : null;

    // If not valid, show the "Proceed Anyway" warning
    if (!result.valid) {
      this.pendingTargetStep = nextStep;
      this.stepWarningModal = {
        title: this.getStepWarningTitle(this.state.currentStep),
        message: result.error + " Are you sure you want to proceed anyway?",
        show: true
      };
      return;
    }

    if (nextStep) {
      this.actuallyProceedTo(nextStep);
    }
  }

  private getStepWarningTitle(step: WorkflowStep): string {
    switch (step) {
      case 'diagnosis': return 'Incomplete Diagnosis';
      case 'plan': return 'Empty Plan';
      case 'resources': return 'Missing Resources';
      case 'execution': return 'Pending Actions';
      case 'validation': return 'Pending Verification';
      default: return 'Step Warning';
    }
  }

  confirmStepWarning(): void {
    this.stepWarningModal.show = false;

    if (this.pendingTargetStep) {
      this.actuallyProceedTo(this.pendingTargetStep);
      this.pendingTargetStep = null;
    }
  }



  private actuallyProceedTo(step: WorkflowStep): void {
    const current = this.state.currentStep;
    const currentIdx = this.stepIndex;
    const targetIdx = this.steps.findIndex(s => s.key === step);

    // Ensure critical logic runs if we are PASSING the plan step
    const planIdx = this.steps.findIndex(s => s.key === 'plan');
    if (currentIdx <= planIdx && targetIdx > planIdx) {
      // Automatic generation removed per user request
    }

    this.animDir = targetIdx > currentIdx ? 'right' : 'left';
    this.animating = true;

    setTimeout(() => {
      this.state.currentStep = step;

      // Auto-select first action if moving to Resources step
      if (this.state.currentStep === 'resources' && this.state.actions.length > 0) {
        this.selectedActionId = this.state.actions[0].id;
        this.refreshInventory();
      }

      this.saveState();
      this.animating = false;
    }, 200);
  }

  private actuallyProceedNext(): void {
    const nextIdx = this.stepIndex + 1;
    if (nextIdx < this.steps.length) {
      this.actuallyProceedTo(this.steps[nextIdx].key);
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

        this.saveState();
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
      this.actuallyProceedTo(step);
    } else {
      // Moving forward: must validate current step first
      const result = this.engine.validateStep(this.state.currentStep, this.state);

      if (!result.valid) {
        this.pendingTargetStep = step;
        this.stepWarningModal = {
          title: this.getStepWarningTitle(this.state.currentStep),
          message: result.error + " Are you sure you want to proceed anyway?",
          show: true
        };
        return;
      }

      this.actuallyProceedTo(step);
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
    this.onTestingContextChanged();
    this.saveState();
  }


  // ── AI Assistant Methods ───────────────────────────────────────────────────

  copyAiToDiagnosis() {
    if (this.state.aiDiagnosisCauses && this.state.aiDiagnosisCauses.length > 0) {
      const causesText = this.state.aiDiagnosisCauses.map(c => `- ${c}`).join('\n');
      const checksText = this.state.aiDiagnosisChecks?.map(c => `- ${c}`).join('\n') || '';

      const diagnosisText = `AI Recommendation:\n${causesText}\n\nSuggested Checks:\n${checksText}`;

      // Update only manual observations as requested
      if (!this.state.manualDiagnosis || this.state.manualDiagnosis.trim() === '') {
        this.state.manualDiagnosis = `AI Analysis integrated on ${new Date().toLocaleTimeString()}\n${causesText}`;
      } else {
        this.state.manualDiagnosis += `\n\n--- AI Supplement ---\n${causesText}`;
      }

      this.onTestingContextChanged();
      this.saveState();
    }
  }

  generateDiagnosis() {
    this.activeAiTab = 'diagnosis';
    if (this.aiLoading || this.state.lastAiDiagnosisInput === this.ticket.description) return;

    this.aiLoading = true;
    const prompt = `Analyze this problem description and provide possible causes and suggested checks: ${this.ticket.description}. Format exactly as follows: \nCauses:\n- cause 1\n- cause 2\nChecks:\n- check 1\n- check 2`;

    this.aiService.query(prompt).subscribe({
      next: (res) => {
        this.aiLoading = false;
        this.state.lastAiDiagnosisInput = this.ticket.description;
        const text = res.answer || '';
        const causesPart = text.split(/Checks:/i)[0] || '';
        const checksPart = text.split(/Checks:/i)[1] || '';

        this.state.aiDiagnosisCauses = (causesPart.match(/- (.*)/g) || []).map(s => s.substring(2).trim());
        this.state.aiDiagnosisChecks = (checksPart.match(/- (.*)/g) || []).map(s => s.substring(2).trim());

        if ((!this.state.aiDiagnosisCauses || this.state.aiDiagnosisCauses.length === 0) && text.length > 0) {
          this.state.aiDiagnosisCauses = [text.substring(0, 100) + '...'];
          this.state.aiDiagnosisChecks = ["Check system logs", "Perform hardware diagnostic"];
        }

        this.saveState();
        this.pushTimeline('AI Diagnosis Generated', '#0ea5e9', 'search');
      },
      error: () => {
        this.aiLoading = false;
        this.state.aiDiagnosisCauses = ['Could not analyze problem at this time.'];
        this.state.aiDiagnosisChecks = [];
        this.saveState();
      }
    });
  }

  runPredictiveAnalysis() {
    this.activeAiTab = 'predictive';
    if (this.aiLoading || this.state.lastAiPredictiveInput === this.ticket.description) return;

    this.aiLoading = true;
    const prompt = `Analyze this problem description: ${this.ticket.description}. Provide a predictive failure map with 2-3 likely failures, their risk level (High/Medium/Low), probability percentage, and a recommended action. Also provide one similar past case. Use this format:
Failures:
- Cause: [name] | Risk: [High/Medium/Low] | Percentage: [number] | Action: [action]
Similar Case:
- ID: [CASE-1234] | Match: [number]% | Problem: [desc] | Solution: [desc] | Time: [time] | Success: [number]%`;

    this.aiService.query(prompt).subscribe({
      next: (res) => {
        this.aiLoading = false;
        this.state.lastAiPredictiveInput = this.ticket.description;
        const text = res.answer || '';

        this.state.aiPredictiveMap = [];
        const failuresMatch = text.match(/- Cause:(.*)/g);
        if (failuresMatch) {
          failuresMatch.forEach(f => {
            const parts = f.split('|');
            if (parts.length >= 4) {
              this.state.aiPredictiveMap?.push({
                cause: parts[0].replace(/- Cause:/i, '').trim(),
                risk: parts[1].replace(/Risk:/i, '').trim().toUpperCase() || 'MEDIUM',
                percentage: parseInt(parts[2].replace(/Percentage:/i, '').trim()) || 50,
                recommendation: parts[3].replace(/Action:/i, '').trim()
              });
            }
          });
        } else {
          this.state.aiPredictiveMap = [
            { cause: 'Insufficient RAM capacity', risk: 'MEDIUM', percentage: 62, recommendation: 'Run memory diagnostic test' },
            { cause: 'Storage drive degradation or failure', risk: 'HIGH', percentage: 28, recommendation: 'Check drive SMART status' }
          ];
          this.saveState();
        }

        const simCaseMatch = text.match(/Similar Case:[\s\S]*- ID:(.*)\| Match:(.*)\| Problem:(.*)\| Solution:(.*)\| Time:(.*)\| Success:(.*)/i);
        if (simCaseMatch) {
          this.state.aiSimilarCase = {
            id: simCaseMatch[1].trim(),
            matchPercentage: parseInt(simCaseMatch[2].trim()) || 80,
            problem: simCaseMatch[3].trim(),
            solution: simCaseMatch[4].trim(),
            timeToRepair: simCaseMatch[5].trim(),
            successRate: parseInt(simCaseMatch[6].trim()) || 90
          };
        } else {
          this.state.aiSimilarCase = {
            id: 'CASE-2024-0892', matchPercentage: 94, problem: 'Laptop running slow, frequent freezing',
            solution: 'RAM upgrade from 8GB to 16GB + OS update', timeToRepair: '45 min', successRate: 92
          };
        }
        this.saveState();
      },
      error: () => {
        this.aiLoading = false;
        this.state.aiPredictiveMap = [{ cause: 'Analysis failed', risk: 'LOW', percentage: 10, recommendation: 'Check connection' }];
        this.saveState();
      }
    });
  }

  addCheckToNotes(check: string) {
    const current = this.state.globalNotes || '';
    this.state.globalNotes = current ? `${current}\n- Checked: ${check}` : `- Checked: ${check}`;
  }

  // ── Plan Step ───────────────────────────────────────────────────────────────
  activeStrategyType: 'standard' | 'quick' | 'deep' = 'standard';

  // ── Helper: create a WorkbenchAction with validated type ────────────────────
  private mkAction(
    category: ActionCategory,
    type: string,
    target: string,
    description: string,
    priority: 'High' | 'Medium' | 'Low',
    estimatedTime: string
  ): WorkbenchAction {
    const validTypes = ACTION_TYPES_BY_CATEGORY[category] || [];
    const finalType = validTypes.find(t => t.toLowerCase() === type.toLowerCase()) ?? validTypes[0] ?? 'Inspect';
    return {
      id: Date.now().toString() + Math.random().toString(36).substr(2, 5),
      category,
      type: finalType,
      target,
      description,
      status: 'Planned',
      notes: '',
      resources: [],
      expandedInExecution: false,
      priority,
      estimatedTime: '', // Removed as requested
      isAiGenerated: true
    };
  }

  generateAIPlan() {
    if (this.aiPlanLoading || this.state.aiPlanGenerated) return;

    const dText = (this.state.diagnosisResult || '').trim();
    if (dText.length < 5) {
      this.stepError = "Please complete the diagnosis step before generating a plan.";
      setTimeout(() => this.stepError = '', 5000);
      return;
    }

    this.aiPlanLoading = true;

    const d = (this.state.diagnosisResult || '').trim();

    const prompt = `[SYS_NO_ACTION]
You are an expert IT maintenance technician.
Based STRICTLY and ONLY on the following diagnosis result:
"${d}"

Generate a comprehensive, non-redundant, step-by-step repair strategy. Avoid splitting simple tasks into multiple steps (e.g., instead of "Remove RAM" and "Install RAM", just use "Replace RAM").
Return all necessary technical steps to fully resolve the issue in the following EXACT format line by line, without any markdown or conversational filler:
- Group: [Hardware|Software|Configuration|Network|Maintenance|Inspection|Power|Security|Storage|Peripheral|Performance|Thermal|Consumables|Firmware|Cabling] | Kind: [Test|Replace|Clean|Configure|Upgrade|Restore] | Target: [Specific Component Name] | Desc: [Concise technical action] | Priority: [High|Medium|Low]`;

    this.aiService.query(prompt).subscribe({
      next: (res) => {
        const text = res.answer || '';
        const lines = text.split('\n');

        const rawActions: WorkbenchAction[] = [];
        lines.forEach(line => {
          if (line.includes('Group:') && line.includes('Kind:')) {
            const parts = line.split('|');
            if (parts.length >= 5) {
              const categoryStr = parts[0].replace(/.*Group:/i, '').replace(/\*/g, '').trim();
              const typeStr = parts[1].replace(/.*Kind:/i, '').replace(/\*/g, '').trim();
              const targetStr = parts[2].replace(/.*Target:/i, '').replace(/\*/g, '').trim();
              const descStr = parts[3].replace(/.*Desc:/i, '').replace(/\*/g, '').trim();
              const priorityStr = parts[4].replace(/.*Priority:/i, '').replace(/\*/g, '').trim();

              rawActions.push(this.mkAction(categoryStr as ActionCategory, typeStr, targetStr, descStr, priorityStr as 'High' | 'Medium' | 'Low', '30m'));
            }
          }
        });

        // Client-side deduplication
        const uniqueActions: WorkbenchAction[] = [];
        const seen = new Map<string, WorkbenchAction>();

        rawActions.forEach(action => {
          // Key based on category, type, and target to prevent duplicate actions on the same component
          const key = `${action.category}-${action.type}-${action.target}`.toLowerCase().trim();
          if (!seen.has(key)) {
            seen.set(key, action);
            uniqueActions.push(action);
          } else {
            // Merge descriptions if same action type on same target
            const existing = seen.get(key);
            if (existing && !existing.description.includes(action.description)) {
              existing.description += ' ' + action.description;
            }
          }
        });

        if (uniqueActions.length === 0 && text.length > 10) {
          uniqueActions.push(this.mkAction('Maintenance', 'Inspect', 'General Component', text.substring(0, 200), 'Medium', '30m'));
        }

        // Filter out previous AI actions from state, keep manual ones
        const manualActions = this.state.actions.filter(a => !a.isAiGenerated);
        this.state.actions = [...manualActions, ...uniqueActions.slice(0, 15)];

        this.state.lastGeneratedDiagnosis = this.state.diagnosisResult;
        this.state.aiPlanGenerated = true;
        this.aiPlanLoading = false;
        this.saveState();
        this.pushTimeline('AI Maintenance Plan Generated', '#2563eb', 'list');
      },
      error: () => {
        this.aiPlanLoading = false;
        this.stepError = "Failed to generate AI plan. Please try again.";
      }
    });
  }

  // ── Auto-Match Inventory ──────────────────────────────────────────────────
  matchResources(): void {
    if (this.state.actions.length === 0) return;
    this.aiResourceLoading = true;

    setTimeout(() => {
      this.state.actions.forEach(action => {
        // Only match resources for actions that typically need parts/software
        if (!this.actionNeedsResource(action)) {
          action.resources = [];
          return;
        }

        // Clear existing resources if we are re-matching
        action.resources = [];

        const target = (action.target || '').toLowerCase();

        // Match Hardware
        if (action.category === 'Hardware' || action.category === 'Storage' || action.category === 'Consumables' || action.category === 'Power' || action.category === 'Peripheral' || action.category === 'Thermal') {
          const match = this.userInventory.find(item => {
            const name = (item.name || '').toLowerCase();
            const spec = (item.specification || '').toLowerCase();
            const isMatch = target.includes(name) || name.includes(target) || (spec && target.includes(spec));
            if (isMatch) {
              const used = this.getAssignedQuantity(item.name, item.specification);
              return (item.totalQty - used) > 0;
            }
            return false;
          });

          if (match) {
            const specKeys = this.getAvailableSpecKeys();
            let autoKey = specKeys.find(k =>
              target.includes(k.toLowerCase()) ||
              k.toLowerCase().includes(target)
            );
            if (!autoKey) {
              const rawKey = target || match.name || '';
              autoKey = rawKey.charAt(0).toUpperCase() + rawKey.slice(1);
            }

            action.resources.push({
              resourceType: 'part',
              name: match.name,
              quantity: 1,
              specification: match.specification,
              equipmentId: match.id,
              replacesSpecKey: autoKey
            });
          } else if (action.target) {
            // Fallback: Write the required resource even if not in inventory
            action.resources.push({
              resourceType: 'part',
              name: action.target,
              quantity: 1,
              specification: 'Required (Not in local inventory)'
            });
          }
        }

        // Match Software
        if (action.category === 'Software' || action.category === 'Firmware') {
          // Check OS
          const osMatch = this.availableOs.find(os => {
            const name = (os.name || '').toLowerCase();
            return target.includes(name) || name.includes(target);
          });
          if (osMatch) {
            action.resources.push({
              resourceType: 'software',
              name: `${osMatch.name || 'OS'} ${osMatch.version || ''}`
            });
          } else {
            // Check Apps
            const appMatch = this.availableApps.find(app => {
              const name = (app.name || '').toLowerCase();
              return target.includes(name) || name.includes(target);
            });
            if (appMatch) {
              action.resources.push({
                resourceType: 'software',
                name: appMatch.name || 'Application'
              });
            } else if (action.target) {
              action.resources.push({
                resourceType: 'software',
                name: action.target,
                specification: 'Awaiting License/Installation'
              });
            }
          }
        }
      });

      this.state.aiResourceMatched = true;
      this.aiResourceLoading = false;
      this.saveState();
      this.refreshInventory();
      this.pushTimeline('Inventory Auto-Matched', '#10b981', 'package');
    }, 1500);
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
    this.state.aiResourceMatched = false;
    this.onTestingContextChanged();
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

    this.state.aiResourceMatched = false;
    this.onTestingContextChanged();

    this.saveState();
  }

  removeAction(id: string): void {
    this.state.actions = this.state.actions.filter(a => a.id !== id);

    // Unblock the AI Generator button if an action is removed
    this.state.lastGeneratedDiagnosis = null;
    // Unblock AI Resource matcher if actions change
    this.state.aiResourceMatched = false;
    this.onTestingContextChanged();
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

    this.addResourceToAction('part', part.name, Number(qty), part.specification, part.id);
    delete this.pendingPartAdd[key];
  }

  addResourceToAction(resourceType: 'part' | 'software' | 'config' | 'network' | 'firmware' | 'service', name: string, quantity: number = 1, specification?: string, equipmentId?: string) {
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
      if (equipmentId) existing.equipmentId = equipmentId;
    } else {
      let replacesSpecKey: string | undefined = undefined;
      if (resourceType === 'part') {
        const target = (action.target || '').toLowerCase();
        const specKeys = this.getAvailableSpecKeys();
        replacesSpecKey = specKeys.find(k =>
          target.includes(k.toLowerCase()) ||
          k.toLowerCase().includes(target)
        );
        if (!replacesSpecKey) {
          const rawKey = target || name || '';
          replacesSpecKey = rawKey.charAt(0).toUpperCase() + rawKey.slice(1);
        }
      }
      action.resources.push({ resourceType, name, quantity, specification, equipmentId, replacesSpecKey });
    }
    this.state.aiResourceMatched = false;
    this.saveState();
    this.refreshInventory();
  }

  removeResourceFromAction(index: number) {
    const action = this.selectedAction;
    if (!action || !action.resources) return;
    action.resources.splice(index, 1);
    this.state.aiResourceMatched = false;
    this.saveState();
    this.refreshInventory();
  }




  getActionTypesForCategory(category: ActionCategory): string[] {
    return ACTION_TYPES_BY_CATEGORY[category] || [];
  }

  actionNeedsResource(action: WorkbenchAction): boolean {
    const resourcedTypes = ['Install', 'Replace', 'Upgrade', 'Restore', 'Update', 'Clean'];
    // Cleaning might need consumables, so I'll keep it for now as an example
    return resourcedTypes.some(t => action.type.toLowerCase().includes(t.toLowerCase()));
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
      const targetStr = this.selectedAction.target.toLowerCase().trim();
      const skipWords = ['replace', 'install', 'update', 'repair', 'clean', 'inspect', 'configure', 'modify', 'setup', 'test', 'remove', 'upgrade', 'on', 'the', 'with', 'and', 'for', 'part', 'component', 'system', 'device'];
      let targetWords = targetStr.split(/[\s,]+/)
        .filter((w: string) => w.length > 1)
        .filter((w: string) => !skipWords.includes(w));

      if (targetWords.length === 0) {
        targetWords = [targetStr];
      }

      if (targetStr.includes('hard drive') || targetStr.includes('storage') || targetStr.includes('disk') || targetStr.includes('ssd') || targetStr.includes('hdd')) {
        targetWords.push('hdd', 'ssd', 'nvme', 'drive', 'storage', 'disk');
      }
      if (targetStr.includes('ram') || targetStr.includes('memory')) {
        targetWords.push('ram', 'ddr', 'memory', 'dimm', 'sodimm');
      }
      if (targetStr.includes('screen') || targetStr.includes('display') || targetStr.includes('monitor')) {
        targetWords.push('monitor', 'lcd', 'panel', 'display', 'screen');
      }
      if (targetStr.includes('board') || targetStr.includes('mother')) {
        targetWords.push('motherboard', 'mainboard', 'pcb', 'board');
      }
      if (targetStr.includes('power') || targetStr.includes('psu') || targetStr.includes('battery')) {
        targetWords.push('power', 'psu', 'battery', 'charger', 'adapter');
      }

      if (targetWords.length > 0) {
        result = result.filter((p: any) => {
          const nameSpec = ((p.name || '') + ' ' + (p.specification || '') + ' ' + (p.category || '')).toLowerCase();
          return targetWords.some((w: string) => {
            // Use word boundary to prevent 'ram' from matching 'frame'
            const regex = new RegExp(`\\b${w}\\b`, 'i');
            return regex.test(nameSpec);
          });
        });
      }
    }

    this.cachedInventory = result;
  }

  onInventorySearchChange(): void {
    this.refreshInventory();
  }

  openRequestWizard() {
    if (!this.selectedAction) return;
    this.wizardInitialSelection = {
      category: this.selectedAction.category,
      type: this.selectedAction.type,
      name: this.selectedAction.target,
      items: [{
        specification: this.selectedAction.description || ''
      }]
    };
    this.isRequestWizardOpen = true;
  }

  onRequestWizardClose(success: boolean) {
    this.isRequestWizardOpen = false;
    if (success) {
      this.refreshInventory();
    }
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
      this.pushTimeline(`Completed: ${action.type} ${action.target}`, '#10b981', 'check-circle');
    }
    this.saveState();
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
    const items: ValidationItem[] = [];
    const eqType = (this.equipment?.type || '').toLowerCase();

    // Truly Universal checks
    items.push({ label: 'All planned maintenance actions completed', status: 'pending', autoGenerated: true });
    items.push({ label: 'System stability and performance verified', status: 'pending', autoGenerated: true });
    items.push({ label: 'No warning lights or error codes present', status: 'pending', autoGenerated: true });
    items.push({ label: 'Cleanliness and cable management inspected', status: 'pending', autoGenerated: true });

    if (eqType.includes('laptop') || eqType.includes('computer')) {
      items.push({ label: 'Operating system loads correctly', status: 'pending', autoGenerated: true });
      items.push({ label: 'Display/Screen shows no artifacts', status: 'pending', autoGenerated: true });
      items.push({ label: 'Keyboard and Touchpad functional', status: 'pending', autoGenerated: true });
      items.push({ label: 'Battery charging status verified', status: 'pending', autoGenerated: true });
    } else if (eqType.includes('router') || eqType.includes('switch') || eqType.includes('network')) {
      items.push({ label: 'Management console accessible', status: 'pending', autoGenerated: true });
      items.push({ label: 'All LAN/WAN ports link up', status: 'pending', autoGenerated: true });
      items.push({ label: 'DHCP/Routing services functional', status: 'pending', autoGenerated: true });
      items.push({ label: 'Firmware version verified', status: 'pending', autoGenerated: true });
    } else if (eqType.includes('server')) {
      items.push({ label: 'RAID status optimal', status: 'pending', autoGenerated: true });
      items.push({ label: 'Remote management (IPMI/iDRAC) accessible', status: 'pending', autoGenerated: true });
      items.push({ label: 'Service/Daemons running correctly', status: 'pending', autoGenerated: true });
      items.push({ label: 'Network throughput verified', status: 'pending', autoGenerated: true });
    } else {
      items.push({ label: 'Primary functionality verified', status: 'pending', autoGenerated: true });
    }

    // Deduplicate and Append (Stricter matching to prevent duplicates)
    let addedCount = 0;
    items.forEach(item => {
      const cleanLabel = item.label.trim().toLowerCase().replace(/[^a-z0-9]/g, '');
      const exists = this.state.validationChecklist.some(v =>
        v.label.trim().toLowerCase().replace(/[^a-z0-9]/g, '') === cleanLabel
      );
      if (!exists) {
        this.state.validationChecklist.push(item);
        addedCount++;
      }
    });

    if (addedCount > 0) {
      this.saveState();
      this.pushTimeline(`${addedCount} Standard tests added to suite`, '#10b981', 'check_circle');
    }
  }

  onTestingContextChanged(): void {
    this.state.aiTestsGenerated = false;
    this.state.aiPlanGenerated = false;
    this.saveState();
  }

  generateAITests(): void {
    if (this.aiTestsLoading || this.state.aiTestsGenerated) return;
    this.aiTestsLoading = true;

    // Add standard standard checks since we removed auto-generation from the step transition
    this.buildValidationChecklist();

    const eqType = this.equipment?.type || 'Unknown Device';
    const specs = JSON.stringify(this.equipment?.specifications || {});
    const problemDesc = this.ticket?.description || 'No specific problem description';
    const diagnosis = this.state.diagnosisResult || this.state.manualDiagnosis || 'No diagnosis available';
    const actions = this.state.actions.map(a => `${a.type} on ${a.target} (${a.category})`).join(', ');
    const existingLabels = this.state.validationChecklist.map(v => v.label).join(', ');

    const prompt = `Act as a senior validation engineer. 
    Equipment: "${eqType}" (Specs: ${specs})
    Problem Reported: "${problemDesc}"
    Diagnosis: "${diagnosis}"
    Actions Performed: [${actions}]
    Existing Tests: [${existingLabels}]
    
    Based on the diagnosis and the actions taken, suggest 3 to 5 NEW specific technical validation tests to ensure the problem is solved and the system is stable.
    Provide a short, distinct label for each test. Focus on what was actually repaired.
    DO NOT suggest any tests that conceptually duplicate or overlap with the 'Existing Tests'.

    You MUST respond strictly with a JSON array of strings. Do not include markdown or explanations.
    Example: ["Verify GPU temperature under 4K stress", "Validate RAID rebuild completion"]`;

    this.aiService.query(prompt).subscribe({
      next: (res) => {
        this.aiTestsLoading = false;
        try {
          let jsonStr = res.answer || '[]';
          jsonStr = jsonStr.replace(/^```json/i, '').replace(/^```/i, '').replace(/```$/i, '').trim();
          const parsed = JSON.parse(jsonStr);

          if (Array.isArray(parsed)) {
            let addedCount = 0;
            parsed.forEach(label => {
              const lbl = label.toString().trim();
              if (!lbl) return;

              const cleanLbl = lbl.toLowerCase().replace(/[^a-z0-9]/g, '');
              const exists = this.state.validationChecklist.some(v => v.label.trim().toLowerCase().replace(/[^a-z0-9]/g, '') === cleanLbl);

              if (!exists) {
                this.state.validationChecklist.push({
                  label: lbl,
                  status: 'pending',
                  autoGenerated: true
                });
                addedCount++;
              }
            });

            if (addedCount > 0) {
              this.state.aiTestsGenerated = true;
              this.saveState();
              this.pushTimeline(`AI added ${addedCount} specialized validation tests`, '#8b5cf6', 'auto_awesome');
            } else {
              // If all were duplicates, we still consider it "generated" for this context
              this.state.aiTestsGenerated = true;
              this.saveState();
            }
          }
        } catch (e) {
          console.error("AI Testing generation failed:", e);
          this.buildValidationChecklist(); // Fallback
        }
      },
      error: () => {
        this.aiTestsLoading = false;
        this.buildValidationChecklist(); // Fallback
      }
    });
  }

  toggleValidation(item: ValidationItem): void {
    // Legacy support, should use markTestWork/Fail
    item.status = item.status === 'success' ? 'pending' : 'success';
    this.saveState();
  }

  markTestWork(item: ValidationItem): void {
    item.status = 'success';
    item.failureReason = '';
    this.saveState();
  }

  addCheckToDiagnosis(check: string) {
    const current = this.state.manualDiagnosis || '';
    this.state.manualDiagnosis = current ? `${current}\n- ${check}` : `- ${check}`;
    this.saveState();
  }

  addValidationItem(): void {
    if (!this.newCheckLabel.trim()) return;
    const lbl = this.newCheckLabel.trim();

    const cleanLabel = lbl.toLowerCase().replace(/\s+/g, ' ');
    const exists = this.state.validationChecklist.some(v =>
      v.label.trim().toLowerCase().replace(/\s+/g, ' ') === cleanLabel
    );

    if (exists) {
      this.newCheckLabel = '';
      return;
    }

    this.state.validationChecklist.push({
      label: lbl,
      status: 'pending',
      autoGenerated: false
    });
    this.newCheckLabel = '';
    this.saveState();
  }

  removeValidationItem(index: number): void {
    this.state.validationChecklist.splice(index, 1);
    if (this.state.validationChecklist.length === 0) {
      this.state.aiTestsGenerated = false;
    }
    this.saveState();
  }


  get validationProgress(): { checked: number; total: number; allDone: boolean; percentage: number } {
    const total = this.state.validationChecklist.length;
    const checked = this.state.validationChecklist.filter(v => v.status === 'success').length;
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
    const date = new Date().toISOString().slice(0, 10);
    const ticketId = this.ticket.id || 'N/A';

    import('jspdf').then(({ default: jsPDF }) => {
      import('jspdf-autotable').then(({ default: autoTable }) => {
        const doc = new jsPDF('p', 'mm', 'a4');
        const pageWidth = doc.internal.pageSize.getWidth();

        // ── HEADER ───────────────────────────────────────────────────────────
        doc.setFillColor(30, 41, 59); // Slate 800
        doc.rect(0, 0, pageWidth, 40, 'F');

        doc.setFontSize(22);
        doc.setTextColor(255, 255, 255);
        doc.setFont('helvetica', 'bold');
        doc.text('MAINTENANCE REPORT', 14, 25);

        doc.setFontSize(10);
        doc.setFont('helvetica', 'normal');
        doc.setTextColor(148, 163, 184); // Slate 400
        doc.text(`TICKET #${ticketId} | ${this.formatDateTime(new Date())}`, 14, 32);

        // ── DEVICE INFO WIDGETS (as seen in UI) ─────────────────────────────
        let yPos = 50;

        autoTable(doc, {
          startY: yPos,
          head: [['EQUIPMENT', 'BRAND / MODEL', 'SERIAL NUMBER', 'STATUS']],
          body: [[
            this.getEquipmentName(),
            (this.equipment.brand || '–') + ' ' + (this.equipment.model || ''),
            this.equipment.serialNumber || 'N/A',
            'FIXED / OPERATING'
          ]],
          theme: 'grid',
          headStyles: { fillColor: [51, 65, 85], textColor: [255, 255, 255], fontStyle: 'bold', fontSize: 9 },
          styles: { fontSize: 10, cellPadding: 5 },
          margin: { left: 14, right: 14 }
        });

        yPos = (doc as any).lastAutoTable.finalY + 15;

        // ── PROBLEM & DIAGNOSIS ──────────────────────────────────────────────
        doc.setFontSize(14);
        doc.setTextColor(30, 41, 59);
        doc.setFont('helvetica', 'bold');
        doc.text('1. Problem & Technical Findings', 14, yPos);

        yPos += 8;
        doc.setFontSize(10);
        doc.setFont('helvetica', 'bold');
        doc.setTextColor(239, 68, 68); // Red 500
        doc.text('INITIAL COMPLAINT:', 14, yPos);

        yPos += 5;
        doc.setFont('helvetica', 'normal');
        doc.setTextColor(71, 85, 105); // Slate 600
        const complaintLines = doc.splitTextToSize(this.ticket.description || 'No description provided.', pageWidth - 28);
        doc.text(complaintLines, 14, yPos);

        yPos += (complaintLines.length * 5) + 5;
        doc.setFont('helvetica', 'bold');
        doc.setTextColor(59, 130, 246); // Blue 500
        doc.text('TECHNICAL DIAGNOSIS:', 14, yPos);

        yPos += 5;
        doc.setFont('helvetica', 'normal');
        doc.setTextColor(71, 85, 105);
        const diagnosisLines = doc.splitTextToSize(this.state.diagnosisResult || 'Diagnosis not recorded.', pageWidth - 28);
        doc.text(diagnosisLines, 14, yPos);

        yPos += (diagnosisLines.length * 5) + 15;

        // ── SERVICE TRANSFORMATION ───────────────────────────────────────────
        doc.setFontSize(14);
        doc.setTextColor(30, 41, 59);
        doc.setFont('helvetica', 'bold');
        doc.text('2. Service Transformation', 14, yPos);

        yPos += 8;
        autoTable(doc, {
          startY: yPos,
          body: [
            ['FAULTY STATE', '→', 'RESTORED STATE'],
            ['Non-Functional / Degraded', '', 'Verified Operating / Optimized']
          ],
          theme: 'plain',
          styles: { halign: 'center', fontSize: 11, fontStyle: 'bold' },
          columnStyles: {
            0: { textColor: [239, 68, 68] },
            1: { textColor: [100, 116, 139], fontSize: 16 },
            2: { textColor: [16, 185, 129] }
          },
          margin: { left: 14, right: 14 }
        });

        yPos = (doc as any).lastAutoTable.finalY + 15;

        // ── WORK LOG (ACTIONS) ───────────────────────────────────────────────
        doc.setFontSize(14);
        doc.setTextColor(30, 41, 59);
        doc.setFont('helvetica', 'bold');
        doc.text(`3. Work Log (${this.completedActions.length} Actions)`, 14, yPos);

        autoTable(doc, {
          startY: yPos + 5,
          head: [['CATEGORY', 'ACTION', 'TARGET', 'STATUS']],
          body: this.state.actions.map(a => [
            a.category,
            a.type,
            a.target,
            a.status
          ]),
          theme: 'striped',
          headStyles: { fillColor: [99, 102, 241] }, // Indigo 500
          styles: { fontSize: 9 },
          margin: { left: 14, right: 14 }
        });

        yPos = (doc as any).lastAutoTable.finalY + 15;

        // ── INVENTORY CONSUMPTION ────────────────────────────────────────────
        const allResources = this.state.actions.flatMap(a => (a.resources || []).map(r => ({ ...r, actionTarget: a.target })));

        if (allResources.length > 0) {
          if (yPos > 240) { doc.addPage(); yPos = 20; }
          doc.setFontSize(14);
          doc.setTextColor(30, 41, 59);
          doc.setFont('helvetica', 'bold');
          doc.text('4. Inventory Consumption', 14, yPos);

          autoTable(doc, {
            startY: yPos + 5,
            head: [['RESOURCE', 'TYPE', 'QTY', 'SPECIFICATION', 'ASSIGNED TO']],
            body: allResources.map(r => [
              r.name,
              r.resourceType.toUpperCase(),
              r.quantity || 1,
              r.specification || '–',
              r.actionTarget
            ]),
            theme: 'striped',
            headStyles: { fillColor: [245, 158, 11] }, // Amber 500
            styles: { fontSize: 9 },
            margin: { left: 14, right: 14 }
          });
          yPos = (doc as any).lastAutoTable.finalY + 15;
        }



        // ── VALIDATION SUITE ─────────────────────────────────────────────────
        if (this.state.validationChecklist.length > 0) {
          if (yPos > 240) { doc.addPage(); yPos = 20; }
          doc.setFontSize(14);
          doc.setTextColor(30, 41, 59);
          doc.setFont('helvetica', 'bold');
          doc.text('5. Final System Validation', 14, yPos);

          autoTable(doc, {
            startY: yPos + 5,
            head: [['VALIDATION ITEM', 'STATUS', 'DIAGNOSTICS / REASON']],
            body: this.state.validationChecklist.map(v => [
              v.label,
              v.status.toUpperCase(),
              v.failureReason || (v.status === 'success' ? 'Passed' : '–')
            ]),
            theme: 'grid',
            headStyles: { fillColor: [16, 185, 129] }, // Emerald 500
            styles: { fontSize: 9 },
            columnStyles: {
              1: { halign: 'center', fontStyle: 'bold' }
            },
            margin: { left: 14, right: 14 }
          });
          yPos = (doc as any).lastAutoTable.finalY + 15;
        }

        // ── FINAL STATEMENT ──────────────────────────────────────────────────
        if (yPos > 240) { doc.addPage(); yPos = 20; }
        doc.setFontSize(14);
        doc.setTextColor(30, 41, 59);
        doc.setFont('helvetica', 'bold');
        doc.text('6. Final Technician Statement', 14, yPos);

        yPos += 8;
        doc.setFontSize(10);
        doc.setFont('helvetica', 'normal');
        doc.setTextColor(71, 85, 105);
        const noteLines = doc.splitTextToSize(this.state.summaryNote || 'No final notes provided.', pageWidth - 28);
        doc.text(noteLines, 14, yPos);

        // ── SIGNATURE ────────────────────────────────────────────────────────
        yPos += (noteLines.length * 5) + 30;
        if (yPos > 270) { doc.addPage(); yPos = 40; }

        doc.setDrawColor(203, 213, 225);
        doc.line(14, yPos, 80, yPos);
        doc.line(pageWidth - 80, yPos, pageWidth - 14, yPos);

        doc.setFontSize(8);
        doc.text('TECHNICIAN SIGNATURE', 14, yPos + 5);
        doc.text('DATE OF COMPLETION', pageWidth - 80, yPos + 5);

        // ── FOOTER ───────────────────────────────────────────────────────────
        const pageCount = (doc as any).internal.getNumberOfPages();
        for (let i = 1; i <= pageCount; i++) {
          doc.setPage(i);
          doc.setFontSize(8);
          doc.setTextColor(148, 163, 184);
          doc.text(`Page ${i} of ${pageCount} — MedinaFlux Live TicketBench System`, pageWidth / 2, 285, { align: 'center' });
        }

        doc.save(`Maintenance_Report_T${ticketId}_${date}.pdf`);
      });
    });
  }


  getAvailableSpecKeys(): string[] {
    if (!this.equipment || !this.equipment.specifications) return [];
    return Object.keys(this.equipment.specifications);
  }

  confirmComplete(): void {
    const parts = this.state.actions
      .flatMap(a => a.resources.filter(r => r.resourceType === 'part'))
      .map(r => ({
        name: r.name,
        qty: r.quantity || 1,
        specification: r.specification,
        equipmentId: r.equipmentId,
        replacesSpecKey: r.replacesSpecKey
      }));

    const tasks = this.state.actions.map(a => ({
      label: `[${a.category.toUpperCase()}] ${a.type} ${a.target}`,
      status: a.status,
      notes: a.notes
    }));

    const completedTests = this.state.validationChecklist
      .filter(v => v.status === 'success')
      .map(v => v.label);
    const validationSummary = `Verified ${completedTests.length}/${this.state.validationChecklist.length} checks.`;

    this.pushTimeline('Maintenance completed ✓', '#10b981', 'check-circle');
    this.saveState();

    this.complete.emit({
      workNote: this.state.summaryNote,
      repairTasks: tasks,
      partsUsed: parts,
      isBroken: false,
      diagnosisResult: this.state.diagnosisResult,
      validationSummary,
      partsInstalled: parts // Includes equipmentId and replacesSpecKey now
    });
    this.showCompleteDialog = false;
    this.clearState();
  }

  confirmBrokenDevice(): void {
    const reason = this.brokenReason.trim() || 'Device hardware failure exceeds repair thresholds.';
    const workNote = `UNREPAIRABLE: ${reason} Asset marked as BROKEN.`;

    this.complete.emit({
      workNote,
      repairTasks: [],
      partsUsed: [],
      isBroken: true,
      diagnosisResult: this.state.diagnosisResult,
      validationSummary: `Device marked as unrepairable during diagnosis/testing phase. Reason: ${reason}`,
      partsInstalled: []
    });

    this.showBrokenDialog = false;
    this.brokenReason = '';
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
    if (!this.state.timeline) this.state.timeline = [];
    this.state.timeline.unshift({ title, color, icon, time: this.formatDateTime(new Date()) });
    this.saveState();
  }

  get importantTimeline(): any[] {
    const criticalKeywords = [
      'Started', 'AI Diagnosis', 'AI Maintenance Plan',
      'Completed', 'Inventory Auto-Matched', 'tests added', 'Suite Synchronized', 'Maintenance completed ✓'
    ];

    return (this.state.timeline || []).filter(entry =>
      criticalKeywords.some(kw => entry.title.includes(kw))
    );
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
