import Foundation
import Network
import Combine

public enum RCONError: Error {
    case connectionFailed
    case authenticationFailed
    case invalidResponse
    case disconnected
}

public final class RCONClient: ObservableObject {
    @Published public var isConnected = false
    @Published public var isAuthenticated = false
    @Published public var logStream: [String] = []
    
    private var connection: NWConnection?
    private let queue = DispatchQueue(label: "com.mineconsole.network")
    private var currentRequestID: Int32 = 42
    
    // Callbacks or publishers for response
    private var commandResponseCallback: ((String?, Error?) -> Void)?
    
    public init() {}
    
    public func connect(host: String, port: Int, password: String) {
        let nwHost = NWEndpoint.Host(host)
        let nwPort = NWEndpoint.Port(rawValue: UInt16(port)) ?? 25575
        
        let parameters = NWParameters.tcp
        connection = NWConnection(host: nwHost, port: nwPort, using: parameters)
        
        connection?.stateUpdateHandler = { [weak self] state in
            guard let self = self else { return }
            switch state {
            case .ready:
                DispatchQueue.main.async {
                    self.isConnected = true
                    self.addLog("[System] TCP Socket Connected. Authenticating...")
                    self.authenticate(password: password)
                }
                self.startReceive()
            case .failed(let error):
                DispatchQueue.main.async {
                    self.isConnected = false
                    self.isAuthenticated = false
                    self.addLog("[Error] Connection failed: \(error.localizedDescription)")
                }
                self.disconnect()
            case .cancelled:
                DispatchQueue.main.async {
                    self.isConnected = false
                    self.isAuthenticated = false
                    self.addLog("[System] Connection cancelled.")
                }
            default:
                break
            }
        }
        
        connection?.start(queue: queue)
    }
    
    public func disconnect() {
        connection?.cancel()
        connection = nil
        DispatchQueue.main.async {
            self.isConnected = false
            self.isAuthenticated = false
        }
    }
    
    private func addLog(_ message: String) {
        DispatchQueue.main.async {
            self.logStream.append(message)
        }
    }
    
    private func authenticate(password: String) {
        let packetID: Int32 = 1000 // Login packet ID
        let loginPacket = RCONPacket(id: packetID, type: 3, payload: password) // Type 3 = SERVERDATA_AUTH
        
        sendPacket(loginPacket) { [weak self] error in
            if let error = error {
                self?.addLog("[Error] Failed to send auth packet: \(error.localizedDescription)")
                self?.disconnect()
            }
        }
    }
    
    public func sendCommand(_ command: String, completion: @escaping (String?, Error?) -> Void) {
        guard isAuthenticated else {
            completion(nil, RCONError.authenticationFailed)
            return
        }
        
        currentRequestID += 1
        let commandPacket = RCONPacket(id: currentRequestID, type: 2, payload: command) // Type 2 = SERVERDATA_EXECCOMMAND
        
        commandResponseCallback = completion
        
        sendPacket(commandPacket) { [weak self] error in
            if let error = error {
                completion(nil, error)
                self?.commandResponseCallback = nil
            }
        }
    }
    
    private func sendPacket(_ packet: RCONPacket, completion: @escaping (Error?) -> Void) {
        let data = packet.encode()
        connection?.send(content: data, completion: .contentProcessed({ error in
            completion(error)
        }))
    }
    
    private func startReceive() {
        // First read 4 bytes length header
        connection?.receive(minimumIncompleteLength: 4, maximumLength: 4) { [weak self] data, context, isComplete, error in
            guard let self = self else { return }
            
            if let error = error {
                self.addLog("[Error] Receive header error: \(error.localizedDescription)")
                self.disconnect()
                return
            }
            
            guard let data = data, data.count == 4 else {
                if isComplete {
                    self.disconnect()
                } else {
                    self.startReceive()
                }
                return
            }
            
            // Convert length from little endian Int32
            let length = data.withUnsafeBytes { buffer in
                buffer.load(fromByteOffset: 0, as: Int32.self)
            }
            
            // Read the remaining bytes (length specifies the packet content size)
            self.connection?.receive(minimumIncompleteLength: Int(length), maximumLength: Int(length)) { payloadData, _, isCompletePayload, payloadError in
                if let payloadError = payloadError {
                    self.addLog("[Error] Receive payload error: \(payloadError.localizedDescription)")
                    self.disconnect()
                    return
                }
                
                if let payloadData = payloadData, payloadData.count == Int(length) {
                    self.handleIncomingPacket(data: payloadData)
                }
                
                if isCompletePayload {
                    self.disconnect()
                } else {
                    self.startReceive()
                }
            }
        }
    }
    
    private func handleIncomingPacket(data: Data) {
        guard data.count >= 8 else { return }
        
        let id = data.withUnsafeBytes { buffer in
            buffer.load(fromByteOffset: 0, as: Int32.self)
        }
        _ = data.withUnsafeBytes { buffer in
            buffer.load(fromByteOffset: 4, as: Int32.self)
        }
        
        // Extract string payload (until first null byte starting at offset 8)
        let payloadData = data.subdata(in: 8..<data.count)
        
        // Find index of first null byte in payload
        var endOfPayload = payloadData.count
        for i in 0..<payloadData.count {
            if payloadData[i] == 0 {
                endOfPayload = i
                break
            }
        }
        
        let payloadString = String(decoding: payloadData.subdata(in: 0..<endOfPayload), as: UTF8.self)
        
        if id == 1000 || id == -1 {
            // Login Response (Type 2 = SERVERDATA_AUTH_RESPONSE)
            DispatchQueue.main.async {
                if id == 1000 {
                    self.isAuthenticated = true
                    self.addLog("[System] Authenticated Successfully! Ready for commands.")
                } else {
                    self.isAuthenticated = false
                    self.addLog("[Error] Authentication Failed: Invalid password.")
                    self.disconnect()
                }
            }
        } else {
            // Command Response (Type 2 = SERVERDATA_RESPONSE_VALUE)
            DispatchQueue.main.async {
                self.addLog(payloadString)
                if let callback = self.commandResponseCallback {
                    callback(payloadString, nil)
                    self.commandResponseCallback = nil
                }
            }
        }
    }
}

// Low-level Struct for RCON Packet
struct RCONPacket {
    let id: Int32
    let type: Int32
    let payload: String
    
    func encode() -> Data {
        let payloadData = payload.data(using: .utf8) ?? Data()
        let length = Int32(4 + 4 + payloadData.count + 2) // ID (4) + Type (4) + Payload length + 2 null terminators
        
        var data = Data()
        // Length header
        var tempLength = length
        withUnsafeBytes(of: &tempLength) { data.append(contentsOf: $0) }
        
        // Request ID
        var tempID = id
        withUnsafeBytes(of: &tempID) { data.append(contentsOf: $0) }
        
        // Packet Type
        var tempType = type
        withUnsafeBytes(of: &tempType) { data.append(contentsOf: $0) }
        
        // Payload string bytes
        data.append(payloadData)
        // Two null padding bytes
        data.append(0x00)
        data.append(0x00)
        
        return data
    }
}
