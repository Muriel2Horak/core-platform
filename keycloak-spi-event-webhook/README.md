# Keycloak Event Webhook SPI

Custom Keycloak SPI pro odesílání webhook událostí o uživatelích do externího backendu s HMAC-SHA256 zabezpečením.

## 🏗️ Funkce

- **Event Mapping**: 
  - `EventType.REGISTER` → `USER_CREATED`
  - `EventType.UPDATE_PROFILE` → `USER_UPDATED`  
  - `AdminEvent USER CREATE/UPDATE/DELETE` → `USER_CREATED/USER_UPDATED/USER_DELETED`

- **HMAC Security**: HMAC-SHA256 podpis v `X-KC-Signature: sha256=<hex>` header
- **Multitenantní**: Mapování realm → tenant přes konfiguraci
- **Retry Logic**: 3 pokusy (250ms, 500ms, 1000ms) pro 5xx/timeout chyby
- **Filtrování**: Konfigurování povolených event typů

## 📦 Instalace

1. **Build JAR**:
```bash
mvn clean package
```

2. **Deploy do Keycloak**:
```bash
cp target/keycloak-spi-event-webhook-1.0.0.jar /opt/keycloak/providers/
```

3. **Rebuild Keycloak**:
```bash
/opt/keycloak/bin/kc.sh build
```

## ⚙️ Konfigurace

### Environment Variables
```bash
KC_EVENTS_LISTENERS=jboss-logging,muriel-webhook
KC_SPI_EVENTS_LISTENER_MURIEL_WEBHOOK_ENDPOINT_URL=http://backend:8080/internal/keycloak/events
KC_SPI_EVENTS_LISTENER_MURIEL_WEBHOOK_SECRET=dev-secret
KC_SPI_EVENTS_LISTENER_MURIEL_WEBHOOK_REALM_TENANT_MAP=core-platform:test-tenant:a887f848-42cf-4b10-aff8-eaa8c488f3b1
KC_SPI_EVENTS_LISTENER_MURIEL_WEBHOOK_ENABLED_TYPES=USER_CREATED,USER_UPDATED,USER_DELETED
```

### Docker Compose
```yaml
environment:
  - KC_EVENTS_LISTENERS=jboss-logging,muriel-webhook
  - KC_SPI_EVENTS_LISTENER_MURIEL_WEBHOOK_ENDPOINT_URL=http://backend:8080/internal/keycloak/events
  - KC_SPI_EVENTS_LISTENER_MURIEL_WEBHOOK_SECRET=${KC_EVENT_WEBHOOK_SECRET}
  - KC_SPI_EVENTS_LISTENER_MURIEL_WEBHOOK_REALM_TENANT_MAP=core-platform:test-tenant:${TENANT_ID}
```

## 📡 Webhook Payload

```json
{
  "type": "USER_CREATED",
  "realm": "core-platform", 
  "tenantKey": "test-tenant",
  "tenantId": "a887f848-42cf-4b10-aff8-eaa8c488f3b1",
  "userId": "user-uuid-123",
  "username": "john.doe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe"
}
```

## 🔐 HTTP Headers

```
Content-Type: application/json
X-KC-Signature: sha256=abc123...def789
X-Realm: core-platform
X-Tenant-Key: test-tenant  
X-Tenant-Id: a887f848-42cf-4b10-aff8-eaa8c488f3b1
```

## 🔧 Konfigurace Parametrů

| Parametr | Environment Variable | Popis |
|----------|---------------------|-------|
| `endpoint-url` | `KC_SPI_EVENTS_LISTENER_MURIEL_WEBHOOK_ENDPOINT_URL` | URL webhook endpointu |
| `secret` | `KC_SPI_EVENTS_LISTENER_MURIEL_WEBHOOK_SECRET` | HMAC secret |
| `realm-tenant-map` | `KC_SPI_EVENTS_LISTENER_MURIEL_WEBHOOK_REALM_TENANT_MAP` | Formát: `realm:tenantKey:tenantId,realm2:key2:id2` |
| `enabled-types` | `KC_SPI_EVENTS_LISTENER_MURIEL_WEBHOOK_ENABLED_TYPES` | Seznam povolených typů (default: všechny) |

## 🧪 Testing

```bash
# Spuštění testů
mvn test

# Test specifické třídy  
mvn test -Dtest=EventWebhookProviderFactoryTest
```

## 📋 Chování

- ✅ **Úspěšné odeslání**: Status 2xx → INFO log
- ⚠️ **Chyba 4xx**: Žádný retry → WARN log  
- 🔄 **Chyba 5xx/timeout**: 3 pokusy s exponential backoff
- 🚫 **Chybějící tenant mapping**: Událost se neodešle → WARN log
- 🗑️ **DELETE události**: Posílá se jen `userId`, ostatní pole jsou `null`

## 🏷️ Provider ID

Registrovaný provider ID: **`muriel-webhook`**

## 📈 Logování

- **INFO**: Úspěšné události, odesílání
- **WARN**: Chybějící konfigurace, HTTP chyby, neplatné formáty
- **DEBUG**: Detaily payload, URL, HMAC signature (první 8 znaků)

## 🐳 Docker integrace

SPI je zabalené do custom Keycloak image (`docker/keycloak/Dockerfile`). Build a start:

```bash
make kc-image
make kc-up
```

Logy:
```bash
make kc-logs
```

Konfigurace přes `.env`: `KC_WEBHOOK_ENDPOINT_URL`, `KC_WEBHOOK_SECRET`, `KC_REALM_TENANT_MAP`, `KC_ENABLED_TYPES`, `KC_LOG_LEVEL`. 

Listener je aktivován přes `KC_EVENTS_LISTENERS=jboss-logging,muriel-webhook`. 

Po startu hledej v logu `"Event listener 'muriel-webhook' initialized"`. Při změně uživatele v příslušném realm Keycloak volá backend na `${KC_WEBHOOK_ENDPOINT_URL}` s HMAC hlavičkou.