# 🎨 Core Platform Design System

Kompletní design systém implementující UX best practices a Material Design principy.

## 🌈 1. Barevnost

### Brand Paleta
```javascript
primary: '#1976d2'    // Hlavní brand barva (modrá)
secondary: '#9c27b0'  // Doplňková barva (fialová)
```

### Neutralní Paleta (4 odstíny)
```javascript
neutral: {
  50: '#fafafa',   // Pozadí
  200: '#e0e0e0',  // Borders, dividers
  600: '#757575',  // Sekundární text
  900: '#212121'   // Hlavní text
}
```

### Feedback Barvy (smysluplné použití)
```javascript
✅ success: '#2e7d32'   // Zelená - úspěch
⚠️ warning: '#f57c00'   // Oranžová - varování  
❌ error: '#d32f2f'     // Červená - chyba
ℹ️ info: '#1976d2'      // Modrá - informace
```

### WCAG AA Compliance
- Všechny barevné kombinace mají kontrast min. 4.5:1
- Barva není jediný nositel významu (kombinace s ikonami/textem)

## 🔤 2. Typografie

### Font Stack (max 2 fonty)
```javascript
// Hlavní font pro UI
fontFamily: "'Inter', 'Roboto', -apple-system, BlinkMacSystemFont, sans-serif"

// Monospace pro kód/logy
code: "'JetBrains Mono', 'Fira Code', 'Consolas', monospace"
```

### Hierarchie
```javascript
H1: 2.5rem  // Název stránky
H2: 2rem    // Hlavní sekce  
H3: 1.5rem  // Podsekce
Body: 1rem  // Hlavní text (16px)
Small: 0.875rem // Menší text (14px)
Caption: 0.75rem // Labels (12px)
```

### Pravidla
- ❌ Žádné CAPS LOCK na celé věty
- ✅ Řádkování 1.4-1.6× velikosti písma
- ✅ Letter-spacing pro lepší čitelnost

## 🖼️ 3. Layout System

### Grid Systém (12 columns)
```jsx
<ResponsiveGrid columns={{ xs: 1, sm: 2, md: 3 }}>
  <ContentCard>Obsah 1</ContentCard>
  <ContentCard>Obsah 2</ContentCard>
  <ContentCard>Obsah 3</ContentCard>
</ResponsiveGrid>
```

### Spacing Scale (4/8/16px)
```javascript
spacing: {
  xs: 4px,   // Malé mezery
  sm: 8px,   // Základní mezery
  md: 16px,  // Střední mezery
  lg: 24px,  // Velké mezery
  xl: 32px,  // Extra velké
  xxl: 48px  // Maximální
}
```

### Responsivita
- 📱 Mobile: <768px
- 📱 Tablet: 768-1279px  
- 💻 Desktop: ≥1280px

## 🖱️ 4. Komponenty

### Tlačítka (konzistentní podle typu akce)

```jsx
// Primární akce - výrazné
<PrimaryButton>Uložit</PrimaryButton>

// Sekundární akce - méně výrazné
<SecondaryButton>Zrušit</SecondaryButton>

// Destruktivní akce - červené
<DestructiveButton>Smazat</DestructiveButton>
```

### Formuláře (UX pravidla)

```jsx
<FormField 
  label="E-mail"           // Label vždy nahoře, aligned left
  placeholder="jan@email.cz" // Placeholder ≠ Label
  required                 // Povinné pole označeno *
  error="Neplatný e-mail"  // Error pod polem, červeně
/>
```

### Layout Komponenty

```jsx
<PageContainer>
  <PageHeader 
    title="Název stránky"           // H1
    subtitle="Popis stránky"
    breadcrumbs={[                  // Breadcrumby pro hierarchii
      { label: 'Domů', href: '/' },
      { label: 'Sekce', href: '/section' },
      { label: 'Aktuální stránka' }
    ]}
    actions={[
      <PrimaryButton>Nová akce</PrimaryButton>
    ]}
  />
  
  <ContentCard title="Obsah">
    <Typography variant="body1">
      Hlavní obsah stránky...
    </Typography>
  </ContentCard>
</PageContainer>
```

## 🔄 5. Interakce a Stavy

