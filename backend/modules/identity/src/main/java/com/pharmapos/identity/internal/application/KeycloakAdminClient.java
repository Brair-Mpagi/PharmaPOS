package com.pharmapos.identity.internal.application;

/**
 * Client for the Keycloak admin API.
 * Used by {@link DisableUserUseCase} to revoke all active sessions for a user.
 */
public interface KeycloakAdminClient {

    /**
     * Revokes all active sessions for the given Keycloak user.
     *
     * @param keycloakUserId the Keycloak subject ID (UUID string in Keycloak)
     */
    void revokeAllSessions(String keycloakUserId);
}
