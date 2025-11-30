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