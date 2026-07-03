import Foundation

/**
 * Oddsmaker SDK错误类型
 */
public enum OddsmakerError: Error, LocalizedError {
    case notInitialized
    case invalidURL(String)
    case invalidResponse(statusCode: Int, message: String?)
    case networkError(Error)
    case serializationError(Error)
    case invalidEventData(String)
    case queueFull(maxSize: Int)
    case rateLimitExceeded(retryAfter: TimeInterval?)
    case authenticationFailed(message: String?)
    case permissionDenied(message: String?)
    case serverError(statusCode: Int, message: String?)
    case timeout
    case unknown(Error?)
    
    public var errorDescription: String? {
        switch self {
        case .notInitialized:
            return "SDK not initialized. Call initSDK() first."
        case .invalidURL(let url):
            return "Invalid URL: \(url)"
        case .invalidResponse(let statusCode, let message):
            return "Invalid response with status code \(statusCode): \(message ?? "unknown error")"
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        case .serializationError(let error):
            return "Serialization error: \(error.localizedDescription)"
        case .invalidEventData(let message):
            return "Invalid event data: \(message)"
        case .queueFull(let maxSize):
            return "Event queue is full (max size: \(maxSize))"
        case .rateLimitExceeded(let retryAfter):
            if let retry = retryAfter {
                return "Rate limit exceeded. Retry after \(Int(retry)) seconds."
            }
            return "Rate limit exceeded."
        case .authenticationFailed(let message):
            return "Authentication failed: \(message ?? "unknown error")"
        case .permissionDenied(let message):
            return "Permission denied: \(message ?? "unknown error")"
        case .serverError(let statusCode, let message):
            return "Server error (\(statusCode)): \(message ?? "unknown error")"
        case .timeout:
            return "Request timed out."
        case .unknown(let error):
            return "Unknown error: \(error?.localizedDescription ?? "no details")"
        }
    }
    
    public var failureReason: String? {
        return errorDescription
    }
}

/**
 * 错误处理器
 * 提供统一的错误处理和日志记录功能
 */
public class ErrorHandler {
    
    /**
     * 日志级别
     */
    public enum LogLevel: Int, Comparable {
        case debug = 0
        case info = 1
        case warning = 2
        case error = 3
        case none = 4
        
        public static func < (lhs: LogLevel, rhs: LogLevel) -> Bool {
            return lhs.rawValue < rhs.rawValue
        }
    }
    
    /**
     * 错误回调
     */
    public typealias ErrorCallback = (OddsmakerError) -> Void
    
    /**
     * 日志回调
     */
    public typealias LogCallback = (LogLevel, String) -> Void
    
    private let logLevel: LogLevel
    private var errorCallback: ErrorCallback?
    private var logCallback: LogCallback?
    private let dateFormatter: DateFormatter
    
    /**
     * 初始化错误处理器
     * - Parameters:
     *   - logLevel: 日志级别
     *   - errorCallback: 错误回调
     *   - logCallback: 日志回调
     */
    public init(logLevel: LogLevel = .warning, errorCallback: ErrorCallback? = nil, logCallback: LogCallback? = nil) {
        self.logLevel = logLevel
        self.errorCallback = errorCallback
        self.logCallback = logCallback
        
        self.dateFormatter = DateFormatter()
        self.dateFormatter.dateFormat = "yyyy-MM-dd HH:mm:ss.SSS"
    }
    
    // MARK: - 错误处理
    
    /**
     * 处理错误
     * - Parameter error: 要处理的错误
     */
    public func handle(_ error: Error) {
        let oddsmakerError: OddsmakerError
        
        if let existing = error as? OddsmakerError {
            oddsmakerError = existing
        } else {
            oddsmakerError = .unknown(error)
        }
        
        // 记录错误日志
        log(.error, "Error: \(oddsmakerError.localizedDescription)")
        
        // 调用错误回调
        errorCallback?(oddsmakerError)
    }
    
