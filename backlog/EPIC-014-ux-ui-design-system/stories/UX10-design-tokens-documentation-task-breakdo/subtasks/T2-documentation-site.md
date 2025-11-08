# T2: Documentation Site

**Parent Story:** S10 - Design Tokens & Documentation  
**LOC:** ~200 | **Effort:** ~3h

## Objective
Create documentation site at `/design-system` showcasing components and design tokens.

## Implementation

```tsx
// frontend/src/pages/DesignSystem.tsx
export const DesignSystemPage: React.FC = () => {
  return (
    <Box>
      <Typography variant="h3" gutterBottom>Design System</Typography>
      
      <Tabs>
        <Tab label="Colors" />
        <Tab label="Typography" />
        <Tab label="Components" />
        <Tab label="Icons" />
      </Tabs>
      
      <TabPanel value={0}>
        <ColorPalette />
      </TabPanel>
    </Box>
  );
};

// Color palette showcase
const ColorPalette: React.FC = () => (
  <Grid container spacing={2}>
    {Object.entries(tokens.colors.primary).map(([shade, color]) => (
      <Grid item xs={12} sm={6} md={4} key={shade}>
        <Paper sx={{ p: 2, bgcolor: color }}>
          <Typography variant="caption" sx={{ color: 'white' }}>
            primary.{shade}
          </Typography>
          <Typography variant="body2" sx={{ color: 'white' }}>
            {color}
          </Typography>
        </Paper>
      </Grid>
    ))}
  </Grid>
);

// Typography showcase
const TypographyShowcase: React.FC = () => (
  <Stack spacing={2}>
    <Typography variant="h1">Heading 1</Typography>
    <Typography variant="h2">Heading 2</Typography>
    <Typography variant="h3">Heading 3</Typography>
    <Typography variant="body1">Body 1 - Regular text</Typography>
    <Typography variant="body2">Body 2 - Secondary text</Typography>
    <Typography variant="caption">Caption text</Typography>
  </Stack>
);

// Component examples
const ComponentShowcase: React.FC = () => (
  <Stack spacing={4}>
    <Box>
      <Typography variant="h5" gutterBottom>Buttons</Typography>
      <Stack direction="row" spacing={2}>
        <Button variant="contained">Contained</Button>
        <Button variant="outlined">Outlined</Button>
        <Button variant="text">Text</Button>
      </Stack>
    </Box>
    
    <Box>
      <Typography variant="h5" gutterBottom>Form Inputs</Typography>
      <Stack spacing={2}>
        <TextField label="Text Field" />
        <FormControl>
          <InputLabel>Select</InputLabel>
          <Select>
            <MenuItem value="option1">Option 1</MenuItem>
          </Select>
        </FormControl>
      </Stack>
    </Box>
  </Stack>
);
```

## Acceptance Criteria
- [ ] `/design-system` route
- [ ] Color palette display
- [ ] Typography samples
- [ ] Component examples
- [ ] Copy-to-clipboard for tokens
- [ ] Searchable
- [ ] Dark/light mode toggle

## Files
- `frontend/src/pages/DesignSystem.tsx`
- `frontend/src/components/design-system/ColorPalette.tsx`
- `frontend/src/components/design-system/TypographyShowcase.tsx`
