# 📊 Diagnostics & Test Logs

Tento adresář obsahuje logy a diagnostiku z build procesu a testů.

## 📁 Struktura

```
diagnostics/
├── tests/                      # Test logy s timestampy
│   ├── backend-YYYYMMDD-HHMMSS.log
│   ├── frontend-YYYYMMDD-HHMMSS.log
│   └── error-summary-YYYYMMDD-HHMMSS.md
└── build.log                   # Build wrapper log (pokud použit)
```

## 🧪 Test Logs

### Backend Tests
- **Formát**: `backend-{timestamp}.log`
- **Obsahuje**: Kompletní Maven Surefire output
- **Najdete zde**: Stack traces, failed assertions, Testcontainers logs

### Frontend Tests  
- **Formát**: `frontend-{timestamp}.log`
- **Obsahuje**: Vitest/Jest output
- **Najdete zde**: Failed test names, assertion errors, coverage info

### Error Summaries
- **Formát**: `error-summary-{timestamp}.md`
- **Obsahuje**: Automaticky vygenerovaný souhrn chyb pro Copilota
- **Struktura**:
  - 📋 Summary (počty failů/errors)
  - ⚠️ Failed test names
  - 🔍 Key error messages (top 50 lines)
  - 🤖 Action items pro Copilota

## 🔍 Jak použít při chybě

### 1. Běh selhal - co dělat?

Když `make clean` nebo `make rebuild` selže, uvidíte:

```bash
❌ Pre-build tests FAILED

📝 Error analysis saved to: diagnostics/tests/error-summary-20251015-203045.md

🤖 GitHub Copilot - please analyze:
[error summary je zobrazený přímo]
```

### 2. Otevřete error summary

```bash
cat diagnostics/tests/error-summary-*.md | tail -100
```

Nebo v VS Code otevřete ten soubor - tam máte:
- ✅ Které testy selhaly
- 🔍 Klíčové error messages  
- 🤖 Návod pro Copilota, co má opravit

### 3. Zkontrolujte plný log

Pokud potřebujete víc kontextu:

```bash
# Backend
cat diagnostics/tests/backend-YYYYMMDD-HHMMSS.log

# Frontend
cat diagnostics/tests/frontend-YYYYMMDD-HHMMSS.log
```

### 4. Dočasný workaround (skip problémového testu)

Pokud je test známě flaky (např. Testcontainers na macOS):

```bash
SKIP_TEST_CLASSES="TenantFilterIntegrationTest,AnotherFlakyTest" make clean
```

⚠️ **POZOR**: Skip je jen dočasný workaround! Vyřešte root cause a odstraňte skip.

## 🤖 Co dělá Copilot

Když Copilot dostane error summary, měl by:

1. **Analyzovat chybu**: Identifikovat root cause (timing issue, missing mock, wrong assertion...)
2. **Navrhnout fix**: Opravit test nebo produkční kód
3. **Nebo doporučit skip**: Pokud je problém infrastrukturní (např. Kafka na ARM64)

## 📈 Přehled testů během běhu

Během testování vidíte real-time statistiky:

```bash
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📍 STEP 1/6: Pre-Build Tests (unit tests before Docker)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

[1/2] Backend unit tests...
    ⏳ Collecting results...
    📊 Total: 145 | ✅ Passed: 144 | ❌ Failures: 0 | 💥 Errors: 1 | ⏭ Skipped: 2
    ✅ PASS Tests run: 145, Failures: 0, Errors: 0, Skipped: 2

[2/2] Frontend tests...
    ✅ PASS Test Files  9 passed (9)
```

**Co vidíte:**
- 📊 **Total**: Celkový počet testů
- ✅ **Passed**: Kolik prošlo
- ❌ **Failures**: Assertion failures
- 💥 **Errors**: Runtime errors (exceptions)
- ⏭ **Skipped**: Přeskočené

## 🗑️ Čištění starých logů

Logy se nehromadí automaticky. Pokud chcete vyčistit:

```bash
# Smazat logy starší než 7 dní
find diagnostics/tests -name "*.log" -mtime +7 -delete
find diagnostics/tests -name "*.md" -mtime +7 -delete

# Nebo všechny
rm -rf diagnostics/tests/*
```

## 💡 Tips

- **CI/CD**: V CI logujte error-summary jako artifact
- **Git**: diagnostics/ je v .gitignore (není commitovaný)
- **Debugging**: Plné logy jsou nefiltrované - máte tam i stack traces
- **Performance**: Backend logy mohou být velké (50+ MB při Testcontainers)

## 📞 Potřebujete pomoct?

1. Podívejte se do error-summary - tam je analýza
2. Ukažte summary Copilotovi pro návrh řešení  
3. Pokud to nepomáhá, zkontrolujte full log pro úplný kontext
