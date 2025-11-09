// User dashboard functionality

// Mode switching
const chatModeBtn = document.getElementById('chatModeBtn');
const guidedModeBtn = document.getElementById('guidedModeBtn');
const chatMode = document.getElementById('chatMode');
const guidedMode = document.getElementById('guidedMode');

chatModeBtn.addEventListener('click', () => {
    chatModeBtn.classList.add('active');
    guidedModeBtn.classList.remove('active');
    chatMode.style.display = 'flex';
    guidedMode.style.display = 'none';
});

guidedModeBtn.addEventListener('click', () => {
    guidedModeBtn.classList.add('active');
    chatModeBtn.classList.remove('active');
    chatMode.style.display = 'none';
    guidedMode.style.display = 'block';
});

// Chat functionality
const chatInput = document.getElementById('chatInput');
const sendBtn = document.getElementById('sendBtn');
const chatMessages = document.getElementById('chatMessages');

function addMessage(text, isUser) {
    const messageDiv = document.createElement('div');
    messageDiv.className = `message ${isUser ? 'user' : 'ai'}`;
    messageDiv.innerHTML = `<div class="message-content">${text}</div>`;
    chatMessages.appendChild(messageDiv);
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

sendBtn.addEventListener('click', () => {
    const message = chatInput.value.trim();
    if (message) {
        addMessage(message, true);
        chatInput.value = '';

        fetch('/api/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ message: message })
        })
            .then(response => response.json())
            .then(data => {
                addMessage(data.response, false);
            })
            .catch(error => {
                addMessage('Error connecting to AI. Make sure Ollama is running!', false);
                console.error('Error:', error);
            });
    }
});

chatInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') {
        sendBtn.click();
    }
});

// Guided mode functionality
let currentStep = 1;
const totalSteps = 7;
const prevBtn = document.getElementById('prevBtn');
const nextBtn = document.getElementById('nextBtn');
const progressFill = document.getElementById('progressFill');
const stepTitle = document.getElementById('stepTitle');
const stepDescription = document.getElementById('stepDescription');

const stepTitles = [
    'Budget & Preferences',
    'Processor (CPU)',
    'Graphics Card (GPU)',
    'Motherboard',
    'Memory (RAM)',
    'Storage',
    'Power Supply (PSU)'
];

const stepDescriptions = [
    "Let's start with your overall budget and basic preferences",
    "Choose your processor platform and preferences",
    "Select your graphics card requirements",
    "Pick your motherboard specifications",
    "Configure your RAM preferences",
    "Choose your storage options",
    "Select your power supply requirements"
];

function updateStep() {
    document.querySelectorAll('.step-content').forEach(step => step.classList.add('hidden'));
    document.getElementById(`step${currentStep}`).classList.remove('hidden');

    progressFill.style.width = `${(currentStep / totalSteps) * 100}%`;
    stepTitle.textContent = `Step ${currentStep}: ${stepTitles[currentStep - 1]}`;
    stepDescription.textContent = stepDescriptions[currentStep - 1];

    prevBtn.style.display = currentStep === 1 ? 'none' : 'block';
    nextBtn.textContent = currentStep === totalSteps ? 'Generate Build →' : 'Next →';
}

prevBtn.addEventListener('click', () => {
    if (currentStep > 1) {
        currentStep--;
        updateStep();
    }
});

nextBtn.addEventListener('click', () => {
    if (currentStep < totalSteps) {
        currentStep++;
        updateStep();
    } else {
        alert('Build generation will be connected to your Spring AI backend!');
    }
});

updateStep();