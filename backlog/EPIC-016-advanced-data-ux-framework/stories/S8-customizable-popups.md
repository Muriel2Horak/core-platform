# S8: Customizable Popup Layouts

**EPIC:** [EPIC-016: Advanced Data UX Framework](../README.md)  
**Status:** 📋 **TODO**  
**Priority:** 🟢 **P2 - MEDIUM**  
**Effort:** ~85 hours  
**Sprint:** 4  
**Owner:** TBD

---

## 📋 STORY DESCRIPTION

**Jako** Power User / Admin,  
**chci** customize popup layouts pro edit/create forms (např. User Edit popup),  
**abych**:
- Uspořádal fields do logical sections (Personal Info, Permissions, Preferences)
- Přidal custom widgets do popupu (Recent Activity chart, Audit Log table)
- Uložil layout jako personal template nebo shared s týmem
- Použil template library pro quick setup nových popupů

---

## 🎯 ACCEPTANCE CRITERIA

### AC1: Popup Layout Designer

**GIVEN** edit popup pro User entity  
**WHEN** kliknu "Customize Layout"  
**THEN** otevře se layout designer:
- **Left sidebar**: Widget palette (Form Fields, Charts, Tables, KPI Tiles)
- **Center canvas**: Drag & drop layout editor (12-column grid)
- **Right sidebar**: Widget properties panel

**Drag & Drop:**
- Drag "Email Field" widget na canvas → vytvoří se form field
- Resize widget (2-12 columns wide)
- Reorder widgets (drag up/down)

### AC2: Widget Palette

**GIVEN** layout designer otevřený  
**WHEN** zobrazím widget palette  
**THEN** obsahuje kategorie:

**Form Fields:**
- Text Input
- Text Area
- Select Dropdown
- Multi-Select
- Date Picker
- Checkbox
- Radio Group
- File Upload

**Data Visualization:**
- Line Chart (Recent Activity)
- Bar Chart (Statistics)
- KPI Tile (Single Metric)
- Table (Related Records)

**Layout Elements:**
- Section Divider
- Tabs Container
- Accordion Panel
- Spacer

### AC3: Layout Persistence (Personal & Shared)

**GIVEN** customized popup layout  
**WHEN** kliknu "Save Layout"  
**THEN** zobrazí se dialog:
- **Name**: "My User Edit Layout"
- **Visibility**:
  - Personal (only me)
  - Shared with Team (all ADMIN users)
  - Public (all roles)
- **Set as Default**: Checkbox (use this layout by default)

**Backend storage:**

```http
POST /api/popup-layouts
{
  "entityType": "User",
  "layoutName": "My User Edit Layout",
  "visibility": "SHARED_WITH_TEAM",
  "isDefault": true,
  "layout": {
    "widgets": [
      {"id": "email-field", "type": "text-input", "x": 0, "y": 0, "w": 6, "h": 1},
      {"id": "role-select", "type": "select", "x": 6, "y": 0, "w": 6, "h": 1},
      {"id": "activity-chart", "type": "line-chart", "x": 0, "y": 1, "w": 12, "h": 4}
    ]
  }
}
```

### AC4: Template Library

**GIVEN** User Edit popup  
**WHEN** kliknu "Choose Template"  
**THEN** zobrazí se template gallery:
- **System Templates** (built-in):
  - "Simple Form" (just fields, no charts)
  - "Admin View" (fields + permissions + audit log)
  - "Analytics View" (fields + activity charts + KPIs)
- **Team Templates** (shared by colleagues)
- **My Templates** (personal saved layouts)

**Template preview:**
- Thumbnail screenshot
- Template name & description
- Author & creation date
- Rating (stars) & usage count

**Actions:**
- **Use Template** → apply layout
- **Duplicate & Edit** → copy layout, customize
- **Delete** (if owner)

---

## 🏗️ IMPLEMENTATION

### Task Breakdown

#### **T1: Layout Designer UI** (25h)

**Implementation:**

