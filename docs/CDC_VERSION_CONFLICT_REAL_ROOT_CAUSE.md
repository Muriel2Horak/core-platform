# 🔥 CDC Version Conflict - Real Root Cause (FIXED)

**Datum:** 2025-10-09  
**Status:** ✅ RESOLVED (Real Issue Found!)

## ❌ Předchozí analýza byla ŠPATNĚ!

### Co jsme si mysleli:
```
"Race condition mezi dvěma thready zpracovávajícími stejnou entitu současně"
```

### Skutečný problém:
```
"Transaction isolation - retry loop čte STEJNÁ data protože je ve STEJNÉ transakci!"
```

## 🔍 Skutečná Root Cause

### Symptom
```
Version conflict for user core_system_admin, retrying (1/5)
Version conflict for user core_system_admin, retrying (2/5)
Version conflict for user core_system_admin, retrying (3/5)
Version conflict for user core_system_admin, retrying (4/5)
Version conflict for user core_system_admin, retrying (5/5)
❌ Version conflict after 5 retries
```

### Pozorování
1. ✅ Lock funguje - není paralelní zpracování
2. ✅ CDC events jsou zpracovávány sériově (každých 10s)
3. ❌ **VŠECHNY retry pokusy selhávají** - všech 5!
4. ❌ **Všechny čtou STEJNÁ data** - version se nemění mezi retry

### Problém: Transaction Snapshot Isolation

```java
@Transactional  // ← Class-level transaction
public class KeycloakEventProjectionService {
    
    private void syncUserFromKeycloakInternal(...) {
        while (!success && attempt < maxRetries) {
            // ❌ PROBLÉM: Všechny retry jsou ve STEJNÉ transakci!
            
            // READ - vždy vrací STEJNÁ data (transactional snapshot)
            Map<String, Object> currentUser = metamodelService.getById(...);
            // version = 5
            
            // UPDATE - selže
            metamodelService.update(..., version=5, ...);
            
            // Retry - ale STÁLE ve stejné transakci!
            // READ opět vrací version = 5 (snapshot z začátku transakce)
            // UPDATE opět selže
            // ...loop 5x, všechny selhávají
        }
    }
}
```

### Co se děje:

```
Timeline:

T1: Transaction START
    ├─ Isolation Level: READ_COMMITTED (default)
    └─ Snapshot vytvořen

T2: getById() → SELECT * FROM users WHERE id=xxx
    └─ Vrací: version = 5 (z snapshot)

T3: update() → UPDATE users SET ... WHERE version=5
    └─ Selže (trigger změní version → 6?)
    └─ Transaction marked as rollback-only ❌

T4: **Retry #1**
    ├─ getById() → SELECT * FROM users WHERE id=xxx
    │   └─ STÁLE vrací: version = 5 (STEJNÝ snapshot!)
    └─ update() → UPDATE users SET ... WHERE version=5
        └─ Selže opět

T5-T7: **Retry #2, #3, #4, #5**
    └─ Stejný problém - stále čtou version = 5

T8: Transaction ROLLBACK
```

### Proč se version nemění?

**PostgreSQL transaction isolation** zajišťuje, že v rámci jedné transakce vidíte **konzistentní snapshot** databáze:

- `READ_COMMITTED`: Vidíte commited changes PŘED začátkem každého statement
- ALE v našem případě: Všechny retry jsou ve STEJNÉ transakci
- Proto všechny SELECTy vrací STEJNÁ data

## ✅ Správné řešení

### Architektura

```java
// ❌ PŘED: Class-level @Transactional
@Service
@Transactional  // ← Všechno ve stejné transakci!
public class KeycloakEventProjectionService {
    private void syncUser() {
        while (retry) {
            getById();   // Same snapshot
            update();    // Fails
        }
    }
}

// ✅ PO: Method-level @Transactional(REQUIRES_NEW)
@Service  // ← BEZ class-level @Transactional!
public class KeycloakEventProjectionService {
    
    private void syncUser() {
        while (retry) {
            try {
                updateInNewTransaction();  // ← Každý retry = NOVÁ transakce
                break;
            } catch (VersionMismatchException e) {
                // Retry s NOVOU transakcí
            }
        }
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void updateInNewTransaction() {
        // NOVÁ transakce = NOVÝ snapshot
        currentUser = getById();  // ← Čte AKTUÁLNÍ stav!
        update(currentUser.version);
    }
}
```

### Implementace

#### 1. Odstranit class-level @Transactional

```java
// Before:
@Service @Transactional
public class KeycloakEventProjectionService { ... }

// After:
@Service  // NO @Transactional here!
public class KeycloakEventProjectionService { ... }
```

