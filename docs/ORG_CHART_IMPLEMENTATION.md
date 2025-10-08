# User Directory - Organizational Chart Implementation

## 📋 Přehled implementace

Implementovány byly všechny 4 požadované funkce pro User Directory:

### ✅ 1. Opraveno zobrazení v tabulce
- **Sloupec "Uživatel"**: Avatar + celé jméno + username ✅
- **Sloupec "Email"**: Email uživatele ✅
- **Sloupec "Zdroj"**: AD/LOCAL s ikonami ✅
- **Sloupec "Nadřízený"**: Nový sloupec zobrazující managera ✅
- **Sloupec "Akce"**: Tlačítko "Detail" místo ikonky oka ✅

### ✅ 2. Organizační chart - hlavní view
- **Tab "Seznam uživatelů"**: Klasická tabulka (původní view)
- **Tab "Org. Chart"**: Nový hierarchický pohled na organizaci
  - Stromová struktura podle nadřízených
  - Expandable/collapsible nodes
  - Vyhledávání v celé hierarchii
  - Barevné rozlišení levelů (primary/secondary/info)
  - Počet podřízených u každého managera

### ✅ 3. Rozšířený detail uživatele
**Dialog obsahuje 2 taby:**

#### Tab 1: Informace
- Email, Jméno, Příjmení
- **Nadřízený**: Kliknutelný chip s možností zobrazit jeho detail
- Tenant (pro adminy)
- Zdroj (AD/LOCAL)
- Stav (Aktivní/Neaktivní)
- Email ověřen

#### Tab 2: Org. Chart
- **Nadřízení**: Zobrazení všech managerů nad uživatelem (hierarchie nahoru)
- **Aktuální uživatel**: Zvýrazněný uprostřed
- **Podřízení**: Všichni podřízení pod uživatelem (hierarchie dolů)
- Všechny položky jsou kliknutelné → otevře jejich detail

### ✅ 4. Samostatný Org Chart s vyhledáváním
- Vyhledávání ve všech uživatelích (jméno, username, email)
- Filtruje celou hierarchii včetně potomků
- Expandable stromová struktura
- Visual indicators pro poziční level
- Informace o počtu podřízených

## 🏗️ Architektura

### Komponenty

#### **UserDirectory.jsx** (hlavní komponenta)
```
- State: mainTab (Seznam / Org Chart)
- State: viewDialogTab (Informace / Org Chart)
- Helper funkce:
  - buildOrgHierarchy() - vytvoří strom z plochého seznamu
  - getUserHierarchy(username) - získá ancestors + descendants
  - getDisplayName(), getInitials()
```

#### **OrgChartView.jsx** (celková org. struktura)
```jsx
<OrgChartView
  users={users}
  onUserClick={handleViewUser}
  getDisplayName={getDisplayName}
  getInitials={getInitials}
  buildOrgHierarchy={buildOrgHierarchy}
  loading={loading}
/>
```

**Funkce:**
- Zobrazení celé hierarchie organizace
- Vyhledávání v org. chartu
- Expandable/collapsible nodes
- Kliknutí → otevře detail uživatele

#### **UserOrgChart.jsx** (hierarchie konkrétního uživatele)
```jsx
<UserOrgChart
  user={selectedUser}
  users={users}
  onUserClick={handleViewUser}
  getDisplayName={getDisplayName}
  getInitials={getInitials}
  getUserHierarchy={getUserHierarchy}
/>
```

**Funkce:**
- 3 sekce: Nadřízení / Aktuální / Podřízení
- Barevné rozlišení (primary/success/secondary)
- Zobrazení level depth
- Všichni kliknutelní

### Data Model

**User Object:**
```javascript
{
  username: string,
  email: string,
  firstName: string,
  lastName: string,
  manager: string,  // username nadřízeného
  enabled: boolean,
  isFederated: boolean,
  directorySource: 'AD' | 'LOCAL',
  tenantKey: string
}
```

