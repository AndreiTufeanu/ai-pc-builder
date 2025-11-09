// Admin dashboard functionality

// Component type specifications
const componentSpecs = {
    CPU: [
        { name: 'socket', label: 'Socket', type: 'select', options: ['AM5', 'AM4', 'LGA1700', 'LGA1200', 'LGA1151'], required: true },
        { name: 'cores', label: 'Cores', type: 'number', required: true },
        { name: 'threads', label: 'Threads', type: 'number', required: true },
        { name: 'baseClock', label: 'Base Clock (GHz)', type: 'number', step: '0.1', required: true },
        { name: 'boostClock', label: 'Boost Clock (GHz)', type: 'number', step: '0.1' },
        { name: 'tdp', label: 'TDP (W)', type: 'number', required: true },
        { name: 'memoryType', label: 'Memory Type', type: 'select', options: ['DDR5', 'DDR4'], required: true }
    ],
    GPU: [
        { name: 'memory', label: 'Memory (GB)', type: 'number', required: true },
        { name: 'memoryType', label: 'Memory Type', type: 'select', options: ['GDDR6', 'GDDR6X', 'GDDR5', 'HBM2'], required: true },
        { name: 'coreClock', label: 'Core Clock (MHz)', type: 'number' },
        { name: 'boostClock', label: 'Boost Clock (MHz)', type: 'number' },
        { name: 'length', label: 'Length (mm)', type: 'number' },
        { name: 'powerConnectors', label: 'Power Connectors', type: 'select', options: ['1x 6-pin', '1x 8-pin', '2x 8-pin', '3x 8-pin', '12VHPWR (16-pin)', '2x 8-pin + 12VHPWR'], required: true },
        { name: 'tdp', label: 'TDP (W)', type: 'number', required: true }
    ],
    PSU: [
        { name: 'wattage', label: 'Wattage (W)', type: 'number', required: true },
        { name: 'efficiency', label: 'Efficiency Rating', type: 'select', options: ['80+ Bronze', '80+ Gold', '80+ Platinum', '80+ Titanium'], required: true },
        { name: 'formFactor', label: 'Form Factor', type: 'select', options: ['ATX', 'SFX', 'SFX-L'], required: true },
        { name: 'modular', label: 'Modular', type: 'select', options: ['Non-modular', 'Semi-modular', 'Full modular'], required: true },
        { name: 'connectors', label: 'Available Connectors', type: 'checkbox-group', options: [
                '24-pin ATX',
                '8-pin EPS (CPU)',
                '4+4 pin EPS (CPU)',
                '6-pin PCIe',
                '8-pin PCIe',
                '12VHPWR (16-pin)',
                'SATA',
                'Molex',
                'Floppy'
            ] }
    ],
    RAM: [
        { name: 'capacity', label: 'Capacity (GB)', type: 'number', required: true },
        { name: 'type', label: 'Type', type: 'select', options: ['DDR4', 'DDR5'], required: true },
        { name: 'speed', label: 'Speed (MHz)', type: 'number', required: true },
        { name: 'latency', label: 'CAS Latency (CL)', type: 'number' },
        { name: 'modules', label: 'Number of Modules', type: 'number' }
    ],
    STORAGE: [
        { name: 'type', label: 'Storage Type', type: 'select', options: ['NVMe SSD', 'SATA SSD', 'HDD'], required: true },
        { name: 'capacity', label: 'Capacity (GB)', type: 'number', required: true },
        { name: 'formFactor', label: 'Form Factor', type: 'select', options: ['M.2', '2.5"', '3.5"'] },
        { name: 'interface', label: 'Interface', type: 'select', options: ['PCIe 4.0', 'PCIe 3.0', 'SATA III'] },
        { name: 'readSpeed', label: 'Read Speed (MB/s)', type: 'number' },
        { name: 'writeSpeed', label: 'Write Speed (MB/s)', type: 'number' }
    ],
    MOTHERBOARD: [
        { name: 'socket', label: 'Socket', type: 'select', options: ['AM5', 'AM4', 'LGA1700', 'LGA1200'], required: true },
        { name: 'formFactor', label: 'Form Factor', type: 'select', options: ['ATX', 'mATX', 'ITX'], required: true },
        { name: 'memoryType', label: 'Memory Type', type: 'select', options: ['DDR5', 'DDR4'], required: true },
        { name: 'memorySlots', label: 'Memory Slots', type: 'number', required: true },
        { name: 'maxMemory', label: 'Max Memory (GB)', type: 'number', required: true },
        { name: 'memorySpeed', label: 'Memory Speed (MHz)', type: 'number' },
        { name: 'chipset', label: 'Chipset', type: 'text', required: true },
        { name: 'features', label: 'Features & Ports', type: 'checkbox-group', options: [
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
            ] }
    ],
    CASE: [
        { name: 'formFactor', label: 'Supported Form Factors', type: 'select', options: ['ATX', 'mATX', 'ITX', 'E-ATX'], required: true },
        { name: 'maxGpuLength', label: 'Max GPU Length (mm)', type: 'number' },
        { name: 'fansIncluded', label: 'Fans Included', type: 'number' },
        { name: 'frontPanelUsb', label: 'Front Panel USB', type: 'text' }
    ]
};