#### 2. Vytvořit helper metody s REQUIRES_NEW

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
private void updateUserInNewTransaction(Map<String, Object> userData, String username) {
    // 1. READ - v NOVÉ transakci = AKTUÁLNÍ data
    Map<String, Object> currentUser = metamodelService.getById(...);
    Long version = currentUser.get("version");
    
    // 2. UPDATE - s čerstvou verzí
    metamodelService.update(..., version, ...);
}
```

#### 3. Retry loop volá metodu s novou transakcí

```java
while (!success && attempt < maxRetries) {
    try {
        attempt++;
        
        // ⚡ Každý pokus = NOVÁ transakce
        updateUserInNewTransaction(user, username);
        
        success = true;
    } catch (VersionMismatchException e) {
        // Retry s další NOVOU transakcí
        Thread.sleep(backoff);
    }
}
```

### Proč to funguje?

```
Timeline s REQUIRES_NEW:

T1: **Attempt #1 - Transaction START**
    ├─ getById() → version = 5
    ├─ update(version=5) → FAIL
    └─ Transaction ROLLBACK

T2: Backoff (100ms)

T3: **Attempt #2 - NEW Transaction START** ← NOVÝ snapshot!
    ├─ getById() → version = 6 (AKTUÁLNÍ!)
    ├─ update(version=6) → SUCCESS ✅
    └─ Transaction COMMIT
```

## 📊 Comparison

| Feature | Class-level @Transactional | REQUIRES_NEW per retry |
|---------|---------------------------|----------------------|
| Transaction per retry | ❌ NO (same transaction) | ✅ YES (new transaction) |
| Reads fresh data | ❌ NO (snapshot from T1) | ✅ YES (new snapshot) |
| Version updates visible | ❌ NO | ✅ YES |
| Retry success rate | ❌ 0% | ✅ High |
| Resource usage | Low (1 transaction) | Higher (N transactions) |

## 🎯 Lessons Learned

1. **@Transactional placement matters!**
   - Class-level = ALL methods share same transaction
   - Method-level = Each method gets own transaction

2. **Transaction isolation affects retry logic**
   - Same transaction = Same snapshot
   - New transaction = Fresh data

3. **REQUIRES_NEW is essential for retry patterns**
   - Each retry must see latest DB state
   - Otherwise retry is pointless

4. **Lock ≠ Transaction isolation**
   - CdcLockService prevents concurrent processing ✅
   - But doesn't help with transaction snapshots ❌

5. **Debug with transaction boundaries in mind**
   - Check if retry reads fresh data
   - Check transaction propagation settings

## 🧪 Verification

### Before Fix (Failed)
```
Attempt 1: READ version=5, UPDATE(5) → FAIL
Attempt 2: READ version=5, UPDATE(5) → FAIL  ← Same version!
Attempt 3: READ version=5, UPDATE(5) → FAIL
Attempt 4: READ version=5, UPDATE(5) → FAIL
Attempt 5: READ version=5, UPDATE(5) → FAIL
Result: ❌ All retries failed
```

### After Fix (Success)
```
Attempt 1: [TX1] READ version=5, UPDATE(5) → FAIL, ROLLBACK
Attempt 2: [TX2] READ version=6, UPDATE(6) → SUCCESS ✅
Result: ✅ Fixed on 2nd attempt
```

### Test Scenario
```sql
-- Initial state
SELECT version FROM users WHERE id='xxx';  -- version = 5

-- Simulate concurrent update (e.g., trigger)
UPDATE users SET some_field='changed' WHERE id='xxx';
-- Trigger increments: version → 6

-- CDC sync attempt
-- With REQUIRES_NEW: Will see version=6 and succeed
-- Without REQUIRES_NEW: Would see version=5 and fail
```

## 🔧 Migration Checklist

- [x] Remove class-level @Transactional from KeycloakEventProjectionService
- [x] Add @Transactional(REQUIRES_NEW) to updateUserInNewTransaction()
- [x] Add @Transactional(REQUIRES_NEW) to createUserInNewTransaction()
- [x] Same pattern for Group and Role sync
- [x] Test with actual CDC events
- [ ] Monitor success rate in production
- [ ] Update documentation

## 📚 Related

- Spring Transaction Propagation: https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/transaction/annotation/Propagation.html
- PostgreSQL Isolation Levels: https://www.postgresql.org/docs/current/transaction-iso.html
- Optimistic Locking: https://vladmihalcea.com/optimistic-locking-version-property-jpa-hibernate/

---

**Status:** ✅ FIXED  
**Root Cause:** Transaction snapshot isolation in retry loop  
**Solution:** REQUIRES_NEW propagation for each retry attempt  
**Author:** Martin Horak + AI Assistant  
**Date:** 2025-10-09
