# 🚨 CDC FIX - Root Cause Analysis

**Datum:** 2024-10-13  
**Status:** 🔴 **KRITICKÉ PROBLÉMY NALEZENY**

---

## 🔍 Root Cause

### Problém #1: CDC Triggery Neexistují ❌
```sql
-- Očekáváno:
user_entity_change_event_trigger ON user_entity
realm_change_event_trigger ON realm

-- Realita:
(0 rows)
```

**Důvod:** V2__init_keycloak_cdc.sql **NEOBSAHUJE TRIGGERY**, pouze tabulku!

```sql
-- V2__init_keycloak_cdc.sql obsahuje:
CREATE TABLE change_events (...);  ✅
CREATE TRIGGER ...;                ❌ CHYBÍ!
```

---

### Problém #2: Kafka Connect Neexistuje ❌
```bash
docker ps | grep connect
# (žádný výstup)
```

**Důvod:** `docker-compose.yml` **NEOBSAHUJE kafka-connect service**

```yaml
# docker/docker-compose.yml obsahuje:
kafka: ...        ✅
kafka-ui: ...     ✅
kafka-connect: ... ❌ CHYBÍ!
```

---

### Problém #3: Backend Consumer Neví Co Konzumovat ❌

Bez Kafka Connect není topic `keycloak.cdc.change_events` → Backend nemá co konzumovat

---

## 🛠️ Kompletní Fix Strategy

### Přístup A: Polling-Based CDC (Jednodušší) ⭐ DOPORUČENO

**Koncept:** Backend přímo polluje `change_events` tabulku, bez Kafka Connect

```
Keycloak DB → Trigger → change_events
                ↓
Backend → @Scheduled Poll → Process Events
```

**Výhody:**
- ✅ Méně dependencies (bez Kafka Connect)
- ✅ Jednodušší setup
- ✅ Menší latence
- ✅ Žádná Kafka topic noise

**Nevýhody:**
- ⚠️ Polling overhead (řešení: long polling)
- ⚠️ Single-consumer (horizontální scaling složitější)

---

### Přístup B: Kafka-Based CDC (Komplexnější)

**Koncept:** Kafka Connect čte `change_events` a publikuje do Kafky

```
Keycloak DB → Trigger → change_events
                ↓
Kafka Connect → Debezium → Kafka Topic
                              ↓
Backend → @KafkaListener → Process Events
```

**Výhody:**
- ✅ Scalable (multiple consumers)
- ✅ Event replay možnost
- ✅ Decoupling

**Nevýhody:**
- ⚠️ Více dependencies (Kafka Connect + Debezium)
- ⚠️ Komplexnější setup
- ⚠️ Vyšší latence

---

## ✅ Doporučené Řešení: Polling-Based CDC

### Krok 1: Vytvořit CDC Triggery

**Nový soubor:** `backend/src/main/resources/db/migration/V2__init_keycloak_cdc_triggers.sql`

