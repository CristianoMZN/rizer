create sequence "public"."attribute_schemas_id_seq";





create table "public"."attribute_schemas" (
    "id" integer not null default nextval('attribute_schemas_id_seq'::regclass),
    "uuid" uuid,
    "entity_type" character(255),
    "context_id" character(255),
    "schema_name" character(255),
    "version" character(255),
    "is_active" boolean,
    "schema_definition" jsonb,
    "created_at" timestamp with time zone,
    "updated_at" timestamp with time zone,
    "deleted_at" timestamp with time zone
);


alter sequence "public"."attribute_schemas_id_seq" owned by "public"."attribute_schemas"."id";

CREATE UNIQUE INDEX attribute_schemas_pkey ON public.attribute_schemas USING btree (id);

alter table "public"."attribute_schemas" add constraint "attribute_schemas_pkey" PRIMARY KEY using index "attribute_schemas_pkey";


