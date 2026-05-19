import { Pipe, PipeTransform, OnDestroy } from '@angular/core';
import { TranslationService } from './translation.service';
import { Subscription } from 'rxjs';

@Pipe({
  name: 'translate',
  standalone: true,
  pure: false // Impure to automatically update when language changes
})
export class TranslatePipe implements PipeTransform, OnDestroy {
  private value: string = '';
  private lastKey: string = '';
  private sub: Subscription | null = null;

  constructor(private translationService: TranslationService) {}

  transform(key: string): string {
    if (!key) return key;

    // If key hasn't changed and we already subscribed, return current value
    if (this.lastKey === key && this.sub) {
      return this.value;
    }

    this.lastKey = key;
    this.value = this.translationService.translate(key);

    if (this.sub) {
      this.sub.unsubscribe();
    }

    this.sub = this.translationService.currentLang$.subscribe(() => {
      this.value = this.translationService.translate(key);
    });

    return this.value;
  }

  ngOnDestroy() {
    if (this.sub) {
      this.sub.unsubscribe();
    }
  }
}
