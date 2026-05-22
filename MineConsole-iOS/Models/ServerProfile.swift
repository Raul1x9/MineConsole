import Foundation
import SwiftData

@Model
public final class ServerProfile {
    @Attribute(.unique) public var id: UUID
    public var name: String
    public var ip: String
    public var rconPort: Int
    public var keychainKey: String
    public var sharedRole: String // "Owner", "Viewer", "Moderator", "Admin"
    public var creationDate: Date
    
    public init(id: UUID = UUID(), name: String, ip: String, rconPort: Int, keychainKey: String, sharedRole: String = "Owner", creationDate: Date = Date()) {
        self.id = id
        self.name = name
        self.ip = ip
        self.rconPort = rconPort
        self.keychainKey = keychainKey
        self.sharedRole = sharedRole
        self.creationDate = creationDate
    }
}
