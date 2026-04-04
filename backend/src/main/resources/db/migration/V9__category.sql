create sequence "public"."category_id_seq";





create table "public"."category" (
    "id" integer not null default nextval('category_id_seq'::regclass),
    "uuid" uuid,
    "store_id" character(1),
    "parent_id" character(1),
    "name" character(1),
    "slug" character(1),
    "level" integer,
    "sort_order" integer,
    "image_url" character(255),
    "icon" character(255),
    "description" text,
    "is_active" boolean,
    "created_at" timestamp with time zone,
    "updated_at" timestamp with time zone,
    "deleted_at" timestamp with time zone
);


alter sequence "public"."category_id_seq" owned by "public"."category"."id";

CREATE UNIQUE INDEX category_pkey ON public.category USING btree (id);

alter table "public"."category" add constraint "category_pkey" PRIMARY KEY using index "category_pkey";


