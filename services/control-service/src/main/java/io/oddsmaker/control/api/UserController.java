package io.oddsmaker.control.api;

import io.oddsmaker.control.jpa.UserEntity;
import io.oddsmaker.control.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用户管理API控制器
 * 提供用户的CRUD操作和权限管理
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public ResponseEntity<UserEntity> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        return userService.findByUsername(username)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取用户列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Page<UserEntity>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        Sort sort = sortDir.equalsIgnoreCase("desc") ? 
            Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<UserEntity> users = userService.listUsers(pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * 搜索用户
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Page<UserEntity>> searchUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<UserEntity> users = userService.searchUsers(query, pageable);
        return ResponseEntity.ok(users);
    }

    /**
     * 获取用户详情
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<UserEntity> getUser(@PathVariable String userId) {
        return userService.findById(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 创建用户
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserEntity> createUser(@RequestBody UserEntity user) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String operatorId = auth.getName();
        
        UserEntity created = userService.createUser(user, operatorId);
        return ResponseEntity.ok(created);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    public ResponseEntity<UserEntity> updateUser(
            @PathVariable String userId,
            @RequestBody UserEntity updates) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String operatorId = auth.getName();
        
        UserEntity updated = userService.updateUser(userId, updates, operatorId);
        return ResponseEntity.ok(updated);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String operatorId = auth.getName();
        
        userService.deleteUser(userId, operatorId);
        return ResponseEntity.ok().build();
    }

    /**
     * 更新用户角色
     */
    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserEntity> updateRoles(
            @PathVariable String userId,
            @RequestBody Set<UserEntity.UserRole> roles) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String operatorId = auth.getName();
        
        UserEntity updated = userService.updateRoles(userId, roles, operatorId);
        return ResponseEntity.ok(updated);
    }

    /**
     * 锁定用户
     */
    @PostMapping("/{userId}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserEntity> lockUser(@PathVariable String userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String operatorId = auth.getName();
        
        UserEntity locked = userService.lockUser(userId, operatorId);
        return ResponseEntity.ok(locked);
    }

    /**
     * 解锁用户
     */
    @PostMapping("/{userId}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserEntity> unlockUser(@PathVariable String userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String operatorId = auth.getName();
        
        UserEntity unlocked = userService.unlockUser(userId, operatorId);
        return ResponseEntity.ok(unlocked);
    }

    /**
     * 启用/禁用双因素认证
     */
    @PutMapping("/{userId}/two-factor")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    public ResponseEntity<UserEntity> toggleTwoFactor(
            @PathVariable String userId,
            @RequestBody Map<String, Boolean> request) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String operatorId = auth.getName();
        
        boolean enabled = request.getOrDefault("enabled", false);
        UserEntity updated = userService.toggleTwoFactor(userId, enabled, operatorId);
        return ResponseEntity.ok(updated);
    }

    /**
     * 获取用户统计信息
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {
        Map<String, Object> stats = userService.getUserStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取最近登录的用户
     */
    @GetMapping("/recent-logins")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserEntity>> getRecentLogins(
            @RequestParam(defaultValue = "10") int limit) {
        
        List<UserEntity> users = userService.getRecentlyLoggedInUsers(limit);
        return ResponseEntity.ok(users);
    }

    /**
     * 根据角色查找用户
     */
    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserEntity>> getUsersByRole(
            @PathVariable UserEntity.UserRole role) {
        
        List<UserEntity> users = userService.findByRole(role);
        return ResponseEntity.ok(users);
    }
}