# T3: Cube.js Integration

**Story:** [S1: Universal Data View Engine](README.md)  
**Effort:** 20 hours  
**Priority:** P0  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Implementovat Cube.js schema detection a automatické načítání dimensions/measures pro danou entitu.

---

## 🎯 ACCEPTANCE CRITERIA

1. Cube.js REST API client
2. Schema introspection (`/cubejs-api/v1/meta`)
3. Automatic dimension/measure detection
4. Query builder integration

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/hooks/useCubeQuery.ts
export const useCubeQuery = (entity: string) => {
  const [schema, setSchema] = useState<CubeSchema | null>(null);
  
  useEffect(() => {
    fetch(`/cubejs-api/v1/meta`)
      .then(res => res.json())
      .then(data => {
        const cubeSchema = data.cubes.find(c => c.name === entity);
        setSchema(cubeSchema);
      });
  }, [entity]);
  
  return { schema, dimensions: schema?.dimensions, measures: schema?.measures };
};
```

---

## ✅ DELIVERABLES

- [ ] Cube.js client hook
- [ ] Schema introspection
- [ ] Auto-detection logic
- [ ] Unit tests

---

**Estimated:** 20 hours
