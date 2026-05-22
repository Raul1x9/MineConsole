/* ==========================================================================
   MineConsole Simulator Client - Core JS Logic
   ========================================================================== */

// Audio Synthesizer (Web Audio API)
class AudioSynth {
    constructor() {
        this.ctx = null;
    }
    
    init() {
        if (!this.ctx) {
            this.ctx = new (window.AudioContext || window.webkitAudioContext)();
        }
        if (this.ctx.state === 'suspended') {
            this.ctx.resume();
        }
    }
    
    playClick() {
        this.init();
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.connect(gain);
        gain.connect(this.ctx.destination);
        
        osc.frequency.setValueAtTime(1000, this.ctx.currentTime);
        gain.gain.setValueAtTime(0.05, this.ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.05);
        
        osc.start();
        osc.stop(this.ctx.currentTime + 0.05);
    }
    
    playSuccess() {
        this.init();
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.connect(gain);
        gain.connect(this.ctx.destination);
        
        osc.type = 'triangle';
        osc.frequency.setValueAtTime(523.25, this.ctx.currentTime); // C5
        osc.frequency.setValueAtTime(659.25, this.ctx.currentTime + 0.1); // E5
        osc.frequency.setValueAtTime(783.99, this.ctx.currentTime + 0.2); // G5
        osc.frequency.setValueAtTime(1046.50, this.ctx.currentTime + 0.3); // C6
        
        gain.gain.setValueAtTime(0.08, this.ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.45);
        
        osc.start();
        osc.stop(this.ctx.currentTime + 0.45);
    }
    
    playError() {
        this.init();
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.connect(gain);
        gain.connect(this.ctx.destination);
        
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(150, this.ctx.currentTime);
        osc.frequency.linearRampToValueAtTime(100, this.ctx.currentTime + 0.25);
        
        gain.gain.setValueAtTime(0.12, this.ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.25);
        
        osc.start();
        osc.stop(this.ctx.currentTime + 0.25);
    }

    playScanner() {
        this.init();
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.connect(gain);
        gain.connect(this.ctx.destination);
        
        osc.type = 'sine';
        osc.frequency.setValueAtTime(300, this.ctx.currentTime);
        osc.frequency.linearRampToValueAtTime(600, this.ctx.currentTime + 0.6);
        osc.frequency.linearRampToValueAtTime(300, this.ctx.currentTime + 1.2);
        
        gain.gain.setValueAtTime(0.03, this.ctx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 1.2);
        
        osc.start();
        osc.stop(this.ctx.currentTime + 1.2);
    }
}

const synth = new AudioSynth();

// Application State Management
const STATE = {
    servers: [],
    activeServer: null,
    commandHistory: [],
    tailscaleEnabled: true,
    biometricsRequired: true,
    isUnlocked: false,
    activeRole: 'Owner', // 'Owner', 'Viewer', 'Moderator', 'Admin'
    isGuestMode: false,
    simulatedLogsInterval: null,
    websocket: null,
    wsConnected: false,
    wsAuthenticated: false,
    pendingRequestId: null
};

// Initial Data Seed (Simulated SwiftData)
const DEFAULT_SERVERS = [
    { id: 'srv-1', name: 'Survival Server', ip: '100.89.4.12', port: 25575, password: 'rconpassword', role: 'Owner' },
    { id: 'srv-2', name: 'Creative Hub', ip: '100.76.12.98', port: 25575, password: 'creativepass', role: 'Moderator' }
];

// Load local database
function loadDatabase() {
    const saved = localStorage.getItem('mineconsole_servers');
    if (saved) {
        STATE.servers = JSON.parse(saved);
    } else {
        STATE.servers = [...DEFAULT_SERVERS];
        saveDatabase();
    }
}

function saveDatabase() {
    localStorage.setItem('mineconsole_servers', JSON.stringify(STATE.servers));
}

