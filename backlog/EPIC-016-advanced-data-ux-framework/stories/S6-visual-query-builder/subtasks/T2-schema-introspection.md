# T2: Schema Introspection

**Story:** [S6: Visual Query Builder](README.md)  
**Effort:** 15 hours  
**Priority:** P0  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Fetch Cube.js schema - auto-populate dimensions/measures.

---

## 🎯 ACCEPTANCE CRITERIA

1. Fetch /cubejs-api/v1/meta
2. Parse cubes, dimensions, measures
3. Display in field list
4. Group by cube

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/hooks/useCubeSchema.ts
interface CubeMeta {
  cubes: {
    name: string;
    dimensions: { name: string; title: string; type: string }[];
    measures: { name: string; title: string; type: string }[];
  }[];
}

export const useCubeSchema = () => {
  const [schema, setSchema] = useState<CubeMeta | null>(null);
  
  useEffect(() => {
    fetch('/cubejs-api/v1/meta')
      .then(res => res.json())
      .then(data => setSchema(data));
  }, []);
  
  const getDimensions = (cubeName: string) => {
    return schema?.cubes.find(c => c.name === cubeName)?.dimensions || [];
  };
  
  const getMeasures = (cubeName: string) => {
    return schema?.cubes.find(c => c.name === cubeName)?.measures || [];
  };
  
  return { schema, getDimensions, getMeasures };
};

// frontend/src/components/query-builder/FieldList.tsx
export const FieldList: React.FC = () => {
  const { schema, getDimensions, getMeasures } = useCubeSchema();
  
  return (
    <List>
      {schema?.cubes.map(cube => (
        <Accordion key={cube.name}>
          <AccordionSummary>{cube.name}</AccordionSummary>
          <AccordionDetails>
            <Typography variant="subtitle2">Dimensions</Typography>
            {getDimensions(cube.name).map(d => (
              <DraggableField key={d.name} field={d} />
            ))}
            <Typography variant="subtitle2">Measures</Typography>
            {getMeasures(cube.name).map(m => (
              <DraggableField key={m.name} field={m} />
            ))}
          </AccordionDetails>
        </Accordion>
      ))}
    </List>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] Schema fetch hook
- [ ] Field list component
- [ ] Grouping by cube
- [ ] Unit tests

---

**Estimated:** 15 hours
