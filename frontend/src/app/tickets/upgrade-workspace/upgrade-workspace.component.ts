import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { Ticket } from '../ticket.model';
import { Equipment } from '../../equipment/equipment.model';
import { EquipmentService } from '../../equipment/equipment.service';
import { ToastService } from '../../shared/toast.service';
import { ConfirmDialogService } from '../../shared/components/confirm-dialog/confirm-dialog.service';
import { PartWorkflowService, TechnicianPartAction } from '../part-workflow.service';

@Component({
  selector: 'app-upgrade-workspace',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './upgrade-workspace.component.html',
  styleUrl: './upgrade-workspace.component.css'
})
export class UpgradeWorkspaceComponent implements OnInit {
  @Input() ticket!: Ticket;
  @Input() equipment: any = null;
  @Input() userInventory: any[] = [];
  @Input() currentUser: any = null;
  @Output() complete = new EventEmitter<void>();
  @Output() close = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();
  @Output() inventoryChanged = new EventEmitter<void>();

  installedParts: Equipment[] = [];
  isLoading = true;
  installingKey: string | null = null;
  uninstallingId: string | null = null;
  isCancelling = false;
  private baselineCaptured = false;
  private baselineInstalledPartIds = new Set<string>();
  private sessionInstalledPartIds = new Set<string>();
  private sessionUninstalledPartIds = new Set<string>();

  constructor(
    private equipmentService: EquipmentService,
    private partWorkflowService: PartWorkflowService,
    private toastService: ToastService,
    private confirmDialogService: ConfirmDialogService
  ) {}

  ngOnInit(): void {
    this.loadInstalledParts();
  }