let currentEditId = null; // Track which component is being edited

// Wait for DOM to be fully loaded
document.addEventListener('DOMContentLoaded', function() {
    console.log('Admin dashboard loaded');
    initializeAdminDashboard();
});

function initializeAdminDashboard() {
    // Tab switching
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            // Remove active class from all tabs and panes
            document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
            document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));

            // Add active class to clicked tab and corresponding pane
            this.classList.add('active');
            document.getElementById(this.dataset.tab).classList.add('active');
        });
    });

    // Component type change listener
    document.getElementById('componentType').addEventListener('change', function() {
        updateComponentFields();
        updateAddComponentButtonState(); // Update button state on change
    });

    // Component form submission
    document.getElementById('componentForm').addEventListener('submit', function(e) {
        e.preventDefault();
        handleComponentFormSubmit();
    });

    // Admin chat functionality
    const adminSendBtn = document.getElementById('adminSendBtn');
    if (adminSendBtn) {
        adminSendBtn.addEventListener('click', handleAdminChat);
    }

    const adminChatInput = document.getElementById('adminChatInput');
    if (adminChatInput) {
        adminChatInput.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                handleAdminChat();
            }
        });
    }

    // Load initial components
    loadComponents();
}

// Component management functions
function showAddComponentForm() {
    document.getElementById('addComponentForm').classList.remove('hidden');
    document.getElementById('componentFormTitle').textContent = 'Add New Component';
    document.getElementById('componentFormSubmitBtn').textContent = 'Add Component';
    currentEditId = null;

    updateComponentFields(); // Initialize fields based on default selection
    updateAddComponentButtonState(); // Set initial button state
}

function hideAddComponentForm() {
    document.getElementById('addComponentForm').classList.add('hidden');
    document.getElementById('componentForm').reset();
    // Clear dynamic fields
    document.getElementById('dynamicFields').innerHTML = '';
    currentEditId = null;
}

// Update button state based on component type selection
function updateAddComponentButtonState() {
    const type = document.getElementById('componentType').value;
    const submitButton = document.querySelector('#componentForm button[type="submit"]');

    if (!type) {
        submitButton.disabled = true;
        submitButton.style.opacity = '0.6';
        submitButton.style.cursor = 'not-allowed';
        submitButton.title = 'Please select a component type first';
    } else {
        submitButton.disabled = false;
        submitButton.style.opacity = '1';
        submitButton.style.cursor = 'pointer';
        submitButton.title = '';
    }
}

