package com.finbridge.security;

import com.finbridge.entity.User;
import org.springframework.security.access.AccessDeniedException;

/**
 * Owner-level (record-scoped) authorization on top of the role-level @PreAuthorize gates.
 *
 * The controllers already restrict these endpoints to staff roles. This guard closes the
 * remaining IDOR gap: a consultant must only act on the records they own, while admin-tier
 * roles (super-admin / admin / crm-admin / department-admin) retain cross-cutting access.
 */
public final class OwnershipGuard {

    private OwnershipGuard() {}

    /** True for roles that legitimately operate across other people's records. */
    public static boolean isAdminTier(User actor) {
        if (actor == null || actor.getRole() == null) return false;
        return switch (actor.getRole()) {
            case "super-admin", "admin", "crm-admin", "department-admin" -> true;
            default -> false;
        };
    }

    /**
     * Allow admin-tier through; require a consultant to own the record (consultant id == owner id).
     * Any other actor that reached here (should not happen given @PreAuthorize) is denied.
     *
     * @param actor the authenticated user performing the action
     * @param owner the consultant the record belongs to
     * @param what  short noun for the error message, e.g. "loan case"
     */
    public static void assertConsultantOwns(User actor, User owner, String what) {
        if (isAdminTier(actor)) return;
        if (actor != null && "consultant".equals(actor.getRole())
                && owner != null && owner.getId() != null
                && owner.getId().equals(actor.getId())) {
            return;
        }
        throw new AccessDeniedException("You do not have access to this " + what);
    }

    /** True when both users are non-null and share the same id. */
    private static boolean sameUser(User a, User b) {
        return a != null && a.getId() != null && b != null && a.getId().equals(b.getId());
    }

    /**
     * Record-scoped check for client-owned data (investments, loans, notifications, profiles).
     * The owning user may act on their own record; admin-tier staff retain cross-cutting access.
     * Everyone else is denied — closing the IDOR where any authenticated user could read or
     * mutate another user's record by guessing its id.
     */
    public static void assertOwnerOrAdmin(User actor, User owner, String what) {
        if (isAdminTier(actor)) return;
        if (sameUser(actor, owner)) return;
        throw new AccessDeniedException("You do not have access to this " + what);
    }

    /**
     * Access to a consultation: the client who booked it, the assigned consultant, or
     * admin-tier staff. Any other authenticated user is denied.
     */
    public static void assertConsultationAccess(User actor, User client, User consultant, String what) {
        if (isAdminTier(actor)) return;
        if (sameUser(actor, client)) return;
        if (sameUser(actor, consultant)) return;
        throw new AccessDeniedException("You do not have access to this " + what);
    }
}
