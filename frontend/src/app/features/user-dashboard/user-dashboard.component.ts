import { Component, signal, ViewChildren, QueryList, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';
import { ComponentService, ComponentType, type ComponentSpec } from '../../core/services/component.service';
import { ChatService } from '../../core/services/chat.service';
import { Subscription } from 'rxjs';

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
export class UserDashboardComponent implements AfterViewChecked {
  activeTab = signal<'chat' | 'guided'>('guided');
  protected readonly Object = Object;
  @ViewChildren('messageElem') private messageElems!: QueryList<ElementRef<HTMLElement>>;

  // Chat functionality
  chatMessages = signal<any[]>([]);
  newMessage = '';
  isChatLoading = false;
  private chatSubscription?: Subscription;

  // Guided build functionality (unchanged)
  currentStep = signal<ComponentType | 'summary'>(ComponentType.CPU);
  buildRequirements = signal<{ [key in ComponentType]?: BuildRequirement }>({});
  totalBudget = signal<number | null>(null);
  componentTypes: ComponentType[] = [ComponentType.CPU, ComponentType.GPU, ComponentType.PSU, ComponentType.RAM, ComponentType.STORAGE, ComponentType.MOTHERBOARD, ComponentType.CASE];
  currentRequirements: BuildRequirement = { type: ComponentType.CPU, specifications: {} };

  constructor(
    private router: Router,
    public authService: AuthService,
    private componentService: ComponentService,
    private chatService: ChatService
  ) {
    this.initializeBuildRequirements();
  }

  ngOnInit(): void {
    this.loadChatHistory();
  }

  ngOnDestroy(): void {
    this.chatSubscription?.unsubscribe();
  }

  private initializeBuildRequirements(): void {
    const initialRequirements: { [key in ComponentType]?: BuildRequirement } = {};
    this.componentTypes.forEach(type => {
      initialRequirements[type] = { type, specifications: {} };
    });
    this.buildRequirements.set(initialRequirements);
    this.updateCurrentRequirements();
  }

  loadChatHistory(): void {
    const userId = this.authService.getCurrentUserId();
    if (!userId) {
      console.error('No user ID available');
      this.showFallbackWelcome();
      return;
    }

    console.log('Loading chat history for user ID:', userId);

    this.chatSubscription = this.chatService.getChatHistory(userId).subscribe({
      next: (messages) => {
        console.log('Received messages:', messages);
        if (messages.length === 0) {
          this.chatMessages.set([
            { role: 'assistant', content: 'Hello! I\'m here to help you build your perfect PC. Ask me anything about components, compatibility, or budget recommendations!' }
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
        console.error('Error loading chat history:', error);
        this.showFallbackWelcome();
      }
    });
  }



  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  private scrollToBottom(): void {
    const last = this.messageElems?.last;
    if (last) {
      try {
        last.nativeElement.scrollIntoView({ behavior: 'instant', block: 'end' });
      } catch {
        // Ignore scrolling errors
      }
    }
  }

  setActiveTab(tab: 'chat' | 'guided'): void {
    this.activeTab.set(tab);
    if (tab === 'chat') {
      this.loadChatHistory();
    }
  }

  // Chat methods
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

    console.log('Sending message for user ID:', userId);

    this.chatService.sendUserMessage(message, userId).subscribe({
      next: (response) => {
        this.chatMessages.update(messages => [
          ...messages,
          { role: 'assistant', content: response.response }
        ]);
        this.isChatLoading = false;
      },
      error: (error) => {
        console.error('Chat error:', error);
        this.chatMessages.update(messages => [
          ...messages,
          { role: 'assistant', content: 'I apologize, but I\'m having trouble connecting right now. Please try again.' }
        ]);
        this.isChatLoading = false;
      }
    });
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

  private showFallbackWelcome(): void {
    this.chatMessages.set([
      { role: 'assistant', content: 'Hello! I\'m here to help you build your perfect PC. Ask me anything about components, compatibility, or budget recommendations!' }
    ]);
  }
}