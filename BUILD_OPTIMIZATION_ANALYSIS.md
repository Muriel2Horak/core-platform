# Build Optimization Analysis

## 🔍 Současný problém

### ❌ Problém: `--no-cache` všude
```bash
# V Makefile je 14x použito --no-cache:
docker compose build --no-cache backend
docker compose build --no-cache frontend
docker compose build --parallel --no-cache
```

**Důsledek:** Docker ignoruje všechny cache mechanismy, včetně:
- ❌ Cache mounty pro Maven dependencies (`/root/.m2`)
- ❌ Cache mounty pro npm packages (`/root/.npm`)
- ❌ Layer cache pro neměněné soubory
- ❌ BuildKit optimalizace

**Výsledek:** Každý build stahuje všechny závislosti znovu (~5-10 minut zbytečně)

---

## ✅ Co už máte SPRÁVNĚ v Dockerfiles

### Backend (Maven)
```dockerfile
# ✅ SPRÁVNĚ: Copy pouze pom.xml (dependency layer)
COPY backend/pom.xml .

# ✅ SPRÁVNĚ: Cache mount pro Maven dependencies
RUN --mount=type=cache,target=/root/.m2 \
  mvn -B -U dependency:go-offline

# ✅ SPRÁVNĚ: Source code v separate layer
COPY backend/src ./src

# ✅ SPRÁVNĚ: Reuse cached dependencies
RUN --mount=type=cache,target=/root/.m2 \
  mvn -B -U package -DskipTests
```

### Frontend (npm)
```dockerfile
# ✅ SPRÁVNĚ: Copy pouze package*.json
COPY frontend/package*.json ./

# ✅ SPRÁVNĚ: Cache mount pro npm
RUN --mount=type=cache,target=/root/.npm \
  npm ci

# ✅ SPRÁVNĚ: Source code separate
COPY frontend/ .
RUN node esbuild.mjs
```

**Toto je optimální struktura! Ale `--no-cache` to všechno ruší.**

---

## 🚀 Doporučená optimalizace

### Strategie: Používat cache, vypnout pouze když je potřeba

```bash
# ✅ NORMÁLNÍ BUILD (s cache) - rychlý
make rebuild

# ❌ FORCE REBUILD (bez cache) - pomalý, ale čistý
make rebuild-clean
```

---

## 📋 Co změnit v Makefile

### 1. Odstranit `--no-cache` z běžných buildů

**Před:**
```makefile
_rebuild_inner:
  @DOCKER_BUILDKIT=1 docker compose build --parallel --no-cache
```

**Po:**
```makefile
_rebuild_inner:
  @DOCKER_BUILDKIT=1 docker compose build --parallel
```

### 2. Přidat nový target `rebuild-clean` pro force rebuild

```makefile
# Clean rebuild (force, ignores cache)
rebuild-clean:
  @scripts/build/wrapper.sh $(MAKE) _rebuild_clean_inner 2>&1 | tee -a $(LOG_FILE)

_rebuild_clean_inner:
  @echo "🧹 Clean rebuild (ignoring cache)..."
  @DOCKER_BUILDKIT=1 docker compose build --parallel --no-cache
  @$(MAKE) up
```

### 3. Aktualizovat dev-clean

**Před:**
```makefile
dev-clean:
  @docker compose build --no-cache
```

**Po:**
```makefile
dev-clean:
  @docker compose build
```

---

## ⏱️ Očekávané zlepšení

### Bez optimalizace (`--no-cache`)
```
Backend build:  ~5-7 minut (stahování Maven dependencies)
Frontend build: ~2-3 minuty (stahování npm packages)
Total:          ~7-10 minut
```

### S optimalizací (cache enabled)
```
Backend build:  ~1-2 minuty (dependencies cached)
Frontend build: ~30-60 sekund (packages cached)
Total:          ~2-3 minuty

Zrychlení: 3-5x rychlejší! ⚡
```

### První build (cold cache)
```
Backend:  ~5-7 minut (poprvé stáhne dependencies)
Frontend: ~2-3 minuty (poprvé stáhne packages)

Ale pak už jen ~2-3 minuty! 🚀
```

---

## 🎯 Kdy použít co

### `make rebuild` (s cache) - DOPORUČENO
**Použít:**
- ✅ Běžný vývoj
- ✅ Po změnách v kódu
- ✅ Po pull z gitu
- ✅ V CI/CD pipeline

**Nepotřebuje stahovat dependencies znovu!**

### `make rebuild-clean` (bez cache)
**Použít:**
- ⚠️ Po změnách v pom.xml / package.json
- ⚠️ Při problémech s dependencies
- ⚠️ Před důležitým release
- ⚠️ Při podezření na cache corruption

### `make clean` (full clean)
**Použít:**
- 🧹 Kompletní reset prostředí
- 🧹 Smazání volumes + images + cache
- 🧹 Před velkými změnami

---

## 🔧 Implementační kroky

1. **Odstranit `--no-cache` z těchto targets:**
   - `_rebuild_inner`
   - `dev-clean`
   - `rebuild-backend`
   - `rebuild-frontend`
   - `production-up`

2. **Přidat nové clean targets:**
   - `rebuild-clean` - Force rebuild bez cache
   - `rebuild-backend-clean` - Force backend rebuild
   - `rebuild-frontend-clean` - Force frontend rebuild

3. **Zachovat `--no-cache` pouze v:**
   - `kc-image` - Keycloak má malé dependencies
   - Nový `rebuild-clean` target

4. **Aktualizovat dokumentaci:**
   - README.md - Přidat `rebuild-clean` do příkladů
   - Build tips - Kdy použít cache vs no-cache

---

## 💡 Bonus optimalizace

### 1. BuildKit cache mount cleanup
```bash
# Občas vyčistit starou cache (disk space)
docker builder prune --filter until=168h  # 7 days
```

### 2. Multi-stage build optimization
```dockerfile
# Backend už má optimálně:
# Stage 1: Builder (s dependencies)
# Stage 2: Runtime (malý final image)

# ✅ Final image je jen ~400MB místo ~1.5GB
```

### 3. .dockerignore optimization
```bash
# Už máte správně:
# Ignoruje node_modules, .git, atd.
# Minimalizuje build context
```

---

## 🎉 Výhody po optimalizaci

### ⚡ Rychlost
- 3-5x rychlejší běžné buildy
- Závislosti staženy pouze jednou
- Layer cache pro neměněné soubory

### 💾 Disk space
- Cache mount sdílený mezi buildy
- Menší final images (multi-stage)
- Automatické cleanup starých layers

### 🔄 Flexibilita
- `rebuild` - rychlý s cache
- `rebuild-clean` - čistý bez cache
- `clean` - kompletní reset

### 🚀 Developer experience
- Méně čekání na buildy
- Rychlejší iterace
- Lepší CI/CD performance

---

## 📊 Srovnání

| Operace | Bez cache | S cache | Zrychlení |
|---------|-----------|---------|-----------|
| First build | ~10 min | ~10 min | - |
| Code change | ~10 min | ~2 min | **5x** |
| pom.xml change | ~10 min | ~7 min | 1.4x |
| package.json change | ~10 min | ~5 min | 2x |

---

## ✅ Doporučení

1. **Implementovat optimalizaci** - Odstranit `--no-cache` z běžných buildů
2. **Přidat `rebuild-clean`** - Pro případy kdy cache potřeba vypnout
3. **Aktualizovat docs** - Vysvětlit kdy použít co
4. **Testovat** - Ověřit že cache funguje správně

**Benefit: 3-5x rychlejší buildy bez kompromisů! ⚡**
