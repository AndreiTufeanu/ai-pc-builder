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

  // Component specifications configuration
  readonly componentSpecs: ComponentSpecs = {
    CPU: [
      { name: 'socket', label: 'Socket', type: 'select', options: ['AM5', 'AM4', 'LGA1700', 'LGA1200', 'LGA1151'], required: true },
      { name: 'cores', label: 'Cores', type: 'number', required: true },
      { name: 'threads', label: 'Threads', type: 'number', required: true },
      { name: 'baseClock', label: 'Base Clock', type: 'number', step: '0.1', required: true, unit: 'GHz' },
      { name: 'boostClock', label: 'Boost Clock', type: 'number', step: '0.1', unit: 'GHz' },
      { name: 'tdp', label: 'TDP', type: 'number', required: true, unit: 'W' },
      { name: 'memoryType', label: 'Memory Type', type: 'select', options: ['DDR5', 'DDR4'], required: true }
    ],
    GPU: [
      { name: 'memory', label: 'Memory', type: 'number', required: true, unit: 'GB' },
      { name: 'memoryType', label: 'Memory Type', type: 'select', options: ['GDDR6', 'GDDR6X', 'GDDR5', 'HBM2'], required: true },
      { name: 'coreClock', label: 'Core Clock', type: 'number', unit: 'MHz' },
      { name: 'boostClock', label: 'Boost Clock', type: 'number', unit: 'MHz' },
      { name: 'length', label: 'Length', type: 'number', unit: 'mm' },
      { name: 'powerConnectors', label: 'Power Connectors', type: 'select', options: ['1x 6-pin', '1x 8-pin', '2x 8-pin', '3x 8-pin', '12VHPWR (16-pin)', '2x 8-pin + 12VHPWR'], required: true },
      { name: 'tdp', label: 'TDP', type: 'number', required: true, unit: 'W' }
    ],
    PSU: [
      { name: 'wattage', label: 'Wattage', type: 'number', required: true, unit: 'W' },
      { name: 'efficiency', label: 'Efficiency Rating', type: 'select', options: ['80+ Bronze', '80+ Gold', '80+ Platinum', '80+ Titanium'], required: true },
      { name: 'formFactor', label: 'Form Factor', type: 'select', options: ['ATX', 'SFX', 'SFX-L'], required: true },
      { name: 'modular', label: 'Modular', type: 'select', options: ['Non-modular', 'Semi-modular', 'Full modular'], required: true },
      {
        name: 'connectors', label: 'Available Connectors', type: 'checkbox-group', options: [
          '24-pin ATX',
          '8-pin EPS (CPU)',
          '4+4 pin EPS (CPU)',
          '6-pin PCIe',
          '8-pin PCIe',
          '12VHPWR (16-pin)',
          'SATA',
          'Molex',
          'Floppy'
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
      { name: 'type', label: 'Storage Type', type: 'select', options: ['NVMe SSD', 'SATA SSD', 'HDD'], required: true },
      { name: 'capacity', label: 'Capacity', type: 'number', required: true, unit: 'TB' },
      { name: 'formFactor', label: 'Form Factor', type: 'select', options: ['M.2', '2.5"', '3.5"'] },
      { name: 'interface', label: 'Interface', type: 'select', options: ['PCIe 4.0', 'PCIe 3.0', 'SATA III'] },
      { name: 'readSpeed', label: 'Read Speed', type: 'number', unit: 'MB/s' },
      { name: 'writeSpeed', label: 'Write Speed', type: 'number', unit: 'MB/s' }
    ],
    MOTHERBOARD: [
      { name: 'socket', label: 'Socket', type: 'select', options: ['AM5', 'AM4', 'LGA1700', 'LGA1200'], required: true },
      { name: 'formFactor', label: 'Form Factor', type: 'select', options: ['ATX', 'mATX', 'ITX'], required: true },
      { name: 'memoryType', label: 'Memory Type', type: 'select', options: ['DDR5', 'DDR4'], required: true },
      { name: 'memorySlots', label: 'Memory Slots', type: 'number', required: true },
      { name: 'maxMemory', label: 'Max Memory', type: 'number', required: true, unit: 'GB' },
      { name: 'memorySpeed', label: 'Memory Speed', type: 'number', unit: 'MHz' },
      { name: 'chipset', label: 'Chipset', type: 'text', required: true },
      {
        name: 'features', label: 'Features & Ports', type: 'checkbox-group', options: [
          'Wi-Fi',
          'Bluetooth',
          '2.5G Ethernet',
          '10G Ethernet',
          'USB-C Front Panel',
          'RGB Headers',
          'Multiple M.2 heatsinks',
          'Thunderbolt',
          'DisplayPort',
          'HDMI'
        ]
      }
    ],
    CASE: [
      { name: 'formFactor', label: 'Supported Form Factors', type: 'select', options: ['ATX', 'mATX', 'ITX', 'E-ATX'], required: true },
      { name: 'maxGpuLength', label: 'Max GPU Length', type: 'number', unit: 'mm' },
      { name: 'fansIncluded', label: 'Fans Included', type: 'number' },
      { name: 'frontPanelUsb', label: 'Front Panel USB', type: 'text' }
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
    return this.http.post<BuildResponse>(`http://localhost:8080/api/build/generate`, buildRequest);
  }

  createBuild(buildRequest: BuildRequest): Observable<BuildResponse> {
    return this.http.post<BuildResponse>(`http://localhost:8080/api/build`, buildRequest);
  }

  getUserBuilds(userId: number): Observable<Build[]> {
    return this.http.get<Build[]>(`http://localhost:8080/api/build/user/${userId}`);
  }

  deleteBuild(userId: number, buildId: number): Observable<void> {
    return this.http.delete<void>(`http://localhost:8080/api/build/${buildId}/user/${userId}`);
  }
}