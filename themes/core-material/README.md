# Core Material Theme pro Keycloak 26.x

Material Design téma pro Keycloak s podporou všech oblastí (login, account, email).

## 🎨 Funkce

- **React + TypeScript**: Moderní React komponenty s TypeScript podporou
- **Material UI**: Stejný design systém jako frontend aplikace
- **Responsivní design**: Optimalizováno pro desktop i mobilní zařízení
- **Keycloak 26.x kompatibilita**: Plně kompatibilní s nejnovější verzí Keycloak
- **Čeština + Angličtina**: Vícejazyčná podpora

## 🏗️ Komponenty

### Login Theme
- **Login.tsx**: Přihlašovací formulář s MUI TextField a Button
- **ResetPassword.tsx**: Formulář pro reset hesla
- **LoginUpdatePassword.tsx**: Aktualizace hesla po přihlášení

### Account Theme  
- **Account.tsx**: Úprava uživatelského profilu s tab navigací
- Osobní údaje (jméno, příjmení, email)
- Změna hesla s validací

## 🚀 Instalace a Build

### 1. Instalace závislostí
```bash
cd themes/core-material
npm install
```

### 2. Development server
```bash
npm run dev
```
Server poběží na `http://localhost:3002`

### 3. Build pro produkci
```bash
npm run build
```

Výstup bude v `dist/` složce s Keycloak kompatibilní strukturou:
```
dist/
├── theme.properties
├── resources/
│   ├── css/
│   │   ├── login-entry.css
│   │   └── account-entry.css
│   └── js/
│       ├── login-entry.js
│       └── account-entry.js
```

## 🐳 Docker Integrace

### Automatické nasazení
Theme se automaticky nasadí do Keycloak při buildu Docker containeru.

V `docker-compose.yml` přidejte volume mount:
```yaml
services:
  keycloak:
    volumes:
      - ../themes/core-material/dist:/opt/keycloak/themes/core-material
```

### Aktivace theme v Keycloak
1. Přihlaste se do Keycloak Admin Console
2. Vyberte realm `core-platform`
3. Jděte do **Realm Settings** → **Themes**
4. Nastavte:
   - **Login Theme**: `core-material`
   - **Account Theme**: `core-material`
5. Klikněte **Save**

## 🎯 Customizace

### Theme Colors
Upravte barvy v `src/theme.ts`:
```typescript
palette: {
  primary: {
    main: '#5D87FF',  // Hlavní barva
    light: '#ECF2FF',
    dark: '#4570EA',
  },
  // ... další barvy
}
```

### Fonts
Theme používá **Plus Jakarta Sans** font stejně jako frontend aplikace.
Font se načítá z Google Fonts v HTML templates.

### Komponenty
Všechny komponenty jsou v `src/` složce:
- `Login.tsx` - Login formulář
- `ResetPassword.tsx` - Reset hesla  
- `Account.tsx` - Uživatelský účet
- `theme.ts` - MUI theme definice

## 📱 Responsive Design

Theme je plně responsivní s breakpointy:
- **xs**: 0-600px (mobily)
- **sm**: 600-900px (tablety)
- **md**: 900-1200px (menší desktop)
- **lg**: 1200px+ (desktop)

## 🔧 Development

### Hot Reload
V development módu (`npm run dev`) se změny automaticky načítají.

### TypeScript
Projekt má plnou TypeScript podporu s strict módem.

### Props Interface
Každá komponenta má definované TypeScript interface pro Keycloak props:
```typescript
interface LoginProps {
  url?: {
    loginAction?: string;
    registrationUrl?: string;
    loginResetCredentialsUrl?: string;
  };
  realm?: {
    displayName?: string;
  };
  // ... další props
}
```

## 🌐 Lokalizace

Theme podporuje češtinu a angličtinu. V `theme.properties`:
```properties
locales=cs,en
```

## 📝 Poznámky

