package com.finbridge.security;

/**
 * Reusable Spring Security authorization expressions for @PreAuthorize.
 * Roles map to authorities as ROLE_&lt;role-uppercased-with-underscores&gt;
 * (e.g. "department-admin" -> ROLE_DEPARTMENT_ADMIN).
 */
public final class SecurityRoles {
    private SecurityRoles() {}

    /** Any internal platform staff member (everyone except clients). */
    public static final String STAFF =
        "hasAnyRole('SUPER_ADMIN','ADMIN','CRM_ADMIN','DEPARTMENT_ADMIN','CONSULTANT')";

    /** Platform administrators only. */
    public static final String ADMINS =
        "hasAnyRole('SUPER_ADMIN','ADMIN')";

    /** Administrators plus department admins (who manage their department's staff). */
    public static final String ADMIN_OR_DEPT =
        "hasAnyRole('SUPER_ADMIN','ADMIN','DEPARTMENT_ADMIN')";
}
