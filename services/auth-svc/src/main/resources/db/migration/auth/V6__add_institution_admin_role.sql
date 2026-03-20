-- V6__add_institution_admin_role.sql
-- Adds INSTITUTION_ADMIN to the role CHECK constraint.
-- INSTITUTION_ADMIN = institution owner, scoped to their own institution's centers only.
-- SUPER_ADMIN remains as the platform-operator role (no self-registration permitted).
-- center_id stays NULL for INSTITUTION_ADMIN users (they own multiple centers).

ALTER TABLE auth_schema.users
    DROP CONSTRAINT chk_users_role;

ALTER TABLE auth_schema.users
    ADD CONSTRAINT chk_users_role CHECK (role IN (
        'SUPER_ADMIN','INSTITUTION_ADMIN','CENTER_ADMIN',
        'TEACHER','PARENT','STUDENT','GUEST'
    ));
