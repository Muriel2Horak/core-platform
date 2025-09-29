# 🏗️ Keycloak Bootstrap & Tenant Management

Tento dokument popisuje automatizované nastavení Keycloak tenant realmů s webhook integrací.

## 🚀 Quick Start

### 1️⃣ Manuální Bootstrap Tenant Realm

```bash
# Vytvoř nový tenant realm s webhook konfigurací
make kc-bootstrap-realm REALM=my-company WEBHOOK_URL=http://backend:8080/internal/keycloak/events WEBHOOK_SECRET=my-secret

# Nebo s předdefinovanými parametry z .env
export REALM=my-company
export WEBHOOK_SECRET=my-secret-123
make kc-bootstrap-realm
```

### 2️⃣ Automatický Bootstrap při Startu

```bash
# 1. Nastav environment proměnné v .env
echo "AUTO_BOOTSTRAP_REALM=my-company" >> .env
echo "AUTO_BOOTSTRAP_TENANT_ADMIN=company-admin" >> .env  
echo "AUTO_BOOTSTRAP_TENANT_ADMIN_PASSWORD=TempPass123!" >> .env

# 2. Spusť s bootstrap profilem
make dev-up-bootstrap

# Nebo manuálně
docker-compose --profile bootstrap up -d
```

## 🔧 Keycloak Management Příkazy

### Realm Management
```bash
make kc-bootstrap-realm        # Vytvoř nový tenant realm
make kc-list-realms           # Seznam všech realmů
make kc-export-realm          # Export core-platform realm
make kc-backup                # Zálohování Keycloak dat
```

### User Management  
```bash
make kc-show-users            # Seznam uživatelů v core-platform realm
make kc-shell                 # Interaktivní shell pro kcadm.sh
```

### Diagnostika
```bash
make kc-logs                  # Zobraz Keycloak logy
make kc-health                # Health check Keycloak služby
```

## 📋 Bootstrap Proces

Bootstrap skript `scripts/kc_bootstrap_realm.sh` provádí:

1. **✅ Ověření připojení** k Keycloak admin API
2. **🏢 Vytvoření tenant realm** na základě šablony
3. **🔗 Konfigurace webhook SPI** pro event synchronizaci
4. **👑 Vytvoření admin uživatele** pro tenant správu
5. **🛡️ Nastavení rolí a skupin** pro multitenancy
6. **🔐 Konfigurace klientů** (web, api, admin-service)

### Vytvořené Role
- `USER_DIRECTORY_VIEW` - Čtení uživatelského adresáře
- `USER_DIRECTORY_RW` - Čtení/zápis uživatelského adresáře  
- `USER_DIRECTORY_ADMIN` - Plná správa uživatelského adresáře

### Vytvořené Skupiny
- `tenant-admins` - Administrátoři tenant organizace

## 🔗 Webhook Konfigurace

### SPI Event Listener
Bootstrap automaticky konfiguruje webhook SPI pro synchronizaci událostí:

```bash
# Konfigurace v Keycloak
KC_SPI_EVENTS_LISTENER_MURIEL_WEBHOOK_ENDPOINT_URL=http://backend:8080/internal/keycloak/events
KC_SPI_EVENTS_LISTENER_MURIEL_WEBHOOK_SECRET=webhook-secret
KC_SPI_EVENTS_LISTENER_MURIEL_WEBHOOK_REALM_TENANT_MAP=my-company:my-tenant:1
```

### Podporované Event Typy
- `USER_CREATED` - Nový uživatel vytvořen
- `USER_UPDATED` - Uživatel aktualizován
- `USER_DELETED` - Uživatel smazán
- `USER_ENABLED` - Uživatel aktivován
- `USER_DISABLED` - Uživatel deaktivován

## 🏢 Multitenancy Architektura

### Realm Struktura
```
master (admin realm)
├── core-platform (vývojový realm)
└── tenant-realms (produkční tenant realmy)
    ├── company-a
    ├── company-b  
    └── company-c
```

### Tenant Mapping
```bash
# Format: realm:tenant-key:tenant-id
KC_REALM_TENANT_MAP="company-a:acme:1,company-b:globex:2"
```

## 🔒 Bezpečnostní Poznámky

### Development
- Webhook secret: `dev-webhook-secret-CHANGE-ME-IN-PRODUCTION`
- Admin hesla: Dočasná, nutno změnit při prvním přihlášení

### Production
- Generuj silné webhook sekrety: `openssl rand -base64 32`
- Používej různé sekrety pro každé prostředí
- Nikdy necommituj skutečné .env do repository
- Webhook secret musí být stejný v Keycloak i Backend

## 🐛 Troubleshooting

### Bootstrap Selhává
```bash
# Zkontroluj Keycloak health
make kc-health

# Zkontroluj logy
make kc-logs

# Manuálně otestuj admin API
make kc-shell
```

### Webhook Nefunguje
```bash
# Zkontroluj konfiguraci v realm
make kc-shell
get events/config -r YOUR_REALM

# Zkontroluj backend webhook endpoint
curl -X POST http://localhost:8080/internal/keycloak/events \
  -H "Content-Type: application/json" \
  -H "X-Webhook-Secret: your-secret" \
  -d '{"test": true}'
```

### Permission Denied
```bash
# Ujisti se, že skript má spustitelná práva
chmod +x scripts/kc_bootstrap_realm.sh

# Zkontroluj Docker volumes
docker-compose down -v
make dev-up
```

## 🔄 Update Process

Při změnách webhook konfigurace:

1. **Update .env** s novými parametry
2. **Restart Keycloak** `make kc-restart`
3. **Re-bootstrap realmy** pokud je potřeba
4. **Test webhook** integrace

---

## 📖 Další Dokumentace

- [MULTITENANCY_ARCHITECTURE.md](./MULTITENANCY_ARCHITECTURE.md) - Architektura multitenancy
- [SECURITY_MIGRATION_GUIDE.md](./SECURITY_MIGRATION_GUIDE.md) - Bezpečnostní migrace
- [keycloak-ssl-setup.md](./keycloak-ssl-setup.md) - SSL konfigurace