# T4: Deployment Automation (Staging & Production)

**Parent Story:** INF-023 Enhanced CI/CD Pipeline  
**Status:** 🔴 TODO  
**Priority:** 🔥 CRITICAL  
**Effort:** 5 hours  
**Owner:** DevOps

---

## 🎯 Objective

Automate deployment: staging on push to `develop`, production on manual approval.

---

## 📋 Tasks

### 1. Create Staging Auto-Deploy

**File:** `.github/workflows/deploy-staging.yml`

```yaml
name: Deploy to Staging

on:
  push:
    branches: [develop]
  workflow_dispatch:  # Manual trigger

env:
  DEPLOY_HOST: staging.core-platform.local
  SSH_USER: deploy
  DOCKER_REGISTRY: ghcr.io

jobs:
  deploy-staging:
    runs-on: ubuntu-latest
    environment:
      name: staging
      url: https://staging.core-platform.local
    steps:
      - uses: actions/checkout@v4

      # =====================================
      # Build & Push Docker Images
      # =====================================
      - name: Login to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build & Push Backend
        uses: docker/build-push-action@v5
        with:
          context: ./backend
          file: ./docker/backend/Dockerfile
          push: true
          tags: |
            ghcr.io/${{ github.repository }}/backend:staging
            ghcr.io/${{ github.repository }}/backend:${{ github.sha }}

      - name: Build & Push Frontend
        uses: docker/build-push-action@v5
        with:
          context: ./frontend
          file: ./docker/frontend/Dockerfile
          push: true
          tags: |
            ghcr.io/${{ github.repository }}/frontend:staging
            ghcr.io/${{ github.repository }}/frontend:${{ github.sha }}

      # =====================================
      # Deploy to Staging Server
      # =====================================
      - name: Configure SSH
        run: |
          mkdir -p ~/.ssh
          echo "${{ secrets.STAGING_SSH_KEY }}" > ~/.ssh/id_rsa
          chmod 600 ~/.ssh/id_rsa
          ssh-keyscan -H ${{ env.DEPLOY_HOST }} >> ~/.ssh/known_hosts

      - name: Deploy via SSH
        run: |
          ssh ${{ env.SSH_USER }}@${{ env.DEPLOY_HOST }} << 'EOF'
            cd /opt/core-platform
            
            # Pull latest images
            docker compose pull
            
            # Restart services (zero-downtime)
            docker compose up -d --no-deps --build backend frontend
            
            # Health check
            sleep 10
            curl -f https://staging.core-platform.local/api/actuator/health || exit 1
          EOF

      # =====================================
      # Post-Deploy Verification
      # =====================================
      - name: Run E2E Tests (Staging)
        run: |
          export POST_BASE_URL=https://staging.core-platform.local
          make test-e2e-post
        timeout-minutes: 15

      - name: Notify Slack
        if: always()
        uses: slackapi/slack-github-action@v1
        with:
          webhook-url: ${{ secrets.SLACK_WEBHOOK }}
          payload: |
            {
              "text": "${{ job.status == 'success' && '✅' || '❌' }} Staging Deploy ${{ job.status }}",
              "blocks": [
                {
                  "type": "section",
                  "text": {
                    "type": "mrkdwn",
                    "text": "*Staging Deployment*\nStatus: ${{ job.status }}\nCommit: ${{ github.sha }}\nURL: https://staging.core-platform.local"
                  }
                }
              ]
            }
```

### 2. Create Production Manual Deploy

**File:** `.github/workflows/deploy-production.yml`