    /**
     * 处理错误并返回默认值
     * - Parameters:
     *   - error: 要处理的错误
     *   - defaultValue: 默认值
     * - Returns: 默认值
     */
    public func handle<T>(_ error: Error, defaultValue: T) -> T {
        handle(error)
        return defaultValue
    }
    
    // MARK: - 日志记录
    
    /**
     * 记录调试日志
     * - Parameter message: 日志消息
     */
    public func debug(_ message: String) {
        log(.debug, message)
    }
    
    /**
     * 记录信息日志
     * - Parameter message: 日志消息
     */
    public func info(_ message: String) {
        log(.info, message)
    }
    
    /**
     * 记录警告日志
     * - Parameter message: 日志消息
     */
    public func warning(_ message: String) {
        log(.warning, message)
    }
    
    /**
     * 记录错误日志
     * - Parameter message: 日志消息
     */
    public func error(_ message: String) {
        log(.error, message)
    }
    
    /**
     * 记录日志
     * - Parameters:
     *   - level: 日志级别
     *   - message: 日志消息
     */
    public func log(_ level: LogLevel, _ message: String) {
        guard level >= logLevel else {
            return
        }
        
        let timestamp = dateFormatter.string(from: Date())
        let logMessage = "[Oddsmaker] [\(timestamp)] [\(level)] \(message)"
        
        // 调用日志回调
        logCallback?(level, logMessage)
        
        // 在调试模式下打印到控制台
        #if DEBUG
        print(logMessage)
        #endif
    }
    
    // MARK: - 错误分类
    
    /**
     * 判断是否为网络错误
     * - Parameter error: 要判断的错误
     * - Returns: 是否为网络错误
     */
    public func isNetworkError(_ error: Error) -> Bool {
        if let urlError = error as? URLError {
            switch urlError.code {
            case .notConnectedToInternet, .networkConnectionLost, .timedOut, .cannotFindHost, .cannotConnectToHost:
                return true
            default:
                return false
            }
        }
        
        if case .networkError = error as? OddsmakerError {
            return true
        }
        
        return false
    }
    
    /**
     * 判断是否为认证错误
     * - Parameter error: 要判断的错误
     * - Returns: 是否为认证错误
     */
    public func isAuthError(_ error: Error) -> Bool {
        if case .authenticationFailed = error as? OddsmakerError {
            return true
        }
        
        if case .permissionDenied = error as? OddsmakerError {
            return true
        }
        
        if let responseError = error as? OddsmakerError,
           case .invalidResponse(let statusCode, _) = responseError,
           statusCode == 401 || statusCode == 403 {
            return true
        }
        
        return false
    }
    
    /**
     * 判断是否为限流错误
     * - Parameter error: 要判断的错误
     * - Returns: 是否为限流错误
     */
    public func isRateLimitError(_ error: Error) -> Bool {
        if case .rateLimitExceeded = error as? OddsmakerError {
            return true
        }
        
        if let responseError = error as? OddsmakerError,
           case .invalidResponse(let statusCode, _) = responseError,
           statusCode == 429 {
            return true
        }
        
        return false
    }
    
    /**
     * 判断是否为服务器错误
     * - Parameter error: 要判断的错误
     * - Returns: 是否为服务器错误
     */
    public func isServerError(_ error: Error) -> Bool {
        if case .serverError = error as? OddsmakerError {
            return true
        }
        
        if let responseError = error as? OddsmakerError,
           case .invalidResponse(let statusCode, _) = responseError,
           statusCode >= 500 {
            return true
        }
        
        return false
    }
    
    /**
     * 获取错误的重试建议
     * - Parameter error: 要分析的错误
     * - Returns: 建议的重试时间（秒），如果不需要重试则返回nil
     */
    public func getRetryDelay(_ error: Error) -> TimeInterval? {
        if case .rateLimitExceeded(let retryAfter) = error as? OddsmakerError {
            return retryAfter ?? 60
        }
        
        if isNetworkError(error) {
            return 5
        }
        
        if isServerError(error) {
            return 30
        }
        
        return nil
    }
}