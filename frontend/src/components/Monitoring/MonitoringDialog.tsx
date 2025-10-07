import { Dialog, DialogContent, DialogTitle, IconButton, Box } from '@mui/material';
import { Close, OpenInNew } from '@mui/icons-material';

interface MonitoringDialogProps {
  open: boolean;
  onClose: () => void;
  title?: string;
}

export const MonitoringDialog = ({ 
  open, 
  onClose,
  title = 'Monitoring Dashboard'
}: MonitoringDialogProps) => {
  const openInNewTab = () => {
    // ✅ OPRAVA: Vždy HTTPS
    const protocol = 'https:';
    const host = window.location.host;
    window.open(`${protocol}//${host}/monitoring`, '_blank');
  };

  return (
    <Dialog 
      open={open} 
      onClose={onClose}
      maxWidth="xl"
      fullWidth
      PaperProps={{
        sx: { height: '90vh' }
      }}
    >
      <DialogTitle>
        {title}
        <Box sx={{ position: 'absolute', right: 8, top: 8 }}>
          <IconButton onClick={openInNewTab} size="small" sx={{ mr: 1 }}>
            <OpenInNew />
          </IconButton>
          <IconButton onClick={onClose} size="small">
            <Close />
          </IconButton>
        </Box>
      </DialogTitle>
      <DialogContent sx={{ p: 0, height: 'calc(100% - 64px)' }}>
        <iframe
          src={`https://${window.location.host}/monitoring`}
          width="100%"
          height="100%"
          frameBorder="0"
          style={{ border: 'none' }}
          title="Grafana Dashboard"
        />
      </DialogContent>
    </Dialog>
  );
};

export default MonitoringDialog;
