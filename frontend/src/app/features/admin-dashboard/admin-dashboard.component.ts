import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './admin-dashboard.component.html',
  styleUrls: ['./admin-dashboard.component.css']
})
export class AdminDashboardComponent {
  activeTab = signal<'chat' | 'components'>('components');
  
  // Sample data for components (will be replaced with API data)
  components = signal<any[]>([
    {
      id: 1,
      name: 'AMD Ryzen 7 7800X3D',
      type: 'CPU',
      price: 389.99,
      manufacturer: 'AMD',
      model: 'Ryzen 7 7800X3D'
    },
    {
      id: 2,
      name: 'NVIDIA GeForce RTX 4070',
      type: 'GPU', 
      price: 549.99,
      manufacturer: 'NVIDIA',
      model: 'RTX 4070'
    },
    {
      id: 3,
      name: 'Corsair RM750x',
      type: 'PSU',
      price: 119.99,
      manufacturer: 'Corsair',
      model: 'RM750x'
    }
  ]);

  // Chat functionality - using regular properties for ngModel
  chatMessages = signal<any[]>([
    { role: 'assistant', content: 'Hello! I\'m ready to help you manage PC components and answer questions.' }
  ]);
  newMessage = ''; // Regular property for ngModel

  // Component form data - using regular properties for ngModel
  showComponentForm = signal(false);
  editingComponent = signal<any>(null);
  componentForm = {
    name: '',
    type: 'CPU',
    price: '',
    manufacturer: '',
    model: '',
    description: ''
  };

  setActiveTab(tab: 'chat' | 'components'): void {
    this.activeTab.set(tab);
  }

  // Chat methods
  sendMessage(): void {
    const message = this.newMessage.trim();
    if (!message) return;

    // Add user message
    this.chatMessages.update(messages => [
      ...messages,
      { role: 'user', content: message }
    ]);

    // Clear input
    this.newMessage = '';

    // Simulate AI response (will be replaced with actual API call)
    setTimeout(() => {
      this.chatMessages.update(messages => [
        ...messages,
        { role: 'assistant', content: 'This is a simulated response. When connected, this will interact with the AI model for component knowledge.' }
      ]);
    }, 1000);
  }

  // Component CRUD methods
  openAddComponentForm(): void {
    this.componentForm = {
      name: '',
      type: 'CPU',
      price: '',
      manufacturer: '',
      model: '',
      description: ''
    };
    this.editingComponent.set(null);
    this.showComponentForm.set(true);
  }

  openEditComponent(component: any): void {
    this.componentForm = {
      name: component.name,
      type: component.type,
      price: component.price.toString(),
      manufacturer: component.manufacturer,
      model: component.model,
      description: component.description || ''
    };
    this.editingComponent.set(component);
    this.showComponentForm.set(true);
  }

  saveComponent(): void {
    // Basic validation
    if (!this.componentForm.name.trim()) {
      alert('Component name is required');
      return;
    }

    if (this.editingComponent()) {
      // Update existing component
      this.components.update(comps => 
        comps.map(comp => 
          comp.id === this.editingComponent()!.id 
            ? { 
                ...comp, 
                ...this.componentForm, 
                price: parseFloat(this.componentForm.price) || 0 
              }
            : comp
        )
      );
    } else {
      // Add new component
      const newComponent = {
        id: Math.max(...this.components().map(c => c.id)) + 1,
        ...this.componentForm,
        price: parseFloat(this.componentForm.price) || 0
      };
      this.components.update(comps => [...comps, newComponent]);
    }

    this.showComponentForm.set(false);
    this.editingComponent.set(null);
  }

  deleteComponent(component: any): void {
    if (confirm(`Are you sure you want to delete ${component.name}?`)) {
      this.components.update(comps => comps.filter(c => c.id !== component.id));
    }
  }

  cancelForm(): void {
    this.showComponentForm.set(false);
    this.editingComponent.set(null);
  }
}