```sql
-- =====================================================
-- KEYCLOAK CDC TRIGGERS
-- Tyto triggery MUSÍ být aplikovány na Keycloak DB
-- =====================================================

-- =====================================================
-- 1) TRIGGER FUNCTION - Insert do change_events
-- =====================================================

CREATE OR REPLACE FUNCTION keycloak_cdc_notify()
RETURNS TRIGGER AS $$
DECLARE
    event_type TEXT;
    realm_val TEXT;
BEGIN
    -- Determine event type
    IF TG_OP = 'INSERT' THEN
        event_type := TG_TABLE_NAME || '_CREATED';
        realm_val := NEW.realm_id;
    ELSIF TG_OP = 'UPDATE' THEN
        event_type := TG_TABLE_NAME || '_UPDATED';
        realm_val := NEW.realm_id;
    ELSIF TG_OP = 'DELETE' THEN
        event_type := TG_TABLE_NAME || '_DELETED';
        realm_val := OLD.realm_id;
    END IF;

    -- Insert change event
    INSERT INTO change_events (event_type, entity_id, realm_id)
    VALUES (
        event_type,
        COALESCE(NEW.id, OLD.id),
        realm_val
    );

    RETURN NULL; -- AFTER trigger
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 2) USER_ENTITY TRIGGERS
-- =====================================================

DROP TRIGGER IF EXISTS user_entity_cdc_trigger ON user_entity;

CREATE TRIGGER user_entity_cdc_trigger
AFTER INSERT OR UPDATE OR DELETE ON user_entity
FOR EACH ROW
EXECUTE FUNCTION keycloak_cdc_notify();

-- =====================================================
-- 3) REALM TRIGGERS (optional - pro realm changes)
-- =====================================================

DROP TRIGGER IF EXISTS realm_cdc_trigger ON realm;

CREATE TRIGGER realm_cdc_trigger
AFTER INSERT OR UPDATE OR DELETE ON realm
FOR EACH ROW
EXECUTE FUNCTION keycloak_cdc_notify();

-- =====================================================
-- VERIFICATION
-- =====================================================

DO $$
BEGIN
    RAISE NOTICE '✅ Keycloak CDC triggers installed';
    RAISE NOTICE '📊 Triggers: user_entity_cdc_trigger, realm_cdc_trigger';
END $$;
```

**DŮLEŽITÉ:** Tato migrace MUSÍ běžet na **Keycloak DB**, ne Core DB!

---

### Krok 2: Polling Service v Backendu

**Nový soubor:** `backend/src/main/java/cz/muriel/core/cdc/KeycloakCDCPollingService.java`

```java
package cz.muriel.core.cdc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * 🔄 KEYCLOAK CDC POLLING SERVICE
 * 
 * Polluje change_events tabulku v Keycloak DB a synchronizuje změny
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KeycloakCDCPollingService {

  @Value("${keycloak.cdc.batch-size:100}")
  private int batchSize;

  @Value("${keycloak.cdc.enabled:true}")
  private boolean cdcEnabled;

  private final DataSource keycloakDataSource; // Inject Keycloak DS
  private final UserSyncService userSyncService;

  /**
   * 🔄 POLL CHANGE EVENTS
   * 
   * Runs every 5 seconds (configurable)
   */
  @Scheduled(fixedDelayString = "${keycloak.cdc.poll-interval-ms:5000}")
  @Transactional("keycloakTransactionManager")
  public void pollChangeEvents() {
    if (!cdcEnabled) {
      return;
    }

    try {
      JdbcTemplate jdbc = new JdbcTemplate(keycloakDataSource);

      // Fetch unprocessed events
      String sql = """
          SELECT id, event_type, entity_id, realm_id, created_at
          FROM change_events
          WHERE processed = FALSE
          ORDER BY created_at ASC
          LIMIT ?
          """;

      List<Map<String, Object>> events = jdbc.queryForList(sql, batchSize);

      if (events.isEmpty()) {
        return; // No events to process
      }

      log.debug("📥 Polling {} CDC events from Keycloak", events.size());

      // Process each event
      for (Map<String, Object> event : events) {
        Long eventId = (Long) event.get("id");
        String eventType = (String) event.get("event_type");
        String entityId = (String) event.get("entity_id");
        String realmId = (String) event.get("realm_id");

        try {
          processEvent(eventType, entityId, realmId);

          // Mark as processed
          jdbc.update("UPDATE change_events SET processed = TRUE, processed_at = NOW() WHERE id = ?", eventId);

          log.debug("✅ Processed CDC event: {} / {}", eventType, entityId);

        } catch (Exception e) {
          log.error("❌ Failed to process CDC event: {} / {}", eventType, entityId, e);
          // Don't mark as processed - will retry
        }
      }

    } catch (Exception e) {
      log.error("❌ CDC polling failed", e);
    }
  }

  /**
   * 🔀 PROCESS EVENT
   */
  private void processEvent(String eventType, String entityId, String realmId) {
    if (eventType.startsWith("user_entity_")) {
      // User event
      if (eventType.endsWith("_CREATED") || eventType.endsWith("_UPDATED")) {
        userSyncService.syncUserFromKeycloak(entityId, realmId);
      } else if (eventType.endsWith("_DELETED")) {
        userSyncService.deleteUserFromDirectory(entityId);
      }
    }
    // TODO: Handle realm events if needed
  }
}
```

