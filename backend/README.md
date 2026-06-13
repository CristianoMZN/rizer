# Backend — core-marketplace

API Spring Boot do projeto rizer Marketplaces.

---

## Stack

- Java 25
- Spring Boot 4.0.5
- Spring Security + OAuth2 (Google)
- Spring Data JPA + Hibernate
- PostgreSQL 16 + PostGIS
- Redis Stack (vector store via Spring AI)
- WebSocket
- OpenFeign
- SpringDoc OpenAPI + Scalar (API Reference)

---

## Rodar em desenvolvimento

Pré-requisito: banco e Redis rodando via Docker Compose (raiz do projeto):

```bash
cd ..
docker compose up -d
```

Caso tenha algum container rodando conflitando na rede:
```bash
docker stop $(docker ps -q)
```

Iniciar backend:

```bash
./mvnw spring-boot:run
```



Observacoes de ambiente local:

- O repositório nao carrega credenciais em `application.yaml`; elas devem vir de variaveis de ambiente ou de um arquivo local `backend/.env`.
- `backend/.env` ja esta ignorado pelo git. Use `backend/.env.example` apenas como referencia para montar o ambiente local.
- `GOOGLE_OAUTH_CLIENT_ID` e `GOOGLE_OAUTH_CLIENT_SECRET` so sao necessarios para usar login OAuth2 do Google; sem eles, o backend sobe normalmente e responde `401` nas rotas protegidas.

No primeiro startup, o Flyway aplica automaticamente:

- schema base para banco zerado (`V1`, `V2`)

---

## Documentação da API

Com a aplicação rodando localmente:

- Scalar UI: `http://localhost:8080/docs`
- OpenAPI (JSON): `http://localhost:8080/openapi`

Notas:
- A interface visual está em Scalar (tema roxo customizado).
- O Swagger UI está desabilitado.

### Padrão para descrever endpoints e grupos

Para manter a documentação consistente no Scalar:

1. Defina grupo por controller com `@Tag(name = "...", description = "...")`.
2. Descreva cada rota com `@Operation(summary = "...", description = "...")`.
3. Documente status HTTP com `@ApiResponses` e `@ApiResponse`.
4. Prefira DTOs de resposta com `@Schema` em vez de expor entidade JPA diretamente.

Exemplo já aplicado:

- `HomeController` usa a tag `Sistema` para endpoints gerais.
- `UserController` usa a tag `Usuarios` para conta/perfil.
- `GET /users/me` retorna `UserMeResponse` com schema detalhado no OpenAPI.

### Base Region-Aware e Tenant-Aware implementada

Fluxo inicial entregue para marketplace multi-seller com tenant por seller:

- Filtro de contexto por pais via primeiro segmento da URL.
- Context holder por request (`ThreadLocal`) para regras regionais.
- Entidades `Product` + `ProductLocalization` com `attributes` em JSONB.
- Tenant por seller (`tenants`) com vinculo de operadores (`seller_users`).
- RBAC por role global (`users.system_role`) + escopo por seller.
- Configuracoes desacopladas por pais (`country_configurations`) e por tenant (`tenant_configurations`).
- Metadados dinamicos de atributos por contexto (`attribute_schemas`) com JSONB schema-driven.
- Persistencia dos atributos no proprio `products.attributes` (JSONB).
- Geolocalizacao com PostGIS (`geometry(Point,4326)`).
- Categoria hierarquica em `category_path` com tipo `ltree`.

Endpoints iniciais:

- `POST /{countryCode}/products` (lista todos os produtos publico)
- `GET /{countryCode}/products/search?lat=-28.448&lon=-52.203&radiusKm=50`
- `GET /{countryCode}/tenants/public` (vitrines publicas para visitantes anonimos)
- `POST /{countryCode}/media/upload` (upload autenticado de imagem para bucket S3 privado)

Exemplo de payload para `POST /BR/products`:

```json
{
  "sellerId": 1,
  "subsubcategorySlug": "suv",
  "realm": "VEHICLES",
  "categoryPath": "veiculos.passeio.suvs",
  "attributes": {
    "marca": "Toyota",
    "modelo": "Corolla Cross",
    "quilometragem": 50000,
    "cor": "Prata"
  },
  "title": "SUV seminovo",
  "description": "Unico dono, revisoes em dia",
  "price": 85000.00,
  "currency": "BRL",
  "status": "ACTIVE",
  "location": {
    "lat": -28.448,
    "lon": -52.203
  }
}
```