// Update dynamic fields based on component type
function updateComponentFields() {
    const type = document.getElementById('componentType').value;
    const dynamicFields = document.getElementById('dynamicFields');

    // Clear existing dynamic fields
    dynamicFields.innerHTML = '';

    if (type && componentSpecs[type]) {
        const fields = componentSpecs[type];

        fields.forEach(field => {
            const formGroup = document.createElement('div');
            formGroup.className = 'form-group';

            const label = document.createElement('label');
            label.textContent = field.label;
            if (field.required) {
                label.innerHTML += ' <span style="color: red;">*</span>';
            }

            let input;
            if (field.type === 'select') {
                input = document.createElement('select');
                input.name = field.name;
                input.required = field.required || false;

                // Add default option
                const defaultOption = document.createElement('option');
                defaultOption.value = '';
                defaultOption.textContent = `Select ${field.label}`;
                input.appendChild(defaultOption);

                // Add options
                field.options.forEach(option => {
                    const optionElement = document.createElement('option');
                    optionElement.value = option;
                    optionElement.textContent = option;
                    input.appendChild(optionElement);
                });
            } else if (field.type === 'checkbox-group') {
                // Create container for checkboxes
                const checkboxContainer = document.createElement('div');
                checkboxContainer.className = 'checkbox-group';

                field.options.forEach(option => {
                    const checkboxWrapper = document.createElement('div');
                    checkboxWrapper.className = 'checkbox-item';

                    const checkbox = document.createElement('input');
                    checkbox.type = 'checkbox';
                    checkbox.name = field.name;
                    checkbox.value = option;
                    checkbox.id = `${field.name}-${option.replace(/\s+/g, '-')}`;

                    const checkboxLabel = document.createElement('label');
                    checkboxLabel.htmlFor = checkbox.id;
                    checkboxLabel.textContent = option;

                    checkboxWrapper.appendChild(checkbox);
                    checkboxWrapper.appendChild(checkboxLabel);
                    checkboxContainer.appendChild(checkboxWrapper);
                });

                input = checkboxContainer;
            } else {
                input = document.createElement('input');
                input.type = field.type;
                input.name = field.name;
                input.required = field.required || false;

                if (field.step) {
                    input.step = field.step;
                }

                if (field.type === 'number') {
                    input.min = '0';
                    input.placeholder = `Enter ${field.label}`;
                } else {
                    input.placeholder = `Enter ${field.label}`;
                }
            }

            formGroup.appendChild(label);
            formGroup.appendChild(input);
            dynamicFields.appendChild(formGroup);
        });
    }

    // Update button state after updating fields
    updateAddComponentButtonState();
}

// Handle component form submission (both add and update)
function handleComponentFormSubmit() {
    const formData = new FormData(document.getElementById('componentForm'));

    // Build the component data object
    const componentData = {
        name: formData.get('name'),
        type: formData.get('type'),
        description: formData.get('description'),
        price: formData.get('price') ? parseFloat(formData.get('price')) : null,
        manufacturer: formData.get('manufacturer') || '',
        model: formData.get('model') || '',
        specifications: {}
    };

    // Collect dynamic fields
    const type = componentData.type;
    if (type && componentSpecs[type]) {
        componentSpecs[type].forEach(field => {
            if (field.type === 'checkbox-group') {
                // Handle checkbox groups - get all checked values as array
                const checkboxes = document.querySelectorAll(`input[name="${field.name}"]:checked`);
                const values = Array.from(checkboxes).map(cb => cb.value);
                if (values.length > 0) {
                    componentData.specifications[field.name] = values;
                }
            } else {
                const value = formData.get(field.name);
                if (value) {
                    // Convert numeric fields to numbers
                    if (field.type === 'number') {
                        componentData.specifications[field.name] = parseFloat(value);
                    } else {
                        componentData.specifications[field.name] = value;
                    }
                }
            }
        });
    }

    console.log('Submitting component:', componentData);

    // Determine if this is an update or create
    const url = currentEditId
        ? `/api/admin/components/${currentEditId}`
        : '/api/admin/components';

    const method = currentEditId ? 'PUT' : 'POST';

    // Send to backend
    fetch(url, {
        method: method,
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(componentData)
    })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => {
                    console.error('Server response:', text);
                    throw new Error(text);
                });
            }
            return response.json();
        })
        .then(component => {
            const action = currentEditId ? 'updated' : 'added';
            alert(`Component ${action} successfully!`);
            hideAddComponentForm();
            loadComponents(); // Reload the list with new data
        })
        .catch(error => {
            console.error('Error:', error);
            // Try to parse error message from server
            try {
                const errorData = JSON.parse(error.message);
                alert('Error saving component: ' + (errorData.message || error.message));
            } catch (e) {
                // If it's not JSON, show the raw error
                if (error.message.includes('Content-Type')) {
                    alert('Error: Server cannot process the request. Please check the component data and try again.');
                } else {
                    alert('Error saving component: ' + error.message);
                }
            }
        });
}

