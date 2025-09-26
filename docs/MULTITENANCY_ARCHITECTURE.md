# 🏢 Multitenancy Architecture

## Přehled

Core Platform implementuje **multitenantovou architekturu** pro izolaci dat mezi různými organizacemi/tenanty. Každý tenant má vlastní izolovaný prostor pro uživatele a data.

## Architektura

### 1. Databázové schéma

```sql
-- Tabulka tenantů
CREATE TABLE tenants (
    id UUID PRIMARY KEY,
    key TEXT UNIQUE NOT NULL,     -- Identifikátor tenantu (např. "company-a")
    name TEXT NOT NULL,           -- Zobrazované jméno
    realm TEXT NOT NULL           -- Keycloak realm
);

-- Adresář uživatelů s multitenancy
CREATE TABLE users_directory (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenants(id),
    keycloak_user_id TEXT NULL,
    username TEXT NOT NULL,
    email TEXT NULL,
    -- ... další atributy
);
```

### 2. Hibernate Tenant Filter

Všechny entity dědící od `MultiTenantEntity` jsou automaticky filtrovány podle aktuálního tenantu:

```java
@MappedSuperclass
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class MultiTenantEntity {
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
}
```

### 3. Tenant Context

ThreadLocal kontext pro aktuální tenant:

```java
// Nastavení tenantu pro aktuální thread
TenantContext.setTenantKey("company-a");

// Získání aktuálního tenantu
String tenant = TenantContext.getTenantKey();

// Vyčištění kontextu
TenantContext.clear();
```

## Komponenty

### TenantResolver
- Zjišťuje tenant z JWT tokenu (claim `tenant`)
- Fallback na default tenant pokud claim chybí
- Cache pro rychlé mapování `tenantKey -> tenantId`

### TenantFilter  
- Servlet filter pro automatické nastavení tenant kontextu
- Spouští se po Spring Security filtru
- Nastavuje MDC pro logování

### TenantCache
- In-memory cache s TTL 5 minut
- Refresh-on-read strategie
- Thread-safe implementace

### HibernateTenantFilterConfig
- AOP aspect pro automatické zapínání Hibernate filtru
- Interceptuje všechny repository operace
- Nastavuje `tenantId` parametr pro filtr

## API Endpointy

### Tenant Management
```
GET /api/tenants/me          - Informace o aktuálním tenantu
```

### User Directory
```
GET /api/users/me           - Aktuální uživatel z directory
GET /api/users/search?q=    - Vyhledávání uživatelů v tenantu
```

## Konfigurace

### Application Properties
```properties
# Default tenant pokud JWT neobsahuje claim
tenancy.default-tenant-key=test-tenant

# Název JWT claimu pro tenant
auth.jwt.tenant-claim=tenant
```

### Docker Environment
```yaml
environment:
  - TENANCY_DEFAULT_TENANT_KEY=test-tenant
  - AUTH_JWT_TENANT_CLAIM=tenant
```

## JWT Token Structure

Pro funkční multitenancy musí JWT tokeny obsahovat tenant claim:

```json
{
  "sub": "user-123",
  "preferred_username": "john.doe",
  "tenant": "company-a",
  "realm_access": {
    "roles": ["CORE_ROLE_USER"]
  }
}
```

## Logování

Všechny logy obsahují tenant informace:
- **MDC**: `tenant` pole v JSON logu
- **Loki**: `tenant` label pro filtrování
- **Pattern**: `[tenant:company-a]` v log message

## Testování

### Unit Testy
```java
@Test
void shouldResolveTenantFromJwt() {
    // Given
    TenantContext.setTenantKey("test-tenant");
    
    // When
    List<UserDirectoryEntity> users = userService.search("");
    
    // Then - pouze uživatelé z test-tenant
    assertThat(users).allMatch(u -> 
        u.getTenantId().equals(testTenantId)
    );
}
```

### Integration Testy
- Test filtrace mezi tenanty
- Test automatického nastavování `tenantId`
- Test izolace dat

## Bezpečnost

### Tenant Isolation
- ✅ Databázová filtrace na úrovni Hibernate
- ✅ Automatické nastavení tenant kontextu
- ✅ Validace existence tenantu
- ✅ Logování s tenant kontextem

### JWT Claims
- ✅ Tenant claim validace
- ✅ Fallback na default tenant
- ✅ Cache pro performance

## Monitoring

### Grafana Queries
```logql
# Logy pro konkrétní tenant
{service="backend",tenant="company-a"}

# Chyby v tenant řešení
{service="backend"} |= "TenantNotFound"

# Statistiky podle tenantů
sum by (tenant) (count_over_time({service="backend"}[1h]))
```

### Metriky
- Počet aktivních tenantů
- Cache hit/miss ratio
- Tenant resolution latency

## Migrace

### Existující Data
1. Spustit migraci `V2__init_multitenancy_and_user_directory.sql`
2. Vytvořit default tenant "test-tenant"
3. Migrace existujících dat do user directory

### Nové Entity
Všechny nové entity pro data specifická tenantu:

```java
@Entity
public class MyEntity extends MultiTenantEntity {
    // Automaticky získá tenant_id sloupec a filtraci
}
```

## Best Practices

### Development
1. Vždy testovat s více tenanty
2. Ověřovat tenant kontext v unit testech
3. Používat `@Transactional` pro konzistenci

### Production
1. Monitoring tenant isolation
2. Pravidelná kontrola cache performance
3. Audit tenant access patterns

### Security
1. Nikdy neobcházet tenant filter
2. Validovat tenant claims v JWT
3. Logovat všechny tenant operace