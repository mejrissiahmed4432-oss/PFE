import { Injectable } from '@angular/core';
import { Observable, switchMap, map, of } from 'rxjs';
import { EquipmentService } from '../equipment/equipment.service';
import { PartRequestService } from '../parts-management/part-request.service';
import { Equipment } from '../equipment/equipment.model';

export interface TechnicianPartAction {
  partId: string;
  parentId: string;
  parentName: string;
  requesterId: string;
  requesterName?: string;
  name: string;
  type?: string;
  specification?: string;
  brand?: string;
  replacesSpecKey: string;
  actor: string;
  qty?: number;
}

@Injectable({ providedIn: 'root' })
export class PartWorkflowService {

  constructor(
    private equipmentService: EquipmentService,
    private partRequestService: PartRequestService
  ) {}

  installPartOnEquipment(action: TechnicianPartAction): Observable<Equipment> {
    const qty = action.qty ?? 1;
    return this.equipmentService.installPartFromMaintenance(action.partId, action.parentId, {
      replacesSpecKey: action.replacesSpecKey,
      actionType: 'Install',
      specification: action.specification,
      brand: action.brand,
      actor: action.actor
    }).pipe(
      switchMap((installed) => {
        if (!action.requesterId) {
          return of(installed);
        }
        return this.partRequestService.consumeParts(action.requesterId, [{
          name: action.name,
          qty,
          type: action.type,
          specification: action.specification,
          equipmentId: action.partId,
          brand: action.brand,
          assignedToEquipmentId: action.parentId,
          assignedToEquipmentName: action.parentName,
          replacesSpecKey: action.replacesSpecKey,
          actionType: 'Install'
        }]).pipe(map(() => installed));
      })
    );
  }

  uninstallPartFromEquipment(action: TechnicianPartAction): Observable<Equipment> {
    const qty = action.qty ?? 1;
    return this.equipmentService.returnPartToTechnician(
      action.partId,
      action.requesterId,
      action.requesterName || action.actor
    ).pipe(
      switchMap((restored) => {
        if (!action.requesterId) {
          return of(restored);
        }
        return this.partRequestService.restoreParts(action.requesterId, [{
          name: action.name,
          qty,
          type: action.type,
          specification: action.specification,
          equipmentId: action.partId,
          brand: action.brand,
          replacesSpecKey: action.replacesSpecKey,
          actionType: 'Install'
        }]).pipe(map(() => restored));
      })
    );
  }
}
