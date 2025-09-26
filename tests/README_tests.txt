# Multitenancy Smoke Tests

## Požadavky
- Docker a docker-compose (běžící services)
- curl, jq, psql, base64 (na macOS/Linux)
- Keycloak s nakonfigurovanými uživateli a client scope pro tenant claim

## Příprava
1. Zkopíruj .env.template → .env a doplň proměnné:
   ```bash
   cp tests/.env.template tests/.env
   vim tests/.env  # nebo jiný editor
   ```

2. Ujisti se, že běží všechny services:
   ```bash
   docker compose up -d
   ```

3. Ověř, že jsou vytvořeni testovací uživatelé v Keycloaku s atributem 'tenant':
   - test1 s tenant="test-tenant"
   - test2 s tenant="company-b"

4. Ujisti se, že je nakonfigurován Client Scope s User Attribute mapperem:
   - Scope name: tenant-scope
   - Mapper: User Attribute → tenant → tenant (v access token)
   - Přiřazen k frontend klientovi

## Spuštění testů
```bash
# Spusť testy a vygeneruj report
make test-and-report

# Nebo jednotlivě:
make test-mt     # pouze testy
make report-mt   # pouze report
```

## Výstup
- TEST_REPORT.md - hlavní report
- artifacts/ - JSON odpovědi, logy, JWT payloady
- tests/summary.json - souhrnné výsledky

## Řešení problémů
- Pokud selže získání JWT tokenů, zkontroluj Keycloak konfiguraci
- Pokud chybí tenant claim, nakonfiguruj User Attribute mapper
- Pokud selže DB seed, zkontroluj DB připojení a schéma
- Pokud není dostupný Loki, test pokračuje s varováním