### Loading States
```jsx
// Skeleton loading - ne prázdná stránka
<LoadingSkeleton variant="text" count={3} />
<LoadingSkeleton variant="rectangular" height={200} />

// Spinner s popisem
<LoadingSpinner message="Načítám data..." />
```

### Feedback System
```jsx
<SuccessAlert>✅ Data byla úspěšně uložena</SuccessAlert>
<WarningAlert>⚠️ Některá pole nejsou vyplněna</WarningAlert>
<ErrorAlert>❌ Chyba při ukládání dat</ErrorAlert>
<InfoAlert>ℹ️ Nová funkce je k dispozici</InfoAlert>
```

### Empty States
```jsx
<EmptyState
  title="Žádná data"
  description="Zatím zde nejsou žádné záznamy"
  action={<PrimaryButton>Přidat první záznam</PrimaryButton>}
/>
```

## ♿ 6. Přístupnost (A11y)

### Focus Management
- ✅ Focus outline nikdy neskrývat
- ✅ Klávesová navigace (TabIndex)
- ✅ ARIA labely pro custom komponenty

```jsx
<AccessibleButton 
  ariaLabel="Zavřít dialog"
  tooltip="Zavře aktuální dialog"
>
  ×
</AccessibleButton>
```

### Accessible Forms
```jsx
<FormField 
  label="Heslo"
  type="password"
  required
  aria-describedby="password-help"
  helperText="Minimálně 8 znaků"
/>
```

## 📐 7. Praktické Příklady

### Typická stránka
```jsx
function UserListPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);

  return (
    <PageContainer>
      <PageHeader 
        title="Správa uživatelů"
        subtitle="Spravujte uživatelské účty"
        breadcrumbs={[
          { label: 'Domů', href: '/' },
          { label: 'Uživatelé' }
        ]}
        actions={[
          <PrimaryButton>Přidat uživatele</PrimaryButton>
        ]}
      />
      
      {loading ? (
        <LoadingSkeleton variant="rectangular" height={400} />
      ) : users.length === 0 ? (
        <EmptyState
          title="Žádní uživatelé"
          description="Začněte přidáním prvního uživatele"
          action={<PrimaryButton>Přidat uživatele</PrimaryButton>}
        />
      ) : (
        <ContentCard>
          <DataTable columns={columns} data={users} />
        </ContentCard>
      )}
    </PageContainer>
  );
}
```

### Formulář
```jsx
function UserForm() {
  return (
    <ContentCard title="Nový uživatel">
      <FormField 
        label="Jméno"
        placeholder="Zadejte jméno"
        required
      />
      <FormField 
        label="E-mail"
        type="email"
        placeholder="jan@example.com"
        required
      />
      <FormField 
        label="Role"
        select
        helperText="Vyberte roli uživatele"
      />
      
      <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
        <SecondaryButton>Zrušit</SecondaryButton>
        <PrimaryButton>Uložit</PrimaryButton>
      </Box>
    </ContentCard>
  );
}
```

## 🚀 8. Implementace

### Import Design Systému
```jsx
import {
  PrimaryButton,
  SecondaryButton,
  FormField,
  ContentCard,
  PageContainer,
  PageHeader
} from '../components/DesignSystem';
```

### Theme Provider
```jsx
import { ThemeProvider } from '@mui/material/styles';
import { coreMaterialTheme } from './styles/theme';

function App() {
  return (
    <ThemeProvider theme={coreMaterialTheme}>
      <CssBaseline />
      <YourApp />
    </ThemeProvider>
  );
}
```

## ✅ Checklist pro vývojáře

- [ ] Používám max 2 fonty (Inter + monospace)
- [ ] Dodržujem barevnou hierarchii (primary, secondary, feedback)
- [ ] WCAG AA kontrast min. 4.5:1
- [ ] Labels nad formulářovými poli
- [ ] Loading states místo prázdných stránek
- [ ] Focus outline nikdy neskrývám
- [ ] Breadcrumbs pro hierarchické části
- [ ] Max 2-3 sloupce na desktopu
- [ ] Konzistentní spacing (4/8/16px scale)
- [ ] Smysluplné použití barev (ne "jen aby to bylo barevné")

---

*Design systém je živý dokument. Aktualizujte podle potřeb projektu.*