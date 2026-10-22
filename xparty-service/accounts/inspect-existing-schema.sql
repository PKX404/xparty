-- Read-only inventory for gydhasaztdanfidrysfp. Inspect BEFORE any migration.
-- Does not read account rows, email addresses, messages or secrets.
select table_schema,table_name,column_name,data_type,udt_name,is_nullable,column_default
from information_schema.columns
where table_schema='public'
order by table_name,ordinal_position;

select n.nspname as schema_name,c.relname as table_name,c.relrowsecurity as rls_enabled,
 c.relforcerowsecurity as rls_forced
from pg_class c join pg_namespace n on n.oid=c.relnamespace
where n.nspname='public' and c.relkind in ('r','p') order by c.relname;

select schemaname,tablename,policyname,roles,cmd,qual,with_check
from pg_policies where schemaname='public' order by tablename,policyname;

select table_name,grantee,privilege_type from information_schema.role_table_grants
where table_schema='public' and grantee in ('anon','authenticated') order by table_name,grantee,privilege_type;

select p.proname,pg_get_function_identity_arguments(p.oid) as arguments,p.prosecdef as security_definer,
 pg_get_functiondef(p.oid) as definition
from pg_proc p join pg_namespace n on n.oid=p.pronamespace
where n.nspname='public' and p.proname like 'xparty_%' order by p.proname;

select n.nspname as schema_name,c.relname as table_name,con.conname,pg_get_constraintdef(con.oid) as definition
from pg_constraint con join pg_class c on c.oid=con.conrelid join pg_namespace n on n.oid=c.relnamespace
where n.nspname='public' order by c.relname,con.conname;

select table_name,column_name,grantee,privilege_type
from information_schema.column_privileges where table_schema='public' and grantee in ('anon','authenticated')
order by table_name,column_name,grantee;

select to_regclass('supabase_migrations.schema_migrations') as migration_ledger;
