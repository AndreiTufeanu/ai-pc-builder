import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ComponentService, ComponentType, type ComponentSpec } from '../../core/services/component.service';

interface BuildRequirement {
  type: ComponentType;
  specifications: { [key: string]: any };
  budget?: number;
}

@Component({
  selector: 'app-user-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-dashboard.component.html',
  styleUrls: ['./user-dashboard.component.css']
})
export class UserDashboardComponent {
  activeTab = signal<'chat' | 'guided'>('guided');
  protected readonly Object = Object;
  
  // Chat functionality
  chatMessages = signal<any[]>([
    { role: 'assistant', content: 'Hello! I\'m here to help you build your perfect PC. Ask me anything about components, compatibility, or budget recommendations!' }
  ]);
  newMessage = '';
  isChatLoading = false;

  // Guided build functionality
  currentStep = signal<ComponentType | 'summary'>(ComponentType.CPU);
  buildRequirements = signal<{ [key in ComponentType]?: BuildRequirement }>({});
  totalBudget = signal<number | null>(null);

  // Available component types for guided mode
  componentTypes: ComponentType[] = [
    ComponentType.CPU,
    ComponentType.GPU,
    ComponentType.PSU,
    ComponentType.RAM,
    ComponentType.STORAGE,
    ComponentType.MOTHERBOARD,
    ComponentType.CASE
  ];

  // Store current requirements for two-way binding
  currentRequirements: BuildRequirement = {
    type: ComponentType.CPU,
    specifications: {}
  };

  constructor(
    private router: Router,
    public authService: AuthService,
    private componentService: ComponentService
  ) {
    // Initialize empty requirements for all component types
    const initialRequirements: { [key in ComponentType]?: BuildRequirement } = {};
    this.componentTypes.forEach(type => {
      initialRequirements[type] = {
        type: type,
        specifications: {}
      };
    });
    this.buildRequirements.set(initialRequirements);
    
    // Initialize current requirements
    this.updateCurrentRequirements();
  }

  setActiveTab(tab: 'chat' | 'guided'): void {
    this.activeTab.set(tab);
  }

  // Chat methods
  sendMessage(): void {
    const message = this.newMessage.trim();
    if (!message) return;

    this.chatMessages.update(messages => [
      ...messages,
      { role: 'user', content: message }
    ]);

    this.newMessage = '';
    this.isChatLoading = true;

    // TODO: Connect to actual AI service
    setTimeout(() => {
      this.chatMessages.update(messages => [
        ...messages,
        { role: 'assistant', content: 'I understand you\'re looking for PC building advice. When connected to the AI service, I\'ll provide personalized recommendations based on your needs and budget.' }
      ]);
      this.isChatLoading = false;
    }, 1500);
  }

  // Guided build methods
  nextStep(): void {
    this.saveCurrentRequirements();
    const steps: (ComponentType | 'summary')[] = [...this.componentTypes, 'summary'];
    const currentIndex = steps.indexOf(this.currentStep());
    if (currentIndex < steps.length - 1) {
      this.currentStep.set(steps[currentIndex + 1]);
      this.updateCurrentRequirements();
    }
  }

  previousStep(): void {
    this.saveCurrentRequirements();
    const steps: (ComponentType | 'summary')[] = [...this.componentTypes, 'summary'];
    const currentIndex = steps.indexOf(this.currentStep());
    if (currentIndex > 0) {
      this.currentStep.set(steps[currentIndex - 1]);
      this.updateCurrentRequirements();
    }
  }

  goToStep(step: ComponentType | 'summary'): void {
    this.saveCurrentRequirements();
    this.currentStep.set(step);
    if (step !== 'summary') {
      this.updateCurrentRequirements();
    }
  }

  updateCurrentRequirements(): void {
    if (this.currentStep() !== 'summary') {
      const requirements = this.buildRequirements()[this.currentStep() as ComponentType];
      if (requirements) {
        this.currentRequirements = { ...requirements };
      }
    }
  }

  saveCurrentRequirements(): void {
    if (this.currentStep() !== 'summary') {
      this.buildRequirements.update(requirements => ({
        ...requirements,
        [this.currentStep() as ComponentType]: { ...this.currentRequirements }
      }));
    }
  }

  getSpecsForCurrentType(): ComponentSpec[] {
    if (this.currentStep() === 'summary') return [];
    return this.componentService.getSpecsForType(this.currentStep() as ComponentType);
  }

  onCheckboxChange(specName: string, value: string, event: any): void {
    const checked = event.target.checked;
    
    if (!this.currentRequirements.specifications[specName]) {
      this.currentRequirements.specifications[specName] = [];
    }

    if (checked) {
      if (!this.currentRequirements.specifications[specName].includes(value)) {
        this.currentRequirements.specifications[specName].push(value);
      }
    } else {
      this.currentRequirements.specifications[specName] = 
        this.currentRequirements.specifications[specName].filter((item: string) => item !== value);
    }
  }

  generateBuild(): void {
    this.saveCurrentRequirements();
    
    // TODO: Connect to AI service to generate build based on requirements
    const requirements = this.buildRequirements();
    const budget = this.totalBudget();
    
    console.log('Generating build with requirements:', requirements);
    console.log('Total budget:', budget);
    
    alert('Build generation feature will be connected to the AI service. This would create a personalized PC build based on your requirements.');
  }

  clearRequirements(): void {
    const resetRequirements: { [key in ComponentType]?: BuildRequirement } = {};
    this.componentTypes.forEach(type => {
      resetRequirements[type] = {
        type: type,
        specifications: {}
      };
    });
    this.buildRequirements.set(resetRequirements);
    this.totalBudget.set(null);
    this.currentStep.set(ComponentType.CPU);
    this.updateCurrentRequirements();
  }

  // Helper to display specifications in summary
  getSpecDisplay(requirements: BuildRequirement): string {
    const specs = requirements.specifications;
    const specStrings = Object.entries(specs)
      .filter(([_, value]) => value !== '' && value !== null && value !== undefined)
      .map(([key, value]) => {
        if (Array.isArray(value)) {
          return value.length > 0 ? `${key}: ${value.join(', ')}` : '';
        }
        return `${key}: ${value}`;
      })
      .filter(str => str !== '');
    
    return specStrings.length > 0 ? specStrings.join('; ') : 'No specific requirements';
  }

  logout(): void {
    this.authService.logout();
  }
}