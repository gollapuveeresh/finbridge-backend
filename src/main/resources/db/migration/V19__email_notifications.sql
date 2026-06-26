-- Email notification history for audit trail and duplicate prevention
CREATE TABLE email_notifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient       VARCHAR(255) NOT NULL,
    type            VARCHAR(100) NOT NULL,
    related_entity_id UUID,
    subject         VARCHAR(500),
    status          VARCHAR(20)  NOT NULL DEFAULT 'sent',
    error_message   TEXT,
    sent_at         TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE INDEX idx_email_notif_dedup ON email_notifications(recipient, type, related_entity_id, status);
CREATE INDEX idx_email_notif_recipient ON email_notifications(recipient);
