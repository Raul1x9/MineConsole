import Foundation
import Network

public final class LocalNetworkPermissionHelper {
    /// Sends a dummy UDP multicast packet to force iOS to present the Local Network Permission dialog.
    public static func triggerPrompt() {
        let host = NWEndpoint.Host("224.0.0.251")
        let port = NWEndpoint.Port(rawValue: 5353) ?? 5353
        
        let connection = NWConnection(host: host, port: port, using: .udp)
        
        connection.stateUpdateHandler = { state in
            switch state {
            case .ready:
                let dummyData = "ping".data(using: .utf8) ?? Data()
                connection.send(content: dummyData, completion: .contentProcessed({ _ in
                    connection.cancel()
                }))
            case .failed:
                connection.cancel()
            default:
                break
            }
        }
        
        connection.start(queue: DispatchQueue.global(qos: .background))
    }
}
