import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import {
  CapabilityResponse,
  WorkbenchState,
  WorkflowStep
} from './workbench.model';

@Injectable({ providedIn: 'root' })
export class WorkbenchEngineService {
  private apiUrl = 'http://localhost:8000/api/capabilities';

  constructor(private http: HttpClient) {}

  getCapabilities(equipmentType: string): Observable<CapabilityResponse> {
    const t = (equipmentType || 'unknown').toLowerCase().trim();
    return this.http.get<CapabilityResponse>(`${this.apiUrl}/${encodeURIComponent(t)}`).pipe(
      catchError(() => of(this.getFallback(equipmentType)))
    );
  }

  private getFallback(equipmentType: string): CapabilityResponse {
    const t = (equipmentType || '').toLowerCase();
    if (t.includes('laptop') || t.includes('pc') || t.includes('desktop')) {
      return {
        equipmentType,
        categories: ['hardware', 'software', 'maintenance'],
        categoryTypes: {
          hardware: ['install', 'replace', 'upgrade', 'remove', 'repair'],
          software: ['install', 'update', 'uninstall', 'configure', 'scan'],
          maintenance: ['clean', 'inspect', 'test', 'calibrate', 'backup']
        },
        categoryTargets: {
          hardware: ['RAM', 'CPU', 'SSD', 'HDD', 'Battery', 'Screen', 'Keyboard', 'Fan', 'GPU', 'Other'],
          software: ['Operating System', 'Drivers', 'Antivirus', 'Application', 'Browser', 'Other'],
          maintenance: ['Physical Cleaning', 'Thermal Paste', 'Cooling System', 'Ports', 'Other']
        }
      };
    }
    if (t.includes('router')) {
      return {
        equipmentType,
        categories: ['configuration', 'network', 'firmware'],
        categoryTypes: {
          configuration: ['configure', 'reset', 'backup', 'restore'],
          network: ['setup', 'test', 'troubleshoot', 'optimize'],
          firmware: ['update', 'rollback', 'verify']
        },
        categoryTargets: {
          configuration: ['IP Address', 'Subnet Mask', 'Gateway', 'DNS', 'VLAN', 'Firewall Rules', 'Other'],
          network: ['LAN', 'WAN', 'WiFi', 'VPN', 'Routing', 'Bandwidth', 'Other'],
          firmware: ['Router Firmware', 'BIOS', 'Controller Firmware', 'Other']
        }
      };
    }
    if (t.includes('server')) {
      return {
        equipmentType,
        categories: ['hardware', 'software', 'services'],
        categoryTypes: {
          hardware: ['install', 'replace', 'upgrade', 'remove', 'repair'],
          software: ['install', 'update', 'uninstall', 'configure', 'patch'],
          services: ['start', 'stop', 'restart', 'configure', 'monitor', 'migrate']
        },
        categoryTargets: {
          hardware: ['RAM', 'CPU', 'SSD', 'HDD', 'Power Supply', 'Network Card', 'GPU', 'Other'],
          software: ['Operating System', 'Drivers', 'Application', 'Firmware', 'Other'],
          services: ['Web Server', 'Database', 'Mail Server', 'File Server', 'Backup Service', 'Other']
        }
      };
    }
    if (t.includes('switch')) {
      return {
        equipmentType,
        categories: ['configuration', 'network', 'maintenance'],
        categoryTypes: {
          configuration: ['configure', 'reset', 'backup', 'restore', 'vlan'],
          network: ['setup', 'test', 'troubleshoot', 'optimize', 'monitor'],
          maintenance: ['clean', 'inspect', 'test', 'replace-port']
        },
        categoryTargets: {
          configuration: ['IP Address', 'VLAN', 'QoS', 'Access Control', 'Other'],
          network: ['LAN', 'Switching', 'Bandwidth', 'Latency', 'Other'],
          maintenance: ['Physical Cleaning', 'Ports', 'Connectors', 'Other']
        }
      };
    }
    return {
      equipmentType,
      categories: ['hardware', 'maintenance'],
      categoryTypes: {
        hardware: ['install', 'replace', 'repair', 'inspect'],
        maintenance: ['inspect', 'test', 'clean', 'repair']
      },
      categoryTargets: {
        hardware: ['Component', 'Port', 'Module', 'Other'],
        maintenance: ['Physical Cleaning', 'Ports', 'Connectors', 'Other']
      }
    };
  }

  validateStep(step: WorkflowStep, state: WorkbenchState): { valid: boolean; error: string } {
    switch (step) {
      case 'diagnosis':
        if (!state.diagnosisResult || state.diagnosisResult.trim().length < 3) {
          return { valid: false, error: 'Please enter a diagnosis result of at least 3 characters.' };
        }
        return { valid: true, error: '' };

      case 'plan':
        if (state.actions.length === 0) {
          return { valid: false, error: 'Add at least one action to the maintenance plan.' };
        }
        return { valid: true, error: '' };

      case 'resources':
        return { valid: true, error: '' };

      case 'execution': {
        const pending = state.actions.filter(a => a.status !== 'Done');
        if (pending.length > 0) {
          return { valid: false, error: `${pending.length} action(s) are not yet marked as Done.` };
        }
        return { valid: true, error: '' };
      }

      case 'validation': {
        const unchecked = state.validationChecklist.filter(v => v.status !== 'success');
        if (unchecked.length > 0) {
          return { valid: false, error: `${unchecked.length} validation item(s) are not marked as Success.` };
        }
        return { valid: true, error: '' };
      }

      default:
        return { valid: true, error: '' };
    }
  }

  buildValidationChecklist(state: WorkbenchState): void {
    const auto = state.actions.map(a => ({
      label: `Verified: ${a.target} after ${a.type}`,
      status: 'pending' as const,
      autoGenerated: true
    }));
    const manual = state.validationChecklist.filter(v => !v.autoGenerated);
    state.validationChecklist = [...auto, ...manual];
  }

  getCategoryColor(category: string): string {
    const map: { [key: string]: string } = {
      Hardware: '#3b82f6',
      Software: '#8b5cf6',
      Configuration: '#f59e0b',
      Network: '#10b981',
      Maintenance: '#94a3b8',
      Inspection: '#06b6d4',
      Power: '#ef4444',
      Security: '#1e293b',
      Storage: '#475569',
      Peripheral: '#ec4899',
      Performance: '#f43f5e',
      Thermal: '#f97316',
      Consumables: '#fbbf24',
      Firmware: '#6366f1',
      Cabling: '#8b5cf6'
    };
    return map[category] || '#6366f1';
  }
}
