package com.finbridge.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Concurrency-safe human-readable id generator backed by Postgres sequences.
 * Replaces the previous {@code repository.count() + 1} scheme, which had a
 * read-then-write race under concurrent inserts (duplicate ids / unique-violation).
 */
@Component
public class SequenceGenerator {

    @PersistenceContext
    private EntityManager em;

    /**
     * Returns the next value for the given sequence formatted as {@code PREFIX-00001}.
     * The sequence name is restricted to the known, hard-coded values in {@link Seq}.
     */
    @Transactional
    public String next(Seq seq) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('" + seq.sequenceName + "')").getSingleResult();
        return seq.prefix + "-" + String.format("%05d", n.longValue());
    }

    /** Known sequences (name + id prefix). Hard-coded to avoid any injection surface. */
    public enum Seq {
        LEAD("lead_seq", "LEAD"),
        LOAN_CASE("loan_case_seq", "LC"),
        DEPT_CASE("dept_case_seq", null),     // prefix is per-department, supplied by caller
        INVOICE("invoice_seq", "INV"),
        SERVICE_REQUEST("service_request_seq", "SR"),
        SUPPORT_TICKET("support_ticket_seq", "TKT"),
        ORG_PAYMENT("org_payment_seq", "PAY");

        final String sequenceName;
        final String prefix;
        Seq(String sequenceName, String prefix) { this.sequenceName = sequenceName; this.prefix = prefix; }
    }

    /** Raw next value (for callers that build their own prefix, e.g. dept cases). */
    @Transactional
    public long nextValue(Seq seq) {
        Number n = (Number) em.createNativeQuery("SELECT nextval('" + seq.sequenceName + "')").getSingleResult();
        return n.longValue();
    }
}
