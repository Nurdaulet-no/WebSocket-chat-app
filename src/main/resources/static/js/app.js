
// Global State
let stompClient = null;
let currentUser = null; // { username: string, token: string }
let currentChat = null; // { id: number, name: string, type: string, participants: string[] } // Added participants
let currentSubscription = null;
const API_BASE_URL = 'http://localhost:8080/api';

// DOM Elements (cached) - Ensure DOM is loaded before accessing these
let disconnectBtn, groupNameInput, createGroupBtn,
    privateChatUsernameInput, createPrivateChatBtn, chatListDiv, refreshChatsBtn,
    chatTitle, messagesDiv, messageInput, sendBtn, statusDiv, pmSection,
    pmGroupNameSpan, participantUsernameInput, addParticipantBtn, removeParticipantBtn,
    participantListDiv, logoutBtn, userInfoDiv;

// Function to cache DOM elements after the DOM is ready
function cacheDOMElements() {
    disconnectBtn = document.getElementById('disconnectBtn');
    groupNameInput = document.getElementById('groupName');
    createGroupBtn = document.getElementById('createGroupBtn');
    privateChatUsernameInput = document.getElementById('privateChatUsername');
    createPrivateChatBtn = document.getElementById('createPrivateChatBtn');
    chatListDiv = document.getElementById('chat-list');
    refreshChatsBtn = document.getElementById('refreshChatsBtn');
    chatTitle = document.getElementById('chat-title');
    messagesDiv = document.getElementById('messages');
    messageInput = document.getElementById('messageInput');
    sendBtn = document.getElementById('sendBtn');
    statusDiv = document.getElementById('status');
    // New Participant Management Elements
    pmSection = document.getElementById('participant-management');
    pmGroupNameSpan = document.getElementById('pm-group-name');
    participantUsernameInput = document.getElementById('participantUsername');
    addParticipantBtn = document.getElementById('addParticipantBtn');
    removeParticipantBtn = document.getElementById('removeParticipantBtn');
    participantListDiv = document.getElementById('participant-list');
    logoutBtn = document.getElementById('logoutBtn');
    userInfoDiv = document.getElementById('userInfo');
}

// --- Logging ---
function log(message, type = 'info') {
    if (!messagesDiv) return; // Don't log if DOM not ready
    console.log(`[${type.toUpperCase()}] ${message}`); // Log to browser console as well
    const msgElement = document.createElement('div');
    msgElement.className = `system-message ${type}`; // Use classes for styling flexibility
    const textNode = document.createTextNode(`[${new Date().toLocaleTimeString()}] ${message}`);
    msgElement.appendChild(textNode);
    messagesDiv.appendChild(msgElement);
    scrollToBottom();
}

function logError(message, error) {
    if (!messagesDiv) { // Log to console even if UI not ready
        console.error(`[ERROR] ${message}`, error);
        return;
    }
    console.error(`[ERROR] ${message}`, error);
    const msgElement = document.createElement('div');
    msgElement.classList.add('system-message', 'error'); // Use system-message for styling
    msgElement.textContent = `[${new Date().toLocaleTimeString()}] ERROR: ${message}`;
    if (error && error.message) {
        const details = document.createElement('span');
        details.style.fontSize = '0.8em';
        details.textContent = ` (${error.message})`;
        msgElement.appendChild(details);
    } else if (error && typeof error === 'object') {
        // Attempt to stringify simple objects, avoid complex ones
        try {
            const errorStr = JSON.stringify(error, null, 2); // Pretty print
            if(errorStr.length < 300) { // Avoid huge logs
                const details = document.createElement('pre');
                details.style.fontSize = '0.8em';
                details.style.whiteSpace = 'pre-wrap'; // Wrap long lines
                details.textContent = errorStr;
                msgElement.appendChild(details);
            }
        } catch(e) { /* Ignore stringify errors */ }
    }
    messagesDiv.appendChild(msgElement);
    scrollToBottom();
}

