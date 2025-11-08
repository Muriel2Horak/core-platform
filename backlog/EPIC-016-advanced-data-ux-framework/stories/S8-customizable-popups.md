# S8: Customizable Popup Layouts

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO** | **Priority:** 🟢 **P2** | **Effort:** ~80h | **Sprint:** 9-10

---

## 📋 USER STORY

**Jako** Power User, **chci** customize layout editačního popupu (přidat grafy, tabulky, custom fields), **abych** měl všechny relevantní data na jednom místě.

---

## 🎯 ACCEPTANCE CRITERIA

1. **Layout Designer**: Drag & drop editor pro popup layout
2. **Add Widgets**: Přidání charts/tables/fields do popupu
3. **Save Layouts**: Uložení personal nebo shared layout
4. **Template Library**: Přednastavené layout templates

---

## 🏗️ TASK BREAKDOWN (~80h)

### T1: Popup Layout Editor (30h)
- Grid layout pro popup content
- Drag & drop widget placement

### T2: Widget Palette (15h)
- Chart widget, Table widget, Form fields
- Drag from palette → drop to popup

### T3: Layout Persistence (20h)
- Save layout → backend
- Personal vs. Shared layouts

### T4: Template Library (10h)
- Pre-built templates (e.g., "User Detail + Activity Chart")

### T5: Testing (5h)

---

## 📦 DEPENDENCIES

- **S3**: Dashboard Grid (reuse GridLayout)
- **S5**: Multi-Window (popup system)
- **EPIC-014 S3**: Forms

---

## 📊 SUCCESS METRICS

- Layout creation < 5min
- Template apply < 1s

