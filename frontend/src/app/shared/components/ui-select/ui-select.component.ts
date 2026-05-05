import { Component, Input, Output, EventEmitter, forwardRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, FormsModule } from '@angular/forms';

export interface SelectOption {
  label: string;
  value: any;
}

@Component({
  selector: 'app-ui-select',
  standalone: true,
  imports: [CommonModule, FormsModule],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => UiSelectComponent),
      multi: true
    }
  ],
  template: `
    <div class="flex flex-col gap-1.5 w-full">
      <label *ngIf="label" [for]="id" class="block text-sm font-semibold text-gray-700">
        {{ label }} <span *ngIf="required" class="text-red-500">*</span>
      </label>
      <select
        [id]="id"
        [disabled]="disabled"
        [required]="required"
        [(ngModel)]="value"
        (ngModelChange)="onModelChange($event)"
        (blur)="onTouched()"
        class="block w-full rounded-lg border-gray-300 py-2 pl-3 pr-10 text-base focus:border-blue-500 focus:outline-none focus:ring-blue-500 sm:text-sm shadow-sm transition-colors disabled:bg-gray-50 disabled:text-gray-500"
        [ngClass]="{'border-red-300 text-red-900 focus:border-red-500 focus:ring-red-500': error}">
        <option *ngIf="placeholder" value="" disabled selected hidden>{{ placeholder }}</option>
        <option *ngFor="let option of options" [value]="option.value">{{ option.label }}</option>
      </select>
      <p *ngIf="error" class="text-xs text-red-600 mt-0.5">{{ error }}</p>
    </div>
  `,
  styles: ``
})
export class UiSelectComponent implements ControlValueAccessor {
  @Input() label: string = '';
  @Input() id: string = `select-${Math.random().toString(36).substring(2, 9)}`;
  @Input() placeholder: string = '';
  @Input() options: SelectOption[] = [];
  @Input() required: boolean = false;
  @Input() error: string = '';

  value: any = '';
  disabled: boolean = false;

  onChange: any = () => {};
  onTouched: any = () => {};

  writeValue(value: any): void {
    this.value = value;
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  onModelChange(value: any) {
    this.value = value;
    this.onChange(value);
  }
}
