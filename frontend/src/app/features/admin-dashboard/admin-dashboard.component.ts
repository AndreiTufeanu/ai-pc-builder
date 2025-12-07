import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PcComponent, ComponentType } from '../../core/models/component.model';
import { ComponentService } from '../../core/services/component.service';
import { ChatService } from '../../core/services/chat.service';
import { AuthService } from '../../core/services/auth.service';
import { Subscription } from 'rxjs';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent {
  activeTab = signal<'chat' | 'components'>('components');

  // Components data
  components = signal<PcComponent[]>([]);
  isLoading = signal(false);

  // Chat functionality
  chatMessages = signal<any[]>([]);
  newMessage = '';
  isChatLoading = false;
  private chatSubscription?: Subscription;

  // Component form data (unchanged)
  showComponentForm = signal(false);
  editingComponent = signal<PcComponent | null>(null);
  componentForm = {
    name: '',
    type: 'CPU' as ComponentType,
    price: '',
    manufacturer: '',
    model: '',
    description: '',
    specifications: {} as { [key: string]: any }
  };
  currentSpecs = signal<any[]>([]);

  constructor(
    private componentService: ComponentService,
    private chatService: ChatService,
    private authService: AuthService,
    private sanitizer: DomSanitizer
  ) {
    this.loadComponents();
  }

  ngOnInit(): void {
    this.loadChatHistory();
  }

  ngOnDestroy(): void {
    this.chatSubscription?.unsubscribe();
  }

  formatMessage(content: string): SafeHtml {
    if (!content) return '';

    // Convert markdown to HTML
    let formatted = content
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/^\s*\*\s+/gm, '<li>')
      .replace(/^\s*(\d+)\.\s+/gm, '<li>$1. ')
      .replace(/\n/g, '<br>')
      .replace(/(<li>.*?(?:<br>|$))/gs, '<ul>$1</ul>');

    return this.sanitizer.bypassSecurityTrustHtml(formatted);
  }

  loadChatHistory(): void {
    const userId = this.authService.getCurrentUserId();
    if (!userId) {
      console.error('No user ID available');
      this.showFallbackWelcome();
      return;
    }

    console.log('Loading chat history for admin user ID:', userId);

    this.chatSubscription = this.chatService.getChatHistory(userId).subscribe({
      next: (messages) => {
        console.log('Received admin messages:', messages);
        if (messages.length === 0) {
          this.chatMessages.set([
            { role: 'assistant', content: 'Hello! I\'m ready to help you manage PC components and answer questions.' }
          ]);
        } else {
          const formattedMessages = messages.flatMap(msg => [
            { role: 'user', content: msg.userMessage },
            { role: 'assistant', content: msg.aiResponse }
          ]);
          this.chatMessages.set(formattedMessages);
        }
      },
      error: (error) => {
        console.error('Error loading admin chat history:', error);
        this.showFallbackWelcome();
      }
    });
  }

  setActiveTab(tab: 'chat' | 'components'): void {
    this.activeTab.set(tab);
    if (tab === 'chat') {
      this.loadChatHistory();
    }
  }

  // Component CRUD methods
  loadComponents(): void {
    this.isLoading.set(true);
    this.componentService.getAllComponents().subscribe({
      next: (components) => {
        this.components.set(components);
        this.isLoading.set(false);
      },
      error: (error) => {
        console.error('Error loading components:', error);
        this.isLoading.set(false);
      }
    });
  }

  onTypeChange(): void {
    // Update specifications when type changes
    const specs = this.componentService.getSpecsForType(this.componentForm.type);
    this.currentSpecs.set(specs);

    // Initialize specifications object
    const newSpecs: { [key: string]: any } = {};
    specs.forEach(spec => {
      // Preserve existing values if they exist
      if (this.componentForm.specifications.hasOwnProperty(spec.name)) {
        newSpecs[spec.name] = this.componentForm.specifications[spec.name];
      } else if (spec.type === 'checkbox-group') {
        newSpecs[spec.name] = [];
      } else {
        newSpecs[spec.name] = '';
      }
    });
    this.componentForm.specifications = newSpecs;
  }

  openAddComponentForm(): void {
    this.componentForm = {
      name: '',
      type: ComponentType.CPU,
      price: '',
      manufacturer: '',
      model: '',
      description: '',
      specifications: {}
    };
    this.editingComponent.set(null);
    this.onTypeChange(); // Initialize specs for default type
    this.showComponentForm.set(true);
  }

  openEditComponent(component: PcComponent): void {
    // First, parse the specifications to extract numeric values from strings with units
    const parsedSpecifications: { [key: string]: any } = {};

    // Get the specs for this component type
    const specs = this.componentService.getSpecsForType(component.type);

    // Parse each specification
    for (const spec of specs) {
      const storedValue = component.specifications[spec.name];

      if (storedValue !== undefined && storedValue !== null) {
        if (spec.type === 'number' && spec.unit) {
          // Extract numeric value from string like "16 GB" or "2300 MHz"
          // Remove the unit and any whitespace, convert to number
          const match = String(storedValue).match(/^([\d.]+)/);
          if (match) {
            parsedSpecifications[spec.name] = parseFloat(match[1]);
          } else {
            parsedSpecifications[spec.name] = '';
          }
        } else if (spec.type === 'checkbox-group') {
          // Ensure checkbox groups are arrays
          parsedSpecifications[spec.name] = Array.isArray(storedValue)
            ? storedValue
            : [storedValue];
        } else {
          // For other types, use as-is
          parsedSpecifications[spec.name] = storedValue;
        }
      } else {
        // If no value, set to empty string
        parsedSpecifications[spec.name] = '';
      }
    }

    this.componentForm = {
      name: component.name,
      type: component.type,
      price: component.price?.toString() || '',
      manufacturer: component.manufacturer || '',
      model: component.model || '',
      description: component.description || '',
      specifications: parsedSpecifications
    };
    this.editingComponent.set(component);
    this.onTypeChange();
    this.showComponentForm.set(true);
  }

  onCheckboxChange(specName: string, value: string, event: any): void {
    const checked = event.target.checked;
    if (checked) {
      if (!this.componentForm.specifications[specName].includes(value)) {
        this.componentForm.specifications[specName].push(value);
      }
    } else {
      this.componentForm.specifications[specName] =
        this.componentForm.specifications[specName].filter((item: string) => item !== value);
    }
  }

  saveComponent(): void {
    // Basic validation
    if (!this.componentForm.name.trim()) {
      alert('Component name is required');
      return;
    }

    // Create a copy of specifications with units applied
    const specificationsWithUnits: { [key: string]: any } = {};

    for (const spec of this.currentSpecs()) {
      const value = this.componentForm.specifications[spec.name];

      if (value !== '' && value !== null && value !== undefined) {
        if (Array.isArray(value)) {
          // For checkbox groups, store as array
          specificationsWithUnits[spec.name] = value;
        } else if (spec.unit && spec.type === 'number') {
          // For numbers with units, store as string with unit
          specificationsWithUnits[spec.name] = `${value} ${spec.unit}`;
        } else {
          // For other types, store as is
          specificationsWithUnits[spec.name] = value;
        }
      }
    }

    // Validate required specifications
    const missingRequired = this.currentSpecs().filter(spec =>
      spec.required && (!specificationsWithUnits[spec.name] ||
        (Array.isArray(specificationsWithUnits[spec.name]) &&
          specificationsWithUnits[spec.name].length === 0))
    );

    if (missingRequired.length > 0) {
      alert(`Please fill in all required specifications: ${missingRequired.map(s => s.label).join(', ')}`);
      return;
    }

    const componentData: PcComponent = {
      name: this.componentForm.name,
      type: this.componentForm.type,
      description: this.componentForm.description || undefined,
      price: this.componentForm.price ? parseFloat(this.componentForm.price) : undefined,
      manufacturer: this.componentForm.manufacturer || undefined,
      model: this.componentForm.model || undefined,
      specifications: specificationsWithUnits  // Use the formatted specifications with units
    };

    if (this.editingComponent()) {
      // Update existing component
      this.componentService.updateComponent(this.editingComponent()!.id!, componentData).subscribe({
        next: () => {
          this.loadComponents();
          this.showComponentForm.set(false);
          this.editingComponent.set(null);
        },
        error: (error) => {
          console.error('Error updating component:', error);
          alert('Error updating component');
        }
      });
    } else {
      // Add new component
      this.componentService.createComponent(componentData).subscribe({
        next: () => {
          this.loadComponents();
          this.showComponentForm.set(false);
        },
        error: (error) => {
          console.error('Error creating component:', error);
          alert('Error creating component');
        }
      });
    }
  }

  deleteComponent(component: PcComponent): void {
    if (confirm(`Are you sure you want to delete ${component.name}?`)) {
      this.componentService.deleteComponent(component.id!).subscribe({
        next: () => {
          this.loadComponents();
        },
        error: (error) => {
          console.error('Error deleting component:', error);
          alert('Error deleting component');
        }
      });
    }
  }

  cancelForm(): void {
    this.showComponentForm.set(false);
    this.editingComponent.set(null);
  }

  // Chat methods (unchanged)
  sendMessage(): void {
    const message = this.newMessage.trim();
    if (!message) return;

    const userId = this.authService.getCurrentUserId();
    if (!userId) {
      console.error('No user ID available');
      return;
    }

    // Add user message immediately
    this.chatMessages.update(messages => [
      ...messages,
      { role: 'user', content: message }
    ]);

    this.newMessage = '';
    this.isChatLoading = true;

    console.log('Sending admin message for user ID:', userId);

    this.chatService.sendAdminMessage(message, userId).subscribe({
      next: (response) => {
        this.chatMessages.update(messages => [
          ...messages,
          { role: 'assistant', content: response.response }
        ]);
        this.isChatLoading = false;
      },
      error: (error) => {
        console.error('Admin chat error:', error);
        this.chatMessages.update(messages => [
          ...messages,
          { role: 'assistant', content: 'I apologize, but I\'m having trouble connecting right now. Please try again.' }
        ]);
        this.isChatLoading = false;
      }
    });
  }

  getImportantSpecs(component: PcComponent): { key: string; label: string; value: any }[] {
    const specs = this.componentService.getSpecsForType(component.type);
    const importantSpecs = specs.filter(spec =>
      ['socket', 'memory', 'wattage', 'capacity', 'formFactor'].includes(spec.name)
    );

    return importantSpecs.map(spec => ({
      key: spec.name,
      label: spec.label,
      value: component.specifications[spec.name]
    })).filter(spec => spec.value && spec.value !== '');
  }

  private showFallbackWelcome(): void {
    this.chatMessages.set([
      { role: 'assistant', content: 'Hello! I\'m ready to help you manage PC components and answer questions.' }
    ]);
  }

}