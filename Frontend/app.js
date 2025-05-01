const chatWindow = document.getElementById('chat-window');
const messageInput = document.getElementById('message-input');
const sendButton = document.getElementById('send-button');
const loadingSpinner = document.getElementById('loading-spinner');

// Use a placeholder for the backend URL, which will be replaced by the entrypoint script
const backendUrl = 'https://veribot-app.delightfulpond-28f1b055.brazilsouth.azurecontainerapps.io';

sendButton.addEventListener('click', sendMessage);
messageInput.addEventListener('keypress', function(e) {
    if (e.key === 'Enter') {
        sendMessage();
    }
});

function sendMessage() {
    const messageText = messageInput.value.trim();
    if (messageText === '') return;

    displayMessage(messageText, 'user');
    messageInput.value = '';
    showSpinner();

    // Send message to backend
    fetch(`${backendUrl}/api/veribot`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({ text: messageText }),
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        return response.json();
    })
    .then(data => {
        hideSpinner();
        if (data && data.type === 'message' && data.text) {
            displayMessage(data.text, 'bot');
        } else {
            displayMessage('Error: Invalid response format from server.', 'bot');
        }
    })
    .catch(error => {
        hideSpinner();
        console.error('Fetch error:', error);
        displayMessage(`Error sending message: ${error.message}`, 'bot');
    });
}

function displayMessage(text, sender) {
    const messageContainer = document.createElement('div');
    messageContainer.classList.add('message', sender === 'user' ? 'user-message' : 'bot-message');

    if (sender === 'bot' && text.includes('VERIFICATION RESULTS SUMMARY:')) {
        // Parse and format the bot's structured response
        const parts = text.split(/\n(?=TRUTHFULNESS:|JUSTIFICATION:|SOURCES USED:)/);

        const summaryPart = parts.find(p => p.startsWith('VERIFICATION RESULTS SUMMARY:'));
        const truthfulnessPart = parts.find(p => p.startsWith('TRUTHFULNESS:'));
        const justificationPart = parts.find(p => p.startsWith('JUSTIFICATION:'));
        const sourcesPart = parts.find(p => p.startsWith('SOURCES USED:'));

        if (summaryPart) {
            const summaryContent = summaryPart.replace('VERIFICATION RESULTS SUMMARY:', '').trim();
            const summaryDiv = document.createElement('div');
            summaryDiv.classList.add('bot-message-section', 'bot-message-summary');
            const title = document.createElement('strong');
            title.textContent = 'Summary:';
            summaryDiv.appendChild(title);
            summaryDiv.appendChild(document.createTextNode(' ' + summaryContent));
            messageContainer.appendChild(summaryDiv);
        }

        if (truthfulnessPart) {
            const truthfulnessContent = truthfulnessPart.replace('TRUTHFULNESS:', '').trim();
            const truthfulnessDiv = document.createElement('div');
            truthfulnessDiv.classList.add('bot-message-section', 'bot-message-truthfulness');
            const title = document.createElement('strong');
            title.textContent = 'Truthfulness:';
            truthfulnessDiv.appendChild(title);
            truthfulnessDiv.appendChild(document.createTextNode(' ' + truthfulnessContent));
            messageContainer.appendChild(truthfulnessDiv);
        }

        if (justificationPart) {
            const justificationContent = justificationPart.replace('JUSTIFICATION:', '').trim();
            const justificationDiv = document.createElement('div');
            justificationDiv.classList.add('bot-message-section', 'bot-message-justification');
            const title = document.createElement('strong');
            title.textContent = 'Justification:';
            justificationDiv.appendChild(title);
            justificationDiv.appendChild(document.createTextNode(' ' + justificationContent));
            messageContainer.appendChild(justificationDiv);
        }

        if (sourcesPart) {
            const sourcesContent = sourcesPart.replace('SOURCES USED:', '').trim();
            const sourcesDiv = document.createElement('div');
            sourcesDiv.classList.add('bot-message-section', 'bot-message-sources');
            const title = document.createElement('strong');
            title.textContent = 'Sources Used:';
            sourcesDiv.appendChild(title);
            sourcesDiv.appendChild(document.createTextNode(' ' + sourcesContent));
            messageContainer.appendChild(sourcesDiv);
        }

    } else {
        // Display regular text message
        messageContainer.textContent = text;
    }

    chatWindow.appendChild(messageContainer);
    // Scroll to the bottom
    chatWindow.scrollTop = chatWindow.scrollHeight;
}

function showSpinner() {
    loadingSpinner.style.display = 'block';
    chatWindow.scrollTop = chatWindow.scrollHeight; // Scroll down to show spinner
}

function hideSpinner() {
    loadingSpinner.style.display = 'none';
}

// Initial message or setup if needed
// displayMessage('Welcome to the chat!', 'bot');