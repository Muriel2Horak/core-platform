# T1: Query Builder UI

**Story:** [S6: Visual Query Builder](README.md)  
**Effort:** 20 hours  
**Priority:** P0  
**Dependencies:** None

---

## 📋 OBJECTIVE

Drag & drop query builder (dimensions, measures, filters).

---

## 🎯 ACCEPTANCE CRITERIA

1. Dimension dropzone
2. Measure dropzone
3. Filter dropzone
4. Auto-generate Cube.js query

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/query-builder/QueryBuilder.tsx
import { DndContext, DragOverlay } from '@dnd-kit/core';

interface QueryBuilderState {
  dimensions: string[];
  measures: string[];
  filters: Filter[];
}

export const QueryBuilder: React.FC = () => {
  const [query, setQuery] = useState<QueryBuilderState>({
    dimensions: [],
    measures: [],
    filters: []
  });
  
  const handleDrop = (event: DragEndEvent) => {
    const { active, over } = event;
    
    if (over?.id === 'dimensions') {
      setQuery(q => ({ ...q, dimensions: [...q.dimensions, active.id as string] }));
    } else if (over?.id === 'measures') {
      setQuery(q => ({ ...q, measures: [...q.measures, active.id as string] }));
    }
  };
  
  return (
    <DndContext onDragEnd={handleDrop}>
      <Grid container spacing={2}>
        <Grid item xs={3}>
          <FieldList fields={availableFields} />
        </Grid>
        <Grid item xs={9}>
          <Dropzone id="dimensions" label="Dimensions" items={query.dimensions} />
          <Dropzone id="measures" label="Measures" items={query.measures} />
          <Dropzone id="filters" label="Filters" items={query.filters} />
        </Grid>
      </Grid>
    </DndContext>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] QueryBuilder component
- [ ] Drag & drop
- [ ] Dropzones
- [ ] Unit tests

---

**Estimated:** 20 hours
