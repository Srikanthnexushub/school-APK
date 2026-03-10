-- EduTech AI Platform — Database Initialisation (local dev)
-- Creates per-service databases, users, schemas + enables extensions.

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- auth-svc
CREATE USER auth_user WITH PASSWORD 'auth_pass_dev';
CREATE DATABASE auth_db OWNER auth_user;
\connect auth_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp"; CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE SCHEMA IF NOT EXISTS auth_schema AUTHORIZATION auth_user;
GRANT ALL ON SCHEMA auth_schema TO auth_user;
\connect postgres

-- parent-svc
CREATE USER parent_user WITH PASSWORD 'parent_pass_dev';
CREATE DATABASE parent_db OWNER parent_user;
\connect parent_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS parent_schema AUTHORIZATION parent_user;
GRANT ALL ON SCHEMA parent_schema TO parent_user;
\connect postgres

-- center-svc
CREATE USER center_user WITH PASSWORD 'center_pass_dev';
CREATE DATABASE center_db OWNER center_user;
\connect center_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS center_schema AUTHORIZATION center_user;
GRANT ALL ON SCHEMA center_schema TO center_user;
\connect postgres

-- assess-svc (pgvector)
CREATE USER assess_user WITH PASSWORD 'assess_pass_dev';
CREATE DATABASE assess_db OWNER assess_user;
\connect assess_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp"; CREATE EXTENSION IF NOT EXISTS vector;
CREATE SCHEMA IF NOT EXISTS assess_schema AUTHORIZATION assess_user;
GRANT ALL ON SCHEMA assess_schema TO assess_user; GRANT USAGE ON SCHEMA public TO assess_user;
\connect postgres

-- psych-svc
CREATE USER psych_user WITH PASSWORD 'psych_pass_dev';
CREATE DATABASE psych_db OWNER psych_user;
\connect psych_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS psych_schema AUTHORIZATION psych_user;
GRANT ALL ON SCHEMA psych_schema TO psych_user;
\connect postgres

-- student-profile-svc
CREATE USER student_profile_user WITH PASSWORD 'student_profile_pass_dev';
CREATE DATABASE student_profile_db OWNER student_profile_user;
\connect student_profile_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS student_schema AUTHORIZATION student_profile_user;
GRANT ALL ON SCHEMA student_schema TO student_profile_user;
\connect postgres

-- exam-tracker-svc
CREATE USER exam_tracker_user WITH PASSWORD 'exam_tracker_pass_dev';
CREATE DATABASE exam_tracker_db OWNER exam_tracker_user;
\connect exam_tracker_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS examtracker_schema AUTHORIZATION exam_tracker_user;
GRANT ALL ON SCHEMA examtracker_schema TO exam_tracker_user;
\connect postgres

-- performance-svc (TimescaleDB)
CREATE USER performance_user WITH PASSWORD 'performance_pass_dev';
CREATE DATABASE performance_db OWNER performance_user;
\connect performance_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp"; CREATE EXTENSION IF NOT EXISTS timescaledb CASCADE;
CREATE SCHEMA IF NOT EXISTS performance_schema AUTHORIZATION performance_user;
GRANT ALL ON SCHEMA performance_schema TO performance_user;
\connect postgres

-- ai-mentor-svc
CREATE USER ai_mentor_user WITH PASSWORD 'ai_mentor_pass_dev';
CREATE DATABASE ai_mentor_db OWNER ai_mentor_user;
\connect ai_mentor_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS aimentor_schema AUTHORIZATION ai_mentor_user;
GRANT ALL ON SCHEMA aimentor_schema TO ai_mentor_user;
\connect postgres

-- career-oracle-svc
CREATE USER career_oracle_user WITH PASSWORD 'career_oracle_pass_dev';
CREATE DATABASE career_oracle_db OWNER career_oracle_user;
\connect career_oracle_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS careeroracle_schema AUTHORIZATION career_oracle_user;
GRANT ALL ON SCHEMA careeroracle_schema TO career_oracle_user;
\connect postgres

-- mentor-svc
CREATE USER mentor_user WITH PASSWORD 'mentor_pass_dev';
CREATE DATABASE mentor_db OWNER mentor_user;
\connect mentor_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS mentor_schema AUTHORIZATION mentor_user;
GRANT ALL ON SCHEMA mentor_schema TO mentor_user;
\connect postgres

-- notification-svc
CREATE USER notification_user WITH PASSWORD 'notification_pass_dev';
CREATE DATABASE notification_db OWNER notification_user;
\connect notification_db
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE SCHEMA IF NOT EXISTS notification_schema AUTHORIZATION notification_user;
GRANT ALL ON SCHEMA notification_schema TO notification_user;
\connect postgres
