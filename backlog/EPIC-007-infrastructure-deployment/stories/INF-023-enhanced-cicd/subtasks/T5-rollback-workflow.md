# T5: Rollback Automation Workflow

**Parent Story:** INF-023 Enhanced CI/CD Pipeline  
**Status:** 🔴 TODO  
**Priority:** 🔥 HIGH  
**Effort:** 3 hours  
**Owner:** DevOps

---

## 🎯 Objective

One-click rollback to previous stable version with automatic DB restore.

---

## 📋 Tasks

### 1. Create Rollback Workflow

**File:** `.github/workflows/rollback.yml`

```yaml
name: Rollback to Previous Version

on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Environment to rollback'
        required: true
        type: choice
        options:
          - staging
          - production
      version:
        description: 'Version to rollback to (leave empty for previous)'
        required: false
        type: string

jobs:
  rollback:
    runs-on: ubuntu-latest
    environment:
      name: ${{ inputs.environment }}
    steps:
      - uses: actions/checkout@v4

      # =====================================
      # Determine Rollback Version
      # =====================================
      - name: Get Previous Version
        id: version
        run: |
          if [ -z "${{ inputs.version }}" ]; then
            # Get previous stable tag
            CURRENT=$(git describe --tags --abbrev=0)
            PREVIOUS=$(git describe --tags --abbrev=0 ${CURRENT}^)
            echo "version=$PREVIOUS" >> $GITHUB_OUTPUT
          else
            echo "version=${{ inputs.version }}" >> $GITHUB_OUTPUT
          fi

      - name: Confirm Rollback
        run: |
          echo "🔄 Rolling back ${{ inputs.environment }} to ${{ steps.version.outputs.version }}"
          echo "Current version: $(git describe --tags --abbrev=0)"

      # =====================================
      # Database Rollback
      # =====================================
      - name: Configure SSH
        run: |
          mkdir -p ~/.ssh
          echo "${{ secrets.DEPLOY_SSH_KEY }}" > ~/.ssh/id_rsa
          chmod 600 ~/.ssh/id_rsa
          ssh-keyscan -H ${{ env.DEPLOY_HOST }} >> ~/.ssh/known_hosts

      - name: Restore Database Backup
        if: inputs.environment == 'production'
        run: |
          ssh deploy@${{ env.DEPLOY_HOST }} << 'EOF'
            cd /opt/core-platform/backups
            
            # Find latest backup before current version
            BACKUP=$(ls -t backup-*.sql | head -n 2 | tail -n 1)
            
            echo "📦 Restoring database from: $BACKUP"
            
            # Create pre-rollback backup
            docker exec core-db pg_dump -U core core > rollback-backup-$(date +%Y%m%d-%H%M%S).sql
            
            # Restore database
            cat $BACKUP | docker exec -i core-db psql -U core core
          EOF

      # =====================================
      # Application Rollback
      # =====================================
      - name: Rollback Docker Images
        run: |
          ssh deploy@${{ env.DEPLOY_HOST }} << EOF
            cd /opt/core-platform
            
            # Pull previous version images
            docker pull ghcr.io/${{ github.repository }}/backend:${{ steps.version.outputs.version }}
            docker pull ghcr.io/${{ github.repository }}/frontend:${{ steps.version.outputs.version }}
            
            # Update docker-compose.yml with rollback version
            sed -i 's|backend:.*|backend:${{ steps.version.outputs.version }}|' docker-compose.yml
            sed -i 's|frontend:.*|frontend:${{ steps.version.outputs.version }}|' docker-compose.yml
            
            # Restart with previous version
            docker compose up -d --force-recreate backend frontend
            
            # Wait for startup
            sleep 20
          EOF

      # =====================================
      # Verification
      # =====================================
      - name: Health Check
        run: |
          HEALTH_URL="https://${{ inputs.environment }}.core-platform.local/api/actuator/health"
          
          for i in {1..30}; do
            if curl -f $HEALTH_URL; then
              echo "✅ Rollback successful - app healthy"
              exit 0
            fi
            echo "Waiting for app to become healthy... ($i/30)"
            sleep 2
          done
          
          echo "❌ Rollback failed - app not healthy"
          exit 1

      # =====================================
      # Notifications
      # =====================================
      - name: Notify Rollback
        if: always()
        uses: slackapi/slack-github-action@v1
        with:
          webhook-url: ${{ secrets.SLACK_WEBHOOK }}
          payload: |
            {
              "text": "${{ job.status == 'success' && '✅' || '❌' }} Rollback ${{ job.status }}",
              "blocks": [
                {
                  "type": "section",
                  "text": {
                    "type": "mrkdwn",
                    "text": "*Rollback Executed*\nEnvironment: ${{ inputs.environment }}\nVersion: ${{ steps.version.outputs.version }}\nStatus: ${{ job.status }}\nTriggered by: ${{ github.actor }}"
                  }
                }
              ]
            }
```