---

### Krok 3: Keycloak DataSource Configuration

**Soubor:** `backend/src/main/java/cz/muriel/core/config/KeycloakDataSourceConfig.java`

```java
package cz.muriel.core.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

/**
 * 🔧 KEYCLOAK DATASOURCE CONFIGURATION
 * 
 * Separate datasource for Keycloak CDC polling
 */
@Slf4j
@Configuration
public class KeycloakDataSourceConfig {

  @Bean
  @ConfigurationProperties("keycloak.datasource")
  public DataSourceProperties keycloakDataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean(name = "keycloakDataSource")
  public DataSource keycloakDataSource() {
    log.info("🔧 Initializing Keycloak DataSource for CDC polling");
    return keycloakDataSourceProperties()
        .initializeDataSourceBuilder()
        .type(HikariDataSource.class)
        .build();
  }

  @Bean(name = "keycloakTransactionManager")
  public PlatformTransactionManager keycloakTransactionManager(
      @Qualifier("keycloakDataSource") DataSource dataSource) {
    return new DataSourceTransactionManager(dataSource);
  }
}
```

---

### Krok 4: UserSyncService

**Soubor:** `backend/src/main/java/cz/muriel/core/cdc/UserSyncService.java`

```java
package cz.muriel.core.cdc;

import cz.muriel.core.auth.KeycloakAdminService;
import cz.muriel.core.entity.UserDirectory;
import cz.muriel.core.repository.UserDirectoryRepository;
import cz.muriel.core.service.TenantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * 🔄 USER SYNC SERVICE
 * 
 * Synchronizes users from Keycloak to user_directory
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSyncService {

  private final KeycloakAdminService keycloakAdminService;
  private final UserDirectoryRepository userDirectoryRepository;
  private final TenantService tenantService;

  /**
   * 🔄 SYNC USER FROM KEYCLOAK
   */
  @Transactional
  public void syncUserFromKeycloak(String keycloakUserId, String realmId) {
    try {
      log.debug("🔄 Syncing user: {} from realm: {}", keycloakUserId, realmId);

      // Fetch user from Keycloak
      Map<String, Object> keycloakUser = keycloakAdminService.getUserById(realmId, keycloakUserId);

      if (keycloakUser == null) {
        log.warn("⚠️ User not found in Keycloak: {}", keycloakUserId);
        return;
      }

      // Extract user data
      String username = (String) keycloakUser.get("username");
      String email = (String) keycloakUser.get("email");
      String firstName = (String) keycloakUser.get("firstName");
      String lastName = (String) keycloakUser.get("lastName");

      // Determine tenant from realm
      String tenantKey = tenantService.getTenantKeyFromRealmId(realmId);

      // Upsert to user_directory
      Optional<UserDirectory> existing = userDirectoryRepository.findByKeycloakUserId(keycloakUserId);

      UserDirectory user;
      if (existing.isPresent()) {
        user = existing.get();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setTenantId(tenantKey);
      } else {
        user = UserDirectory.builder()
            .keycloakUserId(keycloakUserId)
            .username(username)
            .email(email)
            .firstName(firstName)
            .lastName(lastName)
            .tenantId(tenantKey)
            .build();
      }

      user.setSyncedAt(Instant.now());
      userDirectoryRepository.save(user);

      log.info("✅ User synced to directory: {} (tenant: {})", username, tenantKey);

    } catch (Exception e) {
      log.error("❌ Failed to sync user: {}", keycloakUserId, e);
      throw new RuntimeException("User sync failed", e);
    }
  }

  /**
   * 🗑️ DELETE USER FROM DIRECTORY
   */
  @Transactional
  public void deleteUserFromDirectory(String keycloakUserId) {
    try {
      Optional<UserDirectory> user = userDirectoryRepository.findByKeycloakUserId(keycloakUserId);

      if (user.isPresent()) {
        userDirectoryRepository.delete(user.get());
        log.info("✅ User deleted from directory: {}", keycloakUserId);
      } else {
        log.debug("User not found in directory: {}", keycloakUserId);
      }

    } catch (Exception e) {
      log.error("❌ Failed to delete user: {}", keycloakUserId, e);
      throw new RuntimeException("User delete failed", e);
    }
  }
}
```

