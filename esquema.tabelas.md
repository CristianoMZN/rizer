# Esquema de tabelas da arquitetura CBA com JSONB

A arquitetura foi pensada para suportar **dados estáveis em colunas relacionais** e **dados variáveis em JSONB**, com suporte a contexto por país, tenant e categoria.

## 1) `store`
Tabela de referência para países/mercados em que a aplicação opera.

### Campos
- `id` — PK, `VARCHAR(2)` — código do país, ex.: `BR`, `US`, `JP`
- `name` — nome em inglês do país
- `local_name` — nome local do país
- `iso_alpha_2` — código ISO alfa-2
- `iso_alpha_3` — código ISO alfa-3
- `numeric_code` — código numérico ISO
- `currency_code_iso` — código da moeda, ex.: `BRL`, `USD`, `JPY`
- `currency_name` — nome da moeda
- `currency_symbol` — símbolo da moeda
- `currency_symbol_position` — posição do símbolo, `start` ou `end`
- `currency_minor_unit` — número de casas decimais da moeda
- `timezone_default` — fuso horário padrão
- `language_default` — idioma padrão
- `locale_default` — locale padrão
- `store_status` — status da store
- `tenant_status` — status operacional para tenants naquele contexto
- `default_phone_code` — código telefônico do país
- `date_format_default` — formato de data padrão
- `postal_code_required` — indica se CEP/código postal é obrigatório
- `tax_identifier_label` — rótulo do identificador fiscal, ex.: CPF, NIF, EIN
- `address_format` — formato padrão de endereço
- `created_at` — data de criação
- `updated_at` — data de atualização

---

## 2) `users`
Tabela principal de usuários globais.

### Campos
- `id` — PK
- `uuid` — identificador global único
- `username` — nome de usuário
- `email` — e-mail do usuário
- `phone` — telefone principal
- `password` — senha hash
- `attributes` — JSONB com dados adicionais variáveis
- `created_at` — data de criação
- `updated_at` — data de atualização
- `deleted_at` — data de exclusão lógica

---

## 3) `addresses`
Tabela de endereços vinculados a usuários.

### Campos
- `id` — PK
- `uuid` — identificador global único
- `store_id` — FK para `store`
- `user_id` — FK para `users`
- `name` — nome de referência do endereço, ex.: casa, trabalho
- `street` — rua
- `number` — número
- `neighborhood` — bairro
- `city` — cidade
- `state` — estado
- `country` — país
- `postal_code` — CEP ou código postal
- `location` — geolocalização `geography(Point, 4326)`
- `created_at` — data de criação
- `updated_at` — data de atualização
- `deleted_at` — data de exclusão lógica

---

## 4) `tenant`
Tabela de perfil do vendedor/loja dentro de uma store.

### Campos
- `id` — PK
- `uuid` — identificador global único
- `store_id` — FK para `store`
- `user_id` — usuário dono do tenant
- `slug` — identificador amigável
- `name` — nome do tenant
- `description` — descrição
- `image_profile` — imagem de perfil
- `image_cover` — imagem de capa
- `attributes` — JSONB com dados variáveis
- `created_at` — data de criação
- `updated_at` — data de atualização
- `deleted_at` — data de exclusão lógica

---

## 5) `tenant_users`
Tabela de associação entre usuários e tenants, representando funcionários ou colaboradores.

### Campos
- `id` — PK
- `uuid` — identificador global único
- `tenant_id` — FK para `tenant`
- `user_id` — FK para `users`
- `role` — papel do usuário, ex.: `owner`, `manager`, `seller`
- `created_at` — data de criação
- `expire_at` — data de expiração do vínculo

---

## 6) `product`
Tabela principal de produtos/anúncios.

### Campos
- `id` — PK
- `uuid` — identificador global único
- `store_id` — FK para `store`
- `tenant_id` — FK para `tenant`
- `title` — título do anúncio
- `value` — preço do produto
- `description` — descrição
- `location` — geolocalização `geography(Point, 4326)`
- `category_id` — FK para `category`
- `created_at` — data de criação
- `updated_at` — data de atualização
- `deleted_at` — data de exclusão lógica

### Observação
Em vez de armazenar `category_level_1`, `category_level_2` e `category_level_3` no produto, é melhor usar apenas `category_id` e resolver a hierarquia pela tabela de categorias.

---

## 7) `category`
Tabela de categorias de produtos, com até 3 níveis hierárquicos.

### Campos
- `id` — PK
- `uuid` — identificador global único
- `store_id` — FK para `store`
- `parent_id` — FK auto-relacionada para `category`
- `name` — nome da categoria
- `slug` — identificador amigável
- `level` — nível da categoria, de 1 a 3
- `sort_order` — ordem de exibição
- `image_url` — imagem da categoria
- `icon` — ícone da categoria
- `description` — descrição da categoria
- `is_active` — indica se a categoria está ativa
- `created_at` — data de criação
- `updated_at` — data de atualização
- `deleted_at` — data de exclusão lógica

---

## 8) `attribute_schemas`
Tabela que define os esquemas JSONB por entidade e contexto.

### Campos
- `id` — PK serial
- `entity_type` — tipo da entidade, ex.: `user`, `product`, `tenant`
- `context_id` — contexto de aplicação, ex.: país, tenant ou categoria
- `schema_name` — nome do schema
- `version` — versão do schema
- `is_active` — indica se o schema está ativo
- `schema_definition` — JSONB com a definição do esquema
- `created_at` — data de criação
- `updated_at` — data de atualização
- `deleted_at` — data de exclusão lógica

---

# Estrutura geral da arquitetura

A modelagem segue estes princípios:

- **tabelas relacionais** para entidades centrais e estáveis
- **JSONB** para atributos dinâmicos e variáveis
- **schemas por contexto** para validar e controlar os dados variáveis
- **geolocalização** para usuários, endereços e anúncios quando necessário
- **hierarquia de categorias** com no máximo 3 níveis
- **soft delete** nas entidades principais quando apropriado

---

# Decisões importantes

## JSONB
Usado principalmente em:
- `users.attributes`
- `tenant.attributes`
- `product.attributes` se houver campos variáveis
- `attribute_schemas.schema_definition`

## Geolocalização
Usar `geography(Point, 4326)` em:
- `addresses.location`
- `product.location`

## Categorias
- `category.parent_id` define a hierarquia
- `product.category_id` referencia a categoria principal
- filtros por pai podem ser resolvidos com closure table ou consultas hierárquicas se necessário

---

# Observações finais

Essa estrutura foi pensada para ser:

- escalável
- flexível
- fácil de validar
- adequada para PostgreSQL
- compatível com Spring Boot e JPA/Hibernate
- preparada para crescimento multi-país e multi-tenant

Se quiser, eu posso transformar esse texto em um dos formatos abaixo:

1. **documentação mais formal**
2. **tabela markdown**
3. **DDL SQL completo**
4. **projeto de migration Flyway**
5. **README técnico para o repositório**
