# T3: Saved Filters

**Story:** [S2: Advanced Filtering & Search](README.md)  
**Effort:** 15 hours  
**Priority:** P1  
**Dependencies:** T1, T2

---

## 📋 OBJECTIVE

Uložení favorite filters pro rychlé použití.

---

## 🎯 ACCEPTANCE CRITERIA

1. Save current filters button
2. Saved filters dropdown
3. Delete saved filter
4. Backend API (SavedFilter entity)

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/filters/SavedFilters.tsx
export const SavedFilters: React.FC = () => {
  const [saved, setSaved] = useState<SavedFilter[]>([]);
  
  const saveFilters = async (name: string, filters: FilterConfig) => {
    const result = await api.post('/api/saved-filters', { name, filters });
    setSaved([...saved, result]);
  };
  
  return (
    <Box>
      <Select>
        {saved.map(f => (
          <MenuItem key={f.id} onClick={() => applyFilter(f)}>
            {f.name}
          </MenuItem>
        ))}
      </Select>
      
      <Button onClick={() => saveFilters('My Filter', currentFilters)}>
        Save Current Filters
      </Button>
    </Box>
  );
};
```

```java
// backend/src/main/java/cz/muriel/core/filters/SavedFilter.java
@Entity
public class SavedFilter {
  @Id
  private UUID id;
  
  private String name;
  
  @Column(columnDefinition = "jsonb")
  private FilterConfig filters;
  
  @ManyToOne
  private User owner;
}
```

---

## ✅ DELIVERABLES

- [ ] SavedFilters UI
- [ ] Backend entity + API
- [ ] CRUD operations
- [ ] Unit tests

---

**Estimated:** 15 hours
