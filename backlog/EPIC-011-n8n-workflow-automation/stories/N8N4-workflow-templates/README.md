# N8N4: Pre-built Workflow Templates

**Typ:** TASK  
**Epic:** EPIC-011 (n8n External Orchestration Layer)  
**Fase:** Phase 3 (n8n Deployment)  
**Priorita:** MEDIUM  
**Effort:** 500 LOC, 2 dny  
**Dependencies:** N8N1, N8N2, N8N3, WF15 (External Task Executor)  
**Status:** ⏳ TODO

---

## 🎯 Cíl

Vytvořit **pre-built n8n workflow templates** pro běžné use cases:
- **Template 1**: Jira Ticket Approval (External Task → Create Jira → Complete)
- **Template 2**: Confluence Documentation Sync (Webhook → Update Confluence)
- **Template 3**: Trello Card Creation (External Task → Create Card)
- **Template 4**: Slack Notification (External Task → Send Slack)
- Import JSON do n8n instance

---

## 📋 Požadavky

### Funkční Požadavky

1. **Workflow Templates**
   - 4 pre-built workflows (JSON export)
   - Ready-to-import do n8n
   - Dokumentace pro každý template

2. **Integration s Core Platform**
   - External Task pattern (polling /api/n8n/external-tasks/poll)
   - Task completion callback
   - Error handling + retry

3. **Credentials**
   - Jira: OAuth2 nebo Basic Auth
   - Confluence: API token
   - Trello: API key + token
   - Slack: Webhook URL

---

## 🔧 Implementace

### Template 1: Jira Ticket Approval

**Use Case:** Core Platform vytvoří external task → n8n vytvoří Jira ticket → čeká na approval → complete task

**n8n Workflow JSON:** `workflows/jira-ticket-approval.json`

```json
{
  "name": "Jira Ticket Approval",
  "nodes": [
    {
      "parameters": {
        "method": "GET",
        "url": "=https://admin.core-platform.local/api/n8n/external-tasks/poll",
        "authentication": "predefinedCredentialType",
        "nodeCredentialType": "httpBasicAuth",
        "options": {
          "timeout": 30000,
          "allowUnauthorizedCerts": true
        }
      },
      "name": "Poll External Tasks",
      "type": "n8n-nodes-base.httpRequest",
      "typeVersion": 4.2,
      "position": [250, 300],
      "id": "poll-tasks"
    },
    {
      "parameters": {
        "jsCode": "// Extract task from response\nconst task = $input.item.json;\n\nif (!task || !task.id) {\n  return [];\n}\n\nreturn {\n  taskId: task.id,\n  entityId: task.context.entityId,\n  entityType: task.context.entityType,\n  requestedBy: task.context.requestedBy\n};"
      },
      "name": "Parse Task",
      "type": "n8n-nodes-base.code",
      "typeVersion": 2,
      "position": [450, 300],
      "id": "parse-task"
    },
    {
      "parameters": {
        "resource": "issue",
        "operation": "create",
        "project": "={{ $json.entityType }}",
        "issueType": "Task",
        "summary": "=Approval Request for {{ $json.entityType }} #{{ $json.entityId }}",
        "description": "=Approval requested by: {{ $json.requestedBy }}\\n\\nPlease review and approve/reject.",
        "additionalFields": {
          "priority": "High"
        }
      },
      "name": "Create Jira Ticket",
      "type": "n8n-nodes-base.jira",
      "typeVersion": 1,
      "position": [650, 300],
      "credentials": {
        "jiraSoftwareCloudApi": {
          "id": "jira-credentials",
          "name": "Jira Cloud"
        }
      },
      "id": "create-jira"
    },
    {
      "parameters": {
        "method": "POST",
        "url": "=https://admin.core-platform.local/api/n8n/external-tasks/{{ $('Parse Task').item.json.taskId }}/complete",
        "authentication": "predefinedCredentialType",
        "nodeCredentialType": "httpBasicAuth",
        "sendBody": true,
        "bodyParameters": {
          "parameters": [
            {
              "name": "result",
              "value": "={\"jiraTicketKey\": \"{{ $json.key }}\", \"jiraTicketUrl\": \"{{ $json.self }}\"}"
            }
          ]
        }
      },
      "name": "Complete Task",
      "type": "n8n-nodes-base.httpRequest",
      "typeVersion": 4.2,
      "position": [850, 300],
      "id": "complete-task"
    },
    {
      "parameters": {
        "method": "POST",
        "url": "=https://admin.core-platform.local/api/n8n/external-tasks/{{ $('Parse Task').item.json.taskId }}/fail",
        "sendBody": true,
        "bodyParameters": {
          "parameters": [
            {
              "name": "error",
              "value": "={{ $json.error.message }}"
            }
          ]
        }
      },
      "name": "Fail Task on Error",
      "type": "n8n-nodes-base.httpRequest",
      "typeVersion": 4.2,
      "position": [850, 450],
      "onError": "continueRegularOutput",
      "id": "fail-task"
    }
  ],
  "connections": {
    "Poll External Tasks": {
      "main": [[{ "node": "Parse Task", "type": "main", "index": 0 }]]
    },
    "Parse Task": {
      "main": [[{ "node": "Create Jira Ticket", "type": "main", "index": 0 }]]
    },
    "Create Jira Ticket": {
      "main": [[{ "node": "Complete Task", "type": "main", "index": 0 }]]
    }
  },
  "settings": {
    "executionOrder": "v1",
    "errorWorkflow": "fail-task"
  },
  "staticData": null,
  "tags": ["external-task", "jira", "approval"]
}
```