// ==========================================================================
// RCON Local WebSocket Proxy Connection
// ==========================================================================
function initWebSocketProxy() {
    const proxyStatusEl = document.getElementById('proxy-status');
    const rconStatusEl = document.getElementById('rcon-status');

    // Create WS client to NodeJS Proxy
    STATE.websocket = new WebSocket(`ws://${window.location.host || 'localhost:3000'}`);

    STATE.websocket.onopen = () => {
        STATE.wsConnected = true;
        proxyStatusEl.textContent = 'CONNECTED';
        proxyStatusEl.className = 'status-value online';
        console.log('[WebSocket] Connected to local proxy gateway');
    };

    STATE.websocket.onmessage = (event) => {
        let msg;
        try {
            msg = JSON.parse(event.data);
        } catch (e) {
            return;
        }

        switch (msg.event) {
            case 'auth':
                if (msg.success) {
                    STATE.wsAuthenticated = true;
                    rconStatusEl.textContent = 'AUTHENTICATED';
                    rconStatusEl.className = 'status-value online';
                    
                    document.getElementById('console-connection-label').textContent = 'RCON TCP LINK ACTIVE';
                    document.getElementById('console-connection-label').className = 'font-terminal font-sm text-green';
                    document.querySelector('.status-indicator-dot').className = 'status-indicator-dot online';
                    
                    appendLogLine('[System] Proxy authenticated successfully with server RCON. Remote terminal ready.', 'text-green');
                    synth.playSuccess();
                } else {
                    STATE.wsAuthenticated = false;
                    rconStatusEl.textContent = 'AUTH FAILED';
                    rconStatusEl.className = 'status-value offline';
                    
                    document.getElementById('console-connection-label').textContent = 'AUTHENTICATION FAILED';
                    document.getElementById('console-connection-label').className = 'font-terminal font-sm text-red';
                    document.querySelector('.status-indicator-dot').className = 'status-indicator-dot offline';
                    
                    appendLogLine('[Error] RCON authentication rejected. Check password credentials in Keychain.', 'text-red');
                    synth.playError();
                }
                break;
            case 'response':
                // Command executed output
                appendLogLine(msg.output || 'Command delivered (Empty response).', 'text-white');
                break;
            case 'disconnected':
                STATE.wsAuthenticated = false;
                rconStatusEl.textContent = 'DISCONNECTED';
                rconStatusEl.className = 'status-value offline';
                
                document.getElementById('console-connection-label').textContent = 'CONNECTION CLOSED';
                document.getElementById('console-connection-label').className = 'font-terminal font-sm text-red';
                document.querySelector('.status-indicator-dot').className = 'status-indicator-dot offline';
                
                appendLogLine('[System] Server connection closed.', 'text-gray');
                break;
            case 'error':
                appendLogLine(`[Error] socket exception: ${msg.message}`, 'text-red');
                synth.playError();
                break;
        }
    };

    STATE.websocket.onclose = () => {
        STATE.wsConnected = false;
        STATE.wsAuthenticated = false;
        proxyStatusEl.textContent = 'OFFLINE (Simulated)';
        proxyStatusEl.className = 'status-value offline';
        rconStatusEl.textContent = 'OFFLINE';
        rconStatusEl.className = 'status-value offline';
        console.log('[WebSocket] Local proxy is offline. Standard simulation mode active.');
    };
}

// ==========================================================================
// iOS Navigation & Screens
// ==========================================================================
function showScreen(screenId) {
    document.querySelectorAll('.app-screen').forEach(s => s.classList.add('hidden'));
    document.getElementById(screenId).classList.remove('hidden');
}

// Biometric Face ID scanning mockup
function triggerFaceIDUnlock() {
    synth.playClick();
    if (!STATE.biometricsRequired) {
        STATE.isUnlocked = true;
        showScreen('screen-dashboard');
        return;
    }

    const lockStatus = document.getElementById('lock-status');
    const scanner = document.querySelector('.scanner-container');
    
    lockStatus.textContent = 'Scanning face matrix credentials...';
    scanner.classList.remove('success');
    synth.playScanner();

    // Start grid sweep animations (controlled via CSS classes)
    scanner.style.color = '#2ecc71';

    setTimeout(() => {
        // Success auth outcome
        scanner.classList.add('success');
        lockStatus.textContent = 'System Authentication Succeeded.';
        synth.playSuccess();

        setTimeout(() => {
            STATE.isUnlocked = true;
            showScreen('screen-dashboard');
        }, 800);

    }, 1800);
}

