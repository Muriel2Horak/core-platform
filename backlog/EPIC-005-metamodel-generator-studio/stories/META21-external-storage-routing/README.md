# META-021: External Storage Routing

**EPIC:** [EPIC-005: Metamodel Generator & Studio](../README.md)  
**Status:** 🟡 **PLANNED**  
**Priorita:** P2 (Medium)  
**Estimated LOC:** ~1,000 řádků  
**Effort:** 2 týdny (80 hodin)

---

## 📋 Story Description

Storage type runtime (relational/log/external), external REST connector, API gateway integration, n8n workflow integration.

---

## 🎯 Acceptance Criteria

### AC1: External Storage Type

```yaml
entity: ExternalOrder
storageType: external
externalSource:
  type: REST
  baseUrl: "https://api.external-system.com"
  auth:
    type: OAuth2
    tokenUrl: "https://auth.external-system.com/token"
  
  endpoints:
    list: "/orders"
    get: "/orders/{id}"
    create: "/orders"
```

### AC2: Runtime Routing

- **GIVEN** entity s `storageType: external`
- **WHEN** volám `GET /api/external-orders`
- **THEN**:
  - Backend proxyuje request na `https://api.external-system.com/orders`
  - OAuth2 token se automaticky přidá
  - Response se transformuje podle field mappings

### AC3: n8n Workflow Integration

```yaml
externalSource:
  type: n8n
  workflowId: "abc123"
  webhookUrl: "https://n8n.company.com/webhook/orders"
```

---

**Story Owner:** Backend Team  
**Priority:** P2  
**Effort:** 2 týdny