- Theme je optimalizován pro Keycloak 26.x
- Všechny MUI komponenty používají stejné styly jako frontend aplikace
- Build proces generuje ES2015 kompatibilní kód pro širší podporu prohlížečů
- CSS a JS soubory jsou automaticky minifikované v produkčním buildu

## 🚨 Troubleshooting

### Theme se nezobrazuje
1. Zkontrolujte, že je theme správně nakopírován do `/opt/keycloak/themes/core-material`
2. Restartujte Keycloak service
3. Vyčistěte browser cache

### Styling problémy
1. Zkontrolujte, že se CSS soubory načítají správně
2. Otevřete Developer Tools a zkontrolujte Console za chyby
3. Porovnejte s frontend aplikací v `src/theme/DefaultColors.js`

## Struktura složek

```
themes/core-material/
├── theme.properties          # Hlavní konfigurace tématu
├── login/                    # Login stránky (přihlašování)
│   ├── theme.properties      # parent=keycloak.v2
│   ├── login.ftl
│   ├── register.ftl
│   └── resources/
│       └── css/
│           └── styles.css    # Material Design styly pro login
├── account/                  # Account Console v3
│   ├── theme.properties      # parent=keycloak.v3
│   ├── index.ftl
│   ├── template.ftl
│   └── resources/
│       └── css/
│           └── account.css   # Material Design styly pro account
└── email/                    # Email šablony
    ├── theme.properties      # parent=base
    └── html/                 # Email HTML šablony
```

## Parent témata pro Keycloak 26.x

Každá oblast používá jiné parent téma:

- **Login**: `parent=keycloak.v2` - pro přihlašovací stránky
- **Account**: `parent=keycloak.v3` - pro novou Account Console v3
- **Email**: `parent=base` - pro základní email šablony

## Nastavení tématu v Admin Console

1. Přihlaste se do Keycloak Admin Console: `https://core-platform.local/admin`
2. Vyberte realm `core-platform`
3. Přejděte na **Realm Settings** → **Themes**
4. Nastavte témata pro jednotlivé oblasti:
   - **Login Theme**: `core-material`
   - **Account Theme**: `core-material` 
   - **Email Theme**: `core-material`
5. Klikněte na **Save**

## Aktivace tématu

Téma se automaticky aktivuje při startu Keycloak kontejneru díky environment proměnným:

```yaml
environment:
  KC_THEME_DEFAULT: "core-material"
  KC_SPI_THEME_DEFAULT: "core-material"
```

## Development režim

Pro vývoj tématu jsou v docker-compose.yml nastavené proměnné pro hot-reload:

```yaml
environment:
  KC_SPI_THEME_STATIC_MAX_AGE: -1
  KC_SPI_THEME_CACHE_THEMES: "false"
  KC_SPI_THEME_CACHE_TEMPLATES: "false"
```

## Material Design prvky

Téma používá:
- **Font**: Plus Jakarta Sans (stejný jako frontend)
- **Barvy**: Core Platform barevná paleta (primární #5D87FF, sekundární #49BEFF)
- **Komponenty**: Material Design 3 buttons, cards, forms
- **Ikony**: Material Icons
- **Responzivní design**: Mobile-first approach

## Build a deployment

Téma je součástí Keycloak Docker image:

```dockerfile
FROM quay.io/keycloak/keycloak:26.2.0
COPY themes/core-material /opt/keycloak/themes/core-material
```

Pro rebuild tématu:

```bash
cd docker/
docker-compose stop keycloak
docker-compose rm -f keycloak
docker-compose build keycloak --no-cache
docker-compose up -d keycloak
```

## Ověření funkčnosti

Po aktivaci tématu zkontrolujte:

- **Login**: `https://core-platform.local/realms/core-platform/protocol/openid-connect/auth?client_id=web&response_type=code&redirect_uri=https://core-platform.local`
- **Account**: `https://core-platform.local/realms/core-platform/account/`

Obě stránky by měly vracet HTTP 302 (přesměrování) a používat Material Design styling.