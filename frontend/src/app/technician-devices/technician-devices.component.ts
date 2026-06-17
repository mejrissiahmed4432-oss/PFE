import { Component, Input, OnInit, OnDestroy, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Client, IMessage } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { LaptopStatus, DeptPcSummary } from '../technician-departments/technician-departments.component';

interface MetricPoint {
  time: Date;
  cpu: number;
  ram: number;
  disk: number;
  processes: number;
  netIn: number;
  netOut: number;
  temperature: number;
}

@Component({
  selector: 'app-technician-devices',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './technician-devices.component.html',
  styleUrls: ['./technician-devices.component.css']
})
export class TechnicianDevicesComponent implements OnInit, OnDestroy, OnChanges {

  @Input() departmentFilter: string | null = null;

  departments: DeptPcSummary[] = [];
  isConnected = false;
  lastUpdated: Date | null = null;

  // Filters
  deviceSearch = '';
  deviceDeptFilter = 'all';
  deviceStatusFilter = 'all';

  // Sort
  sortCol: 'name' | 'cpu' | 'ram' | 'disk' | 'temperature' = 'cpu';
  sortDir: 1 | -1 = -1;

  // Detail view
  selectedDevice: LaptopStatus | null = null;
  activeTab: 'overview' | 'cpu' | 'memory' | 'disk' | 'network' | 'processes' | 'temperature' = 'overview';

  // History: deviceId → array of metric points (max 25)
  private historyMap = new Map<string, MetricPoint[]>();

  private stompClient!: Client;
  private readonly WS_URL = 'http://localhost:8000/ws-monitoring';

  ngOnInit(): void {
    this.syncInputFilter();
    this.connect();
  }

  ngOnChanges(): void {
    this.syncInputFilter();
  }

  ngOnDestroy(): void {
    this.stompClient?.deactivate();
  }

  private syncInputFilter(): void {
    this.deviceDeptFilter = this.departmentFilter ?? 'all';
  }