// --- UI State ---
function setUIState(connected) {
    if (!disconnectBtn) return; // Don't run if DOM elements aren't cached yet

    disconnectBtn.disabled = !connected;
    createGroupBtn.disabled = !connected;
    createPrivateChatBtn.disabled = !connected;
    refreshChatsBtn.disabled = !connected;
    groupNameInput.disabled = !connected;
    privateChatUsernameInput.disabled = !connected;
    logoutBtn.disabled = !connected;

    const chatSelected = connected && currentChat;
    messageInput.disabled = !chatSelected;
    sendBtn.disabled = !chatSelected;

    // --- Participant Management UI Logic ---
    const groupChatSelected = chatSelected && currentChat.type === 'GROUP';
    if (groupChatSelected) {
        pmSection.style.display = 'block'; // Show PM section
        pmGroupNameSpan.textContent = currentChat.name || `Group ${currentChat.id}`; // Update title
        addParticipantBtn.disabled = false;
        removeParticipantBtn.disabled = false;
        participantUsernameInput.disabled = false;
    } else {
        pmSection.style.display = 'none'; // Hide PM section
        addParticipantBtn.disabled = true;
        removeParticipantBtn.disabled = true;
        participantUsernameInput.disabled = true;
        participantUsernameInput.value = ''; // Clear input if not group
        participantListDiv.innerHTML = ''; // Clear participant list
    }
    // --- End Participant Management UI ---

    if (connected && currentUser) {
        statusDiv.textContent = `Status: Connected as ${currentUser.username}`;
        userInfoDiv.textContent = `Logged in as: ${currentUser.username}`; // Show username
        chatTitle.textContent = currentChat ? `${currentChat.name} (ID: ${currentChat.id}, Type: ${currentChat.type})` : 'Select a chat';
        if(currentChat) messageInput.focus();
    } else {
        statusDiv.textContent = 'Status: Disconnected';
        userInfoDiv.textContent = 'Not logged in';
        chatTitle.textContent = 'No chat selected';
        chatListDiv.innerHTML = '<p style="color: #888; padding: 10px;">Connect to load chats...</p>';
        messagesDiv.innerHTML = '';
        // pmSection is hidden by the logic above
        currentChat = null;
        currentUser = null;
        currentSubscription = null;
    }
}

// --- JWT Decoding ---
function decodeJwtPayload(token) {
    try {
        const base64Url = token.split('.')[1];
        if (!base64Url) throw new Error("Invalid JWT structure");
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
            return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
        }).join(''));
        return JSON.parse(jsonPayload);
    } catch (e) {
        console.error('Could not decode JWT payload', e);
        logError('Invalid JWT token format.'); // Log error to UI
        return null;
    }
}