```yaml
name: Deploy to Production

on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version tag to deploy (e.g., v1.2.3)'
        required: true
        type: string

env:
  DEPLOY_HOST: core-platform.local
  SSH_USER: deploy

jobs:
  # =====================================
  # Pre-Deploy Approval
  # =====================================
  request-approval:
    runs-on: ubuntu-latest
    environment:
      name: production-approval
    steps:
      - name: Request Manual Approval
        run: echo "Waiting for approval to deploy ${{ inputs.version }} to production..."

  # =====================================
  # Production Deployment
  # =====================================
  deploy-production:
    needs: request-approval
    runs-on: ubuntu-latest
    environment:
      name: production
      url: https://core-platform.local
    steps:
      - uses: actions/checkout@v4
        with:
          ref: ${{ inputs.version }}

      - name: Login to Registry
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Pull Production Images
        run: |
          docker pull ghcr.io/${{ github.repository }}/backend:${{ inputs.version }}
          docker pull ghcr.io/${{ github.repository }}/frontend:${{ inputs.version }}

      - name: Configure SSH
        run: |
          mkdir -p ~/.ssh
          echo "${{ secrets.PRODUCTION_SSH_KEY }}" > ~/.ssh/id_rsa
          chmod 600 ~/.ssh/id_rsa
          ssh-keyscan -H ${{ env.DEPLOY_HOST }} >> ~/.ssh/known_hosts

      # =====================================
      # Database Migration (Pre-Deploy)
      # =====================================
      - name: Run Database Migrations
        run: |
          ssh ${{ env.SSH_USER }}@${{ env.DEPLOY_HOST }} << 'EOF'
            cd /opt/core-platform
            
            # Backup database first
            docker exec core-db pg_dump -U core core > backup-$(date +%Y%m%d-%H%M%S).sql
            
            # Run migrations
            docker compose run --rm backend ./mvnw flyway:migrate
          EOF

      # =====================================
      # Blue-Green Deployment
      # =====================================
      - name: Deploy (Blue-Green)
        run: |
          ssh ${{ env.SSH_USER }}@${{ env.DEPLOY_HOST }} << 'EOF'
            cd /opt/core-platform
            
            # Pull new images
            docker compose pull
            
            # Start new containers (blue)
            docker compose up -d --scale backend=2 --no-recreate
            
            # Wait for health check
            sleep 30
            curl -f https://core-platform.local/api/actuator/health || exit 1
            
            # Switch traffic (green -> blue)
            docker compose up -d --scale backend=1 --force-recreate nginx
            
            # Remove old containers
            docker compose ps --filter "status=exited" -q | xargs docker rm
          EOF

      # =====================================
      # Post-Deploy Verification
      # =====================================
      - name: Smoke Test Production
        run: |
          curl -f https://core-platform.local/api/actuator/health
          curl -f https://core-platform.local/

      - name: Notify Deployment
        uses: slackapi/slack-github-action@v1
        with:
          webhook-url: ${{ secrets.SLACK_WEBHOOK }}
          payload: |
            {
              "text": "🚀 Production Deployment Complete",
              "blocks": [
                {
                  "type": "section",
                  "text": {
                    "type": "mrkdwn",
                    "text": "*Production Deployed*\nVersion: ${{ inputs.version }}\nURL: https://core-platform.local\nDeployed by: ${{ github.actor }}"
                  }
                }
              ]
            }
```

### 3. Create Rollback Workflow (See T5)

### 4. Test Deployments

```bash
# Test staging auto-deploy
git checkout develop
git commit --allow-empty -m "test: Trigger staging deploy"
git push origin develop

# Monitor deployment
gh workflow view "Deploy to Staging" --web

# Test production manual deploy
gh workflow run deploy-production.yml -f version=v1.0.0

# Verify deployment
curl -f https://staging.core-platform.local/api/actuator/health
```

---

## ✅ Acceptance Criteria

- [ ] Staging deploys automatically on `develop` push
- [ ] Production requires manual approval
- [ ] Docker images tagged with SHA + semantic version
- [ ] Database migrations run before deployment
- [ ] Health checks pass before switching traffic
- [ ] Slack notifications sent on deploy success/failure
- [ ] Zero-downtime deployment (blue-green)

---

## 🔗 Dependencies

- **BLOCKS:** T5 (Rollback Workflow)
- **REQUIRES:** T3 (Quality Gates)
