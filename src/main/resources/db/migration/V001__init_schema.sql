-- Tabla principal de empresas
CREATE TABLE companies (
    id UUID PRIMARY KEY,                  -- Identificador único de la empresa
    rut VARCHAR(10),                      -- RUT chileno de la empresa, con guión
    business_name VARCHAR,                -- Razón social registrada en el SII
    trade_name VARCHAR,                   -- Nombre de fantasía o comercial
    activity VARCHAR,                     -- Giro o actividad económica
    address VARCHAR,                      -- Dirección de la empresa
    commune VARCHAR,                      -- Comuna según SII
    region VARCHAR,                       -- Región administrativa
    email VARCHAR,                        -- Correo de contacto principal
    phone VARCHAR,                        -- Teléfono de contacto
    logo_url TEXT,                        -- URL del logo de la empresa (opcional)
    status VARCHAR,                       -- Estado general (active, suspended, etc.)
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Usuarios del sistema
CREATE TABLE users (
    id UUID PRIMARY KEY,                      -- Identificador único del usuario
    full_name VARCHAR,                        -- Nombre completo
    email VARCHAR UNIQUE,                     -- Email usado para login
    password_hash TEXT,                       -- Contraseña encriptada
    email_validated BOOLEAN DEFAULT FALSE,    -- ¿Email validado?
    last_password_change TIMESTAMP,           -- Último cambio de contraseña
    must_change_password BOOLEAN DEFAULT FALSE, -- ¿Debe cambiar contraseña en el próximo login?
    status VARCHAR,                           -- Estado del usuario (active, disabled)
    last_login_at TIMESTAMP,                  -- Última vez que ingresó al sistema
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Asociación usuario-empresa con rol
CREATE TABLE user_companies (
    id UUID PRIMARY KEY,                          -- Identificador del vínculo usuario-empresa
    user_id UUID REFERENCES users(id),            -- Usuario asociado
    company_id UUID REFERENCES companies(id),     -- Empresa asociada
    role VARCHAR,                                 -- Rol en esa empresa ('owner', 'admin', 'operator', 'viewer')
    invited_by UUID REFERENCES users(id),         -- Usuario que hizo la invitación (opcional)
    status VARCHAR DEFAULT 'active',              -- Estado del vínculo (active, invited, blocked, disabled)
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE INDEX idx_user_companies_user_id ON user_companies(user_id);
CREATE INDEX idx_user_companies_company_id ON user_companies(company_id);
CREATE UNIQUE INDEX uq_user_companies_user_company ON user_companies(user_id, company_id);

-- Invitaciones de usuario a empresa
CREATE TABLE user_invitations (
    id UUID PRIMARY KEY,                        -- Identificador de la invitación
    invited_email VARCHAR,                      -- Email de la persona invitada
    company_id UUID REFERENCES companies(id),   -- Empresa a la que se invita
    role VARCHAR,                               -- Rol que se asignará
    invited_by UUID REFERENCES users(id),       -- Usuario que genera la invitación
    invitation_token VARCHAR,                   -- Token único de invitación
    status VARCHAR,                             -- Estado (pending, accepted, expired, cancelled)
    expires_at TIMESTAMP,                       -- Expiración del link
    accepted_at TIMESTAMP,                      -- Fecha de aceptación
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Validación de email
CREATE TABLE user_email_verifications (
    id UUID PRIMARY KEY,                        -- Identificador del registro
    user_id UUID REFERENCES users(id),          -- Usuario asociado
    verification_token VARCHAR,                 -- Token de verificación único
    status VARCHAR,                             -- Estado (pending, completed, expired)
    requested_at TIMESTAMP,
    completed_at TIMESTAMP,
    expires_at TIMESTAMP
);

-- Reseteo de contraseña
CREATE TABLE user_password_resets (
    id UUID PRIMARY KEY,                        -- Identificador del registro
    user_id UUID REFERENCES users(id),          -- Usuario asociado
    reset_token VARCHAR,                        -- Token único para resetear
    status VARCHAR,                             -- Estado (pending, used, expired)
    requested_at TIMESTAMP,
    used_at TIMESTAMP,
    expires_at TIMESTAMP
);

-- Logs de intentos de login
CREATE TABLE user_logins (
    id UUID PRIMARY KEY,                        -- Identificador único del log
    user_id UUID REFERENCES users(id),          -- Usuario que intenta el login
    company_id UUID REFERENCES companies(id),   -- Empresa elegida o relacionada
    login_at TIMESTAMP,                         -- Fecha/hora del intento
    ip_address VARCHAR,                         -- IP de origen
    user_agent TEXT,                            -- Navegador/dispositivo
    successful BOOLEAN,                         -- ¿Fue exitoso?
    fail_reason VARCHAR,                        -- Motivo del fallo (si existe)
    created_at TIMESTAMP
);

-- Auditoría de acciones de usuarios
CREATE TABLE user_audit_logs (
    id UUID PRIMARY KEY,                        -- Identificador del registro
    user_id UUID REFERENCES users(id),          -- Usuario que realiza la acción
    company_id UUID REFERENCES companies(id),   -- Empresa relacionada
    action VARCHAR,                             -- Acción ejecutada (create, update, block, etc.)
    target_user_id UUID,                        -- Usuario objetivo (si aplica)
    resource_type VARCHAR,                      -- Tipo de recurso modificado
    resource_id UUID,                           -- Identificador del recurso modificado
    metadata JSONB,                             -- Información adicional relevante
    action_at TIMESTAMP,                        -- Fecha/hora de la acción
    ip_address VARCHAR,                         -- IP de ejecución
    user_agent TEXT,                            -- Navegador/dispositivo
    created_at TIMESTAMP
);

-- Índices recomendados adicionales
CREATE INDEX idx_user_logins_user_id ON user_logins(user_id);
CREATE INDEX idx_user_logins_company_id ON user_logins(company_id);
CREATE INDEX idx_user_audit_logs_company_id ON user_audit_logs(company_id);
CREATE INDEX idx_user_audit_logs_user_id ON user_audit_logs(user_id);
