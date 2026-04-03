# Rizer Marketplaces

Este repositório contém dois projetos principais:

- `backend/` - API Spring Boot (Java, Maven)
- `frontend-motor/` - frontend Vue 3 + Quasar (SPA/SSR/PWA)

---

## 1) Backend (desenvolvimento)

Requisitos
- Java 25 (conforme `backend/pom.xml`)
- Maven (pode usar o wrapper `./mvnw`)
- Docker e Docker Compose (para banco local)

### Subir dependências locais (PostgreSQL + PostGIS + Redis)

Na raiz do projeto:

```bash
docker compose up -d
```

Isso sobe:
- PostgreSQL 16 + PostGIS (`postgis/postgis:16-3.4`)
- Banco: `rizer_marketplaces`
- Usuário: `rizer`
- Senha: `rizer`
- Porta: `5432`
- Redis Stack Server (`redis/redis-stack-server`)
- Porta Redis: `6379`
- Redis Insight (`redis/redisinsight`) para inspeção
- Porta Redis Insight: `5540`

Os scripts SQL de inicialização ficam em `docker/postgres/init/` e já habilitam `postgis` e `postgis_topology`.

### Migrations e dados iniciais (backend)

O backend usa Flyway para controlar schema e seed de desenvolvimento.

Ao executar o backend (`cd backend && ./mvnw spring-boot:run`), as migrations pendentes sao aplicadas automaticamente.

Migrations atuais:

- `V1`: extensoes do Postgres (`postgis`, `postgis_topology`, `ltree`)
- `V2`: schema inicial para banco zerado com:
  - identidade e sellers (`users`, `tenants`, `seller_users`)
  - catalogo regional (`products`, `product_localizations`, `subsubcategories`)
  - arquitetura nova de atributos dinamicos (`attribute_schemas` em JSONB por contexto)

Comandos uteis (na pasta `backend`):

```bash
./mvnw \
  -Dflyway.url=jdbc:postgresql://localhost:5432/rizer_marketplaces \
  -Dflyway.user=rizer \
  -Dflyway.password=rizer \
  flyway:info

./mvnw \
  -Dflyway.url=jdbc:postgresql://localhost:5432/rizer_marketplaces \
  -Dflyway.user=rizer \
  -Dflyway.password=rizer \
  -Dflyway.baselineOnMigrate=true \
  -Dflyway.baselineVersion=0 \
  flyway:migrate
```

Documentacao completa no README do backend: [backend/README.md](backend/README.md).

Execução
```bash
cd backend
./mvnw spring-boot:run
```
ou
```bash
cd backend
mvn spring-boot:run
```

A aplicação roda por padrão em `http://localhost:8080`.

### Documentação da API (Scalar)

Com o backend em execução, a documentação interativa está em:

- Scalar UI: `http://localhost:8080/docs`
- OpenAPI (JSON): `http://localhost:8080/openapi`

Observações:
- O Swagger UI foi desabilitado e substituído por uma página do Scalar.
- O endpoint legado `http://localhost:8080/swagger-ui.html` redireciona para o Scalar.

Estrutura region-aware ja implementada na API:

- Prefixo regional na rota: `/{countryCode}/...` (ex: `/BR/products`).
- Modelo de dados para catalogo com JSONB por contexto:
  - `Product` com `attributes` em JSONB e `categoryPath` em `ltree`.
  - `AttributeSchema` com `schemaDefinition` em JSONB por `entityType + countryCode + categoryPath`.
  - `ProductLocalization` com `geometry(Point,4326)` para busca geoespacial.
- Busca por raio inicial: `GET /{countryCode}/products/search?lat=-28.448&lon=-52.203&radiusKm=50`.

### Nova arquitetura de atributos dinamicos (JSONB)

Agora a validacao dos atributos do produto ocorre por contexto:

- pais da loja/request (`countryCode` da rota)
- categoria do produto (`categoryPath` em formato `ltree`)

Fluxo da criacao de produto:

1. `POST /{countryCode}/products` recebe `attributes` no payload.
2. O backend resolve o schema ativo em `attribute_schemas` por (`PRODUCT_ATTRIBUTES`, `countryCode`, `categoryPath`).
3. Se nao encontrar schema especifico do pais, aplica fallback de `countryCode='*'`.
4. Valida `required`, `type`, `enum`, `minimum/maximum`, `minLength/maxLength`, `pattern`, objetos e arrays.
5. Salva o JSON validado em `products.attributes`.

