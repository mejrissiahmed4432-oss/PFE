import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ProcurementService } from '../procurement/procurement.service';

@Component({
  selector: 'app-supplier-respond',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './supplier-respond.component.html',
  styleUrls: ['./supplier-respond.component.css']
})
export class SupplierRespondComponent implements OnInit {

  token: string | null = null;
  
  isLoading = true;
  errorMsg = '';
  isSuccess = false;
  isSubmitting = false;

  tokenData: any = null;
  requestData: any = null;
  objectKeys = Object.keys;

  // Form
  totalPrice: number | null = null;
  deliveryDays: number | null = null;
  notes: string = '';
  selectedFile: File | null = null;

  constructor(
    private route: ActivatedRoute,
    private procService: ProcurementService
  ) { }

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token');
    
    if (!this.token) {
      this.errorMsg = 'Invalid URL: No secure token provided.';
      this.isLoading = false;
      return;
    }

    this.procService.getPublicRequestByToken(this.token).subscribe({
      next: (res) => {
        this.tokenData = res.token;
        this.requestData = res.request;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Token Error', err);
        this.errorMsg = err.error?.error || 'This link has expired, is invalid, or has already been used.';
        this.isLoading = false;
      }
    });
  }

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    if (file) {
      if (file.type !== 'application/pdf') {
        alert('Please select a PDF file.');
        return;
      }
      this.selectedFile = file;
    }
  }

  submitQuotation(): void {
    if (!this.selectedFile || !this.totalPrice || !this.deliveryDays || !this.token) {
      alert('Please fill out all required fields and attach your PDF quotation.');
      return;
    }

    this.isSubmitting = true;
    const formData = new FormData();
    formData.append('token', this.token);
    formData.append('totalPrice', this.totalPrice.toString());
    formData.append('deliveryDays', this.deliveryDays.toString());
    formData.append('notes', this.notes);
    formData.append('file', this.selectedFile);

    this.procService.submitPublicResponse(formData).subscribe({
      next: () => {
        this.isSuccess = true;
        this.isSubmitting = false;
      },
      error: (err) => {
        console.error('Submit Error', err);
        alert(err.error?.error || 'Failed to submit quotation. Please try again or reply via email.');
        this.isSubmitting = false;
      }
    });
  }
}