**Hierarchy Logic:**
```javascript
// Build tree from flat list
buildOrgHierarchy() {
  - userMap = { [username]: { ...user, children: [] } }
  - Každý user s managerem → připoj do children
  - Uživatelé bez managera → roots
  - Return roots (top-level managers)
}

// Get ancestors and descendants
getUserHierarchy(username) {
  ancestors: [] // Manageři nahoru
  descendants: [] // Všichni podřízení dolů (rekurzivně)
}
```

## 🎨 UX Features

### Vizuální hierarchie
- **Level 0** (Top managers): Primary color
- **Level 1**: Secondary color
- **Level 2+**: Info color
- Odsazení (ml: level * 4 nebo * 2)

### Interaktivita
- **Expand/Collapse**: Ikona ExpandMore/ChevronRight
- **Click na uživatele**: Otevře detail dialog
- **Click na managera v detailu**: Navigace na jeho detail
- **Search**: Real-time filtrování včetně potomků

### Chips & Badges
- **Podřízení count**: "3 podřízení"
- **Level depth**: "Level 2"
- **Source**: AD/LOCAL s ikonami
- **Status**: Aktivní/Neaktivní (tmavé barvy)

## 📊 Use Cases

### UC1: Procházení celé organizace
1. User otevře "Adresář"
2. Klikne na tab "Org. Chart"
3. Vidí stromovou strukturu celé organizace
4. Může expandovat/collapse jednotlivé větve
5. Vyhledávat konkrétní uživatele

### UC2: Zobrazení hierarchie konkrétního uživatele
1. User klikne na uživatele (v tabulce nebo org chartu)
2. Otevře se dialog s detailem
3. Klikne na tab "Org. Chart"
4. Vidí:
   - Všechny managery nad ním
   - Sebe (zvýrazněno)
   - Všechny podřízené pod ním
5. Může kliknout na kohokoli → otevře jeho detail

### UC3: Navigace hierarchií
1. User je v detailu uživatele A
2. Klikne na jeho managera B
3. Dialog se přepne na uživatele B
4. Může pokračovat výš v hierarchii
5. Nebo dolů k podřízeným

### UC4: Vyhledávání v org. struktuře
1. User v tab "Org. Chart" zadá jméno
2. Hierarchie se filtruje
3. Zobrazí se pouze matching uživatelé a jejich kontext
4. Zachovává se strom (parents viditelné)

## 🔧 Technické detaily

### Performance optimalizace
- **Debounced search**: 400ms delay
- **Controlled expansion**: Set pro O(1) lookup
- **Memoized hierarchy**: useCallback pro buildOrgHierarchy
- **Lazy rendering**: Jen expanded nodes

### Edge cases
- **Circular references**: Max depth 10 levelů
- **Missing managers**: Uživatelé bez managera → roots
- **Empty org chart**: Placeholder message
- **No descendants**: "Nemá žádné podřízené"

### Styling
- **Border colors**: Rozlišení levelů
- **Hover effects**: action.hover background
- **Card spacing**: mb: 1, consistent padding
- **Responsive**: Flex layout, auto overflow

## 🚀 Build & Deploy

```bash
# Build frontend
cd frontend && npm run build
# Output: dist/bundle.js 985.8kb

# Restart nginx
docker compose restart nginx
```

## 📝 Budoucí vylepšení

### Možné rozšíření:
1. **Export org chartu**: PDF/PNG/SVG
2. **Grafická vizualizace**: D3.js nebo React Flow diagram
3. **Team view**: Zobrazení podle týmů/oddělení
4. **Statistiky**: Počet podřízených, depth metriky
5. **Quick actions**: Poslat email, chat, atd.
6. **Filtry**: Podle source, tenant, status v org chartu
7. **Zoom/Pan**: Pro velké org struktury
8. **Mini-map**: Náhled celé hierarchie

## ✨ Summary

Všechny 4 požadované funkce byly úspěšně implementovány:

✅ 1. Opraveno zobrazení sloupců v tabulce (uživatel, zdroj, akce, **nadřízený**)
✅ 2. Přidán tab "Org. Chart" s hierarchií celé organizace
✅ 3. Rozšířen detail uživatele o všechny informace + org chart tab
✅ 4. Org chart je procházitelný a vyhledávatelný

**Build successful**: 985.8kb
**Status**: ✅ Production ready
