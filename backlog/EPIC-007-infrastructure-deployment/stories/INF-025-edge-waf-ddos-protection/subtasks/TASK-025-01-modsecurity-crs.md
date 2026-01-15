# TASK-025-01: ModSecurity + OWASP CRS integrace

## 🎯 Goal
Nasadit ModSecurity s OWASP CRS do Nginx edge proxy.

## 📋 Tasks
- [ ] Zvolit image/modul pro ModSecurity + CRS (docker).
- [ ] Pridat WAF include do Nginx configu pro /api, /auth, /n8n.
- [ ] Nastavit zakladni CRS konfiguraci a example exclusions.
- [ ] Zajistit prepinani detection vs blocking modu.

## 📤 Output
- Nginx konfigurace s WAF a CRS pravidly.
- Konfigurovatelny WAF mode (detection/blocking).

## ✅ Acceptance Criteria for This Subtask
- [ ] Nginx startuje s ModSecurity bez chyb.
- [ ] Testovaci SQLi/XSS request je detekovan.
- [ ] WAF mode lze prepnout bez rebuild.
