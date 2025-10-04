# 🎨 Core Platform - Frontend UX Guidelines

## 📋 Přehled

Tento dokument definuje design systém Core Platform s důrazem na konzistentní uživatelské rozhraní, přístupnost a moderní Material Design 3 principy.

---

## 🎨 Barvy

### Primární paleta
- **Primary**: `#667eea` (hlavní akční barva)
- **Primary Light**: `#8fa3f3` (hover stavy)
- **Primary Dark**: `#4d68d1` (aktivní stavy)

### Sekundární paleta  
- **Secondary**: `#764ba2` (podpůrná barva)
- **Secondary Light**: `#9575b8`
- **Secondary Dark**: `#5d3a82`

### Neutralní barvy
- **Grey 50**: `#fafafa` (pozadí)
- **Grey 100**: `#f5f5f5` (kontejnery)
- **Grey 200**: `#eeeeee` (borders)
- **Grey 400**: `#bdbdbd` (disabled)
- **Grey 600**: `#757575` (secondary text)
- **Grey 900**: `#212121` (primary text)

### Feedback barvy
- **Success**: `#4caf50` (úspěch, dokončeno)
- **Warning**: `#ff9800` (upozornění, čeká)
- **Error**: `#f44336` (chyba, neúspěch)
- **Info**: `#2196f3` (informace, neutrální)

### Kontrastní požadavky
- **WCAG AA**: Minimální kontrast 4.5:1 pro normální text
- **WCAG AA**: Minimální kontrast 3:1 pro velký text (18pt+)
- Všechny barvy prošly validací pomocí WebAIM Contrast Checker

---

## ✍️ Typografie

### Font Family
- **Primary**: `Plus Jakarta Sans` (Google Fonts)
- **Monospace**: `Fira Code, Monaco, Consolas` (kód, ID)

### Škála velikostí
- **H1**: 48px / 3rem, weight 700, line-height 1.2
- **H2**: 36px / 2.25rem, weight 600, line-height 1.3
- **H3**: 30px / 1.875rem, weight 600, line-height 1.4
- **H4**: 24px / 1.5rem, weight 600, line-height 1.4
- **H5**: 20px / 1.25rem, weight 600, line-height 1.5
- **H6**: 16px / 1rem, weight 600, line-height 1.5
- **Body1**: 16px / 1rem, weight 400, line-height 1.6
- **Body2**: 14px / 0.875rem, weight 400, line-height 1.6
- **Caption**: 12px / 0.75rem, weight 400, line-height 1.4

### Responsive breakpointy
- **xs**: 0-600px (mobily)
- **sm**: 600-900px (tablety)
- **md**: 900-1200px (menší desktop)
- **lg**: 1200px+ (desktop)

---

## 📐 Layout & Grid

### 12-column grid systém
- **Container max-width**: 1200px
- **Gutters**: 24px (desktop), 16px (tablet), 8px (mobile)
- **Columns**: 12 (flexibilní)

### Spacing škála (8px base)
- **xs**: 4px (0.25rem)
- **sm**: 8px (0.5rem)
- **md**: 16px (1rem)
- **lg**: 24px (1.5rem)
- **xl**: 32px (2rem)
- **xxl**: 48px (3rem)

### Layout pravidla
- **Maximálně 2-3 sloupce** na desktop pro lepší čitelnost
- **Vertikální rytmus**: 8px grid pro konzistentní mezery
- **Content max-width**: 800px pro textové obsahy
- **Sidebar width**: 280px (desktop), collapse na mobile

---

## 🎛️ Ovládací prvky

### Tlačítka
```tsx
// Primary - hlavní akce
<AppButton variant="primary">Uložit</AppButton>

// Secondary - vedlejší akce  
<AppButton variant="secondary">Zrušit</AppButton>

// Danger - destruktivní akce
<AppButton variant="danger">Smazat</AppButton>
```

### Velikosti tlačítek
- **Small**: 32px výška, 12px padding
- **Medium**: 40px výška, 16px padding (default)
- **Large**: 48px výška, 24px padding

### Stavy tlačítek
- **Default**: základní stav
- **Hover**: +5% světlost, subtle shadow
- **Active**: -5% světlost
- **Disabled**: 40% opacity, no interaction
- **Loading**: spinner + disabled stav

---

## 🔄 Interakce & stavy

### Loading stavy
```tsx
// Page level loading
<Loader variant="page" />

// Inline loading  
<CircularProgress size={20} />

// Button loading
<AppButton loading>Ukládám...</AppButton>
```

### Empty stavy
```tsx
<EmptyState 
  icon={<PersonIcon />}
  title="Žádní uživatelé"
  description="Zatím nebyli vytvořeni žádní uživatelé"
  action={<AppButton>Přidat uživatele</AppButton>}
/>
```

### Error stavy
- **Inline errors**: Pod formulářovými poli
- **Page errors**: Alert komponenta nahoře
- **Toast notifications**: Pro feedback akcí