// ==========================================================================
// Dashboard rendering & actions
// ==========================================================================
function renderServerList() {
    const list = document.getElementById('server-list');
    list.innerHTML = '';

    STATE.servers.forEach(server => {
        const card = document.createElement('div');
        card.className = 'server-card fade-in';
        
        // Compute connection state (Tailscale dependent)
        const orbClass = STATE.tailscaleEnabled ? 'online' : 'offline';
        
        card.innerHTML = `
            <div class="status-orb ${orbClass}"></div>
            <div class="server-card-info">
                <h4>${server.name.toUpperCase()}</h4>
                <p>${server.ip}:${server.port}</p>
            </div>
            <div class="server-card-role">${server.role}</div>
            <div class="server-card-arrow">❯</div>
        `;

        card.addEventListener('click', () => {
            openConsole(server);
        });

        list.appendChild(card);
    });

    // Populate share dropdown
    const shareDropdown = document.getElementById('share-server');
    shareDropdown.innerHTML = '';
    STATE.servers.forEach(s => {
        const opt = document.createElement('option');
        opt.value = s.id;
        opt.textContent = s.name.toUpperCase();
        shareDropdown.appendChild(opt);
    });
}

// Open RCON Console
function openConsole(server) {
    synth.playClick();
    
    // Safety check Tailscale connection
    if (!STATE.tailscaleEnabled) {
        showVpnAlert();
        synth.playError();
        return;
    }

    STATE.activeServer = server;
    
    // Set UI Details
    document.getElementById('console-server-title').textContent = server.name.toUpperCase();
    document.getElementById('console-server-ip').textContent = `${server.ip}:${server.port}`;
    
    const roleTag = document.getElementById('console-role-tag');
    const userRole = STATE.isGuestMode ? STATE.activeRole : server.role;
    roleTag.textContent = userRole.toUpperCase();
    
    // Apply access level restrictions
    const lockedPane = document.getElementById('console-locked-pane');
    const activePane = document.getElementById('console-active-pane');
    const presetsBar = document.getElementById('console-presets');

    if (userRole === 'Viewer') {
        lockedPane.classList.remove('hidden');
        activePane.classList.add('hidden');
        presetsBar.classList.add('hidden');
    } else {
        lockedPane.classList.add('hidden');
        activePane.classList.remove('hidden');
        presetsBar.classList.remove('hidden');
        
        // Preset rules for Moderator presets
        const listBtn = presetsBar.querySelector('[data-cmd="/list"]');
        const weatherBtn = presetsBar.querySelector('[data-cmd="/weather clear"]');
        const timeBtn = presetsBar.querySelector('[data-cmd="/time set day"]');
    }

    // Connect RCON
    const term = document.getElementById('terminal-screen');
    term.innerHTML = ''; // clear screen
    appendLogLine(`[System] Directing socket tunnel to RCON service...`, 'text-yellow');

    if (STATE.wsConnected) {
        // Send connect action to NodeJS Proxy RCON server
        appendLogLine(`[System] Handshaking via Local Proxy Gateway...`, 'text-yellow');
        STATE.websocket.send(JSON.stringify({
            action: 'connect',
            ip: server.ip,
            port: server.port,
            password: server.password
        }));
    } else {
        // Fallback to beautiful mock terminal simulation logs
        appendLogLine(`[System] Host environment offline. Initializing simulated RCON engine...`, 'text-yellow');
        document.getElementById('console-connection-label').textContent = 'SIMULATED LIVE MONITOR';
        document.getElementById('console-connection-label').className = 'font-terminal font-sm text-green';
        
        setTimeout(() => {
            runMockStartupLogs();
        }, 600);
    }

    showScreen('screen-console');
}

