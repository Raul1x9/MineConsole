import Foundation
import Combine

public final class PaperMSMPClient: ObservableObject {
    @Published public var isConnected = false
    @Published public var isAuthenticated = false
    @Published public var logStream: [String] = []
    
    private var webSocketTask: URLSessionWebSocketTask?
    private let queue = DispatchQueue(label: "com.mineconsole.msmp")
    private var currentRequestID: Int = 1
    private var serverId: UUID?
    private var callbacks: [Int: (String?, Error?) -> Void] = [:]
    
    public init() {}
    
    public func connect(host: String, port: Int, secret: String, useTLS: Bool, serverId: UUID? = nil) {
        self.serverId = serverId
        
        let protocolStr = useTLS ? "wss" : "ws"
        guard let url = URL(string: "\(protocolStr)://\(host):\(port)") else {
            addLog("[Error] Invalid connection URL.")
            return
        }
        
        addLog("[System] Paper MSMP WS connecting to \(host):\(port)...")
        
        var request = URLRequest(url: url)
        request.timeoutInterval = 5
        request.setValue("Bearer \(secret)", forHTTPHeaderField: "Authorization")
        
        let session = URLSession(configuration: .default)
        webSocketTask = session.webSocketTask(with: request)
        webSocketTask?.resume()
        
        self.ping()
        self.startReceive()
    }
    
    public func disconnect() {
        webSocketTask?.cancel(with: .normalClosure, reason: nil)
        webSocketTask = nil
        DispatchQueue.main.async {
            self.isConnected = false
            self.isAuthenticated = false
        }
        addLog("[System] MSMP Connection Cancelled.")
    }
    
    private func ping() {
        webSocketTask?.sendPing { [weak self] error in
            guard let self = self else { return }
            if let error = error {
                self.addLog("[Error] MSMP Connection failed: \(error.localizedDescription)")
                self.disconnect()
            } else {
                DispatchQueue.main.async {
                    self.isConnected = true
                    self.isAuthenticated = true
                }
                self.addLog("[System] MSMP Socket Connected & Authenticated.")
            }
        }
    }
    
    private func addLog(_ message: String) {
        DispatchQueue.main.async {
            self.logStream.append(message)
            if let serverId = self.serverId {
                ConsoleLogManager.shared.addLog(for: serverId, message: message)
            }
        }
    }
    
    public func sendCommand(_ command: String, completion: @escaping (String?, Error?) -> Void) {
        guard isConnected else {
            completion(nil, NSError(domain: "PaperMSMP", code: -1, userInfo: [NSLocalizedDescriptionKey: "WebSocket disconnected"]))
            return
        }
        
        currentRequestID += 1
        let reqID = currentRequestID
        callbacks[reqID] = completion
        
        let payload: [String: Any] = [
            "jsonrpc": "2.0",
            "method": "minecraft:server/run_command",
            "params": ["command": command],
            "id": reqID
        ]
        
        guard let data = try? JSONSerialization.data(withJSONObject: payload),
              let jsonString = String(data: data, encoding: .utf8) else {
            completion(nil, NSError(domain: "PaperMSMP", code: -2, userInfo: [NSLocalizedDescriptionKey: "Serialization error"]))
            return
        }
        
        let message = URLSessionWebSocketTask.Message.string(jsonString)
        webSocketTask?.send(message) { [weak self] error in
            if let error = error {
                completion(nil, error)
                self?.callbacks.removeValue(forKey: reqID)
            }
        }
    }
    
    private func startReceive() {
        webSocketTask?.receive { [weak self] result in
            guard let self = self else { return }
            switch result {
            case .success(let message):
                switch message {
                case .string(let text):
                    self.handleIncomingMessage(text)
                case .data(let data):
                    if let text = String(data: data, encoding: .utf8) {
                        self.handleIncomingMessage(text)
                    }
                @unknown default:
                    break
                }
                self.startReceive()
            case .failure(let error):
                if self.isConnected {
                    self.addLog("[Error] MSMP Connection error: \(error.localizedDescription)")
                    self.disconnect()
                }
            }
        }
    }
    
    private func handleIncomingMessage(_ text: String) {
        guard let data = text.data(using: .utf8) else { return }
        
        do {
            if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] {
                if let id = json["id"] as? Int {
                    let callback = callbacks.removeValue(forKey: id)
                    
                    if let resultObj = json["result"] {
                        let output: String
                        if let dict = resultObj as? [String: Any], let out = dict["output"] as? String {
                            output = out
                        } else {
                            output = String(describing: resultObj)
                        }
                        self.addLog(output)
                        callback?(output, nil)
                    } else if let errorObj = json["error"] as? [String: Any] {
                        let errorMsg = errorObj["message"] as? String ?? "Unknown error"
                        self.addLog("[Error] \(errorMsg)")
                        callback?(nil, NSError(domain: "PaperMSMP", code: -3, userInfo: [NSLocalizedDescriptionKey: errorMsg]))
                    }
                } else if let method = json["method"] as? String {
                    if method.hasPrefix("notification:") {
                        let params = json["params"] as? [String: Any] ?? [:]
                        let logMessage = parseNotification(method: method, params: params)
                        self.addLog(logMessage)
                    }
                }
            }
        } catch {
            self.addLog(text)
        }
    }
    
    private func parseNotification(method: String, params: [String: Any]) -> String {
        let cleanMethod = method.replacingOccurrences(of: "notification:", with: "")
        
        switch cleanMethod {
        case "chat_message":
            let sender = params["sender"] as? String ?? "System"
            let message = params["message"] as? String ?? ""
            return "<\(sender)> \(message)"
        case "players/joined":
            let name = params["name"] as? String ?? "Unknown Player"
            return "[System] \(name) joined the game"
        case "players/left":
            let name = params["name"] as? String ?? "Unknown Player"
            return "[System] \(name) left the game"
        case "server/log":
            return params["message"] as? String ?? ""
        default:
            return "[\(cleanMethod)] \(params.description)"
        }
    }
}