---

### Template 2: Confluence Documentation Sync

**Use Case:** Webhook trigger → Update Confluence page

**n8n Workflow JSON:** `workflows/confluence-doc-sync.json`

```json
{
  "name": "Confluence Documentation Sync",
  "nodes": [
    {
      "parameters": {
        "httpMethod": "POST",
        "path": "confluence-sync",
        "responseMode": "onReceived"
      },
      "name": "Webhook Trigger",
      "type": "n8n-nodes-base.webhook",
      "typeVersion": 1.1,
      "position": [250, 300],
      "webhookId": "confluence-sync",
      "id": "webhook"
    },
    {
      "parameters": {
        "resource": "page",
        "operation": "update",
        "spaceKey": "={{ $json.spaceKey }}",
        "pageId": "={{ $json.pageId }}",
        "title": "={{ $json.title }}",
        "body": "={{ $json.content }}"
      },
      "name": "Update Confluence Page",
      "type": "n8n-nodes-base.confluence",
      "typeVersion": 1,
      "position": [450, 300],
      "credentials": {
        "confluenceApi": {
          "id": "confluence-credentials",
          "name": "Confluence Cloud"
        }
      },
      "id": "update-confluence"
    }
  ],
  "connections": {
    "Webhook Trigger": {
      "main": [[{ "node": "Update Confluence Page", "type": "main", "index": 0 }]]
    }
  },
  "tags": ["webhook", "confluence", "documentation"]
}
```

---

### Template 3: Trello Card Creation

**n8n Workflow JSON:** `workflows/trello-card-creation.json`

```json
{
  "name": "Trello Card Creation",
  "nodes": [
    {
      "parameters": {
        "method": "GET",
        "url": "https://admin.core-platform.local/api/n8n/external-tasks/poll"
      },
      "name": "Poll Tasks",
      "type": "n8n-nodes-base.httpRequest",
      "typeVersion": 4.2,
      "position": [250, 300]
    },
    {
      "parameters": {
        "resource": "card",
        "operation": "create",
        "listId": "={{ $json.context.listId }}",
        "name": "={{ $json.context.cardTitle }}",
        "description": "={{ $json.context.cardDescription }}"
      },
      "name": "Create Trello Card",
      "type": "n8n-nodes-base.trello",
      "typeVersion": 1,
      "position": [450, 300],
      "credentials": {
        "trelloApi": {
          "id": "trello-credentials"
        }
      }
    },
    {
      "parameters": {
        "method": "POST",
        "url": "=https://admin.core-platform.local/api/n8n/external-tasks/{{ $('Poll Tasks').item.json.id }}/complete",
        "sendBody": true,
        "bodyParameters": {
          "parameters": [
            { "name": "result", "value": "={\"trelloCardId\": \"{{ $json.id }}\", \"trelloCardUrl\": \"{{ $json.url }}\"}" }
          ]
        }
      },
      "name": "Complete Task",
      "type": "n8n-nodes-base.httpRequest",
      "typeVersion": 4.2,
      "position": [650, 300]
    }
  ],
  "connections": {
    "Poll Tasks": {
      "main": [[{ "node": "Create Trello Card", "type": "main", "index": 0 }]]
    },
    "Create Trello Card": {
      "main": [[{ "node": "Complete Task", "type": "main", "index": 0 }]]
    }
  }
}
```

