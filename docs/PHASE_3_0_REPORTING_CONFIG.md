# Fáze 3.0 - Příprava a konfigurace (HOTOVO ✅)

## Přehled

Fáze 3.0 připravila základní strukturu a konfiguraci pro reporting modul. Vytvořena byla základní architektura, konfigurační soubory a feature toggles.

## Implementované komponenty

### 1. Struktura modulů

Vytvořena struktura backend reporting modulu:

```
backend/src/main/java/cz/muriel/core/reporting/
├── api/          # REST API kontrolery
├── app/          # Konfigurace a aplikační logika
├── cube/         # Integrace s Cube.js
├── dsl/          # DSL pro dotazy
├── jobs/         # Bulk operace a joby
├── model/        # Datové modely
├── repo/         # Repository vrstva
├── security/     # Bezpečnostní komponenty
└── support/      # Pomocné třídy
```

### 2. Konfigurace (application-reporting.yml)

**Základní parametry:**
- `reporting.enabled=true` - Master toggle
- `reporting.maxRows=50000` - Maximální počet řádků na dotaz
- `reporting.maxIntervalDays=92` - Maximální časové okno
- `reporting.defaultTtlSeconds=60` - Výchozí TTL cache

**Cache konfigurace:**
- `reporting.cache.provider=redis` (fallback: caffeine)
- `reporting.cache.keyPrefix=rpt:`

**Rate limiting:**
- `reporting.rateLimit.perTenantPerMin=120`

**Cube.js integrace:**
- `reporting.cube.baseUrl=http://cube:4000`
- `reporting.cube.apiToken=${CUBE_API_TOKEN}`
- Timeouty: connect 5s, read 30s

**Bulk operace:**
- `reporting.bulk.chunkSize=1000`
- `reporting.bulk.maxAffectRows=500000`
- `reporting.bulk.queueConcurrency=2`
- `reporting.bulk.timeoutSeconds=300`

### 3. Java komponenty

#### ReportingProperties
- `@ConfigurationProperties(prefix = "reporting")`
- Validace pomocí Hibernate Validator
- Vnořené třídy pro logické skupiny konfigurace

#### ReportingConfiguration
- Redis cache manager (primární)
- Caffeine cache manager (fallback)
- RestClient pro Cube.js API s Bearer autentizací
- Conditional beans podle konfigurace

#### ReportingFeatureToggle
- `isReportingEnabled()` - kontrola master toggle
- `requireReportingEnabled()` - validace s výjimkou
- `isRedisCacheEnabled()` / `isCaffeineCacheEnabled()` - detekce cache provideru

### 4. Závislosti (pom.xml)

Přidány závislosti:
- `com.github.ben-manes.caffeine:caffeine` - Caffeine cache
- `spring-boot-starter-cache` - Spring Cache abstrakce
- `bucket4j-core` a `bucket4j-redis` (v8.10.1) - Rate limiting

## Testy

### Unit testy
- ✅ `ReportingPropertiesTest` - načítání konfigurace
- ✅ `ReportingFeatureToggleTest` - feature toggle logika

### Coverage
- Properties: 100%
- FeatureToggle: 100%

## Definition of Done ✅

- [x] Aplikace startuje s novou konfigurací
- [x] Toggly čitelné v `/actuator/env`
- [x] Žádná změna stávajícího chování (conditionalní beans)
- [x] Unit testy zelené
- [x] Dokumentace vytvořena
- [x] Redis i Caffeine konfigurace funkční
- [x] RestClient pro Cube.js připraven

## Použití

### Aktivace profilu
```yaml
spring:
  profiles:
    active: reporting
```

### Kontrola konfigurace
```bash
curl http://localhost:8080/actuator/env | jq '.propertySources[] | select(.name | contains("reporting"))'
```

### Feature toggle v kódu
```java
@Autowired
private ReportingFeatureToggle featureToggle;

public void someMethod() {
    if (featureToggle.isReportingEnabled()) {
        // reporting logic
    }
}
```

## Další kroky

Pokračovat na **Fázi 3.1 - DSL pro dotazy a guardrails**:
- JSON-DSL DTO pro čtení
- Validace (dimenze, metriky, časová okna)
- MetamodelSpecService
- QueryFingerprint pro cache klíče
