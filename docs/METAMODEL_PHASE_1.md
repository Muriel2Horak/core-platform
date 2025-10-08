# Metamodel – Fáze 1

## Přehled

Fáze 1 zavádí **metamodel-driven architecture** do Core Platform s následujícími funkcemi:

- ✅ **YAML Metamodel Registry** – Definice entit, políček a access policies v YAML
- ✅ **PolicyEngine (RBAC + ABAC)** – Vyhodnocování pravidel s podporou dot-notace (1 úroveň)
- ✅ **CRUD REST API** – Generické endpointy nad existující DB tabulkami
- ✅ **ETag/If-Match** – Optimistic locking pomocí version sloupce
- ✅ **Postgres RLS** – Row Level Security na tenant_id
- ✅ **Edit Locks** – Soft locking s auto-expiry
- ✅ **UI Capabilities** – Endpoint `/api/me/ui-capabilities` generovaný z metamodelu

## Struktura YAML Metamodelu

### Soubor: `backend/src/main/resources/metamodel/user-profile.yaml`

```yaml
entity: UserProfile
table: user_profile
idField: id
versionField: version
tenantField: tenant_id

fields:
  - name: id
    type: uuid
    pk: true
  - name: full_name
    type: string
    required: true
    maxLength: 200
  - name: email
    type: email
    unique: true

accessPolicy:
  read:
    anyOf:
      - role: CORE_ROLE_ADMIN
      - role: CORE_ROLE_TENANT_ADMIN
      - sameUser: true
  
  update:
    anyOf:
      - role: CORE_ROLE_ADMIN
      - allOf:
          - role: CORE_ROLE_TENANT_ADMIN
          - eq:
              left: "${entity.tenant_id}"
              right: "${user.tenant_id}"
  
  columns:
    email:
      read:
        anyOf:
          - role: CORE_ROLE_ADMIN
          - role: CORE_ROLE_TENANT_ADMIN
      write:
        anyOf:
          - role: CORE_ROLE_ADMIN

ui:
  list:
    columns: [full_name, email, department]
  detail:
    sections:
      - name: Main
        fields: [full_name, email]
```

## ABAC Pravidla

### Podporované operátory

- **anyOf** – Alespoň jedno pravidlo musí platit (OR)
- **allOf** – Všechna pravidla musí platit (AND)
- **role** – Kontrola role: `role: CORE_ROLE_ADMIN`
- **group** – Kontrola skupiny (zatím mapováno na role)
- **sameUser** – Entity musí patřit aktuálnímu uživateli
- **eq** – Rovnost: `eq: { left: "${entity.tenant_id}", right: "${user.tenant_id}" }`
- **ne** – Nerovnost
- **contains** – Řetězec obsahuje podřetězec
- **in** – Hodnota je v seznamu

### Dot-notace (1 úroveň)

Podporuje přístup k relacím 1 úrovně hlouběk:

```yaml
eq:
  left: "${entity.department.manager_id}"
  right: "${user.id}"
```

**Limit:** Pouze 1-hop relace (např. `entity.field`, ne `entity.field.nested.field`).

## CRUD REST API

### Endpoints

#### **GET** `/api/entities/{type}`
Seznam entit s filtrováním a stránkováním.

**Query params:**
- `filter` – Filtr: `field=value`, `field__like=%pattern%`, `field__in=val1,val2`
- `sort` – Řazení: `field` nebo `-field` (descending)
- `page`, `size` – Stránkování

**Příklad:**
```bash
GET /api/entities/UserProfile?filter=department=Engineering&sort=-created_at&page=0&size=20
```

#### **GET** `/api/entities/{type}/{id}`
Detail entity podle ID.

**Response:**
- Header `ETag: W/"<version>"`
- Body: Entity JSON (pouze povolené sloupce)

#### **POST** `/api/entities/{type}`
Vytvoření nové entity.

- Automaticky doplní `tenant_id` z JWT
- Nastaví `version=0`

#### **PUT** `/api/entities/{type}/{id}`
Aktualizace entity.

**Headers:**
- `If-Match: W/"<version>"` – Povinný! Optimistic locking

**Response:**
- **200 OK** – Úspěch, vrací aktualizovanou entitu
- **409 Conflict** – Version mismatch, vrací aktuální stav serveru

#### **DELETE** `/api/entities/{type}/{id}`
Smazání entity.

## ETag & Optimistic Locking

### Princip

1. **GET** `/api/entities/UserProfile/123` → `ETag: W/"5"`
2. Klient edituje data
3. **PUT** `/api/entities/UserProfile/123` s `If-Match: W/"5"`
   - ✅ Pokud `version=5` → UPDATE, `version++`
   - ❌ Pokud `version!=5` → **409 Conflict**

### Response při konfliktu (409)

```json
{
  "error": "version_mismatch",
  "message": "Entity was modified by another user",
  "currentVersion": 7,
  "serverEntity": { ... }
}
```

## Row Level Security (RLS)

### Automatická tenant izolace