---

### Krok 5: Configuration Properties

**application.properties:**
```properties
# ====== KEYCLOAK CDC POLLING ======
keycloak.cdc.enabled=true
keycloak.cdc.poll-interval-ms=5000
keycloak.cdc.batch-size=100

# Keycloak DataSource (already exists)
keycloak.datasource.url=jdbc:postgresql://core-db:5432/keycloak
keycloak.datasource.username=${KEYCLOAK_DB_USERNAME:keycloak}
keycloak.datasource.password=${KEYCLOAK_DB_PASSWORD:keycloak}
keycloak.datasource.driver-class-name=org.postgresql.Driver
```

---

## 📋 Deployment Checklist

### 1. Apply Trigger Migration to Keycloak DB
```bash
# Option A: Direct SQL
docker exec -i core-db psql -U keycloak -d keycloak < backend/src/main/resources/db/migration/V2__init_keycloak_cdc_triggers.sql

# Option B: Via psql interactive
docker exec -it core-db psql -U keycloak -d keycloak
\i /path/to/V2__init_keycloak_cdc_triggers.sql
```

### 2. Verify Triggers
```bash
docker exec -it core-db psql -U keycloak -d keycloak -c "
  SELECT tgname, tgrelid::regclass, tgenabled 
  FROM pg_trigger 
  WHERE tgname LIKE '%cdc%'
"
```

**Očekáváno:**
```
          tgname          |   tgrelid   | tgenabled
--------------------------+-------------+-----------
 user_entity_cdc_trigger  | user_entity | O
 realm_cdc_trigger        | realm       | O
```

### 3. Rebuild Backend
```bash
cd /Users/martinhorak/Projects/core-platform
docker compose build backend
docker compose up -d backend
```

### 4. Test CDC Flow
```bash
# 1. Create test user in Keycloak
# (via admin console or API)

# 2. Check change_events
docker exec -it core-db psql -U keycloak -d keycloak -c "
  SELECT * FROM change_events ORDER BY created_at DESC LIMIT 5
"

# 3. Wait 5 seconds (polling interval)

# 4. Check backend logs
docker logs backend | grep -i "cdc\|sync"

# 5. Check user_directory
docker exec -it core-db psql -U core -d core -c "
  SELECT * FROM user_directory ORDER BY synced_at DESC LIMIT 5
"
```

---

## 🎯 Expected Results

### Before Fix:
```
change_events: ✅ Exists
Triggers: ❌ Missing (0 rows)
Backend polling: ❌ Not implemented
user_directory: ❌ Empty
```

### After Fix:
```
change_events: ✅ Exists
Triggers: ✅ Installed (2 triggers)
Backend polling: ✅ Running every 5s
user_directory: ✅ Synced from Keycloak
```

---

## 📊 Summary

| Component | Before | After | Status |
|-----------|--------|-------|--------|
| change_events table | ✅ | ✅ | OK |
| CDC Triggers | ❌ | ✅ | **FIXED** |
| Kafka Connect | ❌ | ⏭️ | **SKIPPED** |
| Backend Polling | ❌ | ✅ | **IMPLEMENTED** |
| User Sync | ❌ | ✅ | **WORKS** |

---

**Status:** ✅ Ready to Implement  
**Next Step:** Vytvořit soubory a aplikovat fix
