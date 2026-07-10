-- Extensiones compartidas por todos los schemas de orderdb
-- Se ejecuta una sola vez al inicializar el contenedor PostgreSQL
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
