create table companies (
 id uuid primary key,
 name varchar(120) not null,
 created_by uuid not null references app_users(id),
 created_at timestamp with time zone not null
);
create table company_members (
 id uuid primary key,
 company_id uuid not null references companies(id) on delete cascade,
 user_id uuid not null references app_users(id) on delete cascade,
 role varchar(20) not null,
 constraint uk_company_member unique(company_id,user_id)
);
insert into companies(id,name,created_by,created_at) select id, username, id, created_at from app_users;
insert into company_members(id,company_id,user_id,role) select id,id,id,'ADMIN' from app_users;
alter table servers add column company_id uuid references companies(id);
update servers set company_id=owner_id;
alter table servers alter column company_id set not null;
create index idx_servers_company on servers(company_id);
