const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const net = require('net');
const path = require('path');

const app = express();
const PORT = process.env.PORT || 3000;

// Serve the simulator static files
app.use(express.static(__dirname));

// Express route for homepage fallback
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'index.html'));
});

// Setup HTTP server
const server = http.createServer(app);

// Setup WebSocket server
const wss = new WebSocket.Server({ server });

wss.on('connection', (ws) => {
    console.log('[Proxy] Client connected to WebSocket gateway');
    
    let tcpSocket = null;
    let authCallback = null;
    let commandCallbacks = new Map();
    let currentRequestId = 1010;

    // Helper: Build RCON packet buffer
    function buildRconPacket(id, type, payload) {
        const payloadBuffer = Buffer.from(payload, 'utf8');
        // Packet layout: Length(4) + RequestID(4) + Type(4) + Payload + 2 Null terminators
        const packetSize = 4 + 4 + payloadBuffer.length + 2; 
        const buffer = Buffer.alloc(4 + packetSize);

        buffer.writeInt32LE(packetSize, 0);     // Total Length Header
        buffer.writeInt32LE(id, 4);             // Request ID
        buffer.writeInt32LE(type, 8);           // Packet Type
        payloadBuffer.copy(buffer, 12);         // Payload String
        buffer.writeUInt8(0, 12 + payloadBuffer.length);     // First Null byte
        buffer.writeUInt8(0, 13 + payloadBuffer.length);     // Second Null byte

        return buffer;
    }

    // Helper: Parse RCON packets from raw TCP buffer
    function parseRconPackets(buffer) {
        const packets = [];
        let offset = 0;

        while (offset + 4 <= buffer.length) {
            const length = buffer.readInt32LE(offset);
            if (offset + 4 + length > buffer.length) {
                break; // incomplete packet, wait for more data
            }

            const id = buffer.readInt32LE(offset + 4);
            const type = buffer.readInt32LE(offset + 8);
            
            // Extract payload string (until null byte starting at byte 12)
            const payloadStart = offset + 12;
            const payloadEnd = offset + 4 + length - 2; // Subtract length header and the 2 padding bytes
            
            let endIdx = payloadStart;
            for (let i = payloadStart; i < payloadEnd; i++) {
                if (buffer[i] === 0) {
                    endIdx = i;
                    break;
                }
            }
            
            const payload = buffer.toString('utf8', payloadStart, endIdx);
            
            packets.push({ id, type, payload });
            offset += 4 + length;
        }

        return { packets, remaining: buffer.slice(offset) };
    }

    let tcpBuffer = Buffer.alloc(0);

    ws.on('message', (message) => {
        let msg;
        try {
            msg = JSON.parse(message);
        } catch (e) {
            console.error('[Proxy] Invalid JSON payload from client');
            return;
        }

        if (msg.action === 'connect') {
            const { ip, port, password } = msg;
            console.log(`[Proxy] Connecting to Minecraft RCON at ${ip}:${port}`);

            // Close existing socket if any
            if (tcpSocket) {
                tcpSocket.destroy();
            }

            tcpSocket = new net.Socket();
            tcpBuffer = Buffer.alloc(0);

            tcpSocket.connect(port, ip, () => {
                console.log('[Proxy] TCP socket established. Sending RCON Auth packet.');
                
                // RCON login is packet type 3 (SERVERDATA_AUTH) with login ID 1000
                const authPacket = buildRconPacket(1000, 3, password);
                tcpSocket.write(authPacket);
            });

            tcpSocket.on('data', (chunk) => {
                tcpBuffer = Buffer.concat([tcpBuffer, chunk]);
                const { packets, remaining } = parseRconPackets(tcpBuffer);
                tcpBuffer = remaining;

                for (const packet of packets) {
                    console.log(`[Proxy] Received TCP RCON response. ID: ${packet.id}, Type: ${packet.type}`);
                    
                    if (packet.id === 1000 || packet.id === -1) {
                        // Auth outcome response
                        if (packet.id === 1000) {
                            console.log('[Proxy] RCON authenticated successfully!');
                            ws.send(JSON.stringify({ event: 'auth', success: true }));
                        } else {
                            console.log('[Proxy] RCON authentication failed (Invalid credentials)');
                            ws.send(JSON.stringify({ event: 'auth', success: false }));
                            tcpSocket.destroy();
                        }
                    } else {
                        // Standard Command response (type 2 or type 0)
                        ws.send(JSON.stringify({
                            event: 'response',
                            requestId: packet.id,
                            output: packet.payload
                        }));
                    }
                }
            });

            tcpSocket.on('error', (err) => {
                console.error('[Proxy] TCP Socket error:', err.message);
                ws.send(JSON.stringify({ event: 'error', message: err.message }));
            });

            tcpSocket.on('close', () => {
                console.log('[Proxy] TCP socket connection closed');
                ws.send(JSON.stringify({ event: 'disconnected' }));
                tcpSocket = null;
            });
        }

        if (msg.action === 'command') {
            if (!tcpSocket) {
                ws.send(JSON.stringify({ event: 'error', message: 'Socket disconnected. Reconnect first.' }));
                return;
            }

            currentRequestId++;
            const cmdPacket = buildRconPacket(currentRequestId, 2, msg.cmd); // Type 2 = SERVERDATA_EXECCOMMAND
            tcpSocket.write(cmdPacket);
            console.log(`[Proxy] Sent command [ID ${currentRequestId}]: "${msg.cmd}"`);
        }
    });

    ws.on('close', () => {
        console.log('[Proxy] Client disconnected from WebSocket gateway');
        if (tcpSocket) {
            tcpSocket.destroy();
        }
    });
});

server.listen(PORT, () => {
    console.log(`==================================================================`);
    console.log(`  MINE_CONSOLE RCON WEB SIMULATOR & CLIENT PROXY STARTED`);
    console.log(`  Access dashboard: http://localhost:${PORT}`);
    console.log(`==================================================================`);
});
