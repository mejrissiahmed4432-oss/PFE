import { Component, OnInit, Input, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, Subscription, of } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap, catchError } from 'rxjs/operators';
import { ProcurementService } from '../procurement.service';
import { EquipmentRequest, EquipmentRequestItem, CatalogItem, ParsedItem, EquipmentSpecification } from '../procurement.models';

@Component({
  selector: 'app-equipment-request-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './equipment-request-form.component.html',
  styleUrls: ['./equipment-request-form.component.css']
})
export class EquipmentRequestFormComponent implements OnInit, OnDestroy {
  @Input() userId: string = '';
  @Input() userName: string = '';
  @Input() userRole: string = '';

  objectKeys = Object.keys;

  catalog: CatalogItem[] = [];
  filteredCatalog: CatalogItem[] = [];
  cart: EquipmentRequestItem[] = [];
  notes: string = '';
  aiText: string = '';
  isParsing = false;
  parsedItems: ParsedItem[] = [];
  isSubmitting = false;
  error = '';
  success = false;
  editItemIndex: number | null = null;
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

    this.aiSub = this.aiInputSubject.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      switchMap(text => {
        if (!text.trim() || text.length < 3) return of([]);
        return this.procService.autocompleteSpecsWithAI(text).pipe(
          catchError(() => of([]))
        );
      })
    ).subscribe(aiSpecs => {
      this.handleAIInputResults(this.aiText, aiSpecs);
    });
  }

  ngOnDestroy(): void {
    if (this.aiSub) this.aiSub.unsubscribe();
  }

  onAiInput(): void {
    this.handleAIInputResults(this.aiText, []);
    this.aiInputSubject.next(this.aiText);
  }

  handleAIInputResults(text: string, aiSpecs: string[]): void {
    if (!text.trim() || this.isParsing) {
      this.autocompleteResults = [];
      this.showAutocomplete = false;
      return;
    }
    const q = text.toLowerCase();
    const nameMatches = this.catalog.filter(c => 
      c.name.toLowerCase().includes(q) || 
      c.category.toLowerCase().includes(q)
    );
    const specSuggestions: CatalogItem[] = aiSpecs
      .filter(spec => !nameMatches.some(m => m.name.toLowerCase() === spec.toLowerCase()))
      .map(spec => ({ name: spec, category: 'AI Suggestion' } as CatalogItem));
    const categoryMatches = Array.from(new Set(this.catalog.map(c => c.category)))
      .filter(cat => cat.toLowerCase().includes(q))
      .filter(cat => !nameMatches.some(m => m.category === cat))
      .map(cat => ({ name: cat, category: 'Category Suggestion' } as CatalogItem));
    this.autocompleteResults = [...nameMatches, ...specSuggestions, ...categoryMatches].slice(0, 10);
    this.showAutocomplete = this.autocompleteResults.length > 0;
  }

  selectAutocomplete(item: CatalogItem): void {
    this.showAutocomplete = false;
    if (item.category === 'Category Suggestion' || item.category.includes('Suggestion')) {
      this.aiText = this.aiText.trim() + ' ' + item.name + ' ';
      return;
    }
    this.aiText = '';
    this.openSpecsPanel(item);
  }

  openSpecsPanel(item: CatalogItem, qty: number = 1, editIndex: number | null = null): void {
    if (!item.id) {
      if (editIndex !== null) alert("Node manifest missing catalog ID. Cannot configure specs.");
      else this.addToCartDirectly(item, qty);
      return;
    }
    this.activeCatalogItem = item;
    this.activeItemQty = qty;
    this.selectedSpecsForActiveItem = {};
    this.editItemIndex = editIndex;
    if (editIndex !== null && this.cart[editIndex].selectedSpecs) this.selectedSpecsForActiveItem = { ...this.cart[editIndex].selectedSpecs };
    this.procService.getSpecifications(item.id).subscribe(specs => {
      if (specs && specs.length > 0) {
        this.activeSpecs = specs;
        specs.forEach(s => {
          if (!this.selectedSpecsForActiveItem[s.name] && s.possibleValues?.length > 0) this.selectedSpecsForActiveItem[s.name] = s.possibleValues[0];
        });
        this.showSpecsPanel = true;
      } else {
        if (editIndex !== null) this.cart[editIndex].quantity = qty;
        else this.addToCartDirectly(item, qty);
      }
    });
  }

  addToCartDirectly(catItem: CatalogItem, qty: number = 1): void {
    const existingIndex = this.cart.findIndex(i => i.catalogItemId === catItem.id);
    if (existingIndex >= 0) this.cart[existingIndex].quantity += qty;
    else this.cart.unshift({ name: catItem.name, quantity: qty, description: '', catalogItemId: catItem.id, selectedSpecs: {} });
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
      this.cart[this.editItemIndex].selectedSpecs = specsCopy;
      this.cart[this.editItemIndex].quantity = this.activeItemQty;
      this.cart = [...this.cart];
    } else {
      const existingIndex = this.cart.findIndex(i => i.catalogItemId === this.activeCatalogItem!.id && JSON.stringify(i.selectedSpecs || {}) === JSON.stringify(specsCopy));
      if (existingIndex >= 0) this.cart[existingIndex].quantity += this.activeItemQty;
      else this.cart.unshift({ name: this.activeCatalogItem.name, quantity: this.activeItemQty, description: '', catalogItemId: this.activeCatalogItem.id, selectedSpecs: specsCopy });
    }
    this.cancelSpecsSelection();
    this.aiText = '';
    this.saveCartToStorage();
  }

  selectSpecValue(specName: string, value: string): void {
    this.selectedSpecsForActiveItem[specName] = value;
    if (this.editItemIndex !== null) {
      this.cart[this.editItemIndex].selectedSpecs = { ...this.selectedSpecsForActiveItem };
      this.cart = [...this.cart];
      this.saveCartToStorage();
    }
  }

  get totalItems(): number { return this.cart.reduce((sum, item) => sum + item.quantity, 0); }

  loadCatalog(): void {
    this.procService.getCatalog().subscribe({
      next: (data) => { this.catalog = data; this.filteredCatalog = data; },
      error: (err) => console.error('Catalog node unreachable', err)
    });
  }

  addCurrentTextToCart(): void {
    if (!this.aiText.trim()) return;
    this.cart.unshift({ name: this.aiText.trim(), quantity: 1, description: 'Manual Addition' });
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
    if (!catalogId) {
      const match = this.getBestMatch(item.name);
      if (match) { catalogId = match.id; this.cart[index].catalogItemId = match.id; }
    }
    const catItem: CatalogItem = { id: catalogId, name: item.name, category: 'Registry Item' };
    this.openSpecsPanel(catItem, item.quantity, index);
  }

  parseWithAI(text: string = this.aiText): void {
    if (!text.trim()) return;
    this.showAutocomplete = false;
    this.isParsing = true;
    this.parsedItems = [];
    this.procService.parseRequestWithAI(text).subscribe({
      next: (res) => {
        this.isParsing = false;
        if (res.success && res.items.length > 0) this.parsedItems = res.items;
        else { this.cart.unshift({ name: text.trim(), quantity: 1, description: 'Neural Engine Fallback' }); this.aiText = ''; }
      },
      error: () => {
        this.cart.unshift({ name: text.trim(), quantity: 1, description: 'Neural Engine Fallback' });
        this.aiText = '';
        this.isParsing = false;
        this.saveCartToStorage();
        this.error = 'Neural Parsing failure.';
        setTimeout(() => this.error = '', 3000);
      }
    });
  }

  getBestMatchName(parsedName: string): string | null {
    const q = parsedName.toLowerCase();
    const match = this.catalog.find(c => c.name.toLowerCase().includes(q));
    return match ? match.name : null;
  }

  getBestMatch(parsedName: string): CatalogItem | undefined {
    const q = parsedName.toLowerCase();
    return this.catalog.find(c => c.name.toLowerCase().includes(q));
  }

  resetAIBuffer(): void {
    this.parsedItems = [];
    this.aiText = '';
    this.showAutocomplete = false;
  }

  addParsedToCart(): void {
    this.parsedItems.forEach(pi => {
      const match = this.getBestMatch(pi.name);
      if (match) {
        const existing = this.cart.find(i => i.catalogItemId === match.id);
        if (existing) existing.quantity += pi.quantity;
        else this.cart.unshift({ name: match.name, quantity: pi.quantity, description: '', catalogItemId: match.id, selectedSpecs: pi.detectedSpecs || {} });
      } else {
        this.cart.unshift({ name: pi.name, quantity: pi.quantity, description: pi.inferredCategory || '', selectedSpecs: pi.detectedSpecs || {} });
      }
    });
    this.parsedItems = [];
    this.aiText = '';
    this.saveCartToStorage();
  }

  saveCartToStorage(): void {
    localStorage.setItem('procurement_cart', JSON.stringify(this.cart));
    localStorage.setItem('procurement_notes', this.notes);
  }

  loadCartFromStorage(): void {
    const savedCart = localStorage.getItem('procurement_cart');
    const savedNotes = localStorage.getItem('procurement_notes');
    if (savedCart) this.cart = JSON.parse(savedCart);
    if (savedNotes) this.notes = savedNotes;
  }

  submit(): void {
    this.error = '';
    this.success = false;
    if (this.cart.length === 0) { this.error = 'Deployment registry empty.'; return; }
    const request: EquipmentRequest = { 
      createdByUserId: this.userId, 
      createdByName: this.userName, 
      createdByRole: this.userRole,
      items: [...this.cart], 
      notes: this.notes 
    };
    this.isSubmitting = true;
    this.procService.createRequest(request).subscribe({
      next: () => {
        this.isSubmitting = false;
        this.success = true;
        this.cart = [];
        this.notes = '';
        this.saveCartToStorage();
        setTimeout(() => this.success = false, 4000);
      },
      error: () => { this.isSubmitting = false; this.error = 'Protocol synchronization failed.'; }
    });
  }
}
