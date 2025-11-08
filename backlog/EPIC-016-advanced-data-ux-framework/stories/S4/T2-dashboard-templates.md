# T2: Dashboard Templates

**Story:** [S4: Role-based Dashboard Defaults](README.md)  
**Effort:** 20 hours  
**Priority:** P1  
**Dependencies:** T1

---

## 📋 OBJECTIVE

Template system - uživatelé si mohou vybrat z galérie template dashboardů.

---

## 🎯 ACCEPTANCE CRITERIA

1. Gallery of templates (Sales, Marketing, Engineering)
2. Preview template
3. Clone template
4. Customize after clone

---

## 🏗️ IMPLEMENTATION

```typescript
// frontend/src/components/dashboard/TemplateGallery.tsx
interface Template {
  id: string;
  name: string;
  description: string;
  preview: string;
  widgets: Widget[];
}

export const TemplateGallery: React.FC = () => {
  const templates: Template[] = [
    { id: 'sales', name: 'Sales Dashboard', description: 'Revenue, leads, conversion', widgets: [...] },
    { id: 'marketing', name: 'Marketing Dashboard', description: 'Campaigns, traffic, ROI', widgets: [...] },
    { id: 'engineering', name: 'Engineering Dashboard', description: 'Deploys, incidents, velocity', widgets: [...] }
  ];
  
  const cloneTemplate = async (template: Template) => {
    await api.post('/api/dashboards', {
      name: `${template.name} (Copy)`,
      widgets: template.widgets
    });
  };
  
  return (
    <Grid container spacing={2}>
      {templates.map(t => (
        <Grid item xs={4} key={t.id}>
          <Card>
            <CardMedia image={t.preview} />
            <CardContent>
              <Typography variant="h6">{t.name}</Typography>
              <Typography>{t.description}</Typography>
              <Button onClick={() => cloneTemplate(t)}>Use Template</Button>
            </CardContent>
          </Card>
        </Grid>
      ))}
    </Grid>
  );
};
```

---

## ✅ DELIVERABLES

- [ ] 5+ templates
- [ ] Gallery UI
- [ ] Clone functionality
- [ ] Unit tests

---

**Estimated:** 20 hours