---

### Template 4: Slack Notification

**n8n Workflow JSON:** `workflows/slack-notification.json`

```json
{
  "name": "Slack Notification",
  "nodes": [
    {
      "parameters": {
        "method": "GET",
        "url": "https://admin.core-platform.local/api/n8n/external-tasks/poll"
      },
      "name": "Poll Tasks",
      "type": "n8n-nodes-base.httpRequest",
      "typeVersion": 4.2,
      "position": [250, 300]
    },
    {
      "parameters": {
        "resource": "message",
        "operation": "post",
        "channel": "={{ $json.context.slackChannel }}",
        "text": "={{ $json.context.message }}",
        "attachments": []
      },
      "name": "Send Slack Message",
      "type": "n8n-nodes-base.slack",
      "typeVersion": 2.1,
      "position": [450, 300],
      "credentials": {
        "slackApi": {
          "id": "slack-credentials"
        }
      }
    },
    {
      "parameters": {
        "method": "POST",
        "url": "=https://admin.core-platform.local/api/n8n/external-tasks/{{ $('Poll Tasks').item.json.id }}/complete",
        "sendBody": true,
        "bodyParameters": {
          "parameters": [
            { "name": "result", "value": "={\"slackMessageTs\": \"{{ $json.ts }}\"}" }
          ]
        }
      },
      "name": "Complete Task",
      "type": "n8n-nodes-base.httpRequest",
      "typeVersion": 4.2,
      "position": [650, 300]
    }
  ],
  "connections": {
    "Poll Tasks": { "main": [[{ "node": "Send Slack Message" }]] },
    "Send Slack Message": { "main": [[{ "node": "Complete Task" }]] }
  }
}
```

---

### Import Script

**File:** `scripts/n8n-import-templates.sh`

```bash
#!/bin/bash
set -e

N8N_API="${N8N_API:-https://admin.core-platform.local/n8n/api/v1}"
N8N_API_KEY="${N8N_API_KEY:-your-api-key}"

echo "📥 Importing n8n workflow templates..."

for workflow in workflows/*.json; do
  WORKFLOW_NAME=$(basename "$workflow" .json)
  echo "Importing: $WORKFLOW_NAME"
  
  curl -k -X POST "${N8N_API}/workflows" \
    -H "X-N8N-API-KEY: ${N8N_API_KEY}" \
    -H "Content-Type: application/json" \
    -d "@${workflow}"
  
  echo "✅ $WORKFLOW_NAME imported"
done

echo "🎉 All templates imported!"
```

---

## ✅ Acceptance Criteria

1. **Templates:**
   - [ ] 4 workflow JSON files created
   - [ ] Each template tested in n8n UI
   - [ ] Documentation for each template

2. **Integration:**
   - [ ] Jira approval workflow creates ticket + completes task
   - [ ] Confluence sync updates page
   - [ ] Trello creates card + returns URL
   - [ ] Slack sends message

3. **Import:**
   - [ ] `n8n-import-templates.sh` successfully imports all workflows
   - [ ] Workflows visible in n8n UI

---

**Related Stories:**
- WF15: EXTERNAL_TASK Executor (polling API)
- N8N6: BFF API (external task endpoints)
