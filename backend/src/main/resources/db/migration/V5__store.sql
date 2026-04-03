create sequence "public"."store_id_seq";





create table "public"."store" (
    "id" integer not null default nextval('store_id_seq'::regclass),
    "name" character(255),
    "local_name" character(255),
    "iso_alpha_2" character(255),
    "iso_alpha_3" character(255),
    "numeric_code" character(255),
    "currency_code_iso" character(255),
    "currency_name" character(255),
    "currency_symbol" character(255),
    "currency_symbol_position" character(255),
    "currency_minor_unit" character(255),
    "timezone_default" character(255),
    "language_default" character(255),
    "locale_default" character(255),
    "store_status" character(255),
    "tenant_status" character(255),
    "default_phone_code" character(255),
    "date_format_default" character(255),
    "postal_code_required" character(255),
    "tax_identifier_label" character(255),
    "address_format" character(255),
    "created_at" timestamp with time zone,
    "updated_at" timestamp with time zone
);


alter sequence "public"."store_id_seq" owned by "public"."store"."id";

CREATE UNIQUE INDEX store_pkey ON public.store USING btree (id);

alter table "public"."store" add constraint "store_pkey" PRIMARY KEY using index "store_pkey";


