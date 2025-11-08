# T3: Responsive Table Patterns

**Parent Story:** S9 - Data Table Components  
**LOC:** ~200 | **Effort:** ~2h

## Objective
Implement responsive table patterns (card view on mobile, horizontal scroll).

## Implementation

```tsx
// frontend/src/components/table/ResponsiveTable.tsx
import { useMediaQuery, useTheme } from '@mui/material';

export const ResponsiveTable = <T extends { id: string | number }>({
  columns,
  rows,
  renderCard,
}: {
  columns: GridColDef[];
  rows: T[];
  renderCard: (row: T) => React.ReactNode;
}) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  
  if (isMobile) {
    return (
      <Stack spacing={2}>
        {rows.map(row => (
          <Card key={row.id}>
            {renderCard(row)}
          </Card>
        ))}
      </Stack>
    );
  }
  
  return <DataTable columns={columns} rows={rows} />;
};

// Card view for mobile
const UserCard: React.FC<{ user: User }> = ({ user }) => (
  <CardContent>
    <Box display="flex" justifyContent="space-between" alignItems="start">
      <Box>
        <Typography variant="h6">{user.name}</Typography>
        <Typography variant="body2" color="text.secondary">
          {user.email}
        </Typography>
      </Box>
      <Chip label={user.status} size="small" />
    </Box>
    <Box mt={2}>
      <IconButton onClick={() => handleEdit(user)}>
        <Edit />
      </IconButton>
    </Box>
  </CardContent>
);

// Horizontal scroll on tablet
const ScrollableTable: React.FC = () => (
  <TableContainer sx={{ overflowX: 'auto' }}>
    <Table sx={{ minWidth: 650 }}>
      {/* Table content */}
    </Table>
  </TableContainer>
);
```

## Acceptance Criteria
- [ ] Mobile: Card view (<md breakpoint)
- [ ] Tablet: Horizontal scroll
- [ ] Desktop: Full table
- [ ] Touch-friendly on mobile
- [ ] Swipe to reveal actions
- [ ] Maintain functionality across sizes

## Files
- `frontend/src/components/table/ResponsiveTable.tsx`
- `frontend/src/components/table/MobileCard.tsx`
