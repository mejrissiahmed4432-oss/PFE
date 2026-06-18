import { Component, OnInit, OnDestroy, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export interface LaptopStatus {
  equipmentId: string;
  equipmentName: string;
  serialNumber: string;
  brand: string;
  model: string;
  department: string;
  ip: string | null;
  upStatus: 'UP' | 'DOWN' | 'NOT_FOUND_YET';
  cpuPercent: number;
  ramPercent: number;
  totalRam: number;
  freeRam: number;
  diskPercent: number;
  totalDisk: number;
  freeDisk: number;
  diskVolumes: { [key: string]: number };
  networkSpeed: string;
  netInSpeed: number;
  netOutSpeed: number;
  topProcesses: { name: string; ramUsageMb: number; pid: number }[];
  os: string;
  macAddress: string;
  lastSeen: string;
  uptime: string;
  temperature: number;
  totalProcesses: number;
}

export interface DeptPcSummary {
  departmentName: string;
  totalLaptops: number;
  onlineCount: number;
  offlineCount: number;
  notFoundCount: number;
  laptops: LaptopStatus[];
  topDevicesByCpu: LaptopStatus[];
  topDevicesByRam: LaptopStatus[];
}

@Component({
  selector: 'app-technician-departments',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './technician-departments.component.html',
  styleUrls: ['./technician-departments.component.css']
})
export class TechnicianDepartmentsComponent implements OnInit, OnDestroy {

  /** Emitted when "View All Devices" is clicked; value = department name */
  @Output() viewDevices = new EventEmitter<string>();

  departments: DeptPcSummary[] = [];
  isConnected = false;
  lastUpdated: Date | null = null;
  deptSearch = '';

  private stompClient!: Client;
  private readonly WS_URL = '/ws-monitoring';

  ngOnInit(): void { this.connect(); }
  ngOnDestroy(): void { this.stompClient?.deactivate(); }

  connect(): void {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(this.WS_URL),
      reconnectDelay: 3000,
      onConnect: () => {
        this.isConnected = true;
        this.stompClient.subscribe('/topic/dept-monitoring', (msg: IMessage) => {
          this.departments = JSON.parse(msg.body) as DeptPcSummary[];
          this.lastUpdated = new Date();
        });
      },
      onDisconnect: () => { this.isConnected = false; },
      onStompError: () => { this.isConnected = false; }
    });
    this.stompClient.activate();
  }

  openDevices(deptName: string): void {
    this.viewDevices.emit(deptName);
  }

  get filteredDepartments(): DeptPcSummary[] {
    if (!this.deptSearch.trim()) return this.departments;
    const q = this.deptSearch.toLowerCase();
    return this.departments.filter(d =>
      d.departmentName.toLowerCase().includes(q) ||
      d.laptops.some(l =>
        (l.equipmentName || '').toLowerCase().includes(q) ||
        (l.serialNumber || '').toLowerCase().includes(q)
      )
    );
  }

  get totalLaptops(): number { return this.departments.reduce((s, d) => s + d.totalLaptops, 0); }
  get totalOnline():  number { return this.departments.reduce((s, d) => s + d.onlineCount, 0); }
  get totalOffline(): number { return this.departments.reduce((s, d) => s + d.offlineCount, 0); }

  cpuColor(v: number): string {
    if (v >= 90) return '#ef4444';
    if (v >= 70) return '#f59e0b';
    return '#10b981';
  }

  deptColor(name: string): string {
    const palette = ['#3b82f6','#10b981','#f59e0b','#ef4444','#8b5cf6','#06b6d4','#ec4899','#14b8a6','#f97316','#6366f1'];
    let h = 0;
    for (let i = 0; i < name.length; i++) h = name.charCodeAt(i) + ((h << 5) - h);
    return palette[Math.abs(h) % palette.length];
  }
}