```typescript
// frontend/src/components/popup/LayoutDesigner.tsx
import React, { useState } from 'react';
import { Box, Drawer, Button } from '@mui/material';
import { DndContext } from '@dnd-kit/core';
import { GridLayout } from '@/components/dashboard/GridLayout'; // Reuse from S3

interface LayoutDesignerProps {
  entityType: string;
  initialLayout?: PopupLayout;
  onSave: (layout: PopupLayout) => void;
}

interface PopupLayout {
  widgets: WidgetConfig[];
}

export const LayoutDesigner: React.FC<LayoutDesignerProps> = ({
  entityType,
  initialLayout,
  onSave
}) => {
  const [layout, setLayout] = useState<PopupLayout>(initialLayout || { widgets: [] });
  const [selectedWidget, setSelectedWidget] = useState<string | null>(null);
  const [paletteOpen, setPaletteOpen] = useState(true);

  const handleDragEnd = (event: any) => {
    const { active, over } = event;
    if (!over) return;

    // Add widget from palette to canvas
    const widgetType = active.id;
    const newWidget = createWidget(widgetType);
    setLayout(prev => ({
      widgets: [...prev.widgets, newWidget]
    }));
  };

  return (
    <Box sx={{ display: 'flex', height: '100vh' }}>
      {/* Left: Widget Palette */}
      <Drawer
        variant="persistent"
        open={paletteOpen}
        sx={{ width: 280, flexShrink: 0 }}
      >
        <Box sx={{ p: 2 }}>
          <WidgetPalette onDragStart={() => {}} />
        </Box>
      </Drawer>

      {/* Center: Canvas */}
      <Box sx={{ flex: 1, p: 3 }}>
        <DndContext onDragEnd={handleDragEnd}>
          <GridLayout
            widgets={layout.widgets}
            editable
            onLayoutChange={(newLayout) => {
              setLayout({ widgets: newLayout });
            }}
            onWidgetClick={(widgetId) => setSelectedWidget(widgetId)}
          />
        </DndContext>

        <Box sx={{ mt: 2 }}>
          <Button variant="contained" onClick={() => onSave(layout)}>
            Save Layout
          </Button>
        </Box>
      </Box>

      {/* Right: Properties Panel */}
      {selectedWidget && (
        <Drawer anchor="right" open variant="persistent" sx={{ width: 320 }}>
          <WidgetPropertiesPanel
            widget={layout.widgets.find(w => w.id === selectedWidget)}
            onChange={(updates) => {
              setLayout(prev => ({
                widgets: prev.widgets.map(w =>
                  w.id === selectedWidget ? { ...w, ...updates } : w
                )
              }));
            }}
          />
        </Drawer>
      )}
    </Box>
  );
};

function createWidget(type: string): WidgetConfig {
  return {
    id: `widget-${Date.now()}`,
    type,
    x: 0,
    y: Infinity, // Auto-place
    w: type.includes('chart') ? 12 : 6,
    h: type.includes('chart') ? 4 : 1,
    config: {}
  };
}
```

**Deliverable:** Layout designer with drag & drop

---

#### **T2: Widget Palette** (15h)

**Implementation:**

```typescript
// frontend/src/components/popup/WidgetPalette.tsx
import React from 'react';
import { Box, Typography, List, ListItem, ListItemIcon, ListItemText, Divider } from '@mui/material';
import {
  TextFields,
  CalendarToday,
  CheckBox,
  BarChart,
  TableChart,
  Assessment
} from '@mui/icons-material';
import { useDraggable } from '@dnd-kit/core';

const widgetCategories = [
  {
    category: 'Form Fields',
    widgets: [
      { id: 'text-input', label: 'Text Input', icon: <TextFields /> },
      { id: 'text-area', label: 'Text Area', icon: <TextFields /> },
      { id: 'select', label: 'Select Dropdown', icon: <TextFields /> },
      { id: 'multi-select', label: 'Multi-Select', icon: <TextFields /> },
      { id: 'date-picker', label: 'Date Picker', icon: <CalendarToday /> },
      { id: 'checkbox', label: 'Checkbox', icon: <CheckBox /> }
    ]
  },
  {
    category: 'Data Visualization',
    widgets: [
      { id: 'line-chart', label: 'Line Chart', icon: <BarChart /> },
      { id: 'bar-chart', label: 'Bar Chart', icon: <BarChart /> },
      { id: 'kpi-tile', label: 'KPI Tile', icon: <Assessment /> },
      { id: 'table', label: 'Data Table', icon: <TableChart /> }
    ]
  }
];

export const WidgetPalette: React.FC = () => {
  return (
    <Box>
      <Typography variant="h6" sx={{ mb: 2 }}>Widget Palette</Typography>
      {widgetCategories.map(({ category, widgets }) => (
        <Box key={category}>
          <Typography variant="subtitle2" sx={{ mt: 2, mb: 1 }}>
            {category}
          </Typography>
          <List dense>
            {widgets.map(widget => (
              <DraggableWidget key={widget.id} {...widget} />
            ))}
          </List>
          <Divider />
        </Box>
      ))}
    </Box>
  );
};

const DraggableWidget: React.FC<{ id: string; label: string; icon: React.ReactNode }> = ({
  id,
  label,
  icon
}) => {
  const { attributes, listeners, setNodeRef } = useDraggable({ id });

  return (
    <ListItem
      ref={setNodeRef}
      {...listeners}
      {...attributes}
      sx={{ cursor: 'grab', '&:hover': { bgcolor: 'action.hover' } }}
    >
      <ListItemIcon>{icon}</ListItemIcon>
      <ListItemText primary={label} />
    </ListItem>
  );
};
```

