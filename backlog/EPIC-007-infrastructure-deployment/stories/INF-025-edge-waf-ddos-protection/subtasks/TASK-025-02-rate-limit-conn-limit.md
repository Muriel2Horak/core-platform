# TASK-025-02: Rate limiting + connection limiting

## 🎯 Goal
Zavest limit_req a limit_conn pro kriticke endpointy.

## 📋 Tasks
- [ ] Definovat limity pro /auth/login, /api a /n8n.
- [ ] Implementovat per-IP/per-token limity v Nginx.
- [ ] Pridat allowlist pro internal/admin IPs.
- [ ] Zdokumentovat limity a dopady.

## 📤 Output
- Nginx rate limit a connection limit pravidla.
- Dokumentovane limity a allowlist.

## ✅ Acceptance Criteria for This Subtask
- [ ] Prekroceni limitu vraci 429.
- [ ] Allowlist bypassuje limity pro vybrane IP.
