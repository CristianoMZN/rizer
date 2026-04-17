



alter table "public"."users" alter column "email" set data type character varying(255) using "email"::character varying(255);


alter table "public"."users" alter column "password" set data type character varying(255) using "password"::character varying(255);

alter table "public"."users" alter column "phone" set data type character varying(255) using "phone"::character varying(255);

alter table "public"."users" alter column "username" set data type character varying(255) using "username"::character varying(255);



