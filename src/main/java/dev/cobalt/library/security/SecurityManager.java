package dev.cobalt.library.security;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Security manager for permissions and access control
 */
public class SecurityManager {

    private final Plugin plugin;
    private final Map<UUID, Set<String>> playerPermissions = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> rolePermissions = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> playerRoles = new ConcurrentHashMap<>();

    public SecurityManager(Plugin plugin) {
        this.plugin = plugin;
        initializeDefaultRoles();
    }

    /**
     * Initialize default roles
     */
    private void initializeDefaultRoles() {
        // Admin role
        Set<String> adminPerms = ConcurrentHashMap.newKeySet();
        adminPerms.add("*");
        rolePermissions.put("admin", adminPerms);

        // Moderator role
        Set<String> modPerms = ConcurrentHashMap.newKeySet();
        modPerms.add("cobalt.moderate");
        modPerms.add("cobalt.kick");
        modPerms.add("cobalt.mute");
        rolePermissions.put("moderator", modPerms);

        // Player role (default)
        Set<String> playerPerms = ConcurrentHashMap.newKeySet();
        playerPerms.add("cobalt.chat");
        playerPerms.add("cobalt.play");
        rolePermissions.put("player", playerPerms);
    }

    /**
     * Check if player has permission
     */
    public boolean hasPermission(Player player, String permission) {
        return hasPermission(player.getUniqueId(), permission);
    }

    /**
     * Check if player has permission by UUID
     */
    public boolean hasPermission(UUID playerId, String permission) {
        // Check direct permissions
        Set<String> perms = playerPermissions.get(playerId);
        if (perms != null && (perms.contains(permission) || perms.contains("*"))) {
            return true;
        }

        // Check role permissions
        Set<String> roles = playerRoles.get(playerId);
        if (roles != null) {
            for (String role : roles) {
                Set<String> rolePerms = rolePermissions.get(role);
                if (rolePerms != null && (rolePerms.contains(permission) || rolePerms.contains("*"))) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Grant permission to player
     */
    public void grantPermission(UUID playerId, String permission) {
        playerPermissions.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(permission);
        plugin.getLogger().info("Granted permission " + permission + " to " + playerId);
    }

    /**
     * Revoke permission from player
     */
    public void revokePermission(UUID playerId, String permission) {
        Set<String> perms = playerPermissions.get(playerId);
        if (perms != null) {
            perms.remove(permission);
            plugin.getLogger().info("Revoked permission " + permission + " from " + playerId);
        }
    }

    /**
     * Add player to role
     */
    public void addRole(UUID playerId, String role) {
        playerRoles.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(role);
        plugin.getLogger().info("Added role " + role + " to " + playerId);
    }

    /**
     * Remove player from role
     */
    public void removeRole(UUID playerId, String role) {
        Set<String> roles = playerRoles.get(playerId);
        if (roles != null) {
            roles.remove(role);
            plugin.getLogger().info("Removed role " + role + " from " + playerId);
        }
    }

    /**
     * Check if player has role
     */
    public boolean hasRole(UUID playerId, String role) {
        Set<String> roles = playerRoles.get(playerId);
        return roles != null && roles.contains(role);
    }

    /**
     * Get player roles
     */
    public Set<String> getRoles(UUID playerId) {
        return playerRoles.getOrDefault(playerId, Set.of());
    }

    /**
     * Get player permissions
     */
    public Set<String> getPermissions(UUID playerId) {
        return playerPermissions.getOrDefault(playerId, Set.of());
    }

    /**
     * Create a new role
     */
    public void createRole(String role, Set<String> permissions) {
        rolePermissions.put(role, ConcurrentHashMap.newKeySet());
        rolePermissions.get(role).addAll(permissions);
        plugin.getLogger().info("Created role: " + role);
    }

    /**
     * Delete a role
     */
    public void deleteRole(String role) {
        rolePermissions.remove(role);

        // Remove role from all players
        playerRoles.values().forEach(roles -> roles.remove(role));

        plugin.getLogger().info("Deleted role: " + role);
    }

    /**
     * Grant permission to role
     */
    public void grantRolePermission(String role, String permission) {
        rolePermissions.computeIfAbsent(role, k -> ConcurrentHashMap.newKeySet()).add(permission);
    }

    /**
     * Revoke permission from role
     */
    public void revokeRolePermission(String role, String permission) {
        Set<String> perms = rolePermissions.get(role);
        if (perms != null) {
            perms.remove(permission);
        }
    }

    /**
     * Get all roles
     */
    public Set<String> getAllRoles() {
        return rolePermissions.keySet();
    }

    /**
     * Get role permissions
     */
    public Set<String> getRolePermissions(String role) {
        return rolePermissions.getOrDefault(role, Set.of());
    }

    /**
     * Clear all player data
     */
    public void clearPlayer(UUID playerId) {
        playerPermissions.remove(playerId);
        playerRoles.remove(playerId);
    }
}