### Animace
- **Duration**: 200-300ms pro micro-interakce
- **Easing**: `cubic-bezier(0.4, 0, 0.2, 1)` Material Design
- **Hover transitions**: všechny interaktivní prvky
- **Page transitions**: Fade/Slide s 300ms

---

## ♿ Přístupnost (A11Y)

### ARIA pravidla
- **role**: správné sémantické role
- **aria-label**: popisky pro screen readery
- **aria-describedby**: propojení s pomocnými texty
- **aria-expanded**: stav rozbalovacích menu

### Klávesnicová navigace
- **Tab order**: logické pořadí focusu
- **Enter/Space**: aktivace tlačítek
- **Arrow keys**: navigace v menu/tabulkách
- **Escape**: zavření dialogů/menu

### Focus management
```css
/* Focus ring - NIKDY neodstraňovat */
:focus-visible {
  outline: 2px solid #667eea;
  outline-offset: 2px;
  border-radius: 4px;
}
```

### Barvy a kontrast
- **Barva NENÍ jediný indikátor** významu
- **Text + ikony** pro rozlišení stavů
- **Patterns/shapes** jako dodatečné rozlišení
- **High contrast mode** support

---

## 👤 Specifika Profil stránky

### Readonly pole
- **Username**: vždy readonly, grey pozadí
- **Tenant**: vždy readonly, grey pozadí
- **Styling**: `backgroundColor: 'grey.50'`, cursor: 'not-allowed'

### Grid layout (12 columns)
```
┌─────────────────────────────────────┐
│ Jméno (6)        │ Příjmení (6)     │
├─────────────────┼──────────────────┤
│ Email (6)        │ Username (3)     │ Tenant (3) │
└─────────────────────────────────────┘
```

### Prázdné hodnoty
- **Zobrazení**: `—` (em dash) místo prázdného pole
- **Styling**: `color: 'text.secondary'`, `fontStyle: 'italic'`
- **Accessibility**: `aria-label="Hodnota není uvedena"`

### Tab navigace
1. **Základní údaje** - jméno, příjmení, email, username, tenant
2. **Organizační struktura** - oddělení, pozice, nadřízený  
3. **Zástupy** - zástupce, období, důvod
4. **Bezpečnost** - role, změna hesla

---

## 🗂️ Sidebar navigace

### Hlavní sekce (pevné)
```tsx
const menuItems = [
  { label: 'Dashboard', icon: <DashboardIcon />, path: '/' },
  { label: 'Uživatelé', icon: <PeopleIcon />, path: '/users' },
  { label: 'Role', icon: <SecurityIcon />, path: '/roles' },
  { label: 'Správa', icon: <SettingsIcon />, path: '/admin' }
];
```

### Tenant & User info
- **Top**: Core Platform logo + tenant name z JWT
- **Bottom**: Avatar + jméno + email z user contextu
- **Tenant parsing**: `/realms/([^/]+)` regex z JWT issuer
- **Fallback**: "Unknown Tenant" pouze při parsing chybě

### Responsive chování
- **Desktop**: 280px fixed sidebar
- **Mobile**: Collapsible drawer overlay
- **Breakpoint**: md (900px)

---

## 🧪 Implementační poznámky

### CSS-in-JS (Emotion)
```tsx
// Preferred styling approach
const StyledComponent = styled(Box)(({ theme }) => ({
  backgroundColor: theme.palette.primary.main,
  padding: theme.spacing(2),
  borderRadius: theme.shape.borderRadius,
}));
```

### Theme tokeny
- **Všechny hodnoty** definované v `tokens.ts`
- **MUI theme** vytvořen z tokens
- **Consistency** napříč komponentami

### TypeScript strict mode
- **Všechny komponenty** typované
- **Props interfaces** exportované
- **No any types** povolené

---

## ✅ Checklist implementace

### Design tokens ✅
- [ ] `tokens.ts` - barvy, spacing, typography
- [ ] `theme.ts` - MUI theme z tokens
- [ ] `GlobalStyles.tsx` - reset + focus styles

### UI komponenty ✅  
- [ ] `AppButton` - styled MUI Button
- [ ] `FormField` - controlled TextField wrapper
- [ ] `PageHeader` - title + actions
- [ ] `EmptyState` - empty content
- [ ] `Loader` - loading states
- [ ] `SidebarNav` - navigation menu

### Stránky ✅
- [ ] Profile page refactor dle grid specifikace
- [ ] Sidebar integration s tenant info
- [ ] A11y lint rules zapnuté

### Testing ✅
- [ ] RTL testy pro komponenty
- [ ] A11y automated testing
- [ ] Snapshot testy pro UI consistency

---

## 📚 Externí zdroje

- [Material Design 3](https://m3.material.io/)
- [WCAG 2.1 Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)
- [Plus Jakarta Sans Font](https://fonts.google.com/specimen/Plus+Jakarta+Sans)
- [WebAIM Contrast Checker](https://webaim.org/resources/contrastchecker/)

---

*Dokument verze 1.0 | Poslední aktualizace: 3. října 2025*