// --- API Calls ---
async function apiFetch(url, options = {}) {
    if (!currentUser || !currentUser.token) {
        logError("Authentication token is missing from state.");
        logout(); // Если нет токена в состоянии, выходим и редиректим
        throw new Error("Not authenticated.");
    }
    const token = currentUser.token;

    const defaultHeaders = {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
    const config = {
        ...options,
        headers: {
            ...defaultHeaders,
            ...options.headers,
        },
    };
    try {
        const response = await fetch(API_BASE_URL + url, config);
        if(response.status === 401 || response.status === 403){
            logError("Authentication error (token might be expired/invalid). Redirecting to login.", response.status);
            logout(); // Clear token and disconnect
            return;
        }
        if (!response.ok) {
            let errorData;
            try { errorData = await response.json(); } catch (e) { errorData = await response.text(); }
            logError(`API Error ${response.status}: ${response.statusText}`, { url, responseData: errorData });
            throw new Error(`API Error: ${response.status}`);
        }
        if (response.status === 204 || response.headers.get("content-length") === "0") { return null; }
        const data = await response.json();
        return data;
    } catch(error) {
        if (!error.message?.includes('API Error')) {
            logError(`Network or parsing error during API fetch to ${url}`, error);
        }
        throw error; // Re-throw after logging
    }
}

// --- WebSocket Logic ---
function connect(token) {
    if (!token) {
        logError("Connect function called without token.");
        handleDisconnect(true);
        window.location.href = '/login.html';
        return;
    }

    const payload = decodeJwtPayload(token);
    if (!payload || !payload.sub) {
        logError("Invalid JWT Token found. Clearing and redirecting to login.");
        localStorage.removeItem('jwtToken');
        handleDisconnect(false);
        window.location.href = '/login.html';
        return;
    }
    currentUser = { username: payload.sub, token: token };
    log(`Attempting to connect as ${currentUser.username}...`);

    const socketFactory = () => new SockJS('http://localhost:8080/ws');
    stompClient = new StompJs.Client({
        webSocketFactory: socketFactory,
        connectHeaders: { Authorization: `Bearer ${currentUser.token}` },
        reconnectDelay: 10000,
        debug: (str) => { if(!str.includes('PING') && !str.includes('PONG')) console.log('STOMP:', str); }
    });

    stompClient.onConnect = async (frame) => {
        setUIState(true);
        log('Successfully connected to STOMP broker', 'success');
        await loadUserChats();
    };
    stompClient.onStompError = (frame) => { logError(`Broker error: ${frame.headers['message']}. Details: ${frame.body}`); handleDisconnect(false); };
    stompClient.onWebSocketError = (event) => { logError('WebSocket connection error', event); handleDisconnect(true); };
    stompClient.onWebSocketClose = (event) => {
        log(`WebSocket closed: Code=${event.code}, Reason=${event.reason || 'N/A'}`, 'warn');
        const wasConnected = currentUser !== null;
        handleDisconnect(false);
        if (wasConnected && event.code !== 1000) {
            logError("Unexpected disconnection. Redirecting to login.");
            alert("Connection lost. Please login again.");
            window.location.href = '/login.html';
        }
    };
    try { stompClient.activate(); } catch (error) { logError('STOMP client activation error', error); handleDisconnect(true); }
}

function subscribeToRoom(roomId) {
    if (!stompClient || !stompClient.connected) { logError("Cannot subscribe, client not connected."); return; }
    unsubscribeCurrent();

    const topic = `/topic/chats/${roomId}`;
    log(`Subscribing to ${topic}...`);
    try {
        currentSubscription = stompClient.subscribe(topic, (message) => {
            try {
                const payload = JSON.parse(message.body);
                console.log("Received WS payload:", payload); // Keep for debug

                // --- Logic based on `payload.sender` ---
                if (payload.sender && payload.sender === "System") {
                    log(`System message: ${payload.content}`); // Log system message to UI
                    if (currentChat && currentChat.id === roomId) {
                        displaySystemMessage(payload.content); // Display in chat area
                        // Check if system message implies participant change
                        const lowerContent = payload.content.toLowerCase();
                        if (lowerContent.includes('joined') || lowerContent.includes('left') || lowerContent.includes('added') || lowerContent.includes('removed')) {
                            log("Participant change detected, refreshing chat list...");
                            loadUserChats(); // Refresh list to get updated participant counts/lists
                            // Optionally, if the current chat is the one affected, fetch its details again
                            if (currentChat && currentChat.id === roomId && currentChat.type === 'GROUP') {
                                fetchChatDetails(roomId); // Fetch updated participant list specifically
                            }
                        }
                    }
                } else if (payload.sender && payload.sender !== currentUser.username) {
                    // Message from another user
                    if (currentChat && currentChat.id === roomId) {
                        displayChatMessage(payload.sender, payload.content, payload.createdAt || new Date().toISOString()); // Display message
                    } else {
                        log(`Message for inactive chat ${roomId} from ${payload.sender}.`);
                        const chatItem = document.getElementById(`chat-${roomId}`);
                        if (chatItem) { // Highlight inactive chat
                            chatItem.style.fontWeight = 'bold';
                            chatItem.style.backgroundColor = '#fffacd';
                        }
                    }
                } else if (payload.sender && payload.sender === currentUser.username) {
                    // Own message echoed back - ignore, already displayed optimistically
                } else {
                    logError("Received message with unexpected/missing 'sender'", payload);
                }
            } catch (e) {
                logError("Error parsing or processing WS message: " + message.body, e);
            }
        }, { id: `sub-room-${roomId}` });
        log(`Subscribed successfully to ${topic}`, 'success');
    } catch (error) {
        logError(`Failed to subscribe to ${topic}`, error);
    }
}

function unsubscribeCurrent() {
    if (currentSubscription) {
        try {
            const subId = currentSubscription.id;
            currentSubscription.unsubscribe();
            log(`Unsubscribed from ${subId || 'previous topic'}`);
        } catch (e) { logError("Error unsubscribing", e); }
        currentSubscription = null;
    }
}

function handleDisconnect(logMsg = true) {
    unsubscribeCurrent(); // Ensure unsubscribed
    if (stompClient && stompClient.active) {
        try { stompClient.deactivate(); } catch (e) { logError("Error during STOMP deactivation", e); }
    }
    stompClient = null; // Clear client object
    setUIState(false); // Reset UI and state variables (currentUser, currentChat)
    if (logMsg) log("Connection closed or failed.", 'warn');
}

function disconnect() {
    log("Disconnecting...  / Logging out...");
    handleDisconnect(true);
}

function logout() {
    log("Logging out...");
    if (stompClient && stompClient.active) {
        try { stompClient.deactivate(); } catch (e) { logError("Error during STOMP deactivation", e); }
    }
    stompClient = null;
    localStorage.removeItem('jwtToken');
    currentUser = null;
    currentChat = null;
    currentSubscription = null;
    setUIState(false); // Reset UI
    log("Logged out successfully.", 'success');
    window.location.href = '/login.html';
}

// --- Chat Functionality ---
async function loadUserChats() {
    if (!currentUser || !refreshChatsBtn) return;
    log("Loading user chats...");
    refreshChatsBtn.disabled = true;
    try {
        const chats = await apiFetch('/chats'); // Expects list of ChatRoomDto
        displayChatList(chats);
        log(`Loaded ${chats.length} chats.`, 'success');
    } catch (error) { chatListDiv.innerHTML = '<p class="error">Failed to load chats. Check console.</p>'; }
    finally { if (refreshChatsBtn) refreshChatsBtn.disabled = !currentUser; }
}

function displayChatList(chats) {
    if (!chatListDiv) return;
    chatListDiv.innerHTML = '';
    if (!chats || chats.length === 0) { chatListDiv.innerHTML = '<p style="color: #888; padding:10px;">No chats found.</p>'; return; }

    chats.sort((a, b) => { // Sort groups first, then by name
        if (a.type === 'GROUP' && b.type !== 'GROUP') return -1;
        if (a.type !== 'GROUP' && b.type === 'GROUP') return 1;
        const nameA = a.name || `Chat ${a.id}`;
        const nameB = b.name || `Chat ${b.id}`;
        return nameA.localeCompare(nameB);
    });

    chats.forEach(chat => {
        const chatDiv = document.createElement('div');
        chatDiv.id = `chat-${chat.id}`;
        chatDiv.className = 'chat-item';
        let displayName = chat.name;
        // Use participantUsernames from DTO if available
        let participantUsernames = chat.participantUsernames || [];

        if (chat.type === 'PRIVATE') {
            // Find the other user's name for display
            displayName = participantUsernames.find(u => u !== currentUser.username) || 'Private Chat';
        } else if (!displayName && chat.type === 'GROUP') {
            displayName = `Group ${chat.id}`; // Fallback name
        }

        chatDiv.dataset.chatId = chat.id;
        chatDiv.dataset.chatName = displayName;
        chatDiv.dataset.chatType = chat.type;

        const nameSpan = document.createElement('span');
        nameSpan.className = 'chat-item-name';
        nameSpan.textContent = displayName;

        const typeSpan = document.createElement('span');
        typeSpan.className = 'chat-item-type';
        typeSpan.textContent = chat.type;
        if (chat.type === 'GROUP') { // Add participant count for groups
            typeSpan.textContent += ` (${participantUsernames.length})`;
        }

        chatDiv.appendChild(nameSpan);
        chatDiv.appendChild(typeSpan);

        if (currentChat && currentChat.id === chat.id) { chatDiv.classList.add('selected'); }

        // Pass participant usernames when selecting the chat
        chatDiv.onclick = () => selectChat(chat.id, displayName, chat.type, participantUsernames);
        chatListDiv.appendChild(chatDiv);
    });
}

async function selectChat(id, name, type, participants = []) { // Accept participants
    if (currentChat && currentChat.id === id) {
        // Optional: If clicking the same chat, refresh its details
        if (type === 'GROUP') {
            await fetchChatDetails(id); // Re-fetch details including participants
        }
        return;
    }

    if (currentChat) { // Deselect previous
        const prevItem = document.getElementById(`chat-${currentChat.id}`);
        if (prevItem) prevItem.classList.remove('selected');
    }
    const currentItem = document.getElementById(`chat-${id}`); // Select new
    if (currentItem) {
        currentItem.classList.add('selected');
        currentItem.style.fontWeight = 'normal'; // Clear any notification highlights
        currentItem.style.backgroundColor = '';
    }

    // Store complete chat info including participants
    currentChat = { id, name, type, participants };
    log(`Selected chat: ${name} (ID: ${id}, Type: ${type})`);
    setUIState(true); // Update UI state (enables PM section if group)
    updateParticipantList(participants); // Display the initial participants list

    messagesDiv.innerHTML = ''; // Clear previous messages
    subscribeToRoom(id);
    await loadChatHistory(id);
}

// New function to fetch detailed chat info (specifically for participants)
async function fetchChatDetails(chatId) {
    if (!currentUser || currentChat?.id !== chatId || currentChat?.type !== 'GROUP') return; // Only for current group chat
    log(`Fetching details for chat ${chatId}...`);
    try {
        const chatDto = await apiFetch(`/chats/${chatId}`); // Assuming endpoint GET /api/chats/{id} exists
        if (chatDto && chatDto.participantUsernames) {
            currentChat.participants = chatDto.participantUsernames; // Update state
            currentChat.name = chatDto.name; // Update name if it changed
            updateParticipantList(currentChat.participants); // Update UI
            // Update chat list item display (count, name)
            const chatItem = document.getElementById(`chat-${chatId}`);
            if (chatItem) {
                const nameSpan = chatItem.querySelector('.chat-item-name');
                const typeSpan = chatItem.querySelector('.chat-item-type');
                if(nameSpan) nameSpan.textContent = currentChat.name;
                if (typeSpan) typeSpan.textContent = `${currentChat.type} (${currentChat.participants.length})`;
            }
            log(`Chat ${chatId} details updated.`, 'success');
        }
    } catch (error) {
        logError(`Failed to fetch details for chat ${chatId}`, error);
    }
}

async function loadChatHistory(roomId) {
    if (!currentUser || !messagesDiv) return;
    log(`Loading history for room ${roomId}...`);
    messagesDiv.innerHTML = '<p class="info system-message">Loading messages...</p>';
    try {
        const page = await apiFetch(`/chats/${roomId}/messages?page=0&size=50&sort=createdAt,asc`);
        messagesDiv.innerHTML = ''; // Clear placeholder
        if (page && page.content && page.content.length > 0) {
            page.content.forEach(msg => displayChatMessage(msg.sender, msg.content, msg.createdAt)); // USE SENDER
            log(`Loaded ${page.content.length} messages for room ${roomId}.`);
            setTimeout(scrollToBottom, 100); // Scroll after rendering
        } else {
            messagesDiv.innerHTML = '<p style="color: #888; text-align: center; margin-top: 20px;" class="system-message">No messages in this chat yet.</p>';
        }
    } catch (error) { messagesDiv.innerHTML = '<p class="error system-message">Failed to load messages.</p>'; }
}

function displayChatMessage(sender, content, timestamp) {
    if (!currentUser || !messagesDiv) { console.warn("currentUser not set or DOM not ready, cannot display message"); return; } // Safety check

    const messageDiv = document.createElement('div');
    messageDiv.className = 'message ' + (sender === currentUser.username ? 'sent' : 'received'); // Compare with currentUser.username

    const senderSpan = document.createElement('span');
    senderSpan.className = 'sender';
    senderSpan.textContent = sender; // Show sender's name

    const contentNode = document.createElement('div'); // Use div for content
    contentNode.className = 'content';
    contentNode.textContent = content; // Safely set text

    const timeSpan = document.createElement('span');
    timeSpan.className = 'time';
    timeSpan.textContent = timestamp ? new Date(timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''; // Format time

    messageDiv.appendChild(senderSpan);
    messageDiv.appendChild(contentNode);
    messageDiv.appendChild(timeSpan);
    messagesDiv.appendChild(messageDiv);

    // Scroll logic
    const isScrolledToBottom = messagesDiv.scrollHeight - messagesDiv.clientHeight <= messagesDiv.scrollTop + 80; // Increased buffer
    if (isScrolledToBottom || sender === currentUser.username) {
        scrollToBottom();
    }
}

function displaySystemMessage(content) {
    if (!messagesDiv) return;
    const messageDiv = document.createElement('div');
    messageDiv.className = 'system-message';
    messageDiv.textContent = `--- ${content} ---`; // Simple display
    messagesDiv.appendChild(messageDiv);
    scrollToBottom();
}

function sendMessage() {
    const messageContent = messageInput.value.trim();
    if (!currentChat) { logError("Cannot send, no chat selected!"); return; }
    if (!messageContent) { return; } // Don't send empty
    if (!currentUser) { logError("Cannot send, currentUser not set!"); return; } // Safety check

    if (stompClient && stompClient.connected) {
        const chatMessage = { content: messageContent }; // Backend expects only content
        const destination = `/app/chat/${currentChat.id}/sendMessage`;
        try {
            // Optimistic UI update
            displayChatMessage(currentUser.username, messageContent, new Date().toISOString()); // Use ISO string for consistency

            stompClient.publish({ destination: destination, body: JSON.stringify(chatMessage) });
            messageInput.value = ''; // Clear input
        } catch (error) {
            logError(`Failed to send message to ${destination}`, error);
            // Optional: Remove the optimistically added message on failure
        }
    } else {
        logError('Cannot send message: STOMP client not connected.');
    }
}

// --- Chat Creation ---
async function createGroupChat() {
    const groupName = groupNameInput.value.trim();
    if (!groupName) { logError("Group name cannot be empty!"); return; }
    if (!currentUser) { logError("Connect first!"); return; }
    log(`Attempting to create group: ${groupName}`);
    createGroupBtn.disabled = true;
    try {
        const requestBody = { groupName: groupName }; // Only name needed
        const newChat = await apiFetch('/chats/group', { method: 'POST', body: JSON.stringify(requestBody) });
        log(`Group chat created: ${JSON.stringify(newChat)}`, 'success');
        groupNameInput.value = '';
        await loadUserChats(); // Refresh list
        // Auto-select the new chat
        selectChat(newChat.id, newChat.name, newChat.type, newChat.participantUsernames);
    } catch (error) { alert('Error creating group. Check console.'); }
    finally { if (createGroupBtn) createGroupBtn.disabled = !currentUser; }
}

async function getOrCreatePrivateChat() {
    const username = privateChatUsernameInput.value.trim();
    if (!username) { logError("Username for private chat cannot be empty!"); return; }
    if (!currentUser) { logError("Connect first!"); return; }
    if (username === currentUser.username) { logError("Cannot create a private chat with yourself!"); alert("Cannot create a private chat with yourself."); return; }

    log(`Attempting to get/create private chat with user: ${username}`);
    createPrivateChatBtn.disabled = true;
    try {
        const newChat = await apiFetch(`/chats/private/${username}`, { method: 'POST' });
        log(`Private chat obtained/created: ${JSON.stringify(newChat)}`, 'success');
        privateChatUsernameInput.value = '';
        await loadUserChats();
        // Auto-select the new/existing chat
        selectChat(newChat.id, newChat.name, newChat.type, newChat.participantUsernames);
    } catch (error) { alert('Error getting/creating private chat. Check console.'); }
    finally { if (createPrivateChatBtn) createPrivateChatBtn.disabled = !currentUser; }
}

// --- Participant Management Functions ---
function updateParticipantList(participants = []) { // Default to empty array
    if (!participantListDiv) return; // Check if element exists

    if (!currentChat || currentChat.type !== 'GROUP') {
        participantListDiv.innerHTML = ''; // Clear if not relevant
        return;
    }

    participantListDiv.innerHTML = ''; // Clear previous list

    // Ensure participants is an array
    const participantArray = Array.isArray(participants) ? participants : [];

    if(participantArray.length === 0) {
        participantListDiv.innerHTML = '<span>No participants found (or list empty).</span>';
        return;
    }

    participantArray.sort(); // Sort alphabetically
    participantArray.forEach(username => {
        const pDiv = document.createElement('span'); // Container for name and button
        const nameText = document.createTextNode(username);
        pDiv.appendChild(nameText);

        // Add a delete button next to each participant (except self)
        if (username !== currentUser.username) {
            const removeBtn = document.createElement('button');
            removeBtn.textContent = 'x';
            removeBtn.title = `Remove ${username}`;
            removeBtn.className = 'danger';
            removeBtn.style.marginLeft = '10px';
            removeBtn.style.padding = '1px 5px';
            removeBtn.style.fontSize = '0.8em';
            removeBtn.style.lineHeight = '1'; // Adjust line height
            removeBtn.onclick = (e) => {
                e.stopPropagation(); // Prevent chat selection if clicking button
                if (confirm(`Are you sure you want to remove ${username}?`)) {
                    participantUsernameInput.value = username; // Pre-fill input for clarity
                    removeParticipant();
                }
            };
            pDiv.appendChild(removeBtn);
        } else {
            const selfIndicator = document.createTextNode(" (You)");
            pDiv.appendChild(selfIndicator);
        }
        participantListDiv.appendChild(pDiv);
    });
}

async function addParticipant() {
    const usernameToAdd = participantUsernameInput.value.trim();
    if (!usernameToAdd) { logError("Enter username to add!"); return; }
    if (!currentChat || currentChat.type !== 'GROUP') { logError("Select a group chat first!"); return; }

    log(`Attempting to add ${usernameToAdd} to group ${currentChat.id}`);
    addParticipantBtn.disabled = true;
    removeParticipantBtn.disabled = true;

    try {
        // API: PUT /api/chats/{chatId}/participants/{username}
        const updatedChatDto = await apiFetch(`/chats/${currentChat.id}/participants/${usernameToAdd}`, {
            method: 'PUT'
        });

        log(`${usernameToAdd} added successfully!`, 'success');
        participantUsernameInput.value = ''; // Clear input

        // Update state and UI from the response DTO
        if (updatedChatDto && updatedChatDto.participantUsernames) {
            currentChat.participants = updatedChatDto.participantUsernames;
            updateParticipantList(currentChat.participants); // Refresh list display
            // Update the count in the main chat list
            const chatItem = document.getElementById(`chat-${currentChat.id}`);
            if (chatItem) {
                const typeSpan = chatItem.querySelector('.chat-item-type');
                if (typeSpan) typeSpan.textContent = `${currentChat.type} (${currentChat.participants.length})`;
            }
        } else {
            await fetchChatDetails(currentChat.id); // Fallback
        }

    } catch (error) {
        logError(`Failed to add ${usernameToAdd}`, error);
        alert(`Failed to add participant. Check console/logs.`);
    } finally {
        if (addParticipantBtn && removeParticipantBtn) {
            const groupChatSelected = currentChat && currentChat.type === 'GROUP';
            addParticipantBtn.disabled = !groupChatSelected;
            removeParticipantBtn.disabled = !groupChatSelected;
        }
    }
}

async function removeParticipant() {
    const usernameToRemove = participantUsernameInput.value.trim();
    if (!usernameToRemove) { logError("Enter username to remove!"); return; }
    if (!currentChat || currentChat.type !== 'GROUP') { logError("Select a group chat first!"); return; }
    if (usernameToRemove === currentUser.username) { logError("Cannot remove yourself this way."); alert("Cannot remove yourself."); return; }

    log(`Attempting to remove ${usernameToRemove} from group ${currentChat.id}`);
    addParticipantBtn.disabled = true;
    removeParticipantBtn.disabled = true;

    try {
        // API: DELETE /api/chats/{chatId}/participants/{username}
        await apiFetch(`/chats/${currentChat.id}/participants/${usernameToRemove}`, {
            method: 'DELETE'
        });

        log(`${usernameToRemove} removed successfully!`, 'success');
        participantUsernameInput.value = ''; // Clear input

        // Manually update the participant list in the state
        if (currentChat.participants) {
            currentChat.participants = currentChat.participants.filter(p => p !== usernameToRemove);
            updateParticipantList(currentChat.participants); // Refresh the UI list
            // Update the count in the main chat list
            const chatItem = document.getElementById(`chat-${currentChat.id}`);
            if (chatItem) {
                const typeSpan = chatItem.querySelector('.chat-item-type');
                if (typeSpan) typeSpan.textContent = `${currentChat.type} (${currentChat.participants.length})`;
            }
        } else {
            await fetchChatDetails(currentChat.id); // Fallback
        }

    } catch (error) {
        logError(`Failed to remove ${usernameToRemove}`, error);
        alert(`Failed to remove participant. Check console/logs.`);
    } finally {
        if (addParticipantBtn && removeParticipantBtn) {
            const groupChatSelected = currentChat && currentChat.type === 'GROUP';
            addParticipantBtn.disabled = !groupChatSelected;
            removeParticipantBtn.disabled = !groupChatSelected;
        }
    }
}

// --- Initialization and Event Listeners ---

// Helper to scroll messages
function scrollToBottom() {
    // Use requestAnimationFrame for smoother scrolling after DOM updates
    requestAnimationFrame(() => {
        if(messagesDiv) {
            messagesDiv.scrollTop = messagesDiv.scrollHeight;
        }
    });
}

// --- Initialize the application after the DOM is fully loaded ---
document.addEventListener('DOMContentLoaded', () => {
    // Cache DOM elements now that they exist
    cacheDOMElements();

    const token = localStorage.getItem('jwtToken');
    if (token) {
        log("Found JWT token in storage, attempting to connect...");
        connect(token); // Try to connect automatically
    } else {
        log("No JWT token found. Please login.", "warn");
        setUIState(false);
        window.location.href = '/login.html';
    }

    // Add event listeners that rely on DOM elements
    messageInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter' && !sendBtn.disabled) {
            sendMessage();
        }
    });

    participantUsernameInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter' && !addParticipantBtn.disabled) {
            addParticipant();
        }
    });

    window.logout = logout;
    window.createGroupChat = createGroupChat;
    window.getOrCreatePrivateChat = getOrCreatePrivateChat;
    window.addParticipant = addParticipant;
    window.removeParticipant = removeParticipant;
    window.loadUserChats = loadUserChats;
    window.sendMessage = sendMessage;
    window.disconnect = disconnect;

    log("Application initialized.");
});