// Terminal line writer helper
function appendLogLine(text, styleClass = 'text-white') {
    const term = document.getElementById('terminal-screen');
    const line = document.createElement('div');
    line.className = `log-line ${styleClass} fade-in`;
    
    // Check if line contains authentic Minecraft codes and colorize
    line.innerHTML = formatMinecraftColors(text);
    term.appendChild(line);
    
    // Auto Scroll to bottom
    term.scrollTop = term.scrollHeight;
}

// Minecraft style color formatting logic
function formatMinecraftColors(text) {
    let clean = text
        .replace(/&0/g, '<span style="color:#000000">')
        .replace(/&1/g, '<span style="color:#0000aa">')
        .replace(/&2/g, '<span style="color:#00aa00">')
        .replace(/&3/g, '<span style="color:#00aaaa">')
        .replace(/&4/g, '<span style="color:#aa0000">')
        .replace(/&5/g, '<span style="color:#aa00aa">')
        .replace(/&6/g, '<span style="color:#ffaa00">')
        .replace(/&7/g, '<span style="color:#aaaaaa">')
        .replace(/&8/g, '<span style="color:#555555">')
        .replace(/&9/g, '<span style="color:#5555ff">')
        .replace(/&a/g, '<span style="color:#55ff55">')
        .replace(/&b/g, '<span style="color:#55ffff">')
        .replace(/&c/g, '<span style="color:#ff5555">')
        .replace(/&d/g, '<span style="color:#ff55ff">')
        .replace(/&e/g, '<span style="color:#ffff55">')
        .replace(/&f/g, '<span style="color:#ffffff">')
        .replace(/&r/g, '</span>');
    
    // count open spans and close them if necessary
    const openCount = (clean.match(/<span/g) || []).length;
    const closeCount = (clean.match(/<\/span/g) || []).length;
    for (let i = 0; i < (openCount - closeCount); i++) {
        clean += '</span>';
    }
    return clean;
}

// Simulated active logs generator
function runMockStartupLogs() {
    if (STATE.simulatedLogsInterval) clearInterval(STATE.simulatedLogsInterval);

    const nowStr = () => new Date().toLocaleTimeString();
    
    appendLogLine(`[${nowStr()} INFO]: Starting minecraft server version 1.20.4`, 'text-gray');
    appendLogLine(`[${nowStr()} INFO]: Loading properties`, 'text-gray');
    appendLogLine(`[${nowStr()} INFO]: Default game type: SURVIVAL`, 'text-gray');
    
    setTimeout(() => {
        appendLogLine(`[${nowStr()} INFO]: Preparing level "world"`, 'text-gray');
        appendLogLine(`[${nowStr()} INFO]: Preparing start region for dimension minecraft:overworld`, 'text-gray');
    }, 400);

    setTimeout(() => {
        appendLogLine(`[${nowStr()} INFO]: Preparing spawn area: 89%`, 'text-gray');
        appendLogLine(`[${nowStr()} INFO]: &a[MineConsole] Connection fully authenticated. ready to accept commands.`, 'text-green');
        appendLogLine(`[${nowStr()} INFO]: notch joined the game`, 'text-blue');
        appendLogLine(`[${nowStr()} INFO]: &e<notch> &fWhat's up administrators! MineConsole is sick.`, 'text-white');
    }, 1000);

    // Dynamic log spawner loop
    let tickCount = 0;
    STATE.simulatedLogsInterval = setInterval(() => {
        if (!STATE.tailscaleEnabled) return; // Freeze log stream if VPN breaks
        
        tickCount++;
        const now = nowStr();
        const rand = Math.random();

        if (rand < 0.2) {
            const players = ['steve', 'alex', 'herobrine', 'Dinnerbone'];
            const p = players[Math.floor(Math.random() * players.length)];
            if (Math.random() > 0.5) {
                appendLogLine(`[${now} INFO]: ${p} joined the game`, 'text-blue');
            } else {
                appendLogLine(`[${now} INFO]: ${p} left the game`, 'text-blue');
            }
        } else if (rand < 0.45) {
            const chats = [
                "Anyone want to trade some emeralds?",
                "Watch out! There is a creeper near the spawn portal!",
                "Mining down at Y=-58 is paying off, diamonds found!",
                "Check out the iron farm near coordinate 120, -500.",
                "Admin can you set the weather to clear please?"
            ];
            const p = ['notch', 'steve', 'alex'][Math.floor(Math.random() * 3)];
            const c = chats[Math.floor(Math.random() * chats.length)];
            appendLogLine(`[${now} INFO]: &e<${p}> &f${c}`, 'text-white');
        } else if (rand < 0.6) {
            // Server status log
            appendLogLine(`[${now} WARN]: Can't keep up! Is the server overloaded? Running 2004ms or 40 ticks behind.`, 'text-yellow');
        } else if (rand < 0.7) {
            appendLogLine(`[${now} INFO]: notch was slain by herobrine`, 'text-red');
        }
    }, 4500);
}