// Load components from actual database
function loadComponents() {
    const componentsList = document.getElementById('componentsList');
    componentsList.innerHTML = '<p>Loading components...</p>';

    console.log('Fetching components from /api/admin/components...');

    fetch('/api/admin/components')
        .then(response => {
            console.log('Response status:', response.status);
            console.log('Response headers:', response.headers);
            if (!response.ok) {
                throw new Error('Network response was not ok: ' + response.status);
            }
            return response.json();
        })
        .then(components => {
            console.log('Components received:', components);
            if (components.length === 0) {
                componentsList.innerHTML = '<p>No components found in database. Add your first component!</p>';
                return;
            }

            componentsList.innerHTML = '';
            components.forEach(component => {
                const componentCard = createComponentCard(component);
                componentsList.appendChild(componentCard);
            });
        })
        .catch(error => {
            console.error('Error loading components:', error);
            componentsList.innerHTML = '<p>Error loading components. Please try again.</p>';
        });
}

// Create component card with real data
function createComponentCard(component) {
    const card = document.createElement('div');
    card.className = 'component-card';

    // Use specifications directly (it's now a Map, not a JSON string)
    const specs = component.specifications || {};

    // Format specifications for display
    let specsText = '';

    if (component.type === 'CPU') {
        specsText = `Socket: ${specs.socket || 'N/A'}, Cores: ${specs.cores || 'N/A'}, TDP: ${specs.tdp || 'N/A'}W`;
    } else if (component.type === 'GPU') {
        specsText = `Memory: ${specs.memory || 'N/A'}GB ${specs.memoryType || ''}, TDP: ${specs.tdp || 'N/A'}W`;
    } else if (component.type === 'PSU') {
        specsText = `Wattage: ${specs.wattage || 'N/A'}W, Efficiency: ${specs.efficiency || 'N/A'}`;
    } else if (component.type === 'RAM') {
        specsText = `Capacity: ${specs.capacity || 'N/A'}GB, Speed: ${specs.speed || 'N/A'}MHz, Type: ${specs.type || 'N/A'}`;
    } else if (component.type === 'STORAGE') {
        specsText = `Capacity: ${specs.capacity || 'N/A'}GB, Type: ${specs.type || 'N/A'}`;
    } else if (component.type === 'MOTHERBOARD') {
        specsText = `Socket: ${specs.socket || 'N/A'}, Form Factor: ${specs.formFactor || 'N/A'}`;
    } else if (component.type === 'CASE') {
        specsText = `Form Factor: ${specs.formFactor || 'N/A'}`;
    }

    card.innerHTML = `
        <h3>${component.name}</h3>
        <p class="component-type">${component.type}</p>
        ${component.description ? `<p class="component-description">${component.description}</p>` : ''}
        ${component.manufacturer ? `<p class="component-manufacturer">Manufacturer: ${component.manufacturer}</p>` : ''}
        ${component.model ? `<p class="component-model">Model: ${component.model}</p>` : ''}
        <p class="component-specs">${specsText}</p>
        ${component.price ? `<p class="component-price">€${component.price}</p>` : '<p class="component-price">Price not set</p>'}
        <div class="component-actions-card">
            <button class="btn btn-warning btn-sm" onclick="editComponent(${component.id})">Edit</button>
            <button class="btn btn-danger btn-sm" onclick="deleteComponent(${component.id})">Delete</button>
        </div>
    `;

    return card;
}

