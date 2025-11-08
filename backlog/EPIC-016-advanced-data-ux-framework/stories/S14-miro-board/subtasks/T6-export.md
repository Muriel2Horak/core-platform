# T6: Export to PNG/PDF

**Story:** [S14: Miro-style Freeform Board](README.md)  
**Effort:** 6 hours  
**Priority:** P2  
**Dependencies:** T1

---

## 📋 TASK DESCRIPTION

Export board jako PNG nebo PDF soubor.

---

## 🎯 ACCEPTANCE CRITERIA

1. **Export PNG** - button → download PNG
2. **Export PDF** - button → download PDF
3. **High resolution** - 2x scale pro quality

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/board/ExportToolbar.tsx
import { jsPDF } from 'jspdf';

export const exportToPNG = (stageRef: React.RefObject<Konva.Stage>) => {
  const uri = stageRef.current?.toDataURL({ pixelRatio: 2 });
  const link = document.createElement('a');
  link.download = 'board.png';
  link.href = uri;
  link.click();
};

export const exportToPDF = (stageRef: React.RefObject<Konva.Stage>) => {
  const uri = stageRef.current?.toDataURL({ pixelRatio: 2 });
  const pdf = new jsPDF();
  pdf.addImage(uri, 'PNG', 0, 0, 210, 297);
  pdf.save('board.pdf');
};
```

---

## 📦 DELIVERABLES

- [ ] PNG export
- [ ] PDF export
- [ ] High resolution rendering

---

**Estimated:** 6 hours