  loadInstalledParts(): void {
    if (!this.equipment?.id) {
      this.isLoading = false;
      return;
    }
    const parentId = this.equipment.id;
    const parentName = this.equipment.equipmentName || this.equipment.name;
    this.equipmentService.getAllEquipment().subscribe({
      next: (all) => {
        this.installedParts = all.filter(e => {
          const status = (e.status || '').toLowerCase();
          const isInstalled = status === 'installed' || status === 'assigned';
          const matchesParent =
            e.assignedToEquipmentId === parentId ||
            (!!parentName && e.assignedToEquipmentName === parentName);
          return isInstalled && matchesParent;
        });
        if (!this.baselineCaptured) {
          this.baselineInstalledPartIds = new Set(
            this.installedParts.map(p => p.id).filter((id): id is string => !!id)
          );
          this.baselineCaptured = true;
        }
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  get availableInventory(): any[] {
    return (this.userInventory || []).filter(i => (i.totalQty || 0) > 0);
  }

  getPartSpecificationEntries(part: Equipment): { key: string; value: string }[] {
    if (!part.specifications) return [];
    return Object.entries(part.specifications).map(([key, value]) => ({ key, value }));
  }

  resolveInstallSpecKey(partType?: string): string {
    const specs = this.equipment?.specifications || {};
    const type = (partType || '').toLowerCase().trim();
    const existing = Object.keys(specs).find(k => k.toLowerCase().trim() === type);
    return existing || partType || 'Part';
  }

  resolveInventoryEquipmentId(item: any): string | undefined {
    return item?.id;
  }

  inventoryKey(item: any): string {
    return `${item.name}||${item.specification || ''}`;
  }

  buildInstallAction(item: any) {
    const partId = this.resolveInventoryEquipmentId(item);
    if (!partId || !this.equipment?.id || !this.currentUser?.id) {
      return null;
    }
    const specKey = this.resolveInstallSpecKey(item.type);
    return {
      partId,
      parentId: this.equipment.id,
      parentName: this.equipment.equipmentName || this.equipment.name || '',
      requesterId: this.currentUser.id,
      requesterName: this.currentUser.firstName || 'Technician',
      name: item.name,
      type: item.type,
      specification: item.specification,
      brand: item.brand,
      replacesSpecKey: specKey,
      actor: this.currentUser.firstName || 'Technician',
      qty: 1
    };
  }

  buildInstallActionFromPart(part: Equipment): TechnicianPartAction | null {
    if (!part.id || !this.equipment?.id || !this.currentUser?.id) {
      return null;
    }
    const specKey = this.resolveInstallSpecKey(part.type);
    return {
      partId: part.id,
      parentId: this.equipment.id,
      parentName: this.equipment.equipmentName || this.equipment.name || '',
      requesterId: this.currentUser.id,
      requesterName: this.currentUser.firstName || 'Technician',
      name: part.equipmentName || '',
      type: part.type,
      specification: this.formatPartSpecification(part),
      brand: part.brand,
      replacesSpecKey: specKey,
      actor: this.currentUser.firstName || 'Technician',
      qty: 1
    };
  }

  buildUninstallAction(part: Equipment) {
    if (!part.id || !this.currentUser?.id) {
      return null;
    }
    const specKey = this.resolveInstallSpecKey(part.type);
    return {
      partId: part.id,
      parentId: this.equipment?.id || '',
      parentName: this.equipment?.equipmentName || this.equipment?.name || '',
      requesterId: this.currentUser.id,
      requesterName: this.currentUser.firstName || 'Technician',
      name: part.equipmentName || '',
      type: part.type,
      specification: this.formatPartSpecification(part),
      brand: part.brand,
      replacesSpecKey: specKey,
      actor: this.currentUser.firstName || 'Technician',
      qty: 1
    };
  }

  formatPartSpecification(part: Equipment): string {
    if (!part.specifications || Object.keys(part.specifications).length === 0) {
      return '';
    }
    return Object.entries(part.specifications)
      .map(([key, value]) => `${key}: ${value}`)
      .join(', ');
  }

  installFromInventory(item: any): void {
    const action = this.buildInstallAction(item);
    if (!action) {
      this.toastService.error('Cannot install: missing part stock record or user session.');
      return;
    }

    const key = this.inventoryKey(item);
    this.installingKey = key;

    this.partWorkflowService.installPartOnEquipment(action).subscribe({
      next: () => {
        if (this.baselineInstalledPartIds.has(action.partId)) {
          this.sessionUninstalledPartIds.delete(action.partId);
        } else {
          this.sessionInstalledPartIds.add(action.partId);
        }
        this.toastService.success(`${item.name} installed — status set to Installed, inventory qty decreased.`);
        this.refreshEquipment();
        this.loadInstalledParts();
        this.inventoryChanged.emit();
        this.installingKey = null;
      },
      error: (err) => {
        console.error('Upgrade install failed', err);
        this.toastService.error('Failed to install part.');
        this.installingKey = null;
      }
    });
  }

  uninstallPart(part: Equipment): void {
    const action = this.buildUninstallAction(part);
    if (!action) {
      this.toastService.error('Cannot uninstall: missing part or user session.');
      return;
    }

    this.uninstallingId = part.id!;
    this.partWorkflowService.uninstallPartFromEquipment(action).subscribe({
      next: () => {
        const partId = part.id!;
        if (this.baselineInstalledPartIds.has(partId)) {
          this.sessionUninstalledPartIds.add(partId);
        } else {
          this.sessionInstalledPartIds.delete(partId);
        }
        this.toastService.success(`${part.equipmentName} uninstalled — status returned to Allocated, inventory qty restored.`);
        this.refreshEquipment();
        this.loadInstalledParts();
        this.inventoryChanged.emit();
        this.uninstallingId = null;
      },
      error: (err) => {
        console.error('Upgrade uninstall failed', err);
        this.toastService.error('Failed to uninstall part.');
        this.uninstallingId = null;
      }
    });
  }

  refreshEquipment(): void {
    if (!this.equipment?.id) return;
    this.equipmentService.getEquipmentById(this.equipment.id).subscribe(eq => {
      this.equipment = { ...this.equipment, ...eq };
    });
  }

  completeUpgrade(): void {
    this.complete.emit();
  }

  async cancelUpgrade(): Promise<void> {
    const confirmed = await this.confirmDialogService.confirm({
      title: 'Cancel Upgrade',
      message: 'All part changes made in this session will be reverted and the upgrade ticket will be deleted. Continue?',
      confirmText: 'Cancel Upgrade',
      isDanger: true
    });
    if (!confirmed) {
      return;
    }

    this.isCancelling = true;
    try {
      for (const partId of this.sessionInstalledPartIds) {
        const part = await firstValueFrom(this.equipmentService.getEquipmentById(partId));
        const action = this.buildUninstallAction(part);
        if (!action) {
          continue;
        }
        await firstValueFrom(this.partWorkflowService.uninstallPartFromEquipment(action));
      }

      for (const partId of this.sessionUninstalledPartIds) {
        const part = await firstValueFrom(this.equipmentService.getEquipmentById(partId));
        const action = this.buildInstallActionFromPart(part);
        if (!action) {
          continue;
        }
        await firstValueFrom(this.partWorkflowService.installPartOnEquipment(action));
      }

      this.toastService.info('Upgrade changes reverted.');
      this.inventoryChanged.emit();
      this.cancel.emit();
    } catch (err) {
      console.error('Cancel upgrade failed', err);
      this.toastService.error('Failed to revert all upgrade changes. Please review equipment and inventory manually.');
    } finally {
      this.isCancelling = false;
    }
  }
}
