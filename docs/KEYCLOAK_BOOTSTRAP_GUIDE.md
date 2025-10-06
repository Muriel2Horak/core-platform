# 🏗️ Keycloak Bootstrap & Tenant Management

Tento dokument popisuje automatizované nastavení Keycloak tenant realmů s Postgres trigger synchronizací.

## 🚀 Quick Start

### 1️⃣ Automatické Vytvoření Tenantu přes API

```bash
# Vytvoř nový tenant (automaticky vytvoří realm i DB záznam)
curl -X POST https://core-platform.local/api/admin/tenants \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"key": "acme-corp", "displayName": "ACME Corporation"}'
```

### 2️⃣ Přidání Domény pro Nový Tenant

```bash
# Přidat doménu pro tenant
make add-tenant-domain TENANT=acme-corp

# Pak se můžeš přihlásit na
# https://acme-corp.core-platform.local
```

## 🔧 Tenant Management Příkazy

### Realm Management
```bash
make kc-list-realms           # Seznam všech realmů
make kc-export-realm          # Export realm konfigurace
make kc-backup                # Zálohování Keycloak dat
```

### User Management  
```bash
make kc-show-users            # Seznam uživatelů v realmu
make kc-shell                 # Interaktivní shell pro kcadm.sh
```

### Diagnostika
```bash
make kc-logs                  # Zobraz Keycloak logy
make kc-health                # Health check Keycloak služby
```

## 📋 Tenant Creation Proces

Vytvoření tenantu přes API (`POST /api/admin/tenants`) provádí:

1. **✅ Validace tenant klíče** (formát, unikátnost)
2. **🏢 Vytvoření DB záznamu** v tabulce tenants
3. **🔑 Vytvoření Keycloak realmu** z šablony
4. **👑 Vytvoření admin uživatele** pro tenant správu
5. **🛡️ Nastavení rolí** (CORE_ROLE_ADMIN, CORE_USER_MANAGER, CORE_ROLE_USER)
6. **🔐 Konfigurace klientů** (web, api)

### Vytvořené Role
- `CORE_ROLE_USER` - Základní uživatelská role
- `CORE_USER_MANAGER` - Správa uživatelů v tenantu
- `CORE_ROLE_ADMIN` - Plná správa tenant organizace

## 🔄 Postgres Trigger Synchronizace

### Automatická Synchronizace
Místo webhook SPI používáme **Postgres Database Triggers** pro real-time synchronizaci:

- **Trigger při INSERT/UPDATE/DELETE** v Keycloak DB
- **Event queue** (`user_change_events` tabulka)
- **Background polling** v backendu každých 10s
- **Automatické čištění** zpracovaných eventů

### Podporované Event Typy
- `USER_CREATED` - Nový uživatel vytvořen
- `USER_UPDATED` - Uživatel aktualizován
- `USER_DELETED` - Uživatel smazán

Viz [POSTGRES_TRIGGER_SYNC_GUIDE.md](./POSTGRES_TRIGGER_SYNC_GUIDE.md) pro detaily.

## 🏢 Multitenancy Architektura

### Realm Struktura
```
admin (master realm pro správu)
├── test-tenant (vývojový realm)
└── tenant-realms (produkční tenant realmy)
    ├── acme-corp
    ├── company-b  
    └── company-c
```

### Subdomain Routing
```
https://admin.core-platform.local      → admin realm
https://acme-corp.core-platform.local  → acme-corp realm
https://company-b.core-platform.local  → company-b realm
```

## 🔒 Bezpečnostní Poznámky

### Development
- Admin hesla: Dočasná, nutno změnit při prvním přihlášení
- JWT validace: Automatická pro všechny tenant realmy

### Production
- Používej silná hesla: `openssl rand -base64 32`
- Různé credentials pro každé prostředí
- Nikdy necommituj skutečné .env do repository

## 🐛 Troubleshooting

### Tenant Creation Selhává
```bash
# Zkontroluj backend logy
docker compose logs backend

# Zkontroluj Keycloak health
make kc-health

# Zkontroluj Keycloak logy
make kc-logs
```

### Uživatelé se Nesynchronizují
```bash
# Zkontroluj event queue
docker exec -it core-db psql -U core -d core -c \
  "SELECT * FROM user_change_events WHERE processed = false ORDER BY created_at DESC LIMIT 10;"

# Zkontroluj backend sync logy
docker compose logs backend | grep "KeycloakEventProjectionService"

# Manuálně spusť test sync
make test-trigger-sync
```

### Permission Denied
```bash
# Zkontroluj JWT token a tenant claim
curl -X GET https://acme-corp.core-platform.local/api/tenants/me \
  -H "Authorization: Bearer $TOKEN"

# Zkontroluj Docker volumes
docker compose down -v
make up
```

## 🔄 Update Process

Při změnách tenant konfigurace:

1. **Update realm template** v `backend/src/main/resources/keycloak/realm-tenant-template.json`
2. **Restart služeb** `make restart`
3. **Test vytvoření nového tenantu**
4. **Ověř synchronizaci** uživatelů

---

## 📖 Další Dokumentace

- [MULTITENANCY_ARCHITECTURE.md](./MULTITENANCY_ARCHITECTURE.md) - Architektura multitenancy
- [POSTGRES_TRIGGER_SYNC_GUIDE.md](./POSTGRES_TRIGGER_SYNC_GUIDE.md) - Trigger synchronizace
- [DYNAMIC_JWT_MULTITENANCY.md](./DYNAMIC_JWT_MULTITENANCY.md) - JWT validace
- [keycloak-ssl-setup.md](./keycloak-ssl-setup.md) - SSL konfigurace