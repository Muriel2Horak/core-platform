import React, { useState, useEffect } from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  IconButton,
  Typography,
  Alert,
  CircularProgress,
  Accordion,
  AccordionSummary,
  AccordionDetails,
  Chip,
  List,
  ListItem,
  ListItemText,
  Tooltip,
} from '@mui/material';
import HelpOutlineIcon from '@mui/icons-material/HelpOutline';
import CloseIcon from '@mui/icons-material/Close';
import ExpandMoreIcon from '@mui/icons-material/ExpandMore';
import InfoIcon from '@mui/icons-material/Info';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import WarningIcon from '@mui/icons-material/Warning';

interface AiHelpWidgetProps {
  routeId: string;
  visible?: boolean;
}

interface AiContext {
  route: {
    routeId: string;
    viewKind: string;
    entity: string;
    title: string;
  };
  fields?: Array<{
    name: string;
    type: string;
    label: string;
    required?: boolean;
    pii?: boolean;
    helpSafe?: boolean;
  }>;
  actions?: Array<{
    code: string;
    label: string;
    help?: string;
    icon?: string;
    dangerous?: boolean;
    howto?: string[];
    preconditions?: string[];
    postconditions?: string[];
  }>;
  validations?: Array<{
    field: string;
    rule: string;
    message: string;
  }>;
  state?: {
    current?: string;
    updating?: boolean;
  };
}

/**
 * AI Help Widget - Frontend Component
 * 
 * Displays AI-powered help for the current screen/route
 * 
 * Features:
 * - Fetches AI context from /api/ai/context
 * - Shows structured help: fields, actions, howto steps, validations
 * - Only visible when AI_ENABLED=true
 * - Shows "Probíhá aktualizace" overlay when state.updating=true
 * - META_ONLY mode - no data values displayed
 */
