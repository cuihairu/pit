package io.oddsmaker.control.service;

import io.oddsmaker.control.jpa.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private UserRepo userRepo;

    @Mock
    private RoleRepo roleRepo;

    @Mock
    private PermissionRepo permissionRepo;

    @Mock
    private UserRoleRepo userRoleRepo;

    @InjectMocks
    private PermissionService permissionService;

    private UserEntity testUser;
    private RoleEntity testRole;
    private PermissionEntity testPermission;
    private UserRoleEntity testUserRole;

    @BeforeEach
    void setUp() {
        // 创建测试用户
        testUser = new UserEntity();
        testUser.id = "user_test123";
        testUser.username = "testuser";
        testUser.status = UserEntity.UserStatus.ACTIVE;
        testUser.roles = Set.of(UserEntity.UserRole.VIEWER);

        // 创建测试权限
        testPermission = new PermissionEntity();
        testPermission.id = "game:read";
        testPermission.name = "Read games";
        testPermission.type = PermissionEntity.PermissionType.API;
        testPermission.resourceType = "game";
        testPermission.action = PermissionEntity.PermissionAction.READ;
        testPermission.scope = PermissionEntity.PermissionScope.GLOBAL;
        testPermission.enabled = true;

        // 创建测试角色
        testRole = new RoleEntity();
        testRole.id = "viewer";
        testRole.name = "Viewer";
        testRole.type = RoleEntity.RoleType.SYSTEM;
        testRole.level = 50;
        testRole.enabled = true;
        testRole.permissions = Set.of(testPermission);

        // 创建测试用户角色关联
        testUserRole = new UserRoleEntity();
        testUserRole.id = 1L;
        testUserRole.userId = "user_test123";
        testUserRole.roleId = "viewer";
        testUserRole.enabled = true;
    }

    @Test
    void hasPermission_UserHasPermission_ReturnsTrue() {
        when(userRepo.findById("user_test123")).thenReturn(Optional.of(testUser));
        when(userRoleRepo.findValidByUserId(eq("user_test123"), any(LocalDateTime.class)))
            .thenReturn(List.of(testUserRole));
        when(roleRepo.findById("viewer")).thenReturn(Optional.of(testRole));

        boolean result = permissionService.hasPermission("user_test123", "game:read");

        assertTrue(result);
    }

    @Test
    void hasPermission_UserNotActive_ReturnsFalse() {
        testUser.status = UserEntity.UserStatus.INACTIVE;

        when(userRepo.findById("user_test123")).thenReturn(Optional.of(testUser));

        boolean result = permissionService.hasPermission("user_test123", "game:read");

        assertFalse(result);
    }

    @Test
    void hasPermission_UserLocked_ReturnsFalse() {
        testUser.status = UserEntity.UserStatus.LOCKED;

        when(userRepo.findById("user_test123")).thenReturn(Optional.of(testUser));

        boolean result = permissionService.hasPermission("user_test123", "game:read");

        assertFalse(result);
    }

    @Test
    void hasPermission_NoRoleAssignment_ReturnsFalse() {
        when(userRepo.findById("user_test123")).thenReturn(Optional.of(testUser));
        when(userRoleRepo.findValidByUserId(eq("user_test123"), any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        boolean result = permissionService.hasPermission("user_test123", "game:read");

        assertFalse(result);
    }

    @Test
    void hasPermission_RoleDoesNotHavePermission_ReturnsFalse() {
        RoleEntity roleWithoutPermission = new RoleEntity();
        roleWithoutPermission.id = "basic";
        roleWithoutPermission.enabled = true;
        roleWithoutPermission.permissions = Collections.emptySet();

        when(userRepo.findById("user_test123")).thenReturn(Optional.of(testUser));
        when(userRoleRepo.findValidByUserId(eq("user_test123"), any(LocalDateTime.class)))
            .thenReturn(List.of(testUserRole));
        when(roleRepo.findById("viewer")).thenReturn(Optional.of(roleWithoutPermission));

        boolean result = permissionService.hasPermission("user_test123", "game:read");

        assertFalse(result);
    }

    @Test
    void hasGamePermission_UserHasGlobalPermission_ReturnsTrue() {
        when(userRepo.findById("user_test123")).thenReturn(Optional.of(testUser));
        when(userRoleRepo.findByUserIdAndGameId("user_test123", "game_123"))
            .thenReturn(Collections.emptyList());
        when(userRoleRepo.findGlobalByUserId("user_test123"))
            .thenReturn(List.of(testUserRole));
        when(roleRepo.findById("viewer")).thenReturn(Optional.of(testRole));

        boolean result = permissionService.hasGamePermission("user_test123", "game_123", "game:read");

        assertTrue(result);
    }

    @Test
    void hasGamePermission_UserHasGamePermission_ReturnsTrue() {
        UserRoleEntity gameRole = new UserRoleEntity();
        gameRole.userId = "user_test123";
        gameRole.roleId = "viewer";
        gameRole.gameId = "game_123";
        gameRole.enabled = true;

        when(userRepo.findById("user_test123")).thenReturn(Optional.of(testUser));
        when(userRoleRepo.findByUserIdAndGameId("user_test123", "game_123"))
            .thenReturn(List.of(gameRole));
        when(roleRepo.findById("viewer")).thenReturn(Optional.of(testRole));

        boolean result = permissionService.hasGamePermission("user_test123", "game_123", "game:read");

        assertTrue(result);
    }

    @Test
    void getUserPermissions_ReturnsAllPermissions() {
        when(userRoleRepo.findValidByUserId(eq("user_test123"), any(LocalDateTime.class)))
            .thenReturn(List.of(testUserRole));
        when(roleRepo.findById("viewer")).thenReturn(Optional.of(testRole));

        Set<String> permissions = permissionService.getUserPermissions("user_test123");

        assertNotNull(permissions);
        assertTrue(permissions.contains("game:read"));
    }

    @Test
    void assignRole_Success() {
        when(userRepo.existsById("user_test123")).thenReturn(true);
        when(roleRepo.existsById("viewer")).thenReturn(true);
        when(userRoleRepo.findByUserIdAndRoleId("user_test123", "viewer"))
            .thenReturn(Optional.empty());
        when(userRoleRepo.save(any(UserRoleEntity.class))).thenReturn(testUserRole);

        UserRoleEntity result = permissionService.assignRole("user_test123", "viewer", null, null, "admin");

        assertNotNull(result);
        assertEquals("user_test123", result.userId);
        assertEquals("viewer", result.roleId);
        verify(userRoleRepo).save(any(UserRoleEntity.class));
    }

    @Test
    void assignRole_AlreadyAssigned_ThrowsException() {
        when(userRepo.existsById("user_test123")).thenReturn(true);
        when(roleRepo.existsById("viewer")).thenReturn(true);
        when(userRoleRepo.findByUserIdAndRoleId("user_test123", "viewer"))
            .thenReturn(Optional.of(testUserRole));

        assertThrows(IllegalArgumentException.class, () -> {
            permissionService.assignRole("user_test123", "viewer", null, null, "admin");
        });
    }

    @Test
    void revokeRole_Success() {
        when(userRoleRepo.findByUserIdAndRoleId("user_test123", "viewer"))
            .thenReturn(Optional.of(testUserRole));

        permissionService.revokeRole("user_test123", "viewer");

        verify(userRoleRepo).delete(testUserRole);
    }

    @Test
    void getUserRoles_ReturnsRoles() {
        when(userRoleRepo.findByUserIdAndEnabledTrue("user_test123"))
            .thenReturn(List.of(testUserRole));
        when(roleRepo.findById("viewer")).thenReturn(Optional.of(testRole));

        List<RoleEntity> roles = permissionService.getUserRoles("user_test123");

        assertNotNull(roles);
        assertEquals(1, roles.size());
        assertEquals("viewer", roles.get(0).id);
    }

    @Test
    void hasPermission_UserNotFound_ThrowsException() {
        when(userRepo.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            permissionService.hasPermission("nonexistent", "game:read");
        });
    }
}