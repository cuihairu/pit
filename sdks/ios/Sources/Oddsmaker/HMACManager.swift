import Foundation
import CryptoKit

/**
 * HMAC签名管理器
 * 提供请求签名和验证功能
 */
public class HMACManager {
    
    /**
     * 签名算法
     */
    public enum Algorithm {
        case sha256
        case sha384
        case sha512
        
        var cryptoAlgorithm: HMAC<SHA256>.Algorithm? {
            switch self {
            case .sha256: return nil // 使用默认
            case .sha384: return nil
            case .sha512: return nil
            }
        }
    }
    
    private let secret: String
    private let algorithm: Algorithm
    
    /**
     * 初始化HMAC管理器
     * - Parameters:
     *   - secret: 密钥
     *   - algorithm: 签名算法
     */
    public init(secret: String, algorithm: Algorithm = .sha256) {
        self.secret = secret
        self.algorithm = algorithm
    }
    
    /**
     * 生成请求签名
     * - Parameters:
     *   - timestamp: 时间戳
     *   - body: 请求体
     *   - method: HTTP方法
     *   - path: 请求路径
     * - Returns: 签名字符串
     */
    public func generateSignature(timestamp: String, body: Data?, method: String = "POST", path: String = "/v1/batch") -> String {
        let bodyString = body.flatMap { String(data: $0, encoding: .utf8) } ?? ""
        let message = "\(timestamp)\n\(method.uppercased())\n\(path)\n\(bodyString)"
        
        guard let messageData = message.data(using: .utf8),
              let secretData = secret.data(using: .utf8) else {
            return ""
        }
        
        let signature: Data
        switch algorithm {
        case .sha256:
            let hmac = HMAC<SHA256>.authenticationCode(for: messageData, using: SymmetricKey(data: secretData))
            signature = Data(hmac)
        case .sha384:
            let hmac = HMAC<SHA384>.authenticationCode(for: messageData, using: SymmetricKey(data: secretData))
            signature = Data(hmac)
        case .sha512:
            let hmac = HMAC<SHA512>.authenticationCode(for: messageData, using: SymmetricKey(data: secretData))
            signature = Data(hmac)
        }
        
        return signature.map { String(format: "%02x", $0) }.joined()
    }
    
    /**
     * 生成签名头
     * - Parameters:
     *   - timestamp: 时间戳
     *   - body: 请求体
     *   - method: HTTP方法
     *   - path: 请求路径
     * - Returns: 签名头值，格式为 "t=<timestamp>,s=<signature>"
     */
    public func generateSignatureHeader(timestamp: String, body: Data?, method: String = "POST", path: String = "/v1/batch") -> String {
        let signature = generateSignature(timestamp: timestamp, body: body, method: method, path: path)
        return "t=\(timestamp),s=\(signature)"
    }
    
    /**
     * 验证签名
     * - Parameters:
     *   - signature: 要验证的签名
     *   - timestamp: 时间戳
     *   - body: 请求体
     *   - method: HTTP方法
     *   - path: 请求路径
     * - Returns: 签名是否有效
     */
    public func verifySignature(_ signature: String, timestamp: String, body: Data?, method: String = "POST", path: String = "/v1/batch") -> Bool {
        let expectedSignature = generateSignature(timestamp: timestamp, body: body, method: method, path: path)
        return signature == expectedSignature
    }
    
    /**
     * 为URLRequest添加签名头
     * - Parameters:
     *   - request: 要签名的URLRequest
     *   - body: 请求体（如果为nil则使用request的httpBody）
     * - Returns: 添加了签名头的URLRequest
     */
    public func signRequest(_ request: URLRequest, body: Data? = nil) -> URLRequest {
        var signedRequest = request
        let timestamp = String(Int(Date().timeIntervalSince1970))
        let requestBody = body ?? request.httpBody
        let path = request.url?.path ?? "/v1/batch"
        let method = request.httpMethod ?? "POST"
        
        let signatureHeader = generateSignatureHeader(
            timestamp: timestamp,
            body: requestBody,
            method: method,
            path: path
        )
        
        signedRequest.setValue(signatureHeader, forHTTPHeaderField: "x-signature")
        signedRequest.setValue(timestamp, forHTTPHeaderField: "x-timestamp")
        
        return signedRequest
    }
    
    /**
     * 生成时间戳
     * - Returns: 当前时间的Unix时间戳（秒）
     */
    public static func generateTimestamp() -> String {
        return String(Int(Date().timeIntervalSince1970))
    }
    
    /**
     * 检查时间戳是否在有效期内
     * - Parameters:
     *   - timestamp: 要检查的时间戳
     *   - maxAgeSec: 最大有效期（秒）
     * - Returns: 时间戳是否在有效期内
     */
    public static func isTimestampValid(_ timestamp: String, maxAgeSec: TimeInterval = 300) -> Bool {
        guard let timestampDouble = Double(timestamp) else {
            return false
        }
        
        let now = Date().timeIntervalSince1970
        return abs(now - timestampDouble) <= maxAgeSec
    }
}