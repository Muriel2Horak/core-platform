# T3: Migrate Redis & MinIO Secrets

**Parent Story:** INF-003 Docker Secrets Migration  
**Status:** 🔴 TODO  
**Priority:** 🟡 MEDIUM  
**Effort:** 2 hours  
**Owner:** DevOps

---

## 🎯 Objective

Migrate Redis password and MinIO root credentials to Docker Secrets.

---

## 📋 Tasks

### 1. Create Secret Files

```bash
# secrets/redis_password.txt
openssl rand -base64 32 > secrets/redis_password.txt

# secrets/minio_root_user.txt
echo "minioadmin" > secrets/minio_root_user.txt

# secrets/minio_root_password.txt
openssl rand -base64 32 > secrets/minio_root_password.txt
```

### 2. Update docker-compose.yml

```yaml
services:
  redis:
    secrets:
      - redis_password
    command: redis-server --requirepass $(cat /run/secrets/redis_password)
  
  minio:
    secrets:
      - minio_root_user
      - minio_root_password
    environment:
      MINIO_ROOT_USER_FILE: /run/secrets/minio_root_user
      MINIO_ROOT_PASSWORD_FILE: /run/secrets/minio_root_password

secrets:
  redis_password:
    file: ./secrets/redis_password.txt
  minio_root_user:
    file: ./secrets/minio_root_user.txt
  minio_root_password:
    file: ./secrets/minio_root_password.txt
```

### 3. Update Backend application.yml

```yaml
spring:
  data:
    redis:
      password: ${REDIS_PASSWORD}

# MinIO S3 client
minio:
  access-key: ${MINIO_ROOT_USER}
  secret-key: ${MINIO_ROOT_PASSWORD}
```

---

## ✅ Acceptance Criteria

- [ ] Redis requires password authentication
- [ ] MinIO credentials in Docker Secrets
- [ ] Backend can connect to Redis
- [ ] Backend can upload/download from MinIO
- [ ] No credentials in `.env`

---

## 🔗 Dependencies

- Follows T1, T2 pattern
- Low priority (Redis/MinIO currently unused in production)