// Delete component function
function deleteComponent(componentId) {
    if (confirm('Are you sure you want to delete this component?')) {
        fetch(`/api/admin/components/${componentId}`, {
            method: 'DELETE'
        })
            .then(response => {
                if (response.ok) {
                    alert('Component deleted successfully!');
                    loadComponents(); // Reload the list
                } else {
                    // Try to get error message from response
                    response.text().then(errorMessage => {
                        try {
                            const errorData = JSON.parse(errorMessage);
                            alert('Error deleting component: ' + (errorData.message || errorMessage));
                        } catch (e) {
                            alert('Error deleting component: ' + errorMessage);
                        }
                    });
                }
            })
            .catch(error => {
                console.error('Error:', error);
                alert('Error deleting component: ' + error.message);
            });
    }
}

// Edit component function
function editComponent(componentId) {
    // Fetch component details
    fetch(`/api/admin/components/${componentId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to fetch component details');
            }
            return response.json();
        })
        .then(component => {
            // Populate the form with component data
            document.getElementById('componentName').value = component.name || '';
            document.getElementById('componentType').value = component.type || '';
            document.getElementById('componentDescription').value = component.description || '';
            document.getElementById('componentManufacturer').value = component.manufacturer || '';
            document.getElementById('componentModel').value = component.model || '';
            document.getElementById('componentPrice').value = component.price || '';

            // Update dynamic fields based on type
            updateComponentFields();

            // Populate dynamic fields with existing specifications
            setTimeout(() => {
                populateSpecificationFields(component);
            }, 100);

            // Change form to edit mode
            document.getElementById('addComponentForm').classList.remove('hidden');
            document.getElementById('componentFormTitle').textContent = 'Edit Component';
            document.getElementById('componentFormSubmitBtn').textContent = 'Update Component';
            currentEditId = componentId;
        })
        .catch(error => {
            console.error('Error fetching component:', error);
            alert('Error loading component details: ' + error.message);
        });
}

// Populate specification fields with existing data
function populateSpecificationFields(component) {
    let specs = {};
    try {
        if (component.specifications) {
            specs = JSON.parse(component.specifications);
        }
    } catch (e) {
        console.error('Error parsing specifications:', e);
    }

    // Populate each specification field
    Object.keys(specs).forEach(key => {
        const value = specs[key];

        // Find the input/select element
        const input = document.querySelector(`[name="${key}"]`);
        if (input) {
            if (input.type === 'checkbox') {
                // For checkbox groups, we need to handle differently
                const checkboxes = document.querySelectorAll(`input[name="${key}"]`);
                if (Array.isArray(value)) {
                    // If value is an array, check all matching checkboxes
                    checkboxes.forEach(checkbox => {
                        checkbox.checked = value.includes(checkbox.value);
                    });
                }
            } else if (input.type === 'select-one') {
                // For select elements
                input.value = value;
            } else {
                // For text/number inputs
                input.value = value;
            }
        }
    });
}

// Admin chat functionality
function handleAdminChat() {
    const adminChatInput = document.getElementById('adminChatInput');
    const message = adminChatInput.value.trim();

    if (message) {
        addAdminMessage(message, true);
        adminChatInput.value = '';

        const currentUser = JSON.parse(localStorage.getItem('currentUser'));

        fetch('/api/admin/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                message: message,
                userId: currentUser.id
            })
        })
            .then(response => response.json())
            .then(data => {
                addAdminMessage(data.response, false);
            })
            .catch(error => {
                addAdminMessage('Error: Could not process training message.', false);
                console.error('Error:', error);
            });
    }
}

function addAdminMessage(text, isUser) {
    const adminChatMessages = document.getElementById('adminChatMessages');
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${isUser ? 'user' : 'ai'}`;
    messageDiv.innerHTML = `<div class="message-content">${text}</div>`;
    adminChatMessages.appendChild(messageDiv);
    adminChatMessages.scrollTop = adminChatMessages.scrollHeight;
}