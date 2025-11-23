import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PcComponent {
  id?: number;
  name: string;
  type: ComponentType;
  description?: string;
  price?: number;
  manufacturer?: string;
  model?: string;
  specifications: { [key: string]: any };
}

export interface BuildResponse {
  build: Build;
  message: string;
  success: boolean;
}

export interface BuildRequest {
  userId: number;
  name: string;
  description: string;
  budget: number | null;
  requirements: { [key in ComponentType]?: { specifications: { [key: string]: any } } };
}

export interface Build {
  id: number;
  userId: number;
  name: string;
  description: string;
  totalPrice: number | null;
  budget: number | null;
  createdAt: string;
  cpuId: number | null;
  gpuId: number | null;
  psuId: number | null;
  ramId: number | null;
  storageId: number | null;
  motherboardId: number | null;
  caseId: number | null;
}

export interface BuildWithComponents {
  id: number;
  userId: number;
  name: string;
  description: string;
  totalPrice: number | null;
  budget: number | null;
  createdAt: string;
  cpu: string;
  gpu: string;
  psu: string;
  ram: string;
  storage: string;
  motherboard: string;
  pcCase: string;
}

export enum ComponentType {
  CPU = 'CPU',
  GPU = 'GPU',
  PSU = 'PSU',
  RAM = 'RAM',
  STORAGE = 'STORAGE',
  MOTHERBOARD = 'MOTHERBOARD',
  CASE = 'CASE'
}

export interface ComponentSpec {
  name: string;
  label: string;
  type: 'text' | 'number' | 'select' | 'checkbox-group';
  options?: string[];
  required?: boolean;
  step?: string;
  unit?: string;
}

export interface ComponentSpecs {
  [key: string]: ComponentSpec[];
}

@Injectable({
  providedIn: 'root'
})
export class ComponentService {
  private baseUrl = 'http://localhost:8080/api/admin/components';
  private buildBaseUrl = 'http://localhost:8080/api/build';

  // Component specifications configuration
  readonly componentSpecs: ComponentSpecs = {
    CPU: [
      { name: 'socket', label: 'Socket', type: 'select', options: ['AM5', 'AM4', 'LGA1851', 'LGA1700'], required: true },
      { name: 'cores', label: 'Cores', type: 'number', required: true },
      { name: 'threads', label: 'Threads', type: 'number', required: true },
      { name: 'baseClock', label: 'Base Clock', type: 'number', step: '0.1', required: true, unit: 'GHz' },
      { name: 'boostClock', label: 'Boost Clock', type: 'number', step: '0.1', unit: 'GHz' },
      { name: 'tdp', label: 'TDP', type: 'number', required: true, unit: 'W' },
    ],
    GPU: [
      { name: 'memory', label: 'Memory', type: 'number', required: true, unit: 'GB' },
      { name: 'memoryType', label: 'Memory Type', type: 'select', options: ['GDDR7', 'GDDR6X', 'GDDR6'], required: true },
      { name: 'coreClock', label: 'Core Clock', type: 'number', unit: 'MHz' },
      { name: 'boostClock', label: 'Boost Clock', type: 'number', unit: 'MHz' },
      { name: 'length', label: 'Length', type: 'number', unit: 'mm' },
      { name: 'powerConnectors', label: 'Power Connectors', type: 'select', options: ['1 x 8-pin', '2 x 8-pin', '3 x 8-pin', '12VHPWR (16-pin)'], required: true },
      { name: 'tdp', label: 'TDP', type: 'number', required: true, unit: 'W' }
    ],
    PSU: [
      { name: 'wattage', label: 'Wattage', type: 'number', required: true, unit: 'W' },
      { name: 'efficiency', label: 'Efficiency Rating', type: 'select', options: ['80+ Silver', '80+ Gold', '80+ Platinum', '80+ Titanium'], required: true },
      { name: 'formFactor', label: 'Form Factor', type: 'select', options: ['ATX', 'SFX'], required: true },
      { name: 'atxStandard', label: 'ATX Standard', type: 'select', options: ['ATX 3.0', 'ATX 3.1'], required: true },
      {
        name: 'connectors', label: 'Available Connectors', type: 'checkbox-group', options: [
          '1 x 8-pin',
          '2 x 8-pin',
          '3 x 8-pin',
          '12VHPWR (16-pin)',
          'SATA',
        ]
      }
    ],
    RAM: [
      { name: 'capacity', label: 'Capacity', type: 'number', required: true, unit: 'GB' },
      { name: 'type', label: 'Type', type: 'select', options: ['DDR4', 'DDR5'], required: true },
      { name: 'speed', label: 'Speed', type: 'number', required: true, unit: 'MHz' },
      { name: 'latency', label: 'CAS Latency', type: 'number', unit: 'CL' },
      { name: 'modules', label: 'Number of Modules', type: 'number' }
    ],
    STORAGE: [
      { name: 'type', label: 'Storage Type', type: 'select', options: ['NVMe 4.0', 'SATA III'], required: true },
      { name: 'capacity', label: 'Capacity', type: 'number', required: true, unit: 'TB' },
      { name: 'readSpeed', label: 'Read Speed', type: 'number', unit: 'MB/s' },
      { name: 'writeSpeed', label: 'Write Speed', type: 'number', unit: 'MB/s' }
    ],
    MOTHERBOARD: [
      { name: 'socket', label: 'Socket', type: 'select', options: ['AM5', 'AM4', 'LGA1851', 'LGA1700'], required: true },
      { name: 'formFactor', label: 'Form Factor', type: 'select', options: ['E-ATX', 'ATX', 'mATX', 'ITX'], required: true },
      {
        name: 'features', label: 'Features & Ports', type: 'checkbox-group', options: [
          'Wi-Fi',
        ]
      }
    ],
    CASE: [
      { name: 'formFactor', label: 'Supported Form Factors', type: 'select', options: ['E-ATX', 'ATX', 'mATX', 'ITX'], required: true },
      { name: 'maxGpuLength', label: 'Max GPU Length', type: 'number', unit: 'mm' },
      { name: 'fansIncluded', label: 'Fans Included', type: 'number' },
    ]
  };

  constructor(private http: HttpClient) { }

  getAllComponents(): Observable<PcComponent[]> {
    return this.http.get<PcComponent[]>(this.baseUrl);
  }

  getComponentById(id: number): Observable<PcComponent> {
    return this.http.get<PcComponent>(`${this.baseUrl}/${id}`);
  }

  createComponent(component: PcComponent): Observable<PcComponent> {
    return this.http.post<PcComponent>(this.baseUrl, component);
  }

  updateComponent(id: number, component: PcComponent): Observable<PcComponent> {
    return this.http.put<PcComponent>(`${this.baseUrl}/${id}`, component);
  }

  deleteComponent(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getSpecsForType(type: ComponentType): ComponentSpec[] {
    return this.componentSpecs[type] || [];
  }

  generateBuild(buildRequest: BuildRequest): Observable<BuildResponse> {
    return this.http.post<BuildResponse>(`${this.buildBaseUrl}/generate`, buildRequest);
  }

  createBuild(buildRequest: BuildRequest): Observable<BuildResponse> {
    return this.http.post<BuildResponse>(`${this.buildBaseUrl}`, buildRequest);
  }

  getUserBuilds(userId: number): Observable<BuildWithComponents[]> {
    return this.http.get<BuildWithComponents[]>(`${this.buildBaseUrl}/user/${userId}/builds`);
  }

  deleteBuild(userId: number, buildId: number): Observable<void> {
    return this.http.delete<void>(`${this.buildBaseUrl}/${buildId}/user/${userId}`);
  }
}