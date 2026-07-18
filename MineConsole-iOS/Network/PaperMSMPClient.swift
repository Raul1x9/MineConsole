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
    
    private struct PendingCmdInfo {
        let originalCommand: String
        let successMessage: String?
        let formatter: (([String: Any]) -> String)?
    }
    
    private var pendingCommandInfos: [Int: PendingCmdInfo] = [:]
    
    private struct TranslatedCommand {
        let method: String
        let params: Any
        let successMessage: String?
        let formatter: (([String: Any]) -> String)?
        
        init(method: String, params: Any, successMessage: String? = nil, formatter: (([String: Any]) -> String)? = nil) {
            self.method = method
            self.params = params
            self.successMessage = successMessage
            self.formatter = formatter
        }
    }
    
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
        
        let translated = translateCommand(command)
        if translated.method == "unsupported" {
            completion(nil, NSError(domain: "PaperMSMP", code: -3, userInfo: [NSLocalizedDescriptionKey: "MSMP does not support running arbitrary console commands. Please use RCON connection type for full console shell access."]))
            return
        }
        pendingCommandInfos[reqID] = PendingCmdInfo(originalCommand: command, successMessage: translated.successMessage, formatter: translated.formatter)
        
        var payload: [String: Any] = [:]
        payload["jsonrpc"] = "2.0"
        payload["method"] = translated.method
        if let dict = translated.params as? [String: Any], !dict.isEmpty {
            payload["params"] = dict
        } else if let arr = translated.params as? [Any], !arr.isEmpty {
            payload["params"] = arr
        }
        payload["id"] = reqID
        
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
                self?.pendingCommandInfos.removeValue(forKey: reqID)
            }
        }
    }
    
    private func translateCommand(_ input: String) -> TranslatedCommand {
        let clean = input.trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: "/", with: "").trimmingCharacters(in: .whitespacesAndNewlines)
        let parts = clean.components(separatedBy: .whitespacesAndNewlines).filter { !$0.isEmpty }
        if parts.isEmpty {
            return TranslatedCommand(method: "minecraft:server/run_command", params: ["command": input])
        }
        
        let base = parts[0].lowercased()
        
        switch base {
        case "list":
            return TranslatedCommand(
                method: "minecraft:players",
                params: [String: Any](),
                formatter: { (response: [String: Any]) -> String in
                    let result = response["result"]
                    let playersArray: [[String: Any]]?
                    if let resultDict = result as? [String: Any] {
                        playersArray = (resultDict["players"] as? [[String: Any]]) ?? (resultDict["list"] as? [[String: Any]])
                    } else {
                        playersArray = result as? [[String: Any]]
                    }
                    
                    if let players = playersArray, !players.isEmpty {
                        let names = players.compactMap { $0["name"] as? String }.filter { !$0.isEmpty }
                        return "Connected Players: \(names.joined(separator: ", "))"
                    }
                    
                    // Try parsing as string list
                    if let resultDict = result as? [String: Any], let playersStr = resultDict["players"] as? [String] {
                        return "Connected Players: \(playersStr.joined(separator: ", "))"
                    } else if let playersStr = result as? [String] {
                        return "Connected Players: \(playersStr.joined(separator: ", "))"
                    }
                    
                    return "No players online."
                }
            )
        case "whitelist":
            if parts.count >= 2 {
                let sub = parts[1].lowercased()
                if sub == "add", parts.count >= 3 {
                    let player = parts[2]
                    return TranslatedCommand(
                        method: "minecraft:allowlist/add",
                        params: [[["name": player]]],
                        successMessage: "Added \(player) to the whitelist."
                    )
                } else if sub == "remove", parts.count >= 3 {
                    let player = parts[2]
                    return TranslatedCommand(
                        method: "minecraft:allowlist/remove",
                        params: [[["name": player]]],
                        successMessage: "Removed \(player) from the whitelist."
                    )
                } else if sub == "list" {
                    return TranslatedCommand(
                        method: "minecraft:allowlist/list",
                        params: [String: Any](),
                        formatter: { (response: [String: Any]) -> String in
                            let result = response["result"]
                            let playersArray = (result as? [[String: Any]]) ?? ((result as? [String: Any])?["players"] as? [[String: Any]])
                            
                            if let players = playersArray, !players.isEmpty {
                                let names = players.compactMap { $0["name"] as? String }.filter { !$0.isEmpty }
                                return "Whitelisted Players: \(names.joined(separator: ", "))"
                            }
                            
                            if let playersStr = result as? [String] {
                                return "Whitelisted Players: \(playersStr.joined(separator: ", "))"
                            }
                            
                            return "Whitelist is empty."
                        }
                    )
                }
            }
        case "op":
            if parts.count >= 2 {
                let player = parts[1]
                return TranslatedCommand(
                    method: "minecraft:operators/add",
                    params: [[["name": player]]],
                    successMessage: "Opped \(player)."
                )
            }
        case "deop":
            if parts.count >= 2 {
                let player = parts[1]
                return TranslatedCommand(
                    method: "minecraft:operators/remove",
                    params: [[["name": player]]],
                    successMessage: "De-opped \(player)."
                )
            }
        case "stop":
            return TranslatedCommand(
                method: "minecraft:server/stop",
                params: [String: Any](),
                successMessage: "Stopping the server..."
            )
        case "save-all":
            return TranslatedCommand(
                method: "minecraft:server/save",
                params: [String: Any](),
                successMessage: "Saving the world..."
            )
        default:
            break
        }
        
        return TranslatedCommand(
            method: "unsupported",
            params: [String: Any]()
        )
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
                    let cmdInfo = pendingCommandInfos.removeValue(forKey: id)
                    
                    if let resultObj = json["result"] {
                        let output: String
                        if let formatter = cmdInfo?.formatter {
                            output = formatter(json)
                        } else if let successMsg = cmdInfo?.successMessage {
                            output = successMsg
                        } else if let dict = resultObj as? [String: Any], let out = dict["output"] as? String {
                            output = out
                        } else {
                            output = String(describing: resultObj)
                        }
                        self.addLog(output)
                        callback?(output, nil)
                    } else if let errorObj = json["error"] as? [String: Any] {
                        let errorCode = errorObj["code"] as? Int ?? 0
                        let errorMsg = errorObj["message"] as? String ?? "Unknown error"
                        
                        let displayMsg: String
                        if errorCode == -32601 {
                            displayMsg = "[Error] Command execution failed: Vanilla MSMP does not support running arbitrary console commands. Try using specific supported commands like /list, /whitelist, /op, /deop, /stop, or /save-all."
                        } else {
                            displayMsg = "[Error] \(errorMsg)"
                        }
                        
                        self.addLog(displayMsg)
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
