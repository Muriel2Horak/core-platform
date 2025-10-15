# Build Progress Tracker - Visual Guide

## Co dělá tento systém?

Progress tracker zobrazuje **živou tabulku** během `make clean` nebo `make rebuild`, která vám ukazuje:

1. **Které kroky už proběhly** ✅
2. **Který krok právě běží** ⏳  
3. **Co vás ještě čeká** ⏸️
4. **Celkový progress** a elapsed time

## Jak to vypadá?

```
╔══════════════════════════════════════════════════════════════════════════╗
║  🏗️  MAKE CLEAN - FULL PIPELINE                                        ║
╠══════════════════════════════════════════════════════════════════════════╣
║  ✅ 1/6  Cleanup containers        [████████████] DONE (8s)             ║
║  ✅ 2/6  Pre-build tests           [████████████] DONE (42s)            ║
║  ⏳ 3/6  Build Docker images       [██████░░░░░░] IN PROGRESS           ║
║  ⏸️  4/6  Start services            [░░░░░░░░░░░░] PENDING              ║
║  ⏸️  5/6  E2E pre-deploy tests      [░░░░░░░░░░░░] PENDING              ║
║  ⏸️  6/6  E2E post-deploy tests     [░░░░░░░░░░░░] PENDING              ║
╠══════════════════════════════════════════════════════════════════════════╣
║  Overall: [████░░░░░░░░] 2/6 (33%)  │  Elapsed: 0m 50s                 ║
╚══════════════════════════════════════════════════════════════════════════╝

🐳 Building backend...
 => [backend 1/8] FROM docker.io/library/maven:3.9-eclipse-temurin-21
 => CACHED [backend 2/8] WORKDIR /app
...
```

## Když nastane chyba

Panel **zůstane nahoře** a chyba se zobrazí pod ním:

```
╔══════════════════════════════════════════════════════════════════════════╗
║  🏗️  MAKE CLEAN - FULL PIPELINE                                        ║
╠══════════════════════════════════════════════════════════════════════════╣
║  ✅ 1/6  Cleanup containers        [████████████] DONE (8s)             ║
║  ❌ 2/6  Pre-build tests           [████████████] FAILED (42s)          ║
║  ⏸️  3/6  Build Docker images       [░░░░░░░░░░░░] CANCELLED            ║
║  ⏸️  4/6  Start services            [░░░░░░░░░░░░] CANCELLED            ║
║  ⏸️  5/6  E2E pre-deploy tests      [░░░░░░░░░░░░] CANCELLED            ║
║  ⏸️  6/6  E2E post-deploy tests     [░░░░░░░░░░░░] CANCELLED            ║
╠══════════════════════════════════════════════════════════════════════════╣
║  Overall: [██░░░░░░░░░░] 1/6 (17%)  │  Elapsed: 0m 50s                 ║
╚══════════════════════════════════════════════════════════════════════════╝

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
❌ STEP 2 FAILED: Pre-build tests
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Backend Tests: Tests run: 145, Failures: 1, Errors: 0, Skipped: 0

Failed Tests:
  • TenantFilterIntegrationTest.testKafkaFiltering -- Time elapsed: 5.123 s <<< ERROR!
  
Error Details:
  org.testcontainers.containers.ContainerLaunchException: Container startup failed
    at org.testcontainers.containers.GenericContainer.doStart(GenericContainer.java:357)
  Caused by: com.github.dockerjava.api.exception.InternalServerErrorException: 
    {"message":"sh: /tmp/testcontainers_start.sh: Text file busy"}

💡 Suggestion: This appears to be a Testcontainers issue (common on macOS).
   Fix: SKIP_TEST_CLASSES="TenantFilterIntegrationTest" make clean

📁 Full log: diagnostics/tests/backend-20251015-212547.log

🤖 GitHub Copilot: Error details are visible above - analyze and suggest fixes.
```

## Výhody

✅ **Copilot může číst chybu přímo z terminálu** - nemusí otevírat logy  
✅ **Vidíte kolik práce zbývá** - nemusíte čekat do neznáma  
✅ **Panel zůstává viditelný** - i během scrollování výstupu  
✅ **Časování každého kroku** - víte kde jsou bottlenecks  

## Použití

Stačí spustit normálně:
```bash
make clean
# nebo
SKIP_TEST_CLASSES="TenantFilterIntegrationTest" make clean
```

Progress tracker běží automaticky!