### 2. Create Rollback Helper Script

**File:** `scripts/deployment/rollback.sh`

```bash
#!/bin/bash
set -e

# Usage: ./rollback.sh [staging|production] [version]

ENV=${1:-production}
VERSION=${2}

echo "🔄 Rolling back $ENV environment"

# Determine rollback version
if [ -z "$VERSION" ]; then
  CURRENT=$(docker inspect core-backend --format='{{.Config.Image}}' | cut -d: -f2)
  VERSION=$(git tag --sort=-version:refname | grep -A1 "$CURRENT" | tail -n1)
  echo "Auto-detected rollback version: $VERSION"
fi

# Confirm
read -p "Rollback to $VERSION? (y/N): " confirm
if [ "$confirm" != "y" ]; then
  echo "Rollback cancelled"
  exit 1
fi

# Backup current state
echo "📦 Creating pre-rollback backup..."
docker exec core-db pg_dump -U core core > "backups/rollback-backup-$(date +%Y%m%d-%H%M%S).sql"

# Pull previous version
echo "🐳 Pulling version $VERSION..."
docker pull ghcr.io/USER/REPO/backend:$VERSION
docker pull ghcr.io/USER/REPO/frontend:$VERSION

# Update docker-compose.yml
sed -i.bak "s|backend:.*|backend:$VERSION|" docker-compose.yml
sed -i.bak "s|frontend:.*|frontend:$VERSION|" docker-compose.yml

# Restart services
echo "♻️ Restarting services..."
docker compose up -d --force-recreate backend frontend

# Health check
echo "🏥 Waiting for health check..."
sleep 10
if curl -f https://${ENV}.core-platform.local/api/actuator/health; then
  echo "✅ Rollback successful!"
else
  echo "❌ Rollback failed - restoring from backup..."
  cat "backups/rollback-backup-$(date +%Y%m%d-%H%M%S).sql" | docker exec -i core-db psql -U core core
  exit 1
fi
```

### 3. Test Rollback

```bash
# Make script executable
chmod +x scripts/deployment/rollback.sh

# Test staging rollback
gh workflow run rollback.yml \
  -f environment=staging \
  -f version=v1.0.0

# Monitor rollback
gh run watch

# Verify rollback
curl -f https://staging.core-platform.local/api/actuator/health

# Test manual rollback script
./scripts/deployment/rollback.sh staging v1.0.0
```

---

## ✅ Acceptance Criteria

- [ ] Rollback workflow accessible in GitHub Actions UI
- [ ] Auto-detects previous stable version if not specified
- [ ] Database backup created before rollback
- [ ] Database restored from backup before current version
- [ ] Docker images rolled back to specified version
- [ ] Health check passes after rollback
- [ ] Slack notification sent with rollback status
- [ ] Manual rollback script works locally

---

## 🔗 Dependencies

- **REQUIRES:** T4 (Deployment Automation)
- Database backups created during deployment
