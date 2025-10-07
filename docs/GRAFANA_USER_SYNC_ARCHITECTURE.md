# 📊 Grafana User Auto-Sync Architecture

## 🎯 Cíl
Automatická synchronizace uživatelů s monitoring rolemi z Keycloak do Grafany pomocí existující CDC (Change Data Capture) infrastruktury.

## 🏗️ Architektura

```
Keycloak Role Assignment (UI/API)
         ↓
PostgreSQL Trigger (USER_ROLE_MAPPING table)
         ↓
change_events table (CDC queue)
         ↓
ChangeEventProcessor (polling každých 10s)
         ↓
GrafanaUserSyncService
         ↓
Grafana Admin API (create/update/delete users)
```

## 📋 Keycloak Role Mapping

### Monitorovací Role:

| Keycloak Role               | Grafana Role | Popis                          |
|-----------------------------|--------------|--------------------------------|
| `CORE_ROLE_ADMIN`          | **Admin**    | Plný přístup ke všem dashboardům + admin práva |
| `CORE_ROLE_MONITORING`     | **Editor**   | Může editovat dashboardy a panel  |
| `CORE_ROLE_TENANT_MONITORING` | **Viewer** | Pouze zobrazení tenant-specific dashboardů |

## 🔄 Workflow

### 1. Přiřazení Role v Keycloaku
```
Admin → Keycloak Admin Console → Users → test_admin → Role Mappings
      → Add Role: CORE_ROLE_MONITORING
```

### 2. PostgreSQL Trigger Fire
```sql
-- Trigger na user_role_mapping tabulce
INSERT INTO change_events (event_type, entity_id, realm_id)
VALUES ('USER_ROLE_ASSIGNED', 'user-uuid', 'admin');
```

### 3. CDC Processor (každých 10s)
```java
@Scheduled(fixedDelayString = "10000")
public void pollAndProcessEvents() {
    // Fetch unprocessed events
    // Call GrafanaUserSyncService.handleUserRoleChange(event)
}
```

### 4. Grafana User Sync
```java
public void handleUserRoleChange(Map<String, Object> event) {
    1. Načti user detaily z Keycloak Admin API
    2. Zkontroluj monitoring role
    3. IF (má CORE_ROLE_* monitoring roli):
         → Vytvoř/aktualizuj Grafana uživatele
       ELSE:
         → Smaž/deaktivuj Grafana uživatele
}
```

## 📝 Implementation Steps

### ✅ HOTOVO:
1. ✅ CDC infrastruktura existuje (`change_events` table)
2. ✅ PostgreSQL triggery na `user_role_mapping` tabulce
3. ✅ `ChangeEventProcessor` polling service
4. ✅ `GrafanaUserSyncService` skeleton vytvořen
5. ✅ Propojení CDC → Grafana sync

### 🚧 TODO (Pro dokončení):
1. **Keycloak Admin API integrace**:
   ```java
   // Potřebujeme injektovat Keycloak Admin Client
   @Bean
   public Keycloak keycloakAdmin() {
       return KeycloakBuilder.builder()
           .serverUrl("http://keycloak:8080")
           .realm("master")
           .username("admin")
           .password("admin")
           .clientId("admin-cli")
           .build();
   }
   ```

2. **Implementovat metody v GrafanaUserSyncService**:
   - `getUserFromKeycloak(userId, realmId)` → UserRepresentation
   - `getUserRoles(userId, realmId)` → Set<String>
   - `createOrUpdateGrafanaUser(user, roles)` → Grafana API
   - `deactivateGrafanaUser(username)` → Grafana API

3. **Grafana Admin API credentials**:
   ```yaml
   # application.yml
   grafana:
     url: http://grafana:3000
     admin:
       user: admin
       password: ${GRAFANA_ADMIN_PASSWORD:admin}
   ```

4. **Vytvořit Keycloak role**:
   - `CORE_ROLE_MONITORING` (global monitoring access)
   - `CORE_ROLE_TENANT_MONITORING` (tenant-specific)

5. **Testing**:
   - Přiřadit roli `CORE_ROLE_MONITORING` uživateli v Keycloaku
   - Zkontrolovat CDC event v `change_events`
   - Ověřit vytvoření Grafana uživatele

## 🔧 Konfigurační soubory

### docker-compose.yml (Grafana)
```yaml
grafana:
  environment:
    # JWT auth zůstává pro iframe embedding
    - GF_AUTH_JWT_ENABLED=true
    - GF_AUTH_JWT_AUTO_SIGN_UP=true
    
    # Admin API credentials pro backend
    - GF_SECURITY_ADMIN_USER=admin
    - GF_SECURITY_ADMIN_PASSWORD=admin
```

### Backend pom.xml (dependencies)
```xml
<!-- Keycloak Admin Client -->
<dependency>
    <groupId>org.keycloak</groupId>
    <artifactId>keycloak-admin-client</artifactId>
    <version>${keycloak.version}</version>
</dependency>
```

## 🎯 Výhody tohoto řešení

✅ **Centralizovaná správa** - vše v Keycloaku  
✅ **Automatická synchronizace** - žádná manuální práce  
✅ **Konzistentní s architekturou** - využívá existující CDC  
✅ **Škálovatelné** - funguje i pro multi-tenancy  
✅ **Bezpečné** - Grafana uživatelé řízeni přes Keycloak RBAC  
✅ **Audit trail** - všechny změny v `change_events`  

## 📊 Monitoring & Troubleshooting

### Debug CDC flow:
```sql
-- Zobraz nepřečtené eventy
SELECT * FROM change_events WHERE NOT processed ORDER BY id DESC LIMIT 10;

-- Zobraz role change eventy
SELECT * FROM change_events 
WHERE event_type IN ('USER_ROLE_ASSIGNED', 'USER_ROLE_REMOVED')
ORDER BY id DESC LIMIT 20;
```

### Check Grafana users:
```bash
# Via Admin API
curl -u admin:admin http://localhost:3000/api/users

# Via logs
docker logs core-grafana | grep -i "Created user"
```

### Backend logs:
```bash
docker logs core-backend | grep "GrafanaUserSyncService"
docker logs core-backend | grep "Processing role change"
```

## 🚀 Deployment Checklist

- [ ] Vytvořit Keycloak role: `CORE_ROLE_MONITORING`, `CORE_ROLE_TENANT_MONITORING`
- [ ] Přidat Keycloak Admin Client dependency do pom.xml
- [ ] Konfigurovat Keycloak Admin bean
- [ ] Dokončit GrafanaUserSyncService implementation
- [ ] Nastavit Grafana Admin credentials
- [ ] Testovat celý flow
- [ ] Dokumentovat pro team

---

**Autor**: AI Assistant  
**Datum**: 2025-10-07  
**Status**: 🚧 In Progress (skeleton ready, need Keycloak API integration)
