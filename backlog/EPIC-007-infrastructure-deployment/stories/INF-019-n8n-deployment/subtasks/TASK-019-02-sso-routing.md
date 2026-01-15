# TASK-019-02: SSO + routing (Keycloak + Nginx)

## Goal
Zajistit prihlaseni pres Keycloak a routovani na n8n subdomenu.

## Tasks
- [ ] Vytvorit Keycloak client pro n8n (redirect URIs, logout URL, scopes).
- [ ] Nastavit OAuth2/SSO promennne v n8n service (authorization URL, token URL, client id/secret).
- [ ] Pridat routovani pro `workflows.${DOMAIN}` v Nginx/Traefik (TLS + proxy headers).
- [ ] Nastavit callback URL a otestovat login flow.
- [ ] Zapsat konfiguraci do docs/runbooku pro n8n.

## Output
- Keycloak SSO pro n8n.
- Subdomain routing na n8n s platnym TLS.

## Acceptance Criteria for This Subtask
- [ ] Pri pristupu na `https://workflows.${DOMAIN}` probiha login pres Keycloak.
- [ ] Po uspesnem loginu je pristup do n8n UI bez basic auth.
- [ ] Callback/redirect URL je validni a bez smycek.
- [ ] Routing funguje i po restartu stacku.
