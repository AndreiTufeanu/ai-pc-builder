// Tab switching
document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        // Remove active class from all tabs and panes
        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.tab-pane').forEach(p => p.classList.remove('active'));

        // Add active class to clicked tab and corresponding pane
        btn.classList.add('active');
        document.getElementById(btn.dataset.tab).classList.add('active');
    });
});

// Component management functions
function showAddComponentForm() {
    document.getElementById('addComponentForm').classList.remove('hidden');
}

function hideAddComponentForm() {
    document.getElementById('addComponentForm').classList.add('hidden');
    document.getElementById('componentForm').reset();
}

// Load components (simulated)
function loadComponents() {
    const componentsList = document.getElementById('componentsList');
    componentsList.innerHTML = '<p>Loading components...</p>';

    // Simulated API call
    setTimeout(() => {
        componentsList.innerHTML = `
            <div class="component-card">
                <h3>AMD Ryzen 7 7800X3D</h3>
                <p class="component-type">CPU</p>
                <p class="component-specs">Socket: AM5, Cores: 8, TDP: 120W</p>
                <p class="component-price">€389.99</p>
                <div class="component-actions-card">
                    <button class="btn btn-warning btn-sm">Edit</button>
                    <button class="btn btn-danger btn-sm">Delete</button>
                </div>
            </div>
            <div class="component-card">
                <h3>NVIDIA GeForce RTX 4070</h3>
                <p class="component-type">GPU</p>
                <p class="component-specs">Memory: 12GB GDDR6X, TDP: 200W</p>
                <p class="component-price">€549.99</p>
                <div class="component-actions-card">
                    <button class="btn btn-warning btn-sm">Edit</button>
                    <button class="btn btn-danger btn-sm">Delete</button>
                </div>
            </div>
        `;
    }, 1000);
}

// Admin chat functionality
const adminChatInput = document.getElementById('adminChatInput');
const adminSendBtn = document.getElementById('adminSendBtn');
const adminChatMessages = document.getElementById('adminChatMessages');

function addAdminMessage(text, isUser) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${isUser ? 'user' : 'ai'}`;
    messageDiv.innerHTML = `<div class="message-content">${text}</div>`;
    adminChatMessages.appendChild(messageDiv);
    adminChatMessages.scrollTop = adminChatMessages.scrollHeight;
}

adminSendBtn.addEventListener('click', () => {
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
});

adminChatInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        adminSendBtn.click();
    }
});

// Component form submission
document.getElementById('componentForm').addEventListener('submit', function(e) {
    e.preventDefault();

    const formData = {
        name: document.getElementById('componentName').value,
        type: document.getElementById('componentType').value,
        description: document.getElementById('componentDescription').value,
        price: document.getElementById('componentPrice').value ?
            parseFloat(document.getElementById('componentPrice').value) : null
    };

    fetch('/api/admin/components', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData)
    })
        .then(response => response.json())
        .then(component => {
            alert('Component added successfully!');
            hideAddComponentForm();
            loadComponents();
        })
        .catch(error => {
            alert('Error adding component');
            console.error('Error:', error);
        });
});

// Load components on page load
loadComponents();