// Stop dynamic logs
function clearMockInterval() {
    if (STATE.simulatedLogsInterval) {
        clearInterval(STATE.simulatedLogsInterval);
        STATE.simulatedLogsInterval = null;
    }
}

// ==========================================================================
// RCON COMMAND PARSER (Browser Simulator)
// ==========================================================================
function runSimulatedCommand(cmd) {
    const now = new Date().toLocaleTimeString();
    
    // Command privilege checks for Moderator
    const activeRole = STATE.isGuestMode ? STATE.activeRole : STATE.activeServer.role;
    if (activeRole === 'Moderator') {
        const destructiveCommands = ['/stop', '/ban', '/op', '/deop', '/whitelist'];
        const firstWord = cmd.split(' ')[0].toLowerCase();
        
        if (destructiveCommands.includes(firstWord)) {
            synth.playError();
            appendLogLine(`[${now} WARN]: &c[Security Alert] Permission denied. Command ${firstWord} restricted for MODERATOR tier.`, 'text-red');
            return;
        }
    }

    appendLogLine(`> ${cmd}`, 'text-green');
    synth.playClick();

    // Store in history
    if (!STATE.commandHistory.includes(cmd)) {
        STATE.commandHistory.push(cmd);
        renderHistoryList();
    }

    setTimeout(() => {
        const parts = cmd.trim().split(' ');
        const base = parts[0].toLowerCase();
        
        switch (base) {
            case '/list':
                appendLogLine(`[${now} INFO]: There are 4 of a max of 20 players online: notch, steve, alex, Dinnerbone`, 'text-white');
                break;
            case '/tps':
                appendLogLine(`[${now} INFO]: TPS from last 1m, 5m, 15m: &a20.00&f, &a19.98&f, &a19.96`, 'text-white');
                break;
            case '/weather':
                const w = parts[1] || 'clear';
                appendLogLine(`[${now} INFO]: Changed weather state to [${w}]`, 'text-white');
                break;
            case '/time':
                const t = parts[2] || 'day';
                appendLogLine(`[${now} INFO]: Adjusted global server time scale to ${t}`, 'text-white');
                break;
            case '/op':
                const p = parts[1] || 'notch';
                appendLogLine(`[${now} INFO]: Made &e${p}&f a server operator`, 'text-white');
                break;
            case '/ban':
                const target = parts[1] || 'herobrine';
                appendLogLine(`[${now} INFO]: Banned player &c${target}&f: Security exile protocol.`, 'text-white');
                break;
            case '/kick':
                const k = parts[1] || 'steve';
                appendLogLine(`[${now} INFO]: Kicked player &c${k}&f: Kicked by administrator command.`, 'text-white');
                break;
            case '/say':
                const msg = parts.slice(1).join(' ') || 'Hello Server!';
                appendLogLine(`[${now} INFO]: [Server Broadcast] ${msg}`, 'text-yellow');
                break;
            case '/stop':
                appendLogLine(`[${now} INFO]: Stopping the server`, 'text-red');
                appendLogLine(`[${now} INFO]: Saving chunks for level "world"`, 'text-gray');
                appendLogLine(`[${now} INFO]: Thread shut down. Socket closed.`, 'text-red');
                document.getElementById('console-connection-label').textContent = 'SERVER SHUTDOWN DETECTED';
                document.getElementById('console-connection-label').className = 'font-terminal font-sm text-red';
                document.querySelector('.status-indicator-dot').className = 'status-indicator-dot offline';
                synth.playError();
                clearMockInterval();
                break;
            default:
                // Unrecognized fallback command
                appendLogLine(`[${now} INFO]: &cUnknown command. Type /help for help.`, 'text-red');
                synth.playError();
        }
    }, 400);
}

