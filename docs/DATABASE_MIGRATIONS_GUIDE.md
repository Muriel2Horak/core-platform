# 🗃️ Databázové Migrace - Návod k Použití

## 📋 **Kdy se spouští migrační skripty:**

### ✅ **Automatické spuštění při startu backendu**
```properties
# application.properties
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true  # Klíčové nastavení!
```

## 🎯 **Scénáře nasazení:**

### 1. **NOVÉ PROSTŘEDÍ** (prázdná databáze)
```bash
# Flyway automaticky spustí všechny migrace v pořadí:
# V2__init_multitenancy_and_user_directory.sql  ← Vytvoří novou strukturu s tenant_key
# V3__kc_event_webhook_and_projection.sql       ← Přidá Keycloak sync
# V4__migrate_tenant_id_to_tenant_key.sql       ← Přeskočí (není potřeba)

docker-compose up backend
```

**Výsledek:** ✅ Čistá databáze s novou `tenant_key` strukturou

### 2. **EXISTUJÍCÍ PROSTŘEDÍ** (s UUID tenant_id)
```bash
# V2 detekuje starou strukturu a VYHODÍ CHYBU:
# "Found existing users_directory table with UUID tenant_id"
# Musíte provést manuální migraci dat:

# Krok 1: Zastavit backend
docker-compose stop backend

# Krok 2: Ručně spustit datovou migraci V4
docker-compose exec db psql -U core -d core -f /path/to/V4__migrate_tenant_id_to_tenant_key.sql

# Krok 3: Spustit backend (V2 detekuje novou strukturu a přeskočí)
docker-compose up backend
```

**Výsledek:** ✅ Existující data migrovány na `tenant_key` strukturu

### 3. **PROSTŘEDÍ S NOVOU STRUKTUROU** (už má tenant_key)
```bash
# V2 detekuje tenant_key a přeskočí s hláškou:
# "V2 Migration skipped: users_directory already has new tenant_key structure"

docker-compose up backend
```

**Výsledek:** ✅ Žádné změny, pokračuje normálně

## 🔍 **Kontrolní příkazy:**

### Zkontrolovat aktuální strukturu databáze:
```sql
-- Zkontrolovat sloupce v users_directory
SELECT column_name, data_type 
FROM information_schema.columns 
WHERE table_name = 'users_directory' 
AND column_name IN ('tenant_id', 'tenant_key');

-- Zkontrolovat aplikované migrace
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

### Zkontrolovat data po migraci:
```sql
-- Počet uživatelů podle tenant_key
SELECT tenant_key, COUNT(*) 
FROM users_directory 
GROUP BY tenant_key;

-- Ověřit cizí klíče
SELECT constraint_name, table_name, column_name, foreign_table_name, foreign_column_name
FROM information_schema.key_column_usage k
JOIN information_schema.referential_constraints r ON k.constraint_name = r.constraint_name
WHERE k.table_name = 'users_directory';
```

## ⚠️ **Bezpečnostní opatření:**

1. **Záloha před migrací:**
   ```bash
   # Vytvořit zálohu před spuštěním migrace
   docker-compose exec db pg_dump -U core -d core > backup_before_migration.sql
   ```

2. **Test na kopii dat:**
   ```bash
   # Doporučuje se vždy testovat migraci na kopii produkční databáze
   ```

3. **Rollback plán:**
   ```sql
   -- V případě problémů lze vrátit tenant_id_old zpět:
   ALTER TABLE users_directory RENAME COLUMN tenant_key TO tenant_key_backup;
   ALTER TABLE users_directory RENAME COLUMN tenant_id_old TO tenant_id;
   ```

## 🎉 **Výhody nové struktury:**

- **Lepší zálohování**: Export/import podle tenant_key bez problémů s UUID
- **Jednodušší migrace**: Vazby se nerozpadnou při přesunu dat
- **Čitelnější**: tenant_key je human-readable
- **Konzistentní**: Stejný klíč jako v Keycloak realmu