  connect(): void {
    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(this.WS_URL),
      reconnectDelay: 3000,
      onConnect: () => {
        this.isConnected = true;
        this.stompClient.subscribe('/topic/dept-monitoring', (msg: IMessage) => {
          const depts = JSON.parse(msg.body) as DeptPcSummary[];
          this.departments = depts;
          this.lastUpdated = new Date();

          // Accumulate history for every device
          depts.forEach(d => d.laptops.forEach(l => {
            if (l.upStatus === 'UP' || l.upStatus === 'DOWN') {
              const key = l.equipmentId || l.serialNumber;
              const hist = this.historyMap.get(key) ?? [];
              const isUp = l.upStatus === 'UP';
              hist.push({
                time: new Date(),
                cpu: isUp ? (l.cpuPercent ?? 0) : 0,
                ram: isUp ? (l.ramPercent ?? 0) : 0,
                disk: isUp ? (l.diskPercent ?? 0) : 0,
                processes: isUp ? (l.totalProcesses || 0) : 0,
                netIn: isUp ? (l.netInSpeed ?? 0) : 0,
                netOut: isUp ? (l.netOutSpeed ?? 0) : 0,
                temperature: isUp ? (l.temperature ?? 0) : 0
              });
              if (hist.length > 25) hist.shift();
              this.historyMap.set(key, hist);
            }
          }));

          // If a device is selected, keep it fresh
          if (this.selectedDevice) {
            const key = this.selectedDevice.equipmentId || this.selectedDevice.serialNumber;
            const fresh = depts.flatMap(d => d.laptops).find(l =>
              (l.equipmentId || l.serialNumber) === key
            );
            if (fresh) this.selectedDevice = fresh;
          }
        });
      },
      onDisconnect: () => { this.isConnected = false; },
      onStompError:  () => { this.isConnected = false; }
    });
    this.stompClient.activate();
  }



  // ── Device list data ─────────────────────────────────────
  get allDevices(): LaptopStatus[] {
    return this.departments.flatMap(d => d.laptops);
  }

  get filteredDevices(): LaptopStatus[] {
    let list = this.allDevices;
    const q = this.deviceSearch.toLowerCase().trim();
    if (q) list = list.filter(l =>
      (l.equipmentName || '').toLowerCase().includes(q) ||
      (l.ip || '').toLowerCase().includes(q) ||
      (l.serialNumber || '').toLowerCase().includes(q)
    );
    if (this.deviceDeptFilter !== 'all') list = list.filter(l => l.department === this.deviceDeptFilter);
    if (this.deviceStatusFilter !== 'all') list = list.filter(l => this.resolveStatus(l) === this.deviceStatusFilter);
    return [...list].sort((a, b) => {
      if (this.sortCol === 'cpu')  return (a.cpuPercent  - b.cpuPercent)  * this.sortDir;
      if (this.sortCol === 'ram')  return (a.ramPercent  - b.ramPercent)  * this.sortDir;
      if (this.sortCol === 'disk') return (a.diskPercent - b.diskPercent) * this.sortDir;
      if (this.sortCol === 'temperature') return ((a.temperature || 0) - (b.temperature || 0)) * this.sortDir;
      return (a.equipmentName || '').localeCompare(b.equipmentName || '') * this.sortDir;
    });
  }

  get uniqueDepartments(): string[] {
    return [...new Set(this.allDevices.map(d => d.department))].sort();
  }

  setSort(col: 'name' | 'cpu' | 'ram' | 'disk' | 'temperature'): void {
    this.sortDir = (this.sortCol === col) ? (this.sortDir === 1 ? -1 : 1) : -1;
    this.sortCol = col;
  }

  // ── Detail view ──────────────────────────────────────────
  openDevice(device: LaptopStatus): void {
    this.selectedDevice = device;
    this.activeTab = 'overview';
  }

  closeDevice(): void {
    this.selectedDevice = null;
  }

  get selectedHistory(): MetricPoint[] {
    if (!this.selectedDevice) return [];
    const key = this.selectedDevice.equipmentId || this.selectedDevice.serialNumber;
    return this.historyMap.get(key) ?? [];
  }

  get cpuHistory(): number[]      { return this.selectedHistory.map(p => p.cpu); }
  get ramHistory(): number[]      { return this.selectedHistory.map(p => p.ram); }
  get diskHistory(): number[]     { return this.selectedHistory.map(p => p.disk); }
  get processHistory(): number[]  { return this.selectedHistory.map(p => p.processes); }
  get networkInHistory(): number[] { return this.selectedHistory.map(p => p.netIn / 1024); } // KB/s
  get networkOutHistory(): number[] { return this.selectedHistory.map(p => p.netOut / 1024); } // KB/s
  get temperatureHistory(): number[] { return this.selectedHistory.map(p => p.temperature); }

  get deviceDisks(): { name: string; percent: number }[] {
    if (!this.selectedDevice || !this.selectedDevice.diskVolumes) return [];
    return Object.entries(this.selectedDevice.diskVolumes)
      .map(([name, percent]) => ({ name, percent }))
      .sort((a, b) => a.name.localeCompare(b.name));
  }

  get currentProcesses(): number {
    const h = this.selectedHistory;
    return h.length > 0 ? h[h.length - 1].processes : 0;
  }

  // ── SVG Chart builder ────────────────────────────────────
  buildLinePath(values: number[], w = 600, h = 160, padY = 10): string {
    if (values.length < 2) return '';
    const min = Math.max(0, Math.min(...values) - padY);
    const max = Math.min(100, Math.max(...values) + padY);
    const range = Math.max(max - min, 1);
    const xStep = w / (values.length - 1);
    return values.map((v, i) => {
      const x = i * xStep;
      const y = h - ((v - min) / range) * h;
      return `${i === 0 ? 'M' : 'L'} ${x.toFixed(1)} ${y.toFixed(1)}`;
    }).join(' ');
  }

  buildAreaPath(values: number[], w = 600, h = 160, padY = 10): string {
    if (values.length < 2) return '';
    const min = Math.max(0, Math.min(...values) - padY);
    const max = Math.min(100, Math.max(...values) + padY);
    const range = Math.max(max - min, 1);
    const xStep = w / (values.length - 1);
    const pts = values.map((v, i) => ({
      x: i * xStep,
      y: h - ((v - min) / range) * h
    }));
    const line = pts.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' ');
    return `${line} L ${pts[pts.length - 1].x.toFixed(1)} ${h} L 0 ${h} Z`;
  }

  /** For processes chart – values are raw counts, not percentages */
  buildProcessPath(values: number[], w = 600, h = 160): string {
    if (values.length < 2) return '';
    const min = Math.min(...values) - 5;
    const max = Math.max(...values) + 5;
    const range = Math.max(max - min, 1);
    const xStep = w / (values.length - 1);
    return values.map((v, i) => {
      const x = i * xStep;
      const y = h - ((v - min) / range) * h;
      return `${i === 0 ? 'M' : 'L'} ${x.toFixed(1)} ${y.toFixed(1)}`;
    }).join(' ');
  }

  buildProcessAreaPath(values: number[], w = 600, h = 160): string {
    if (values.length < 2) return '';
    const min = Math.min(...values) - 5;
    const max = Math.max(...values) + 5;
    const range = Math.max(max - min, 1);
    const xStep = w / (values.length - 1);
    const pts = values.map((v, i) => ({
      x: i * xStep,
      y: h - ((v - min) / range) * h
    }));
    const line = pts.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' ');
    return `${line} L ${pts[pts.length - 1].x.toFixed(1)} ${h} L 0 ${h} Z`;
  }

  buildStackedAreaPath(baseValues: number[], topValues: number[], w = 600, h = 160, padY = 10, isTop = false): string {
    if (baseValues.length < 2 || topValues.length < 2) return '';
    const totalValues = baseValues.map((v, i) => v + topValues[i]);
    const max = Math.max(...totalValues) + padY;
    const range = Math.max(max, 1);
    const xStep = w / (baseValues.length - 1);

    if (!isTop) {
      // Bottom layer (e.g. Incoming)
      const pts = baseValues.map((v, i) => ({
        x: i * xStep,
        y: h - (v / range) * h
      }));
      const line = pts.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' ');
      return `${line} L ${w} ${h} L 0 ${h} Z`;
    } else {
      // Top layer (e.g. Outgoing), sits on top of bottom layer
      const topPts = totalValues.map((v, i) => ({
        x: i * xStep,
        y: h - (v / range) * h
      }));
      const bottomPts = baseValues.map((v, i) => ({
        x: i * xStep,
        y: h - (v / range) * h
      }));
      const topLine = topPts.map((p, i) => `${i === 0 ? 'M' : 'L'} ${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' ');
      const bottomLineRev = bottomPts.reverse().map((p, i) => `L ${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' ');
      return `${topLine} ${bottomLineRev} Z`;
    }
  }

  buildStackedLinePath(baseValues: number[], topValues: number[], w = 600, h = 160, padY = 10, isTop = false): string {
    if (baseValues.length < 2 || topValues.length < 2) return '';
    const totalValues = baseValues.map((v, i) => v + topValues[i]);
    const max = Math.max(...totalValues) + padY;
    const range = Math.max(max, 1);
    const xStep = w / (baseValues.length - 1);

    const targetValues = isTop ? totalValues : baseValues;
    return targetValues.map((v, i) => {
      const x = i * xStep;
      const y = h - (v / range) * h;
      return `${i === 0 ? 'M' : 'L'} ${x.toFixed(1)} ${y.toFixed(1)}`;
    }).join(' ');
  }

  stackedYLabels(baseValues: number[], topValues: number[], steps = 5, padY = 10): number[] {
    if (baseValues.length === 0) return [];
    const totalValues = baseValues.map((v, i) => v + topValues[i]);
    const max = Math.max(...totalValues) + padY;
    const step = max / steps;
    return Array.from({length: steps + 1}, (_, i) => Math.round(max - i * step));
  }

  chartYLabels(values: number[], steps = 5, padY = 10): number[] {
    const min = Math.max(0, Math.min(...values) - padY);
    const max = Math.min(100, Math.max(...values) + padY);
    const step = (max - min) / steps;
    return Array.from({length: steps + 1}, (_, i) => Math.round(max - i * step));
  }

  processYLabels(values: number[], steps = 4): number[] {
    const min = Math.min(...values) - 5;
    const max = Math.max(...values) + 5;
    const step = (max - min) / steps;
    return Array.from({length: steps + 1}, (_, i) => Math.round(max - i * step));
  }

  timeLabels(points: MetricPoint[]): string[] {
    if (points.length < 2) return [];
    const count = 5;
    const labels: string[] = [];
    for (let i = 0; i < count; i++) {
      const idx = Math.floor(i * (points.length - 1) / (count - 1));
      labels.push(points[idx].time.toTimeString().slice(0, 8));
    }
    return labels;
  }

  // ── Status helpers ───────────────────────────────────────
  resolveStatus(d: LaptopStatus): string {
    return d.upStatus;
  }

  statusLabel(d: LaptopStatus): string {
    const s = this.resolveStatus(d);
    if (s === 'UP')      return 'Online';
    if (s === 'DOWN')    return 'Offline';
    return 'Not Found Yet';
  }

  statusCls(d: LaptopStatus): string {
    const s = this.resolveStatus(d);
    if (s === 'UP')      return 'badge-up';
    if (s === 'DOWN')    return 'badge-down';
    return 'badge-unknown';
  }

  // ── Color helpers ────────────────────────────────────────
  cpuColor(v: number): string {
    if (v >= 90) return '#ef4444'; if (v >= 70) return '#f59e0b'; return '#10b981';
  }
  ramColor(v: number): string {
    if (v >= 90) return '#ef4444'; if (v >= 70) return '#f59e0b'; return '#8b5cf6';
  }
  diskColor(v: number): string {
    if (v >= 85) return '#ef4444'; if (v >= 65) return '#f59e0b'; return '#3b82f6';
  }
  tempColor(v: number): string {
    if (v >= 80) return '#ef4444'; if (v >= 60) return '#f59e0b'; return '#10b981';
  }
  deptColor(name: string): string {
    const p = ['#3b82f6','#10b981','#f59e0b','#ef4444','#8b5cf6','#06b6d4','#ec4899','#14b8a6','#f97316','#6366f1'];
    let h = 0;
    for (let i = 0; i < name.length; i++) h = name.charCodeAt(i) + ((h << 5) - h);
    return p[Math.abs(h) % p.length];
  }

  trackByDevice(index: number, device: LaptopStatus): string {
    return device.equipmentId || device.serialNumber;
  }
}