**Deliverable:** Widget palette with categories

---

#### **T3: Layout Persistence (Backend)** (20h)

**Implementation:**

```java
// backend/src/main/java/cz/muriel/core/popup/model/PopupLayout.java
package cz.muriel.core.popup.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "popup_layouts")
@Data
public class PopupLayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String entityType; // "User", "Tenant", "Workflow"

    @Column(nullable = false)
    private String layoutName;

    @Enumerated(EnumType.STRING)
    private LayoutVisibility visibility; // PERSONAL, SHARED_WITH_TEAM, PUBLIC

    @Column(nullable = false)
    private Long createdBy; // User ID

    @Column(columnDefinition = "jsonb")
    private String layoutData; // JSON layout config

    private Boolean isDefault = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

enum LayoutVisibility {
    PERSONAL,
    SHARED_WITH_TEAM,
    PUBLIC
}
```

**Service:**

```java
// backend/src/main/java/cz/muriel/core/popup/service/PopupLayoutService.java
@Service
public class PopupLayoutService {

    private final PopupLayoutRepository layoutRepository;
    private final UserRepository userRepository;

    /**
     * Get available layouts for user (personal + shared + public)
     */
    public List<PopupLayout> getAvailableLayouts(String entityType, Long userId) {
        var user = userRepository.findById(userId).orElseThrow();

        return layoutRepository.findAll().stream()
            .filter(layout -> layout.getEntityType().equals(entityType))
            .filter(layout -> {
                if (layout.getVisibility() == LayoutVisibility.PERSONAL) {
                    return layout.getCreatedBy().equals(userId);
                } else if (layout.getVisibility() == LayoutVisibility.SHARED_WITH_TEAM) {
                    return hasSameRole(user, layout.getCreatedBy());
                } else {
                    return true; // PUBLIC
                }
            })
            .collect(Collectors.toList());
    }

    /**
     * Save layout
     */
    @Transactional
    public PopupLayout saveLayout(PopupLayoutDTO dto, Long userId) {
        var layout = new PopupLayout();
        layout.setEntityType(dto.getEntityType());
        layout.setLayoutName(dto.getLayoutName());
        layout.setVisibility(dto.getVisibility());
        layout.setCreatedBy(userId);
        layout.setLayoutData(dto.getLayoutData());
        layout.setIsDefault(dto.getIsDefault());
        layout.setCreatedAt(LocalDateTime.now());

        return layoutRepository.save(layout);
    }

    private boolean hasSameRole(User user, Long otherUserId) {
        var otherUser = userRepository.findById(otherUserId).orElse(null);
        return otherUser != null && user.getRole().equals(otherUser.getRole());
    }
}
```

**Deliverable:** Backend persistence with visibility levels

---

#### **T4: Template Library UI** (15h)

**Implementation:**

```typescript
// frontend/src/components/popup/TemplateLibrary.tsx
import React, { useState, useEffect } from 'react';
import { Dialog, DialogTitle, DialogContent, Grid, Card, CardMedia, CardContent, Typography, Button, Rating } from '@mui/material';

interface Template {
  id: string;
  name: string;
  description: string;
  thumbnail: string;
  author: string;
  createdAt: string;
  rating: number;
  usageCount: number;
  visibility: 'SYSTEM' | 'TEAM' | 'PERSONAL';
}

interface TemplateLibraryProps {
  entityType: string;
  onSelect: (template: Template) => void;
}

export const TemplateLibrary: React.FC<TemplateLibraryProps> = ({ entityType, onSelect }) => {
  const [templates, setTemplates] = useState<Template[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<'SYSTEM' | 'TEAM' | 'PERSONAL'>('SYSTEM');

  useEffect(() => {
    fetch(`/api/popup-layouts/templates?entityType=${entityType}`)
      .then(res => res.json())
      .then(setTemplates);
  }, [entityType]);

  const filteredTemplates = templates.filter(t => t.visibility === selectedCategory);

  return (
    <Dialog open fullWidth maxWidth="lg">
      <DialogTitle>Choose a Template</DialogTitle>
      <DialogContent>
        {/* Category tabs */}
        <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
          <Button
            variant={selectedCategory === 'SYSTEM' ? 'contained' : 'outlined'}
            onClick={() => setSelectedCategory('SYSTEM')}
          >
            System Templates
          </Button>
          <Button
            variant={selectedCategory === 'TEAM' ? 'contained' : 'outlined'}
            onClick={() => setSelectedCategory('TEAM')}
          >
            Team Templates
          </Button>
          <Button
            variant={selectedCategory === 'PERSONAL' ? 'contained' : 'outlined'}
            onClick={() => setSelectedCategory('PERSONAL')}
          >
            My Templates
          </Button>
        </Box>

        {/* Template grid */}
        <Grid container spacing={3}>
          {filteredTemplates.map(template => (
            <Grid item xs={12} sm={6} md={4} key={template.id}>
              <Card>
                <CardMedia
                  component="img"
                  height="140"
                  image={template.thumbnail || '/placeholder-template.png'}
                  alt={template.name}
                />
                <CardContent>
                  <Typography variant="h6">{template.name}</Typography>
                  <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                    {template.description}
                  </Typography>
                  <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
                    <Rating value={template.rating} size="small" readOnly />
                    <Typography variant="caption" sx={{ ml: 1 }}>
                      ({template.usageCount} uses)
                    </Typography>
                  </Box>
                  <Typography variant="caption" color="text.secondary">
                    By {template.author} • {new Date(template.createdAt).toLocaleDateString()}
                  </Typography>
                  <Box sx={{ mt: 2 }}>
                    <Button size="small" variant="contained" onClick={() => onSelect(template)}>
                      Use Template
                    </Button>
                    <Button size="small" sx={{ ml: 1 }}>
                      Duplicate
                    </Button>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </DialogContent>
    </Dialog>
  );
};
```

