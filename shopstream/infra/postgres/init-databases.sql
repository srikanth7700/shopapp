-- Database-per-service: each microservice owns its own database, and no
-- service ever reads another service's tables. They share data only through
-- REST calls and Kafka events.
--
-- For local development all six databases live in one PostgreSQL server to
-- save memory. In production they could be separate servers.
--
-- Written with \gexec so it can run more than once without failing
-- (useful when you run it by hand against RDS or Azure Database for PostgreSQL).
SELECT 'CREATE DATABASE users_db'         WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'users_db')\gexec
SELECT 'CREATE DATABASE products_db'      WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'products_db')\gexec
SELECT 'CREATE DATABASE inventory_db'     WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'inventory_db')\gexec
SELECT 'CREATE DATABASE orders_db'        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'orders_db')\gexec
SELECT 'CREATE DATABASE payments_db'      WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payments_db')\gexec
SELECT 'CREATE DATABASE notifications_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notifications_db')\gexec