Observacao para ambiente local:

- O bootstrap do Postgres agora habilita: `postgis`, `postgis_topology` e `ltree`.
- Se o banco ja existia antes da mudanca, recrie os containers com `docker compose down -v && docker compose up -d`.

### Comentarios exibidos no /docs (Scalar)

Os endpoints regionais ja foram enriquecidos com descricoes no OpenAPI, e aparecem no `/docs` com:

- Grupo/tag de rotas (`Products`).
- `summary` e `description` por endpoint.
- Campos do request/response com `@Schema(description, example)`.
- Parametros de busca (`lat`, `lon`, `radiusKm`, `realm`, `limit`) com exemplos.

Arquivos principais dessas anotacoes:

- `ProductController` (operacoes e parametros).
- `CreateProductRequest` (schema de entrada).
- `ProductCreatedResponse` e `ProductSearchResultResponse` (schemas de saida).
- `OpenApiConfig` (titulo, versao e descricao geral da API).

---

## Decisões técnicas

### Migrations de banco (schema)

O projeto esta configurado para Flyway em desenvolvimento e producao.

Configuracao ativa em [src/main/resources/application.yaml](src/main/resources/application.yaml):

- `spring.flyway.enabled=true`
- `spring.flyway.locations=classpath:db/migration`
- `spring.flyway.baseline-on-migrate=true`
- `spring.flyway.baseline-version=0`
- `spring.jpa.hibernate.ddl-auto=validate`
- `spring.sql.init.mode=never`

Arquivos de migration:

- [src/main/resources/db/migration/V1__enable_extensions.sql](src/main/resources/db/migration/V1__enable_extensions.sql)
- [src/main/resources/db/migration/V2__create_core_schema_with_dynamic_attribute_schemas.sql](src/main/resources/db/migration/V2__create_core_schema_with_dynamic_attribute_schemas.sql)

Como aplicar as migrations no ambiente local:

1. Suba o banco com `docker compose up -d` na raiz do repositorio.
2. No backend, rode `./mvnw spring-boot:run`.
3. No startup, o Flyway aplica automaticamente todas as migrations pendentes.

Aplicacao manual via Maven (opcional):

```bash
./mvnw \
-Dflyway.url=jdbc:postgresql://localhost:5432/rizer_prod \
-Dflyway.user=rizer \
-Dflyway.password=rizer \
-Dflyway.baselineOnMigrate=true \
-Dflyway.baselineVersion=0 \
flyway:migrate
```

Validar status das migrations:

```bash
./mvnw \
-Dflyway.url=jdbc:postgresql://localhost:5432/rizer_prod \
-Dflyway.user=rizer \
-Dflyway.password=rizer \
flyway:info
```


Instalar Migra:
```bash
sudo apt update
sudo apt install python3-setuptools pipx libpq-dev postgresql-client build-essential
python3 -m venv .venv
source .venv/bin/activate
pip install --upgrade pip wheel "setuptools<70" psycopg2-binary psycopg2-binary migra sqlbag
migra --version
pipx inject migra setuptools

```
Gerar migrações:
```bash
migra --unsafe --schema public \
postgresql://rizer:rizer@localhost:5432/rizer_dev \
postgresql://rizer:rizer@localhost:5432/rizer_prod \
| grep -vE "flyway_schema_history|valid_detail|geometry_dump|postgis|ltree" \
> ./src/main/resources/db/migration/V11__schema_changes.sql
```


Reaplicar tudo do zero (somente desenvolvimento):

1. `docker compose down -v` na raiz do projeto.
2. `docker compose up -d`.
3. `cd backend && ./mvnw spring-boot:run`.

Sobre os dados iniciais no banco zerado:

- `V2` cria o schema completo usado pelas entidades JPA atuais.
- `V2` inclui seed minima de `subsubcategories` e `attribute_schemas` para contexto BR de veiculos.
- O schema global (`country_code='*'`) funciona como fallback quando nao existir schema especifico do pais.

### Validacao dinamica via schema JSONB

