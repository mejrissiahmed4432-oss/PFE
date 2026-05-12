import { Component, OnInit, Input, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, catchError, map } from 'rxjs/operators';
import { ProcurementService } from '../procurement.service';
import { EquipmentRequest, EquipmentRequestItem, CatalogItem, ParsedItem, EquipmentSpecification } from '../procurement.models';

@Component({
  selector: 'app-equipment-request-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="max-w-7xl mx-auto flex flex-col lg:flex-row gap-6">
      
      <!-- LEFT PANEL: Search, AI & Suggestions -->
      <div class="flex-1 flex flex-col gap-6">
        
        <!-- Header -->
        <div class="bg-white/70 backdrop-blur-md border border-slate-200/60 rounded-2xl p-6 shadow-sm">
          <div class="flex items-center gap-4">
            <div class="w-12 h-12 rounded-xl flex items-center justify-center bg-indigo-50 text-indigo-600 shrink-0">
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="12" y1="18" x2="12" y2="12"/><line x1="9" y1="15" x2="15" y2="15"/></svg>
            </div>
            <div>
              <h2 class="text-lg font-bold text-slate-800 m-0">Smart Request Builder</h2>
              <p class="text-sm text-slate-500 mt-1">Use AI or search the catalog to add items.</p>
            </div>
          </div>
        </div>

        <!-- AI Smart Assistant Card (Premium Redesign) -->
        <div class="bg-white border border-blue-100 rounded-3xl shadow-sm relative z-20">
          <!-- Header Section -->
          <div class="p-6 pb-4 flex items-center justify-between">
            <div class="flex items-center gap-4">
              <div class="w-12 h-12 rounded-2xl bg-blue-500 flex items-center justify-center text-white shadow-lg shadow-blue-500/20">
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24">
                  <path d="m12 3-1.912 5.813a2 2 0 0 1-1.275 1.275L3 12l5.813 1.912a2 2 0 0 1 1.275 1.275L12 21l1.912-5.813a2 2 0 0 1 1.275-1.275L21 12l-5.813-1.912a2 2 0 0 1-1.275-1.275L12 3Z"/>
                </svg>
              </div>
              <div>
                <h3 class="text-lg font-bold text-slate-800">AI Smart Assistant</h3>
                <span class="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-blue-100 text-blue-600 uppercase tracking-wider mt-1">
                  AI-POWERED
                </span>
              </div>
            </div>
            <button class="text-slate-400">
              <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><polyline points="18 15 12 9 6 15"/></svg>
            </button>
          </div>

          <!-- Body Section (Light Blue) -->
          <div class="bg-blue-50/50 p-6 pt-2 border-t border-blue-50 rounded-b-3xl">
            <p class="text-sm text-slate-600 mb-4 leading-relaxed">
              Our AI will analyze your equipment needs to generate a customized procurement request, ensuring all specifications are precisely captured.
            </p>
            
            <div class="relative">
              <textarea [(ngModel)]="aiText" (ngModelChange)="onAiInput()" rows="3" 
                        placeholder="Type your request (e.g. 'I need 3 laptops with 16GB RAM and 2 monitors')" 
                        class="w-full px-4 py-4 bg-white border border-blue-100 rounded-2xl text-sm text-slate-800 focus:outline-none focus:ring-4 focus:ring-blue-500/10 focus:border-blue-400 transition-all resize-none shadow-sm"></textarea>
              
              <!-- Autocomplete Dropdown -->
              <div *ngIf="showAutocomplete && autocompleteResults.length > 0" 
                   class="absolute z-50 w-full mt-2 bg-white border border-blue-100 rounded-2xl shadow-xl max-h-60 overflow-y-auto overflow-x-hidden">
                <div *ngFor="let res of autocompleteResults" 
                     class="p-4 hover:bg-blue-50 cursor-pointer border-b border-slate-50 last:border-0 flex flex-col gap-1 transition-colors" 
                     (click)="selectAutocomplete(res)">
                  <div class="font-bold text-sm text-slate-800">{{ res.name }}</div>
                  <div class="text-[10px] font-bold text-blue-500 uppercase tracking-widest">{{ res.category }}</div>
                </div>
              </div>
              
              <div class="absolute bottom-4 right-4 flex gap-2">
                <button class="flex items-center justify-center px-4 py-2.5 bg-slate-100 text-slate-600 hover:bg-slate-200 rounded-xl transition-all font-bold text-xs gap-2"
                        (click)="addCurrentTextToCart()" [disabled]="isParsing || !aiText.trim()">
                  <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                  Add Item
                </button>
                <button class="flex items-center justify-center px-4 py-2.5 bg-blue-600 text-white hover:bg-blue-700 rounded-xl transition-all shadow-lg shadow-blue-600/20 disabled:opacity-50 font-bold text-xs gap-2"
                        (click)="parseWithAI(aiText)" [disabled]="isParsing || !aiText.trim()">
                  <div *ngIf="isParsing" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                  <svg *ngIf="!isParsing" xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" viewBox="0 0 24 24"><polygon points="5 3 19 12 5 21 5 3"/></svg>
                  {{ isParsing ? 'Parsing...' : 'Process with AI' }}
                </button>
              </div>
            </div>
          </div>
          
          <!-- Specifications Panel (Interactive) -->
          <div *ngIf="showSpecsPanel && activeCatalogItem" class="mt-4 p-5 bg-white border-2 border-purple-200 rounded-xl shadow-sm">
            <div class="flex justify-between items-start mb-4">
              <div>
                <h4 class="text-sm font-bold text-slate-800">Configure: {{ activeCatalogItem.name }}</h4>
                <p class="text-xs text-slate-500">Select specifications before adding to cart</p>
              </div>
              <button class="text-slate-400 hover:text-slate-600" (click)="cancelSpecsSelection()">
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
            
            <div class="flex flex-col gap-4 mb-4">
              <div *ngFor="let spec of activeSpecs">
                <label class="block text-xs font-bold text-slate-600 mb-2">{{ spec.name }}</label>
                <div class="flex flex-wrap gap-2">
                  <button *ngFor="let val of spec.possibleValues" 
                          class="px-3 py-1.5 border rounded-lg text-xs font-semibold transition-colors"
                          [class.border-purple-500]="selectedSpecsForActiveItem[spec.name] === val"
                          [class.bg-purple-50]="selectedSpecsForActiveItem[spec.name] === val"
                          [class.text-purple-700]="selectedSpecsForActiveItem[spec.name] === val"
                          [class.border-slate-200]="selectedSpecsForActiveItem[spec.name] !== val"
                          [class.text-slate-600]="selectedSpecsForActiveItem[spec.name] !== val"
                          [class.hover:border-purple-300]="selectedSpecsForActiveItem[spec.name] !== val"
                          (click)="selectSpecValue(spec.name, val)">
                    {{ val }}
                  </button>
                </div>
              </div>
            </div>
            
            <div class="flex justify-end">
              <button class="px-4 py-2 text-xs font-bold bg-purple-600 hover:bg-purple-700 text-white rounded-xl transition-colors shadow-sm" (click)="confirmSpecsAndAddToCart()">
                {{ editItemIndex !== null ? 'Update Item' : 'Add to Cart' }}
              </button>
            </div>
          </div>

          <!-- AI Parsed Results Confirmation -->
          <div *ngIf="parsedItems.length > 0 && !showSpecsPanel" class="mt-4 p-4 bg-purple-50/50 border border-purple-100 rounded-xl">
            <h4 class="text-xs font-bold text-purple-800 uppercase tracking-wider mb-3">Confirm AI Matches</h4>
            <div class="flex flex-col gap-2 mb-3">
              <div *ngFor="let p of parsedItems" class="flex flex-col p-3 bg-white rounded-lg border border-purple-100 text-sm">
                <div class="flex justify-between items-center">
                  <div class="flex items-center gap-2">
                    <span class="font-bold text-purple-700">{{ p.quantity }}x</span>
                    <span class="text-slate-700">{{ p.name }}</span>
                  </div>
                  <div class="text-xs text-slate-400 italic">Matched: {{ getBestMatchName(p.name) || 'Custom Item' }}</div>
                </div>
                <!-- Show detected specs -->
                <div *ngIf="p.detectedSpecs && objectKeys(p.detectedSpecs).length > 0" class="mt-2 flex flex-wrap gap-1">
                  <span *ngFor="let key of objectKeys(p.detectedSpecs)" class="px-2 py-0.5 bg-purple-100 text-purple-700 text-[10px] rounded-full font-semibold">
                    {{ key }}: {{ p.detectedSpecs[key] }}
                  </span>
                </div>
              </div>
            </div>
            <div class="flex justify-end gap-2">
              <button class="px-3 py-1.5 text-xs font-semibold text-slate-600 hover:bg-slate-200 rounded-lg transition-colors" (click)="parsedItems = []">Cancel</button>
              <button class="px-3 py-1.5 text-xs font-semibold bg-purple-600 hover:bg-purple-700 text-white rounded-lg transition-colors shadow-sm shadow-purple-600/20" (click)="addParsedToCart()">Add All to Cart</button>
            </div>
          </div>
        </div>




      </div>

      <!-- RIGHT PANEL: Cart -->
      <div class="lg:w-[400px] xl:w-[450px] shrink-0 flex flex-col gap-6">
        <div class="bg-white/70 backdrop-blur-md border border-slate-200/60 rounded-2xl shadow-sm flex flex-col h-full sticky top-6">
          <div class="p-6 border-b border-slate-100 flex justify-between items-center bg-slate-50/50 rounded-t-2xl">
            <h3 class="text-sm font-bold text-slate-800 uppercase tracking-widest">Your Request Cart</h3>
            <span class="px-2.5 py-1 bg-indigo-100 text-indigo-800 text-xs font-bold rounded-full">{{ totalItems }} items</span>
          </div>

          <div class="p-6 flex-1 overflow-y-auto">
            <div *ngIf="cart.length === 0" class="flex flex-col items-center justify-center h-40 text-slate-400">
              <svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1" class="mb-3 opacity-50" viewBox="0 0 24 24"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
              <p class="text-sm font-medium">Cart is empty</p>
              <p class="text-xs mt-1 text-slate-400">Search the catalog to add items</p>
            </div>

            <div class="flex flex-col gap-4">
              <div *ngFor="let item of cart; let i = index" class="p-4 bg-white border border-slate-200 rounded-xl relative group">
                <div class="flex justify-between items-start mb-2">
                  <div class="font-semibold text-sm text-slate-800 pr-6">{{ item.name }}</div>
                  <div class="absolute top-3 right-3 flex items-center gap-2">
                    <button class="text-slate-300 hover:text-indigo-500 transition-colors" (click)="editCartItem(i)" title="Edit Specifications">
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                    </button>
                    <button class="text-slate-300 hover:text-red-500 transition-colors" (click)="removeFromCart(i)" title="Remove Item">
                      <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                    </button>
                  </div>
                </div>
                
                <div class="flex items-start gap-3">
                  <div class="flex items-center border border-slate-200 rounded-lg bg-slate-50 mt-1">
                    <button class="w-8 h-8 flex items-center justify-center text-slate-500 hover:text-indigo-600 disabled:opacity-30" (click)="item.quantity = item.quantity - 1; saveCartToStorage()" [disabled]="item.quantity <= 1">-</button>
                    <div class="w-8 text-center text-sm font-bold text-slate-800">{{ item.quantity }}</div>
                    <button class="w-8 h-8 flex items-center justify-center text-slate-500 hover:text-indigo-600" (click)="item.quantity = item.quantity + 1; saveCartToStorage()">+</button>
                  </div>
                  <div class="flex-1 flex flex-col gap-2">
                    <!-- Specs displaying -->
                    <div *ngIf="item.selectedSpecs && objectKeys(item.selectedSpecs).length > 0" class="flex flex-wrap gap-1">
                      <span *ngFor="let key of objectKeys(item.selectedSpecs)" class="px-2 py-0.5 bg-slate-100 text-slate-600 border border-slate-200 text-[10px] rounded-md font-medium">
                        {{ key }}: {{ item.selectedSpecs[key] }}
                      </span>
                    </div>
                    <input type="text" [(ngModel)]="item.description" placeholder="Notes (optional)" 
                           class="w-full px-3 py-1.5 bg-transparent border-b border-slate-200 text-xs text-slate-600 focus:outline-none focus:border-indigo-500 transition-colors">
                  </div>
                </div>
              </div>
            </div>

            <div class="mt-6" *ngIf="cart.length > 0">
              <label class="block text-xs font-bold text-slate-400 uppercase tracking-widest mb-2">General Request Notes</label>
              <textarea [(ngModel)]="notes" (ngModelChange)="saveCartToStorage()" rows="2" placeholder="Urgency, project context..." 
                        class="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-sm text-slate-800 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-shadow resize-none"></textarea>
            </div>
          </div>

          <div class="p-6 border-t border-slate-100 bg-slate-50/50 rounded-b-2xl">
            <div *ngIf="error" class="mb-4 p-3 bg-red-50 text-red-700 text-xs font-semibold rounded-lg border border-red-200">{{ error }}</div>
            <div *ngIf="success" class="mb-4 p-3 bg-emerald-50 text-emerald-700 text-xs font-semibold rounded-lg border border-emerald-200 flex items-center gap-2">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><polyline points="20 6 9 17 4 12"/></svg> Request submitted successfully!
            </div>
            
            <button class="w-full flex items-center justify-center gap-2 px-6 py-3.5 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-bold rounded-xl shadow-md shadow-indigo-600/20 transition-all hover:-translate-y-0.5 disabled:opacity-60 disabled:pointer-events-none disabled:hover:translate-y-0" 
                    (click)="submit()" [disabled]="isSubmitting || cart.length === 0">
              <span *ngIf="isSubmitting" class="w-5 h-5 border-2 border-white/40 border-t-white rounded-full animate-spin"></span>
              <svg *ngIf="!isSubmitting" xmlns="http://www.w3.org/2000/svg" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
              {{ isSubmitting ? 'Sending Request...' : 'Submit Request' }}
            </button>
          </div>
        </div>
      </div>

    </div>
  `
})
export class EquipmentRequestFormComponent implements OnInit, OnDestroy {
  @Input() userId: string = '';
  @Input() userName: string = '';

  objectKeys = Object.keys;

  // Data
  catalog: CatalogItem[] = [];
  filteredCatalog: CatalogItem[] = [];
  
  // Cart
  cart: EquipmentRequestItem[] = [];
  notes: string = '';
  
  // UI State
  aiText: string = '';
  isParsing = false;
  parsedItems: ParsedItem[] = [];
  
  isSubmitting = false;
  error = '';
  success = false;

  editItemIndex: number | null = null;

  // --- New Real-Time AI & Specs Features ---
  aiInputSubject = new Subject<string>();
  aiSub!: Subscription;
  autocompleteResults: CatalogItem[] = [];
  showAutocomplete = false;



  activeSpecs: EquipmentSpecification[] = [];
  selectedSpecsForActiveItem: Record<string, string> = {};
  activeCatalogItem: CatalogItem | null = null;
  activeItemQty: number = 1;
  showSpecsPanel = false;



  constructor(private procService: ProcurementService) {}

  ngOnInit(): void {
    this.loadCatalog();
    this.loadCartFromStorage();

    // Debounce AI Input for autocomplete & parsing
    this.aiSub = this.aiInputSubject.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap(text => {
        if (!text.trim()) return of({ text, aiSpecs: [] });
        return this.procService.autocompleteSpecsWithAI(text).pipe(
          map(aiSpecs => ({ text, aiSpecs })),
          catchError(() => of({ text, aiSpecs: [] }))
        );
      })
    ).subscribe(({ text, aiSpecs }) => {
      this.handleAIInputResults(text, aiSpecs);
    });

  }

  ngOnDestroy(): void {
    if (this.aiSub) this.aiSub.unsubscribe();
  }

  onAiInput(): void {
    this.aiInputSubject.next(this.aiText);
  }

  handleAIInputResults(text: string, aiSpecs: string[]): void {
    if (!text.trim() || this.isParsing) {
      this.autocompleteResults = [];
      this.showAutocomplete = false;
      return;
    }
    
    const q = text.toLowerCase();
    
    // Find matching names
    const nameMatches = this.catalog.filter(c => c.name.toLowerCase().includes(q));
    
    // Dynamic AI Spec Suggestions
    const specSuggestions: CatalogItem[] = aiSpecs.map(spec => ({
      name: spec,
      category: 'AI Suggestion'
    } as CatalogItem));
    
    // Find matching categories to suggest as a general word completion
    const categoryMatches = Array.from(new Set(this.catalog.map(c => c.category)))
      .filter(cat => cat.toLowerCase().includes(q))
      .map(cat => ({ name: cat, category: 'Category Suggestion' } as CatalogItem));
      
    this.autocompleteResults = [...categoryMatches, ...specSuggestions, ...nameMatches].slice(0, 8);
    this.showAutocomplete = this.autocompleteResults.length > 0;
  }

  selectAutocomplete(item: CatalogItem): void {
    this.showAutocomplete = false;
    
    if (item.category === 'Category Suggestion' || item.category.includes('Suggestion')) {
      // Complete the word/spec in the input
      this.aiText = this.aiText.trim() + ' ' + item.name + ' ';
      return;
    }
    
    this.aiText = '';
    // Show specs panel before adding to cart
    this.openSpecsPanel(item);
  }

  onManualInput(): void {
    // Deprecated manual entry
  }

  handleManualInputResults(text: string, aiSpecs: string[]): void {
    // Deprecated manual entry
  }

  openSpecsPanel(item: CatalogItem, qty: number = 1, editIndex: number | null = null): void {
    if (!item.id) {
      // If still no ID, we can't show specs panel
      if (editIndex !== null) {
        // Just let them edit quantity in cart directly (handled by UI)
        // Or we could show a simple edit modal, but user wants specs
        alert("This item is not in the catalog, so specifications cannot be configured.");
      } else {
        this.addToCartDirectly(item, qty);
      }
      return;
    }

    this.activeCatalogItem = item;
    this.activeItemQty = qty;
    this.selectedSpecsForActiveItem = {};
    this.editItemIndex = editIndex;
    
    // If editing, pre-populate with existing specs
    if (editIndex !== null && this.cart[editIndex].selectedSpecs) {
      this.selectedSpecsForActiveItem = { ...this.cart[editIndex].selectedSpecs };
    }
    
    this.procService.getSpecifications(item.id).subscribe(specs => {
      if (specs && specs.length > 0) {
        this.activeSpecs = specs;
        // Pre-select values if not already set (not editing or missing spec)
        specs.forEach(s => {
          if (!this.selectedSpecsForActiveItem[s.name] && s.possibleValues?.length > 0) {
            this.selectedSpecsForActiveItem[s.name] = s.possibleValues[0];
          }
        });
        this.showSpecsPanel = true;
      } else {
        // No specs to configure
        if (editIndex !== null) {
          this.cart[editIndex].quantity = qty;
        } else {
          this.addToCartDirectly(item, qty);
        }
        alert("No configurable specifications found for this item.");
      }
    });
  }

  addToCartDirectly(catItem: CatalogItem, qty: number = 1): void {
    const existingIndex = this.cart.findIndex(i => i.catalogItemId === catItem.id);
    if (existingIndex >= 0) {
      this.cart[existingIndex].quantity += qty;
    } else {
      this.cart.unshift({
        name: catItem.name,
        quantity: qty,
        description: '',
        catalogItemId: catItem.id,
        selectedSpecs: {}
      });
    }
    this.saveCartToStorage();
  }

  cancelSpecsSelection(): void {
    this.showSpecsPanel = false;
    this.activeCatalogItem = null;
    this.activeSpecs = [];
    this.editItemIndex = null;
  }

  confirmSpecsAndAddToCart(): void {
    if (!this.activeCatalogItem) return;
    
    const specsCopy = { ...this.selectedSpecsForActiveItem };
    
    if (this.editItemIndex !== null) {
      // Update existing item
      this.cart[this.editItemIndex].selectedSpecs = specsCopy;
      this.cart[this.editItemIndex].quantity = this.activeItemQty;
      // Trigger change detection by spreading the array
      this.cart = [...this.cart];
    } else {
      // Check if identical item+specs already in cart
      const existingIndex = this.cart.findIndex(i => 
        i.catalogItemId === this.activeCatalogItem!.id && 
        JSON.stringify(i.selectedSpecs || {}) === JSON.stringify(specsCopy)
      );

      if (existingIndex >= 0) {
        this.cart[existingIndex].quantity += this.activeItemQty;
      } else {
        this.cart.unshift({
          name: this.activeCatalogItem.name,
          quantity: this.activeItemQty,
          description: '',
          catalogItemId: this.activeCatalogItem.id,
          selectedSpecs: specsCopy
        });
      }
    }

    this.cancelSpecsSelection();
    this.aiText = ''; // Clear AI text after successful add
    this.saveCartToStorage();
  }

  selectSpecValue(specName: string, value: string): void {
    this.selectedSpecsForActiveItem[specName] = value;
    
    // Auto-save if editing
    if (this.editItemIndex !== null) {
      this.cart[this.editItemIndex].selectedSpecs = { ...this.selectedSpecsForActiveItem };
      // Deep copy to trigger change detection
      this.cart = [...this.cart];
      this.saveCartToStorage();
    }
  }

  get totalItems(): number {
    return this.cart.reduce((sum, item) => sum + item.quantity, 0);
  }

  loadCatalog(): void {
    this.procService.getCatalog().subscribe({
      next: (data) => {
        this.catalog = data;
        this.filteredCatalog = data;
      },
      error: (err) => console.error('Failed to load catalog', err)
    });
  }

  addToCart(catItem: CatalogItem): void {
    this.openSpecsPanel(catItem); // Intercept add to cart to show specs
  }

  addCurrentTextToCart(): void {
    if (!this.aiText.trim()) return;
    this.cart.unshift({
      name: this.aiText.trim(),
      quantity: 1,
      description: 'Manual Addition'
    });
    this.aiText = '';
    this.showAutocomplete = false;
    this.saveCartToStorage();
  }

  removeFromCart(index: number): void {
    this.cart.splice(index, 1);
    this.saveCartToStorage();
  }

  editCartItem(index: number): void {
    const item = this.cart[index];
    let catalogId = item.catalogItemId;

    // If no catalog ID, try to find a match now
    if (!catalogId) {
      const match = this.getBestMatch(item.name);
      if (match) {
        catalogId = match.id;
        // Update the cart item with the found ID so it's linked now
        this.cart[index].catalogItemId = match.id;
      }
    }

    const catItem: CatalogItem = {
      id: catalogId,
      name: item.name,
      category: 'Cart Item'
    };
    this.openSpecsPanel(catItem, item.quantity, index);
  }

  // --- AI Parsing ---

  parseWithAI(text: string = this.aiText): void {
    if (!text.trim()) return;
    this.showAutocomplete = false;
    this.isParsing = true;
    this.parsedItems = [];
    
    this.procService.parseRequestWithAI(text).subscribe({
      next: (res) => {
        this.isParsing = false;
        if (res.success && res.items.length > 0) {
          this.parsedItems = res.items;
        } else {
          // Fallback: If AI couldn't parse it but user clicked send, add as a custom item
          this.cart.unshift({
            name: text.trim(),
            quantity: 1,
            description: 'Added via AI Smart Assistant'
          });
          this.aiText = '';
        }
      },
      error: (err) => {
        // Fallback on error too
        this.cart.unshift({
          name: text.trim(),
          quantity: 1,
          description: 'Added via AI Smart Assistant'
        });
        this.aiText = '';
        this.isParsing = false;
        this.saveCartToStorage();
        this.error = 'AI Parsing failed.';
        setTimeout(() => this.error = '', 3000);
      }
    });
  }

  getBestMatchName(parsedName: string): string | null {
    const q = parsedName.toLowerCase();
    // Only match by name, not broadly by category, to avoid forcing wrong models
    const match = this.catalog.find(c => c.name.toLowerCase().includes(q));
    return match ? match.name : null;
  }

  getBestMatch(parsedName: string): CatalogItem | undefined {
    const q = parsedName.toLowerCase();
    return this.catalog.find(c => c.name.toLowerCase().includes(q));
  }

  addParsedToCart(): void {
    this.parsedItems.forEach(pi => {
      const match = this.getBestMatch(pi.name);
      if (match) {
        // Add catalog item
        const existing = this.cart.find(i => i.catalogItemId === match.id);
        if (existing) {
          existing.quantity += pi.quantity;
        } else {
          this.cart.unshift({
            name: match.name,
            quantity: pi.quantity,
            description: '',
            catalogItemId: match.id,
            selectedSpecs: pi.detectedSpecs || {}
          });
        }
      } else {
        // Add as custom item
        this.cart.unshift({
          name: pi.name,
          quantity: pi.quantity,
          description: pi.inferredCategory ? 'Category: ' + pi.inferredCategory : '',
          selectedSpecs: pi.detectedSpecs || {}
        });
      }
    });
    
    this.parsedItems = [];
    this.aiText = '';
    this.saveCartToStorage();
  }

  // --- Smart Suggestions ---

  saveCartToStorage(): void {
    localStorage.setItem('procurement_cart', JSON.stringify(this.cart));
    localStorage.setItem('procurement_notes', this.notes);
  }

  loadCartFromStorage(): void {
    const savedCart = localStorage.getItem('procurement_cart');
    const savedNotes = localStorage.getItem('procurement_notes');
    if (savedCart) {
      this.cart = JSON.parse(savedCart);
    }
    if (savedNotes) {
      this.notes = savedNotes;
    }
  }



  // --- Submission ---

  submit(): void {
    this.error = '';
    this.success = false;

    if (this.cart.length === 0) {
      this.error = 'Your cart is empty.';
      return;
    }

    const request: EquipmentRequest = {
      createdByUserId: this.userId,
      createdByName: this.userName,
      items: [...this.cart],
      notes: this.notes
    };

    this.isSubmitting = true;
    this.procService.createRequest(request).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.success = true;
        
        // Reset
        this.cart = [];
        this.notes = '';
        this.saveCartToStorage();
        
        setTimeout(() => this.success = false, 4000);
      },
      error: (err: any) => {
        this.isSubmitting = false;
        this.error = 'Failed to submit request. Please try again.';
      }
    });
  }
}
