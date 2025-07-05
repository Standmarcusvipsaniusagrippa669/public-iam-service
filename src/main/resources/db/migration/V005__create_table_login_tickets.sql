CREATE TABLE login_tickets (
    id VARCHAR PRIMARY KEY,            -- UUID como string, identificador único del ticket
    email VARCHAR NOT NULL,            -- Email asociado al ticket
    expires_at TIMESTAMP NOT NULL,     -- Fecha/hora de expiración
    used BOOLEAN NOT NULL DEFAULT FALSE -- Si el ticket ya fue usado
);

CREATE INDEX idx_login_tickets_email ON login_tickets(email);
CREATE INDEX idx_login_tickets_expires_at ON login_tickets(expires_at);
