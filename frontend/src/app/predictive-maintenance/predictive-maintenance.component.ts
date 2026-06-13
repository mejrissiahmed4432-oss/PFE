import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';

export interface Prediction {
  id: string;
  equipmentId: string;
  serialNumber: string;
  equipmentName: string;
  department: string;
  riskLevel: string;
  riskScore: number;
  predictedIssues: string[];
  recommendedActions: string[];
  lastAnalyzedAt: string;
}

@Component({
  selector: 'app-predictive-maintenance',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './predictive-maintenance.component.html',
  styleUrls: ['./predictive-maintenance.component.css']
})
export class PredictiveMaintenanceComponent implements OnInit {
  predictions: Prediction[] = [];
  loading = true;
  selectedPrediction: Prediction | null = null;

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.fetchPredictions();
  }

  fetchPredictions() {
    this.loading = true;
    this.http.get<Prediction[]>('http://localhost:8000/api/monitoring/predictions')
      .subscribe({
        next: (data) => {
          this.predictions = data.sort((a, b) => b.riskScore - a.riskScore);
          this.loading = false;
        },
        error: (err) => {
          console.error('Failed to fetch predictions', err);
          this.loading = false;
        }
      });
  }

  openDetail(pred: Prediction) {
    this.selectedPrediction = pred;
  }

  closeDetail() {
    this.selectedPrediction = null;
  }

  getRiskClass(level: string): string {
    switch ((level || '').toUpperCase()) {
      case 'CRITICAL': return 'risk-critical';
      case 'HIGH':     return 'risk-high';
      case 'MEDIUM':   return 'risk-medium';
      default:         return 'risk-low';
    }
  }

  getScoreColor(score: number): string {
    if (score >= 86) return '#ef4444';
    if (score >= 61) return '#f97316';
    if (score >= 31) return '#f59e0b';
    return '#10b981';
  }

  formatDate(dateStr: string): string {
    if (!dateStr) return '—';
    return new Date(dateStr).toLocaleDateString('en-GB', {
      day: '2-digit', month: 'short', year: 'numeric'
    }) + ' ' + new Date(dateStr).toLocaleTimeString('en-GB', {
      hour: '2-digit', minute: '2-digit'
    });
  }
}
