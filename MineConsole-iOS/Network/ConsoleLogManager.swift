import Foundation
import Combine

public final class ConsoleLogManager: ObservableObject {
    public static let shared = ConsoleLogManager()
    
    @Published public var logs: [UUID: [String]] = [:]
    
    private init() {}
    
    public func getLogs(for serverId: UUID) -> [String] {
        return logs[serverId] ?? []
    }
    
    public func addLog(for serverId: UUID, message: String) {
        DispatchQueue.main.async {
            if self.logs[serverId] == nil {
                self.logs[serverId] = []
            }
            self.logs[serverId]?.append(message)
        }
    }
    
    public func clearLogs(for serverId: UUID) {
        DispatchQueue.main.async {
            self.logs[serverId] = []
        }
    }
}
