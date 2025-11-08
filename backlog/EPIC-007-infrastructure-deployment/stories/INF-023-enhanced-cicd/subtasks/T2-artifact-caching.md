# T2: Configure Artifact & Dependency Caching

**Parent Story:** INF-023 Enhanced CI/CD Pipeline  
**Status:** 🔴 TODO  
**Priority:** 🔥 HIGH  
**Effort:** 3 hours  
**Owner:** DevOps

---

## 🎯 Objective

Implement aggressive caching to reduce pipeline time from ~30 min to ~10 min.

---

## 📋 Tasks

### 1. Maven Dependency Cache

**File:** `.github/workflows/ci.yml`

```yaml
  build-backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java with Maven Cache
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: 'maven'  # ✅ Caches ~/.m2/repository

      - name: Cache Maven Wrapper
        uses: actions/cache@v3
        with:
          path: ~/.m2/wrapper
          key: maven-wrapper-${{ hashFiles('**/maven-wrapper.properties') }}

      - name: Build with Cache
        working-directory: ./backend
        run: ./mvnw package -DskipTests
```

### 2. NPM Dependency Cache

```yaml
  build-frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js with NPM Cache
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: Install Dependencies (Cached)
        working-directory: ./frontend
        run: npm ci  # Uses npm cache automatically
```

### 3. Docker Layer Cache

**File:** `.github/workflows/ci.yml`

```yaml
  build-docker-images:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Cache Docker Layers
        uses: actions/cache@v3
        with:
          path: /tmp/.buildx-cache
          key: docker-${{ runner.os }}-buildx-${{ github.sha }}
          restore-keys: |
            docker-${{ runner.os }}-buildx-

      - name: Build Backend Image
        uses: docker/build-push-action@v5
        with:
          context: ./backend
          file: ./docker/backend/Dockerfile
          push: false
          tags: core-platform/backend:${{ github.sha }}
          cache-from: type=local,src=/tmp/.buildx-cache
          cache-to: type=local,dest=/tmp/.buildx-cache-new,mode=max

      # Rotate cache (avoid unbounded growth)
      - name: Move Cache
        run: |
          rm -rf /tmp/.buildx-cache
          mv /tmp/.buildx-cache-new /tmp/.buildx-cache
```

### 4. Test Results Cache

```yaml
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Cache Test Results
        uses: actions/cache@v3
        with:
          path: |
            backend/target/surefire-reports
            frontend/coverage
          key: test-results-${{ github.sha }}
          restore-keys: test-results-

      - name: Run Tests
        run: make test-backend test-frontend
```

### 5. Measure Cache Effectiveness

**Script:** `scripts/ci/cache-stats.sh`

```bash
#!/bin/bash
# Measure cache hit rates

echo "📊 Cache Statistics"
echo "==================="

# Maven cache
MAVEN_CACHE_SIZE=$(du -sh ~/.m2/repository 2>/dev/null | cut -f1)
echo "Maven cache size: ${MAVEN_CACHE_SIZE:-0MB}"

# NPM cache
NPM_CACHE_SIZE=$(du -sh ~/.npm 2>/dev/null | cut -f1)
echo "NPM cache size: ${NPM_CACHE_SIZE:-0MB}"

# Docker cache
DOCKER_CACHE_SIZE=$(du -sh /tmp/.buildx-cache 2>/dev/null | cut -f1)
echo "Docker cache size: ${DOCKER_CACHE_SIZE:-0MB}"
```

### 6. Test Caching

```bash
# First run (cold cache)
time gh workflow run ci.yml

# Second run (warm cache) - should be 3x faster
time gh workflow run ci.yml
```

---

## ✅ Acceptance Criteria

- [ ] Maven dependencies cached (~500MB → 30s restore time)
- [ ] NPM dependencies cached (~200MB → 15s restore time)
- [ ] Docker layers cached (~1GB → 2 min rebuild time)
- [ ] Second run 3x faster than first run
- [ ] Cache hit rate > 80%
- [ ] Cache size < 5GB (automatic cleanup)

---

## 🔗 Dependencies

- **BLOCKS:** T3 (Test Gates)
- **REQUIRES:** T1 (Multi-Stage Pipeline)