Implementacao nova no backend:

- O servico `DynamicAttributeValidationService` agora resolve schema por (`entityType`, `countryCode`, `categoryPath`).
- A validacao e recursiva para objetos/arrays e suporta regras comuns de JSON Schema.
- O endpoint `POST /{countryCode}/products` usa esse fluxo antes de salvar `products.attributes`.

Arquivos principais desta implementacao:

- `AttributeSchema` (entidade JPA da tabela `attribute_schemas`)
- `AttributeSchemaRepository` (lookup com fallback por pais)
- `DynamicAttributeValidationService` (validacao schema-driven)
- `ProductService` (integracao da validacao no fluxo de criacao)

---

## Upload de imagens (S3 publico) Magalu cloud

Foi adicionado um endpoint para upload de imagens com:

- autenticacao obrigatoria
- processamento por contexto (formato, compressao e tamanho)
- armazenamento em bucket S3 publico

Endpoint:

- `POST /media/upload`
- `Content-Type: multipart/form-data`

Campos esperados:

- `file` (arquivo)
- `type` (atualmente: `picture`)
- `context` (exemplos: `announce-gallery`, `store-logo`)
- `width` (opcional)
- `height` (opcional)

Observacao:

- o alias `annoince-galery` e aceito e mapeado para `announce-gallery`

Regras de contexto atualmente implementadas:

- `announce-gallery`: `webp`, qualidade 70%, tamanho padrao 400x400
- `store-logo`: `png`, qualidade 90%, tamanho padrao 512x512

Exemplo com curl:

```bash
curl -X POST "http://localhost:8080/media/upload" \
  -H "Authorization: Bearer <SEU_TOKEN>" \
  -F "file=@/caminho/imagem.jpg" \
  -F "type=picture" \
  -F "context=announce-gallery" \
  -F "width=400" \
  -F "height=400"
```



Exemplo de resposta:

```json
{
  "type": "picture",
  "context": "announce-gallery",
  "objectKey": "uploads/announce-gallery/9f8e7d6c-4b3a-4f8d-b457-9ad8c31f5bde.webp",
  "objectUrl": "https://rizer-pic.br-se1.magaluobjects.com/announce-gallery/9f8e7d6c-4b3a-4f8d-b457-9ad8c31f5bde.webp",
  "authorizationToken": "X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=...",
  "width": 400,
  "height": 400,
  "format": "webp",
  "sizeBytes": 81234
}
```

### Configuracao S3

Propriedades configuraveis em `application.yaml`:

- `app.s3.bucket`
- `app.s3.region`
- `app.s3.key-prefix`
- `app.s3.presigned-duration-minutes`

Variaveis de ambiente equivalentes:

```bash
export APP_S3_BUCKET=rizer-marketplaces-private
export APP_S3_REGION=us-east-1
export APP_S3_KEY_PREFIX=uploads
export APP_S3_PRESIGNED_DURATION_MINUTES=15
```

Credenciais AWS:

- o backend usa a cadeia padrao do AWS SDK (env vars, profile local, role, etc.)
- em ambiente local, normalmente: `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY`

---

## Variáveis de ambiente

Veja `backend/.env.example` para a lista completa de variáveis configuráveis.

---

## Controle RBAC:
Descrição das Roles:
- `sys_admin`:  Role de administração, tem acesso permissivo à toda a aplicação.
- `sys_manager`:  Role de administração de baixo nível, algumas informações e segredos da plataforma e crientes são ocultos
- `sys_employee`: Role utilizado por agente de suporte, não ve diversas informações.
- `agency_owner`: Role utilizada por contas que são proprietárias de uma concessionária, ou garagem de revenda de veículos
- `agency_admin`: Role utilizada por contas administradoras de concessionárias, garagem ou revendas.
- `agency_employee`: Role utilizada por contas que são vendedores ou atendentes das concessionárias e garagens
- `user`: Role utilizada por um usuário consumidor na plataforma.
- `none` ou sem role: Role para usuários não registrados, que não estão autenticados.

## Referências

- [Spring Boot](https://docs.spring.io/spring-boot/4.0.5)
- [Flyway](https://documentation.red-gate.com/flyway)
- [SpringDoc OpenAPI](https://springdoc.org)



