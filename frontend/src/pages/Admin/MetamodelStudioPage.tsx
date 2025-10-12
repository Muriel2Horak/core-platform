import { Container, Typography, Box } from '@mui/material';
import { Schema as StudioIcon } from '@mui/icons-material';
import { GlassPaper } from '../../shared/ui';

/**
 * W0/S10: Metamodel Studio Page (placeholder)
 * 
 * Phase W0: Foundation - empty placeholder
 * Phase S10: Will wire to existing BE metamodel services
 */
export const MetamodelStudioPage = () => {
  return (
    <Container maxWidth="xl" sx={{ py: 4 }}>
      <Box display="flex" alignItems="center" gap={2} mb={3}>
        <StudioIcon fontSize="large" color="primary" />
        <Box>
          <Typography variant="h4">Metamodel Studio</Typography>
          <Typography variant="body2" color="text.secondary">
            GUI pro správu metamodelu s diff/preview/approval
          </Typography>
        </Box>
      </Box>

      <GlassPaper sx={{ p: 3 }}>
        <Typography variant="body1" color="text.secondary">
          🚧 W0: Foundation placeholder
          <br />
          S10 přidá: entity editor, validation, preview, proposals
        </Typography>
      </GlassPaper>
    </Container>
  );
};

export default MetamodelStudioPage;