// ==========================================================================
// Tailscale Connection Alerts Toggling
// ==========================================================================
function updateTailscaleState(enabled) {
    STATE.tailscaleEnabled = enabled;
    
    // Sync toggles on both control panel and inside app settings view
    document.getElementById('tailscale-toggle').checked = enabled;
    document.getElementById('settings-vpn-toggle').checked = enabled;
    
    const indicator = document.getElementById('tailscale-indicator');
    const alertBanner = document.getElementById('app-vpn-alert');
    const iosVpn = document.getElementById('ios-vpn');
    
    if (enabled) {
        indicator.textContent = 'TUNNEL SECURE (100.x.x.x)';
        indicator.className = 'control-status active';
        alertBanner.classList.add('hidden');
        iosVpn.classList.remove('hidden');
    } else {
        indicator.textContent = 'TUNNEL INTERRUPTED';
        indicator.className = 'control-status inactive';
        
        // Show alert inside dashboard if active
        alertBanner.classList.remove('hidden');
        iosVpn.classList.add('hidden');
        synth.playError();
        
        // Disconnect active consoles
        if (STATE.activeServer) {
            appendLogLine(`[Error] Tailscale VPN Link disconnected. Port socket closed.`, 'text-red');
            document.getElementById('console-connection-label').textContent = 'TUNNEL LOST (OFFLINE)';
            document.getElementById('console-connection-label').className = 'font-terminal font-sm text-red';
            document.querySelector('.status-indicator-dot').className = 'status-indicator-dot offline';
            clearMockInterval();
        }
    }
}

function showVpnAlert() {
    const alertBanner = document.getElementById('app-vpn-alert');
    alertBanner.classList.remove('hidden');
}

// ==========================================================================
// Command History Panel
// ==========================================================================
function renderHistoryList() {
    const list = document.getElementById('history-list');
    list.innerHTML = '';

    if (STATE.commandHistory.length === 0) {
        list.innerHTML = `<p class="font-terminal font-sm text-gray" style="text-align:center;">History empty.</p>`;
        return;
    }

    STATE.commandHistory.forEach(cmd => {
        const btn = document.createElement('button');
        btn.className = 'history-item-btn font-terminal';
        btn.innerHTML = `<span>❯</span> ${cmd}`;
        
        btn.addEventListener('click', () => {
            document.getElementById('console-cmd-input').value = cmd;
            document.getElementById('sheet-history').classList.add('hidden');
            synth.playClick();
        });
        
        list.appendChild(btn);
    });
}

// ==========================================================================
// Guest Access & Links generator Panel
// ==========================================================================
function updateGuestRoleSelector(role) {
    document.querySelectorAll('.role-btn').forEach(b => {
        b.classList.remove('active');
        if (b.getAttribute('data-role') === role) {
            b.classList.add('active');
        }
    });
    STATE.activeRole = role;
}

function generateGuestToken() {
    const sId = document.getElementById('share-server').value;
    const serverObj = STATE.servers.find(s => s.id === sId) || STATE.servers[0];
    
    const mockKey = Math.random().toString(36).substring(2, 8);
    const token = `mineconsole://guest?key=${mockKey}&role=${STATE.activeRole.toLowerCase()}&srv=${encodeURIComponent(serverObj.name)}`;
    
    document.getElementById('token-link').value = token;
    document.getElementById('token-box').classList.remove('hidden');
    synth.playSuccess();
}