**Deliverable:** Template library with preview

---

#### **T5: Widget Properties Panel** (10h)

**Implementation:**

```typescript
// frontend/src/components/popup/WidgetPropertiesPanel.tsx
import React from 'react';
import { Box, Typography, TextField, Select, MenuItem, Switch, FormControlLabel } from '@mui/material';

interface WidgetPropertiesPanelProps {
  widget: WidgetConfig;
  onChange: (updates: Partial<WidgetConfig>) => void;
}

export const WidgetPropertiesPanel: React.FC<WidgetPropertiesPanelProps> = ({ widget, onChange }) => {
  if (!widget) return null;

  return (
    <Box sx={{ p: 2 }}>
      <Typography variant="h6" sx={{ mb: 2 }}>Widget Properties</Typography>

      {/* Common properties */}
      <TextField
        label="Label"
        value={widget.config.label || ''}
        onChange={(e) => onChange({ config: { ...widget.config, label: e.target.value } })}
        fullWidth
        sx={{ mb: 2 }}
      />

      {/* Type-specific properties */}
      {widget.type === 'text-input' && (
        <>
          <TextField
            label="Placeholder"
            value={widget.config.placeholder || ''}
            onChange={(e) => onChange({ config: { ...widget.config, placeholder: e.target.value } })}
            fullWidth
            sx={{ mb: 2 }}
          />
          <FormControlLabel
            control={
              <Switch
                checked={widget.config.required || false}
                onChange={(e) => onChange({ config: { ...widget.config, required: e.target.checked } })}
              />
            }
            label="Required"
          />
        </>
      )}

      {widget.type === 'line-chart' && (
        <>
          <Select
            label="Data Source"
            value={widget.config.dataSource || ''}
            onChange={(e) => onChange({ config: { ...widget.config, dataSource: e.target.value } })}
            fullWidth
            sx={{ mb: 2 }}
          >
            <MenuItem value="userActivity">User Activity</MenuItem>
            <MenuItem value="loginHistory">Login History</MenuItem>
          </Select>
          <TextField
            label="Time Range (days)"
            type="number"
            value={widget.config.timeRange || 30}
            onChange={(e) => onChange({ config: { ...widget.config, timeRange: parseInt(e.target.value) } })}
            fullWidth
          />
        </>
      )}
    </Box>
  );
};
```

**Deliverable:** Properties panel for widget configuration

---

## 🧪 TESTING

```typescript
// e2e/specs/popup/layout-designer.spec.ts
test('Customize popup layout', async ({ page }) => {
  await page.goto('/users/1/edit');
  await page.click('button:has-text("Customize Layout")');

  // Drag widget from palette
  await page.dragAndDrop('[data-widget="line-chart"]', '.layout-canvas');

  // Verify widget added
  await expect(page.locator('.layout-canvas [data-widget-type="line-chart"]')).toBeVisible();

  // Save layout
  await page.fill('[name=layoutName]', 'My Custom Layout');
  await page.click('button:has-text("Save Layout")');

  // Verify saved
  await expect(page.locator('text=Layout saved successfully')).toBeVisible();
});
```

---

## 📊 SUCCESS METRICS

- ✅ Layout customization < 5min (first-time user)
- ✅ Template apply < 1s
- ✅ 30% users create custom layouts (good adoption!)

---

## 🔗 DEPENDENCIES

- **S3:** GridLayout component (reuse for popup canvas)
- **EPIC-014 S3:** Form components

---

**Status:** 📋 TODO  
**Next:** S9: Tile Click Actions

