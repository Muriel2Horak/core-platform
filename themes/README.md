# Core Platform Themes

Kolekce custom theme pro Keycloak, které odpovídají designu Core Platform aplikace.

## 🎨 Dostupné Themes

### Core Material Theme
- **Lokace**: `themes/core-material/`
- **Technologie**: React + TypeScript + Material UI
- **Kompatibilita**: Keycloak 26.x
- **Funkce**: Login, Reset Password, Account Management

## 🚀 Rychlé nasazení

### 1. Build theme
```bash
cd themes/core-material
npm install
npm run build
# nebo použijte build script:
./build.sh
```

### 2. Spusťte Docker stack
```bash
cd docker
./scripts/docker/up.sh
```

### 3. Konfigurujte Keycloak
1. Otevřete https://core-platform.local/admin/
2. Přihlašte se jako admin/admin
3. Vyberte realm `core-platform`
4. Jděte do **Realm Settings** → **Themes**
5. Nastavte:
   - **Login Theme**: `core-material`
   - **Account Theme**: `core-material`
6. Klikněte **Save**

## 🎯 Výsledek

Po konfiguraci budou všechny Keycloak stránky používat Material UI design stejný jako frontend aplikace:

- **Login stránka**: Moderní formulář s MUI komponenty
- **Reset hesla**: Stylový formulář pro obnovení přístupu  
- **Můj účet**: Pokročilé rozhraní pro správu profilu
- **Responsivní design**: Optimalizováno pro všechna zařízení

## 🔧 Development

Pro vývoj theme použijte development server:
```bash
cd themes/core-material
npm run dev
```

Server bude dostupný na `http://localhost:3002` pro preview komponent.

## 📝 Poznámky

- Theme se automaticky mountuje do Keycloak containeru přes Docker volume
- Podporuje češtinu a angličtinu
- Hot-reload je povolen v development módu
- Všechny MUI komponenty používají stejné barvy a typografii jako frontend