function applyGuestToken() {
    const token = document.getElementById('token-link').value;
    if (!token) return;

    STATE.isGuestMode = true;
    
    // Parse link variables
    const url = new URL(token.replace('mineconsole://', 'http://'));
    const role = url.searchParams.get('role');
    const srvName = url.searchParams.get('srv');
    
    STATE.activeRole = role.charAt(0).toUpperCase() + role.slice(1);
    
    // Highlight guest card on dashboard
    const guestSec = document.getElementById('guest-node-section');
    const guestCard = document.getElementById('guest-active-card');
    
    guestSec.classList.remove('hidden');
    guestCard.className = 'server-card fade-in';
    guestCard.innerHTML = `
        <div class="status-orb online"></div>
        <div class="server-card-info">
            <h4>${srvName.toUpperCase()} [GUEST]</h4>
            <p>100.89.4.12:25575</p>
        </div>
        <div class="server-card-role">${STATE.activeRole}</div>
        <div class="server-card-arrow">❯</div>
    `;

    guestCard.onclick = () => {
        openConsole({
            id: 'guest-mock',
            name: srvName,
            ip: '100.89.4.12',
            port: 25575,
            password: 'guestpassword',
            role: STATE.activeRole
        });
    };

    // Show dashboard
    showScreen('screen-dashboard');
    synth.playSuccess();
}