Esse modelo permite que um carro, um imovel ou um alimento tenham estruturas totalmente diferentes sem alterar o schema relacional principal.

Obs: no `/docs`, os endpoints de products ja incluem descricoes de campos e exemplos de payload.

Obs 2: os comentarios de codigo foram atualizados no backend para destacar explicitamente a validacao schema-driven por contexto e o papel de `attribute_schemas`.

### Upload de imagens (S3 privado)

O backend agora possui endpoint autenticado para upload de imagem:

- `POST /media/upload`
- `Content-Type: multipart/form-data`
- campos: `file`, `type`, `context`, `width` (opcional), `height` (opcional)

Exemplo rapido:

```bash
curl -X POST "http://localhost:8080/media/upload" \
  -H "Authorization: Bearer <SEU_TOKEN>" \
  -F "file=@/caminho/imagem.jpg" \
  -F "type=picture" \
  -F "context=announce-gallery" \
  -F "width=400" \
  -F "height=400"
```

Regras de contexto atuais (exemplo):

- `announce-gallery`: webp, 70%, 400x400
- `store-logo`: png, 90%, 512x512

Configuracao por ambiente:

- `APP_S3_BUCKET`
- `APP_S3_REGION`
- `APP_S3_KEY_PREFIX`
- `APP_S3_PRESIGNED_DURATION_MINUTES`

Detalhes completos no README do backend: [backend/README.md](backend/README.md).

Configuração default do backend (`backend/src/main/resources/application.yaml`):
- URL: `jdbc:postgresql://localhost:5432/riser_marketplaces`
- Usuário: `riser`
- Senha: `riser`
- Redis host: `localhost`
- Redis port: `6379`

Você pode sobrescrever com variáveis de ambiente:

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/riser_marketplaces
export SPRING_DATASOURCE_USERNAME=riser
export SPRING_DATASOURCE_PASSWORD=riser
export SPRING_DATA_REDIS_HOST=localhost
export SPRING_DATA_REDIS_PORT=6379
```

Exemplo pronto: `backend/.env.example`.

Redis Insight (opcional): `http://localhost:5540`

### Parar banco local

Na raiz do projeto:

```bash
docker compose down
```

Para remover também o volume de dados:

```bash
docker compose down -v
```

---

## 2) Frontend (desenvolvimento)

Requisitos
- Node.js 18+ (compatível com Quasar)
- pnpm ou npm

Instalar dependências
```bash
cd frontend-motor
pnpm install
# ou
npm install
```

Rodar modo SPA (padrão)
```bash
cd frontend-motor
pnpm quasar dev
# ou
npm run dev
```

Rodar modo SSR
```bash
cd frontend-motor
pnpm quasar dev -m ssr
# ou
npm run dev -- -m ssr
```

---

## 3) Variáveis de ambiente úteis

- `VITE_API_URL`: URL da API (quando `useBackend = true` no `src/services/api.ts`)
- `VITE_BASE_DOMAIN`: domínio base para multi-tenant (SSR/local)

Exemplo .env
```env
VITE_API_URL=http://localhost:8080/api
VITE_BASE_DOMAIN=motorise.com.br
```

---

## 4) Comandos extras

- `cd frontend-motor && npx vue-tsc --noEmit` (typecheck)
- `cd frontend-motor && npm run lint` (lint)
- `cd frontend-motor && npm run format` (format)

---

## 5) Contato rápido

- Backend: `backend/HELP.md`
- Frontend: `frontend-motor/README.md`

---

## 6) Dockerfile de produção (backend)

Foi criado `backend/Dockerfile` (build multi-stage com Maven + runtime Java).

Build da imagem:

```bash
docker build -t riser-backend:prod ./backend
```

Executar container (exemplo):

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/riser_marketplaces \
  -e SPRING_DATASOURCE_USERNAME=riser \
  -e SPRING_DATASOURCE_PASSWORD=riser \
  riser-backend:prod
```

Se estiver em Linux e `host.docker.internal` não resolver, use o IP do host (ou rode backend e banco na mesma rede Docker).
