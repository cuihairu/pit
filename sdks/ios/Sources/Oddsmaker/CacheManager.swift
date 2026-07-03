import Foundation

/**
 * 缓存管理器
 * 提供实验配置和用户数据的缓存功能
 */
public class CacheManager {
    
    private let userDefaults: UserDefaults
    private let prefix: String
    
    /**
     * 初始化缓存管理器
     * - Parameters:
     *   - userDefaults: UserDefaults实例
     *   - prefix: 缓存键前缀
     */
    public init(userDefaults: UserDefaults = .standard, prefix: String = "oddsmaker") {
        self.userDefaults = userDefaults
        self.prefix = prefix
    }
    
    // MARK: - 实验配置缓存
    
    /**
     * 缓存实验配置
     * - Parameters:
     *   - data: 实验配置数据
     *   - gameId: 游戏ID
     *   - environment: 环境
     */
    public func cacheExperiments(_ data: Data, gameId: String, environment: String) {
        let key = experimentsKey(gameId, environment)
        let entry = "\(Int(Date().timeIntervalSince1970))\n\(String(data: data, encoding: .utf8) ?? "")"
        userDefaults.set(entry, forKey: key)
    }
    
    /**
     * 获取缓存的实验配置
     * - Parameters:
     *   - gameId: 游戏ID
     *   - environment: 环境
     *   - ttlSec: 缓存有效期（秒）
     * - Returns: 缓存的实验配置数据，如果缓存不存在或已过期则返回nil
     */
    public func getCachedExperiments(gameId: String, environment: String, ttlSec: TimeInterval = 300) -> Data? {
        let key = experimentsKey(gameId, environment)
        guard let entry = userDefaults.string(forKey: key) else {
            return nil
        }
        
        let parts = entry.split(separator: "\n", maxSplits: 1, omittingEmptySubsequences: false)
        guard parts.count == 2,
              let timestamp = TimeInterval(parts[0]),
              Date().timeIntervalSince1970 - timestamp < ttlSec else {
            return nil
        }
        
        return Data(String(parts[1]).utf8)
    }
    
    /**
     * 清除实验配置缓存
     * - Parameters:
     *   - gameId: 游戏ID
     *   - environment: 环境
     */
    public func clearExperimentsCache(gameId: String, environment: String) {
        let key = experimentsKey(gameId, environment)
        userDefaults.removeObject(forKey: key)
    }
    
    // MARK: - 用户数据缓存
    
    /**
     * 缓存用户属性
     * - Parameters:
     *   - props: 用户属性
     *   - userId: 用户ID
     */
    public func cacheUserProps(_ props: [String: Any], userId: String) {
        let key = userPropsKey(userId)
        if let data = try? JSONSerialization.data(withJSONObject: props) {
            userDefaults.set(String(data: data, encoding: .utf8), forKey: key)
        }
    }
    
