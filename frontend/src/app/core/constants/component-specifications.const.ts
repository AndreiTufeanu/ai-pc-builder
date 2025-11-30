import { ComponentSpecs } from '../models/component.model';

export const COMPONENT_SPECIFICATIONS: ComponentSpecs = {
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