package com.pharmapos.identity.internal.web;

import com.pharmapos.identity.internal.application.DisableUserUseCase;
import com.pharmapos.identity.internal.application.PermissionMatrixService;
import com.pharmapos.identity.internal.domain.Role;
import com.pharmapos.identity.internal.domain.RoleAssignment;
import com.pharmapos.identity.internal.domain.User;
import com.pharmapos.identity.internal.persistence.RoleAssignmentRepository;
import com.pharmapos.identity.internal.persistence.RoleRepository;
import com.pharmapos.identity.internal.persistence.UserRepository;
import com.pharmapos.shared.multitenancy.TenantContextHolder;
import com.pharmapos.shared.security.RequiresPermission;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * REST controller for user and role-assignment management (TDD §6.2 "Identity").
 *
 * <ul>
 *   <li>{@code GET    /api/v1/identity/users}                   — list users in tenant</li>
 *   <li>{@code GET    /api/v1/identity/users/{id}}              — get user by id</li>
 *   <li>{@code POST   /api/v1/identity/users/{id}/disable}      — disable user</li>
 *   <li>{@code GET    /api/v1/identity/users/{id}/permissions}  — effective permissions on branch</li>
 *   <li>{@code POST   /api/v1/identity/roles}                   — create role</li>
 *   <li>{@code GET    /api/v1/identity/roles}                   — list roles</li>
 *   <li>{@code POST   /api/v1/identity/users/{id}/role-assignments}  — assign role</li>
 *   <li>{@code DELETE /api/v1/identity/users/{id}/role-assignments/{assignmentId} — remove assignment</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/identity")
@RequiresPermission("iam.manage")
public class UserController {

    private final UserRepository           userRepository;
    private final RoleRepository           roleRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final DisableUserUseCase       disableUserUseCase;
    private final PermissionMatrixService  permissionMatrixService;

    public UserController(
            UserRepository           userRepository,
            RoleRepository           roleRepository,
            RoleAssignmentRepository roleAssignmentRepository,
            DisableUserUseCase       disableUserUseCase,
            PermissionMatrixService  permissionMatrixService
    ) {
        this.userRepository           = userRepository;
        this.roleRepository           = roleRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.disableUserUseCase       = disableUserUseCase;
        this.permissionMatrixService  = permissionMatrixService;
    }

    // ── Users ────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        var ctx = TenantContextHolder.get();
        return userRepository.findAll().stream()
            .filter(u -> u.getTenantId().equals(ctx.tenantId()))
            .map(UserResponse::from)
            .toList();
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserResponse> getUser(@PathVariable UUID id) {
        var ctx = TenantContextHolder.get();
        return userRepository.findByIdAndTenantId(id, ctx.tenantId())
            .map(u -> ResponseEntity.ok(UserResponse.from(u)))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/users/{id}/disable")
    public ResponseEntity<Void> disableUser(
            @PathVariable UUID id,
            @RequestBody DisableRequest req
    ) {
        var ctx = TenantContextHolder.get();
        disableUserUseCase.disable(ctx.tenantId(), id, ctx.userId(), req.reason());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{id}/permissions")
    public Set<String> effectivePermissions(
            @PathVariable UUID id,
            @RequestParam UUID branchId
    ) {
        var ctx = TenantContextHolder.get();
        return permissionMatrixService.effectivePermissions(ctx.tenantId(), id, branchId);
    }

    // ── Roles ────────────────────────────────────────────────────────────────

    @PostMapping("/roles")
    public ResponseEntity<RoleResponse> createRole(@RequestBody CreateRoleRequest req) {
        var ctx = TenantContextHolder.get();
        Role role = new Role(ctx.tenantId(), req.name(), req.permissions());
        role = roleRepository.save(role);
        return ResponseEntity.status(HttpStatus.CREATED).body(RoleResponse.from(role));
    }

    @GetMapping("/roles")
    public List<RoleResponse> listRoles() {
        var ctx = TenantContextHolder.get();
        return roleRepository.findAllByTenantId(ctx.tenantId()).stream()
            .map(RoleResponse::from)
            .toList();
    }

    // ── Role Assignments ─────────────────────────────────────────────────────

    @PostMapping("/users/{id}/role-assignments")
    public ResponseEntity<AssignmentResponse> assignRole(
            @PathVariable UUID id,
            @RequestBody AssignRoleRequest req
    ) {
        var ctx = TenantContextHolder.get();
        RoleAssignment assignment = new RoleAssignment(id, req.roleId(), req.branchId(), ctx.userId());
        assignment = roleAssignmentRepository.save(assignment);
        return ResponseEntity.status(HttpStatus.CREATED).body(AssignmentResponse.from(assignment));
    }

    @DeleteMapping("/users/{id}/role-assignments/{assignmentId}")
    public ResponseEntity<Void> removeRoleAssignment(
            @PathVariable UUID id,
            @PathVariable UUID assignmentId
    ) {
        roleAssignmentRepository.deleteById(assignmentId);
        return ResponseEntity.noContent().build();
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public record UserResponse(UUID id, String username, String email, String status, boolean mfaEnabled) {
        static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getUsername(), u.getEmail(),
                u.getStatus().name(), u.isMfaEnabled());
        }
    }

    public record RoleResponse(UUID id, String name, Set<String> permissions) {
        static RoleResponse from(Role r) {
            return new RoleResponse(r.getId(), r.getName(), r.getPermissions());
        }
    }

    public record AssignmentResponse(UUID id, UUID userId, UUID roleId, UUID branchId) {
        static AssignmentResponse from(RoleAssignment a) {
            return new AssignmentResponse(a.getId(), a.getUserId(), a.getRoleId(), a.getBranchId());
        }
    }

    public record DisableRequest(String reason) {}
    public record CreateRoleRequest(String name, Set<String> permissions) {}
    public record AssignRoleRequest(UUID roleId, UUID branchId) {}
}
