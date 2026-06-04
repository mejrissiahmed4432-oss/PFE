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

  constructor(private http: HttpClient) {}

  ngOnInit() {
    this.fetchPredictions();
  }

  fetchPredictions() {
    this.http.get<Prediction[]>('http://localhost:8000/api/monitoring/predictions')
      .subscribe({
        next: (data) => {
          this.predictions = data.sort((a, b) => b.riskScore - a.riskScore); // Sort by highest risk
          this.loading = false;
        },
        error: (err) => {
          console.error('Failed to fetch predictions', err);
          this.loading = false;
        }
      });
  }
}
