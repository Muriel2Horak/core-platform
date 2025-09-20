# 🎯 AKTIVACE KEYCLOAK MATERIAL THEME

## 1. Spusťte Docker stack
```bash
cd /Users/martinhorak/Projects/core-platform/docker
../scripts/docker/up.sh
```

## 2. Otevřete Keycloak Admin Console  
https://core-platform.local/admin/

## 3. Přihlaste se jako admin
- Username: admin
- Password: admin

## 4. Konfigurujte realm core-platform
1. Vyberte realm "core-platform" (vlevo nahoře)
2. Jděte do **Realm Settings** → **Themes**  
3. Nastavte:
   - **Login Theme**: core-material
   - **Account Theme**: core-material  
4. Klikněte **Save**

## 5. Testování
- Login: https://core-platform.local/realms/core-platform/protocol/openid-connect/auth?client_id=web&response_type=code&redirect_uri=https://core-platform.local
- Account: https://core-platform.local/realms/core-platform/account/

## ✅ Očekávaný výsledek:
- Material Design login formulář s outlined text fields
- Stejné barvy jako frontend aplikace
- Plus Jakarta Sans font
- Responsivní design pro mobil i desktop
- Material UI buttons s ripple efektem

## 🐛 Troubleshooting:
- Pokud theme není vidět: Restartujte Keycloak container
- CSS chyby: Zkontrolujte DevTools → Network tab
- 404 na resources: Ověřte volume mount v docker-compose.yml