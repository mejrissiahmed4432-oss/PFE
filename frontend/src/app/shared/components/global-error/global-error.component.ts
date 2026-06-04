import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { GlobalErrorService, GlobalErrorState } from '../../../core/services/global-error.service';
import { Observable } from 'rxjs';
import { trigger, state, style, transition, animate } from '@angular/animations';

@Component({
  selector: 'app-global-error',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './global-error.component.html',
  styleUrls: ['./global-error.component.css'],
  animations: [
    trigger('fadeSlideInOut', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(-20px) scale(0.95)' }),
        animate('300ms cubic-bezier(0.4, 0.0, 0.2, 1)', style({ opacity: 1, transform: 'translateY(0) scale(1)' }))
      ]),
      transition(':leave', [
        animate('250ms cubic-bezier(0.4, 0.0, 0.2, 1)', style({ opacity: 0, transform: 'translateY(-20px) scale(0.95)' }))
      ])
    ]),
    trigger('backdropFade', [
      transition(':enter', [
        style({ opacity: 0 }),
        animate('300ms ease-out', style({ opacity: 1 }))
      ]),
      transition(':leave', [
        animate('250ms ease-in', style({ opacity: 0 }))
      ])
    ])
  ]
})
export class GlobalErrorComponent implements OnInit {
  errorState$: Observable<GlobalErrorState>;

  constructor(private globalErrorService: GlobalErrorService) {
    this.errorState$ = this.globalErrorService.errorState$;
  }

  ngOnInit(): void {}

  closeDialog(): void {
    this.globalErrorService.hideError();
  }
}
