create sequence "public"."addresses_id_seq";


create sequence "public"."tenant_id_seq";





create table "public"."addresses" (
    "id" integer not null default nextval('addresses_id_seq'::regclass),
    "uuid" uuid,
    "store_id" character(255),
    "user_id" character(255),
    "name" character(255),
    "street" character(255),
    "number" character(255),
    "neighborhood" character(255),
    "city" character(255),
    "state" character(255),
    "country" character(255),
    "postal_code" character(255),
    "location" geography,
    "created_at" timestamp with time zone,
    "updated_at" timestamp with time zone,
    "deleted_at" timestamp with time zone
);




create table "public"."tenant" (
    "id" integer not null default nextval('tenant_id_seq'::regclass),
    "uuid" uuid,
    "store_id" character(255),
    "user_id" character(255),
    "slug" character(255),
    "name" character(255),
    "description" character(255),
    "image_profile" character(255),
    "image_cover" character(255),
    "attributes" jsonb,
    "created_at" timestamp with time zone,
    "updated_at" timestamp with time zone,
    "deleted_at" timestamp with time zone
);


alter sequence "public"."addresses_id_seq" owned by "public"."addresses"."id";

alter sequence "public"."tenant_id_seq" owned by "public"."tenant"."id";

CREATE UNIQUE INDEX addresses_pkey ON public.addresses USING btree (id);

CREATE UNIQUE INDEX tenant_pkey ON public.tenant USING btree (id);

alter table "public"."addresses" add constraint "addresses_pkey" PRIMARY KEY using index "addresses_pkey";

alter table "public"."tenant" add constraint "tenant_pkey" PRIMARY KEY using index "tenant_pkey";


