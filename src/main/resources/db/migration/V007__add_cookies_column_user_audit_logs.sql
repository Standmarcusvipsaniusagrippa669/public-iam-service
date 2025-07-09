ALTER TABLE public.user_audit_logs
ADD COLUMN IF NOT EXISTS cookies jsonb NULL;