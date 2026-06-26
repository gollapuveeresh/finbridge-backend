-- Concurrency-safe human-readable id generation via sequences (replaces count()+1 race).
-- Each sequence is initialized to the max existing numeric suffix so new ids never collide.

CREATE SEQUENCE IF NOT EXISTS lead_seq;
CREATE SEQUENCE IF NOT EXISTS loan_case_seq;
CREATE SEQUENCE IF NOT EXISTS dept_case_seq;
CREATE SEQUENCE IF NOT EXISTS invoice_seq;
CREATE SEQUENCE IF NOT EXISTS service_request_seq;
CREATE SEQUENCE IF NOT EXISTS support_ticket_seq;
CREATE SEQUENCE IF NOT EXISTS org_payment_seq;

-- helper: greatest existing numeric suffix in a VARCHAR id column, defaulting to a floor
-- (setval with is_called=true means the NEXT nextval() returns value+1)
SELECT setval('lead_seq',
  GREATEST(COALESCE((SELECT MAX(CAST(regexp_replace(lead_id,'\D','','g') AS INTEGER))
                     FROM leads WHERE lead_id ~ '\d'), 0), 0) + 1, false);

SELECT setval('loan_case_seq',
  GREATEST(COALESCE((SELECT MAX(CAST(regexp_replace(case_id,'\D','','g') AS INTEGER))
                     FROM loan_cases WHERE case_id ~ '\d'), 0), 0) + 1, false);

SELECT setval('dept_case_seq',
  GREATEST(COALESCE((SELECT MAX(CAST(regexp_replace(case_id,'\D','','g') AS INTEGER))
                     FROM dept_cases WHERE case_id ~ '\d'), 0), 0) + 1, false);

SELECT setval('invoice_seq',
  GREATEST(COALESCE((SELECT MAX(CAST(regexp_replace(invoice_number,'\D','','g') AS INTEGER))
                     FROM invoices WHERE invoice_number ~ '\d'), 0), 0) + 1, false);

-- B2B counters previously started at 1000 in memory; keep that floor.
SELECT setval('service_request_seq',
  GREATEST(COALESCE((SELECT MAX(CAST(regexp_replace(request_number,'\D','','g') AS INTEGER))
                     FROM service_requests WHERE request_number ~ '\d'), 0), 1000) + 1, false);

SELECT setval('support_ticket_seq',
  GREATEST(COALESCE((SELECT MAX(CAST(regexp_replace(ticket_number,'\D','','g') AS INTEGER))
                     FROM support_tickets WHERE ticket_number ~ '\d'), 0), 1000) + 1, false);

SELECT setval('org_payment_seq', 1001, false);
