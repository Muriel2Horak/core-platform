# S4: Workflow Templates & Documentation

> **Enablement:** Provide starter templates and user guide for n8n workflows

## 📋 Story

**As a** workflow developer  
**I want** starter templates and documentation  
**So that** I can quickly create common automation patterns

## 🎯 Acceptance Criteria

**GIVEN** n8n is accessible  
**WHEN** user logs in for first time  
**THEN** starter templates are available  
**AND** user guide explains workflow creation  
**AND** best practices are documented

## 🏗️ Implementation

### 1. Starter Workflow Templates

**File:** `workflows/templates/http-api-call.json`

```json
{
  "name": "HTTP API Call Template",
  "nodes": [
    {
      "parameters": {
        "method": "GET",
        "url": "https://api.example.com/data",
        "authentication": "predefinedCredentialType",
        "nodeCredentialType": "httpHeaderAuth",
        "options": {}
      },
      "name": "HTTP Request",
      "type": "n8n-nodes-base.httpRequest",
      "position": [250, 300]
    },
    {
      "parameters": {
        "values": {
          "string": [
            {
              "name": "status",
              "value": "={{$json.status}}"
            }
          ]
        }
      },
      "name": "Set Response",
      "type": "n8n-nodes-base.set",
      "position": [450, 300]
    }
  ],
  "connections": {
    "HTTP Request": {
      "main": [[{"node": "Set Response", "type": "main", "index": 0}]]
    }
  }
}
```

**File:** `workflows/templates/database-query.json`

```json
{
  "name": "Database Query Template",
  "nodes": [
    {
      "parameters": {
        "operation": "executeQuery",
        "query": "SELECT * FROM users WHERE active = true",
        "options": {}
      },
      "name": "PostgreSQL",
      "type": "n8n-nodes-base.postgres",
      "credentials": {
        "postgres": {
          "id": "1",
          "name": "Core DB"
        }
      },
      "position": [250, 300]
    },
    {
      "parameters": {
        "operation": "limit",
        "maxItems": 10
      },
      "name": "Limit Results",
      "type": "n8n-nodes-base.limit",
      "position": [450, 300]
    }
  ],
  "connections": {
    "PostgreSQL": {
      "main": [[{"node": "Limit Results", "type": "main", "index": 0}]]
    }
  }
}
```

**File:** `workflows/templates/email-notification.json`

```json
{
  "name": "Email Notification Template",
  "nodes": [
    {
      "parameters": {
        "fromEmail": "no-reply@core-platform.local",
        "toEmail": "admin@core-platform.local",
        "subject": "Workflow Alert",
        "text": "={{$json.message}}",
        "options": {}
      },
      "name": "Send Email",
      "type": "n8n-nodes-base.emailSend",
      "credentials": {
        "smtp": {
          "id": "2",
          "name": "SMTP"
        }
      },
      "position": [250, 300]
    }
  ]
}
```

### 2. User Guide

**File:** `docs/n8n-user-guide.md`

```markdown
# n8n Workflow Automation - User Guide

## 🚀 Getting Started

### Access n8n
1. Navigate to: https://admin.core-platform.local/n8n
2. Login with Keycloak credentials
3. Dashboard opens with workflow list

### Create First Workflow
1. Click **"New Workflow"**
2. Drag nodes from left panel
3. Connect nodes by dragging arrows
4. Configure node parameters
5. Click **"Execute Workflow"** to test
6. Click **"Save"** when ready

## 📚 Common Patterns

### HTTP API Integration
- Use **HTTP Request** node
- Set method (GET/POST/PUT/DELETE)
- Configure headers/body
- Test with **Execute Node**

### Database Queries
- Add **PostgreSQL** node
- Select **"Execute Query"** operation
- Write SQL query
- Use **{{$json.field}}** for dynamic values

### Scheduled Workflows
- Add **Cron** trigger node
- Set schedule (e.g., "0 9 * * *" for 9 AM daily)
- Connect to automation nodes
- Activate workflow

### Error Handling
- Add **IF** node after operations
- Check for error conditions
- Route to error notification
- Use **Email** or **Webhook** for alerts

## 🔒 Best Practices

### Security
- Store credentials in n8n Credentials (not in nodes)
- Use environment variables for secrets
- Limit webhook access to trusted sources
- Review execution history regularly

### Performance
- Limit result sets (use LIMIT in SQL)
- Avoid infinite loops
- Set execution timeouts
- Use **Wait** node for rate limiting

### Maintenance
- Name workflows descriptively
- Add **Notes** to complex nodes
- Test workflows before activation
- Monitor execution logs in Dashboard

## 🆘 Troubleshooting

### Workflow Fails to Execute
1. Check node configuration (red markers)
2. Review execution logs (bottom panel)
3. Test each node individually
4. Verify credentials are valid

### Webhook Not Receiving Data
1. Check URL: `/n8n/webhook/your-webhook-id`
2. Verify HTTP method matches
3. Test with curl: `curl -X POST https://admin.core-platform.local/n8n/webhook/test`
4. Review Nginx logs if 404

### Database Connection Issues
1. Verify credentials in n8n Credentials
2. Check database is accessible from n8n container
3. Test query syntax in psql first
4. Review PostgreSQL logs

## 📞 Support
- Internal docs: https://wiki.core-platform.local/n8n
- n8n documentation: https://docs.n8n.io
- Platform team: #platform-support
```

## ✅ Testing

```bash
# 1. Import templates
curl -X POST https://admin.core-platform.local/n8n/api/v1/workflows \
  -H "Authorization: Bearer $N8N_API_KEY" \
  -H "Content-Type: application/json" \
  -d @workflows/templates/http-api-call.json

# 2. Verify templates visible in UI
# Open: https://admin.core-platform.local/n8n
# Check: Templates tab shows 3 workflows

# 3. Test HTTP template execution
# Click: HTTP API Call Template → Execute Workflow
# Verify: Node executes successfully
```

## 🎯 Acceptance Checklist

- [x] 3 starter templates created (HTTP, DB, Email)
- [x] User guide written (Markdown)
- [x] Best practices documented
- [x] Troubleshooting section added
- [x] Templates importable via UI/API

---

**Effort**: ~4 hours  
**LOC**: ~300 lines