    /**
     * 获取缓存的用户属性
     * - Parameter userId: 用户ID
     * - Returns: 缓存的用户属性，如果缓存不存在则返回nil
     */
    public func getCachedUserProps(userId: String) -> [String: Any]? {
        let key = userPropsKey(userId)
        guard let jsonString = userDefaults.string(forKey: key),
              let data = jsonString.data(using: .utf8),
              let props = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }
        return props
    }
    
    /**
     * 清除用户属性缓存
     * - Parameter userId: 用户ID
     */
    public func clearUserPropsCache(userId: String) {
        let key = userPropsKey(userId)
        userDefaults.removeObject(forKey: key)
    }
    
    // MARK: - 会话缓存
    
    /**
     * 缓存会话信息
     * - Parameters:
     *   - sessionId: 会话ID
     *   - startTime: 会话开始时间
     *   - gameId: 游戏ID
     *   - environment: 环境
     */
    public func cacheSession(_ sessionId: String, startTime: TimeInterval, gameId: String, environment: String) {
        let key = sessionKey(gameId, environment)
        let entry = "\(sessionId)\n\(startTime)"
        userDefaults.set(entry, forKey: key)
    }
    
    /**
     * 获取缓存的会话信息
     * - Parameters:
     *   - gameId: 游戏ID
     *   - environment: 环境
     *   - maxAgeSec: 会话最大有效期（秒）
     * - Returns: (会话ID, 开始时间) 元组，如果缓存不存在或已过期则返回nil
     */
    public func getCachedSession(gameId: String, environment: String, maxAgeSec: TimeInterval = 30 * 60) -> (sessionId: String, startTime: TimeInterval)? {
        let key = sessionKey(gameId, environment)
        guard let entry = userDefaults.string(forKey: key) else {
            return nil
        }
        
        let parts = entry.split(separator: "\n")
        guard parts.count == 2,
              let startTime = TimeInterval(parts[1]),
              Date().timeIntervalSince1970 - startTime < maxAgeSec else {
            return nil
        }
        
        return (sessionId: String(parts[0]), startTime: startTime)
    }
    
    /**
     * 清除会话缓存
     * - Parameters:
     *   - gameId: 游戏ID
     *   - environment: 环境
     */
    public func clearSessionCache(gameId: String, environment: String) {
        let key = sessionKey(gameId, environment)
        userDefaults.removeObject(forKey: key)
    }
    
    // MARK: - 设备ID缓存
    
    /**
     * 缓存设备ID
     * - Parameters:
     *   - deviceId: 设备ID
     *   - gameId: 游戏ID
     *   - environment: 环境
     */
    public func cacheDeviceId(_ deviceId: String, gameId: String, environment: String) {
        let key = deviceIdKey(gameId, environment)
        userDefaults.set(deviceId, forKey: key)
    }
    
    /**
     * 获取缓存的设备ID
    * - Parameters:
     *   - gameId: 游戏ID
     *   - environment: 环境
     * - Returns: 缓存的设备ID，如果缓存不存在则返回nil
     */
    public func getCachedDeviceId(gameId: String, environment: String) -> String? {
        let key = deviceIdKey(gameId, environment)
        return userDefaults.string(forKey: key)
    }
    
    // MARK: - 通用缓存操作
    
    /**
     * 缓存任意数据
     * - Parameters:
     *   - data: 要缓存的数据
     *   - key: 缓存键
     *   - ttlSec: 缓存有效期（秒）
     */
    public func cacheData(_ data: Data, forKey key: String, ttlSec: TimeInterval? = nil) {
        let cacheKey = prefixedKey(key)
        if let ttl = ttlSec {
            let entry = "\(Int(Date().timeIntervalSince1970))\n\(String(data: data, encoding: .utf8) ?? "")"
            userDefaults.set(entry, forKey: cacheKey)
        } else {
            userDefaults.set(String(data: data, encoding: .utf8), forKey: cacheKey)
        }
    }
    
    /**
     * 获取缓存的数据
     * - Parameters:
     *   - key: 缓存键
     *   - ttlSec: 缓存有效期（秒）
     * - Returns: 缓存的数据，如果缓存不存在或已过期则返回nil
     */
    public func getCachedData(forKey key: String, ttlSec: TimeInterval? = nil) -> Data? {
        let cacheKey = prefixedKey(key)
        guard let value = userDefaults.string(forKey: cacheKey) else {
            return nil
        }
        
        if let ttl = ttlSec {
            let parts = value.split(separator: "\n", maxSplits: 1, omittingEmptySubsequences: false)
            guard parts.count == 2,
                  let timestamp = TimeInterval(parts[0]),
                  Date().timeIntervalSince1970 - timestamp < ttl else {
                return nil
            }
            return Data(String(parts[1]).utf8)
        }
        
        return value.data(using: .utf8)
    }
    
    /**
     * 删除缓存
     * - Parameter key: 缓存键
     */
    public func removeCache(forKey key: String) {
        let cacheKey = prefixedKey(key)
        userDefaults.removeObject(forKey: cacheKey)
    }
    
    /**
     * 清除所有Oddsmaker缓存
     */
    public func clearAll() {
        let dictionary = userDefaults.dictionaryRepresentation()
        for key in dictionary.keys {
            if key.hasPrefix(prefix) {
                userDefaults.removeObject(forKey: key)
            }
        }
    }
    
    // MARK: - 私有方法
    
    private func prefixedKey(_ key: String) -> String {
        return "\(prefix)_\(key)"
    }
    
    private func experimentsKey(_ gameId: String, _ environment: String) -> String {
        return prefixedKey("experiments_\(gameId)_\(environment)")
    }
    
    private func userPropsKey(_ userId: String) -> String {
        return prefixedKey("user_props_\(userId)")
    }
    
    private func sessionKey(_ gameId: String, _ environment: String) -> String {
        return prefixedKey("session_\(gameId)_\(environment)")
    }
    
    private func deviceIdKey(_ gameId: String, _ environment: String) -> String {
        return prefixedKey("device_id_\(gameId)_\(environment)")
    }
}