// ==========================================================================
// Event Listeners Initialization
// ==========================================================================
document.addEventListener('DOMContentLoaded', () => {
    
    loadDatabase();
    initWebSocketProxy();
    renderServerList();

    // Set dynamic iOS time ticker
    setInterval(() => {
        const d = new Date();
        const hrs = String(d.getHours()).padStart(2, '0');
        const mins = String(d.getMinutes()).padStart(2, '0');
        document.getElementById('ios-time').textContent = `${hrs}:${mins}`;
    }, 1000);

    // Left Panel Control Listeners
    document.getElementById('tailscale-toggle').addEventListener('change', (e) => {
        updateTailscaleState(e.target.checked);
    });

    document.getElementById('biometrics-toggle').addEventListener('change', (e) => {
        STATE.biometricsRequired = e.target.checked;
        document.getElementById('settings-faceid-toggle').checked = e.target.checked;
        document.getElementById('biometrics-indicator').textContent = e.target.checked ? 'FaceID Protection Enabled' : 'FaceID protection inactive';
    });

    document.getElementById('trigger-lock').addEventListener('click', () => {
        STATE.isUnlocked = false;
        showScreen('screen-lock');
        document.getElementById('lock-status').textContent = 'Awaiting biometric hardware authorization...';
        document.querySelector('.scanner-container').classList.remove('success');
    });

    document.querySelectorAll('.role-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            synth.playClick();
            updateGuestRoleSelector(e.target.getAttribute('data-role'));
        });
    });

    document.getElementById('generate-token-btn').addEventListener('click', generateGuestToken);
    
    document.getElementById('copy-token-btn').addEventListener('click', () => {
        const linkInput = document.getElementById('token-link');
        linkInput.select();
        document.execCommand('copy');
        synth.playSuccess();
        const copyBtn = document.getElementById('copy-token-btn');
        copyBtn.textContent = 'Copied!';
        setTimeout(() => copyBtn.textContent = 'Copy', 1500);
    });

    document.getElementById('apply-token-btn').addEventListener('click', applyGuestToken);

    // iOS Screen Locks
    document.getElementById('lock-auth-btn').addEventListener('click', triggerFaceIDUnlock);

    // iOS Dashboard navigation
    document.getElementById('nav-settings-btn').addEventListener('click', () => {
        synth.playClick();
        showScreen('screen-settings');
    });

    document.getElementById('settings-close-btn').addEventListener('click', () => {
        synth.playClick();
        showScreen('screen-dashboard');
    });

    document.getElementById('alert-dismiss').addEventListener('click', () => {
        document.getElementById('app-vpn-alert').classList.add('hidden');
        synth.playClick();
    });

    // iOS sheet controls
    document.getElementById('nav-add-server-btn').addEventListener('click', () => {
        synth.playClick();
        document.getElementById('sheet-add-server').classList.remove('hidden');
    });

    document.getElementById('add-sheet-cancel').addEventListener('click', () => {
        synth.playClick();
        document.getElementById('sheet-add-server').classList.add('hidden');
    });

    // App Add Server execution
    document.getElementById('add-sheet-save').addEventListener('click', () => {
        const name = document.getElementById('add-server-name').value.trim();
        const ip = document.getElementById('add-server-ip').value.trim();
        const port = parseInt(document.getElementById('add-server-port').value);
        const pass = document.getElementById('add-server-password').value.trim();
        
        if (!name || !ip || !port || !pass) {
            document.getElementById('add-sheet-error').classList.remove('hidden');
            synth.playError();
            return;
        }

        document.getElementById('add-sheet-error').classList.add('hidden');
        
        // Push record (Simulated SwiftData db transaction)
        STATE.servers.push({
            id: 'srv-' + Math.random().toString(36).substr(2, 9),
            name, ip, port, password: pass, role: 'Owner'
        });
        saveDatabase();
        renderServerList();

        // Clear values
        document.getElementById('add-server-name').value = '';
        document.getElementById('add-server-ip').value = '';
        document.getElementById('add-server-password').value = '';

        document.getElementById('sheet-add-server').classList.add('hidden');
        synth.playSuccess();
    });

    // In-App Settings actions
    document.getElementById('settings-faceid-toggle').addEventListener('change', (e) => {
        STATE.biometricsRequired = e.target.checked;
        document.getElementById('biometrics-toggle').checked = e.target.checked;
        document.getElementById('biometrics-indicator').textContent = e.target.checked ? 'FaceID Protection Enabled' : 'FaceID protection inactive';
    });

    document.getElementById('settings-vpn-toggle').addEventListener('change', (e) => {
        updateTailscaleState(e.target.checked);
    });

    document.getElementById('btn-purge-keychain').addEventListener('click', () => {
        synth.playError();
        if (confirm("Verify keychain purge: This cleans all locked RCON credentials?")) {
            localStorage.clear();
            STATE.servers = [];
            STATE.isGuestMode = false;
            document.getElementById('guest-node-section').classList.add('hidden');
            saveDatabase();
            renderServerList();
            synth.playSuccess();
            showScreen('screen-dashboard');
        }
    });

    // RCON Terminal Inputs
    document.getElementById('console-back-btn').addEventListener('click', () => {
        synth.playClick();
        clearMockInterval();
        STATE.activeServer = null;
        showScreen('screen-dashboard');
    });

    // Preset keys handler
    document.querySelectorAll('.preset-btn').forEach(btn => {
        btn.addEventListener('click', (e) => {
            const cmd = e.target.getAttribute('data-cmd');
            if (STATE.wsConnected && STATE.wsAuthenticated) {
                appendLogLine(`> ${cmd}`, 'text-green');
                STATE.websocket.send(JSON.stringify({ action: 'command', cmd }));
            } else {
                runSimulatedCommand(cmd);
            }
        });
    });

    // Command submission box
    function handleConsoleSubmit() {
        const input = document.getElementById('console-cmd-input');
        const cmd = input.value.trim();
        if (!cmd) return;

        if (STATE.wsConnected && STATE.wsAuthenticated) {
            appendLogLine(`> ${cmd}`, 'text-green');
            STATE.websocket.send(JSON.stringify({ action: 'command', cmd }));
        } else {
            runSimulatedCommand(cmd);
        }
        input.value = '';
    }

    document.getElementById('console-send-btn').addEventListener('click', handleConsoleSubmit);
    document.getElementById('console-cmd-input').addEventListener('keydown', (e) => {
        if (e.key === 'Enter') {
            handleConsoleSubmit();
        }
    });

    // Command History panels
    document.getElementById('history-sheet-btn').addEventListener('click', () => {
        synth.playClick();
        renderHistoryList();
        document.getElementById('sheet-history').classList.remove('hidden');
    });

    document.getElementById('history-sheet-close').addEventListener('click', () => {
        synth.playClick();
        document.getElementById('sheet-history').classList.add('hidden');
    });

});