Všechny dotazy jsou automaticky filtrovány podle `tenant_id`:

```sql
ALTER TABLE user_profile ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON user_profile
  USING (tenant_id = current_setting('app.tenant_id', true));
```

### Jak funguje

1. `TenantContextFilter` nastaví `app.tenant_id` v DB session po ověření JWT
2. Postgres RLS automaticky aplikuje WHERE klauzuli na všechny SELECT/UPDATE/DELETE
3. **CORE_ROLE_ADMIN** má bypass (vidí všechny tenanty)

## Edit Locks

### Endpoints

#### **POST** `/api/locks/{type}/{id}`
Získání nebo obnovení zámku.

```json
{
  "ttlSeconds": 300,
  "lockType": "soft"
}
```

**Response:**
- **200 OK** – Zámek získán/obnoven
- **409 Conflict** – Entita zamčena jiným uživatelem

#### **DELETE** `/api/locks/{type}/{id}`
Uvolnění zámku.

**Pravidla:**
- Pouze držitel může uvolnit
- **CORE_ROLE_ADMIN** může uvolnit libovolný zámek

#### **GET** `/api/locks/{type}/{id}`
Kontrola stavu zámku.

### Auto-expiry

Janitor (`@Scheduled`) každých **15 sekund** čistí expirované zámky:

```java
@Scheduled(fixedDelay = 15000)
public void cleanupExpiredLocks() { ... }
```

## UI Capabilities

### Endpoint

**GET** `/api/me/ui-capabilities`

**Response:**
```json
{
  "menu": ["user_profiles", "roles", "groups"],
  "features": ["user_profile_management", "user_profile_edit_own"]
}
```

### Generování z metamodelu

```yaml
navigation:
  menu:
    - id: user_profiles
      label: User Profiles
      requiredRole: CORE_ROLE_TENANT_ADMIN

features:
  - id: user_profile_management
    requiredRole: CORE_ROLE_TENANT_ADMIN
```

## Databázová migrace

### V3__metamodel_core.sql

```sql
-- Version sloupec pro existující tabulky
ALTER TABLE users_directory ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;

-- Edit locks tabulka
CREATE TABLE edit_locks (
  id BIGSERIAL PRIMARY KEY,
  tenant_id TEXT NOT NULL,
  entity_type TEXT NOT NULL,
  entity_id TEXT NOT NULL,
  user_id TEXT NOT NULL,
  lock_type TEXT NOT NULL CHECK (lock_type IN ('soft')),
  acquired_at TIMESTAMPTZ DEFAULT NOW(),
  expires_at TIMESTAMPTZ NOT NULL,
  UNIQUE (tenant_id, entity_type, entity_id)
);

-- RLS policies
ALTER TABLE user_profile ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON user_profile
  USING (tenant_id = current_setting('app.tenant_id', true));
```

## Limity Fáze 1

### ✅ Co JE podporováno

- RBAC + ABAC s anyOf/allOf/role/eq/ne/contains/in
- Dot-notace 1 úroveň (např. `entity.department`)
- Column-level projekce (whitelisting)
- Tenant isolation (RLS)
- Optimistic locking (ETag/If-Match)
- Soft locks s auto-expiry

### ❌ Co NENÍ (plánováno do 1.1–2)

- ❌ Stavy entit (workflow, lifecycle)
- ❌ SLA tracking
- ❌ WebSocket presence (kdo edituje)
- ❌ Fulltext search
- ❌ Dokumenty/přílohy
- ❌ Auditní log změn
- ❌ Multi-hop dot-notace (např. `entity.dept.manager.email`)

## Testování

### Unit testy

```bash
cd backend
./mvnw test -Dtest=PolicyEngineTest
```

### Integrační testy

```bash
./mvnw test -Dtest=MetamodelCrudIntegrationTest
```

Používá **Testcontainers** (Postgres + Keycloak token stub).

## Deployment

1. **Rebuild backend:**
   ```bash
   make rebuild-backend
   ```

2. **Migrate DB:**
   Flyway automaticky aplikuje V3__metamodel_core.sql

3. **Test endpoint:**
   ```bash
   curl -H "Authorization: Bearer $TOKEN" \
     https://api.admin.core-platform.local/api/me/ui-capabilities
   ```

## Keycloak

**Beze změny!** JWT obsahuje:
- `sub` – User ID
- `tenant_id` – Tenant key
- `roles` – Role uživatele

FE nadále používá `/api/me/ui-capabilities` pro menu/features.

## Další kroky (Fáze 1.1)

- [ ] Implementace WebSocket presence (kdo právě edituje)
- [ ] Audit log (kdo, kdy, co změnil)
- [ ] Fulltext search (Postgres FTS nebo Elasticsearch)
- [ ] Dokumenty/přílohy (S3/MinIO storage)
- [ ] SLA tracking (deadline, escalation)
- [ ] Workflow states (draft → review → approved)

---

**Version:** 1.0  
**Date:** 2025-01-08  
**Status:** 🚧 In Progress