export const AiHelpWidget: React.FC<AiHelpWidgetProps> = ({ routeId, visible = true }) => {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [context, setContext] = useState<AiContext | null>(null);
  const [aiEnabled, setAiEnabled] = useState(false);

  // Check if AI is enabled on mount
  useEffect(() => {
    checkAiStatus();
  }, []);

  const checkAiStatus = async () => {
    try {
      const response = await fetch('/api/admin/ai/status');
      if (response.ok) {
        const status = await response.json();
        setAiEnabled(status.enabled === true);
      }
    } catch (err) {
      console.debug('AI status check failed, assuming disabled:', err);
      setAiEnabled(false);
    }
  };

  const fetchContext = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch(`/api/ai/context?routeId=${encodeURIComponent(routeId)}`);
      
      if (response.status === 404) {
        setError('AI funkce nejsou dostupné. Kontaktujte administrátora.');
        return;
      }
      
      if (response.status === 423) {
        setError('Stránka je momentálně uzamčena pro úpravy.');
        return;
      }
      
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      
      const data = await response.json();
      setContext(data);
    } catch (err: any) {
      console.error('Failed to fetch AI context:', err);
      setError(`Chyba při načítání nápovědy: ${err.message}`);
    } finally {
      setLoading(false);
    }
  };

  const handleOpen = () => {
    setOpen(true);
    fetchContext();
  };

  const handleClose = () => {
    setOpen(false);
  };

  // Don't render if AI is disabled or widget is hidden
  if (!aiEnabled || !visible) {
    return null;
  }

  return (
    <>
      {/* Help Button */}
      <Tooltip title="Zobrazit nápovědu s pomocí AI">
        <Button
          variant="outlined"
          color="primary"
          startIcon={<HelpOutlineIcon />}
          onClick={handleOpen}
          data-testid="ai-help-button"
          sx={{ borderRadius: 2 }}
        >
          Nápověda
        </Button>
      </Tooltip>

      {/* Help Dialog */}
      <Dialog
        open={open}
        onClose={handleClose}
        maxWidth="md"
        fullWidth
        data-testid="ai-help-dialog"
      >
        <DialogTitle>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <HelpOutlineIcon color="primary" />
              <Typography variant="h6">
                {context?.route?.title || 'Nápověda'}
              </Typography>
              <Chip label="META_ONLY" size="small" color="info" variant="outlined" />
            </Box>
            <IconButton onClick={handleClose} size="small">
              <CloseIcon />
            </IconButton>
          </Box>
        </DialogTitle>

        <DialogContent dividers>
          {loading && (
            <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
              <CircularProgress />
            </Box>
          )}

          {error && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {error}
            </Alert>
          )}

          {context?.state?.updating && (
            <Alert severity="warning" icon={<WarningIcon />} sx={{ mb: 2 }}>
              <strong>Probíhá aktualizace</strong> - Data se právě zpracovávají. Některé akce mohou být dočasně nedostupné.
            </Alert>
          )}

          {context && !loading && (
            <Box>
              {/* Route Info */}
              <Accordion defaultExpanded>
                <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                  <Typography variant="h6">📋 Co tato stránka zobrazuje</Typography>
                </AccordionSummary>
                <AccordionDetails>
                  <List dense>
                    <ListItem>
                      <ListItemText
                        primary="Typ pohledu"
                        secondary={context.route.viewKind || 'N/A'}
                      />
                    </ListItem>
                    <ListItem>
                      <ListItemText
                        primary="Entita"
                        secondary={context.route.entity || 'N/A'}
                      />
                    </ListItem>
                    {context.state?.current && (
                      <ListItem>
                        <ListItemText
                          primary="Aktuální stav"
                          secondary={context.state.current}
                        />
                      </ListItem>
                    )}
                  </List>
                </AccordionDetails>
              </Accordion>

              {/* Fields */}
              {context.fields && context.fields.length > 0 && (
                <Accordion>
                  <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                    <Typography variant="h6">📝 Pole ({context.fields.length})</Typography>
                  </AccordionSummary>
                  <AccordionDetails>
                    <List dense>
                      {context.fields.map((field) => (
                        <ListItem key={field.name}>
                          <ListItemText
                            primary={
                              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                                <Typography variant="body1">{field.label}</Typography>
                                {field.required && (
                                  <Chip label="Povinné" size="small" color="error" />
                                )}
                                {field.pii && (
                                  <Chip label="PII" size="small" color="warning" />
                                )}
                              </Box>
                            }
                            secondary={
                              <Box>
                                <Typography variant="caption" color="text.secondary">
                                  {field.name} ({field.type})
                                </Typography>
                              </Box>
                            }
                          />
                        </ListItem>
                      ))}
                    </List>
                  </AccordionDetails>
                </Accordion>
              )}

              {/* Actions */}
              {context.actions && context.actions.length > 0 && (
                <Accordion defaultExpanded>
                  <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                    <Typography variant="h6">⚡ Možné akce ({context.actions.length})</Typography>
                  </AccordionSummary>
                  <AccordionDetails>
                    <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
                      {context.actions.map((action) => (
                        <Box key={action.code} sx={{ p: 2, bgcolor: 'grey.50', borderRadius: 1 }}>
                          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
                            <Typography variant="subtitle1" fontWeight="bold">
                              {action.icon} {action.label}
                            </Typography>
                            {action.dangerous && (
                              <Chip label="⚠️ Nebezpečná akce" size="small" color="error" />
                            )}
                          </Box>

                          {action.help && (
                            <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
                              {action.help}
                            </Typography>
                          )}

                          {action.preconditions && action.preconditions.length > 0 && (
                            <Box sx={{ mb: 1 }}>
                              <Typography variant="caption" color="text.secondary">
                                <InfoIcon fontSize="small" sx={{ verticalAlign: 'middle', mr: 0.5 }} />
                                Podmínky:
                              </Typography>
                              <List dense>
                                {action.preconditions.map((cond, idx) => (
                                  <ListItem key={idx} sx={{ pl: 3 }}>
                                    <ListItemText
                                      primary={cond}
                                      primaryTypographyProps={{ variant: 'caption' }}
                                    />
                                  </ListItem>
                                ))}
                              </List>
                            </Box>
                          )}

                          {action.howto && action.howto.length > 0 && (
                            <Box>
                              <Typography variant="caption" fontWeight="bold" color="primary">
                                <CheckCircleIcon fontSize="small" sx={{ verticalAlign: 'middle', mr: 0.5 }} />
                                Postup:
                              </Typography>
                              <List dense>
                                {action.howto.map((step, idx) => (
                                  <ListItem key={idx} sx={{ pl: 3 }}>
                                    <ListItemText
                                      primary={`${idx + 1}. ${step}`}
                                      primaryTypographyProps={{ variant: 'body2' }}
                                    />
                                  </ListItem>
                                ))}
                              </List>
                            </Box>
                          )}

                          {action.postconditions && action.postconditions.length > 0 && (
                            <Box sx={{ mt: 1 }}>
                              <Typography variant="caption" color="text.secondary">
                                Výsledek:
                              </Typography>
                              <List dense>
                                {action.postconditions.map((post, idx) => (
                                  <ListItem key={idx} sx={{ pl: 3 }}>
                                    <ListItemText
                                      primary={post}
                                      primaryTypographyProps={{ variant: 'caption' }}
                                    />
                                  </ListItem>
                                ))}
                              </List>
                            </Box>
                          )}
                        </Box>
                      ))}
                    </Box>
                  </AccordionDetails>
                </Accordion>
              )}

              {/* Validations */}
              {context.validations && context.validations.length > 0 && (
                <Accordion>
                  <AccordionSummary expandIcon={<ExpandMoreIcon />}>
                    <Typography variant="h6">✓ Validace ({context.validations.length})</Typography>
                  </AccordionSummary>
                  <AccordionDetails>
                    <List dense>
                      {context.validations.map((val, idx) => (
                        <ListItem key={idx}>
                          <ListItemText
                            primary={val.field}
                            secondary={`${val.rule}: ${val.message}`}
                          />
                        </ListItem>
                      ))}
                    </List>
                  </AccordionDetails>
                </Accordion>
              )}

              {/* Footer Note */}
              <Alert severity="info" icon={<InfoIcon />} sx={{ mt: 2 }}>
                <Typography variant="caption">
                  <strong>META_ONLY režim:</strong> Tato nápověda zobrazuje pouze metadata (strukturu a pravidla).
                  Žádné reálné hodnoty dat nejsou zpřístupněny AI.
                </Typography>
              </Alert>
            </Box>
          )}
        </DialogContent>

        <DialogActions>
          <Button onClick={handleClose} color="primary">
            Zavřít
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default AiHelpWidget;
