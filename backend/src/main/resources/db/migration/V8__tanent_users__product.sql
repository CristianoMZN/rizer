create sequence "public"."product_id_seq";

create sequence "public"."product_store_id_seq";

create sequence "public"."tenant_users_id_seq";





create table "public"."product" (
    "id" integer not null default nextval('product_id_seq'::regclass),
    "uuid" uuid,
    "store_id" integer not null default nextval('product_store_id_seq'::regclass),
    "tenant_id" character(1),
    "title" character(255),
    "value" character(255),
    "description" character(255),
    "location" geography,
    "category_id" character(1),
    "created_at" timestamp with time zone,
    "updated_at" timestamp with time zone,
    "deleted_at" timestamp with time zone
);


create table "public"."tenant_users" (
    "id" integer not null default nextval('tenant_users_id_seq'::regclass),
    "uuid" uuid,
    "tenant_id" character(255),
    "user_id" character(255),
    "role" character(255),
    "created_at" timestamp with time zone,
    "expire_at" time with time zone
);


alter sequence "public"."product_id_seq" owned by "public"."product"."id";

alter sequence "public"."product_store_id_seq" owned by "public"."product"."store_id";

alter sequence "public"."tenant_users_id_seq" owned by "public"."tenant_users"."id";

CREATE UNIQUE INDEX tenant_users_pkey ON public.tenant_users USING btree (id);

alter table "public"."tenant_users" add constraint "tenant_users_pkey" PRIMARY KEY using index "tenant_users_pkey";


