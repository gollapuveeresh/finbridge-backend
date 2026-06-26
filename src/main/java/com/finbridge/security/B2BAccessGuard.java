package com.finbridge.security;

import com.finbridge.entity.OrganizationUser;
import com.finbridge.entity.User;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

/**
 * Organization-scoped authorization for the B2B portal.
 *
 * The /api/b2b/** routes only require an authenticated principal, which means a logged-in
 * organization user could otherwise reach another organization's data by changing the
 * {orgId} in the URL (a cross-org IDOR). This guard enforces that an organization user may
 * only act within their own organization, while CRM admin-tier staff keep cross-cutting access.
 */
public final class B2BAccessGuard {

    private B2BAccessGuard() {}

    /** Verify the authenticated caller may act within the given organization. */
    public static void assertOrgAccess(Object principal, UUID orgId) {
        if (orgId == null) throw new AccessDeniedException("Organization not specified");
        // CRM admin-tier staff (internal B2B oversight) retain cross-cutting access.
        if (principal instanceof User u && OwnershipGuard.isAdminTier(u)) return;
        if (principal instanceof OrganizationUser ou
                && ou.getOrganization() != null
                && orgId.equals(ou.getOrganization().getId())) {
            return;
        }
        throw new AccessDeniedException("You do not have access to this organization");
    }

    /**
     * Verify the caller is internal CRM admin-tier staff. Used by cross-organization views
     * (e.g. a department admin listing every org's service requests for their department),
     * which must never be reachable by an organization user.
     */
    public static void assertStaff(Object principal) {
        if (principal instanceof User u && OwnershipGuard.isAdminTier(u)) return;
        throw new AccessDeniedException("Staff access required");
    }
}
