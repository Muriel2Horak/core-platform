import { useState } from 'react';
// 🎨 Design System Demo - ukázka všech UX principů v praxi
import { 
  Typography, 
  Grid, 
  Box, 
  Divider,
  Switch,
  FormControlLabel,
  Alert,
  Chip
} from '@mui/material';
import { 
  CheckCircle, 
  Warning, 
  Error, 
  Info,
  Person,
  Email,
  Phone,
  LocationOn
} from '@mui/icons-material';

// Import našeho Design Systému
import {
  PageContainer,
  PageHeader,
  ContentCard,
  FormField,
  PrimaryButton,
  SecondaryButton,
  DestructiveButton,
  LoadingSkeleton,
  LoadingSpinner,
  SuccessAlert,
  WarningAlert,
  ErrorAlert,
  InfoAlert,
  ResponsiveGrid,
  EmptyState,
  AccessibleButton
} from './DesignSystem';

const DesignSystemDemo = () => {
  const [loading, setLoading] = useState(false);
  const [showEmptyState, setShowEmptyState] = useState(false);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone: '',
    role: ''
  });
  const [formErrors, setFormErrors] = useState({});

  // Simulace loading stavu
  const handleLoadingDemo = () => {
    setLoading(true);
    setTimeout(() => setLoading(false), 3000);
  };

  // Validace formuláře
  const validateForm = () => {
    const errors = {};
    if (!formData.name) errors.name = 'Jméno je povinné';
    if (!formData.email) errors.email = 'E-mail je povinný';
    else if (!/\S+@\S+\.\S+/.test(formData.email)) errors.email = 'Neplatný e-mail';
    
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleFormSubmit = (e) => {
    e.preventDefault();
    if (validateForm()) {
      alert('✅ Formulář úspěšně odeslán!');
    }
  };

  return (
    <PageContainer>
      {/* 🔤 TYPOGRAFIE - H1 → H2 → H3 hierarchie */}
      <PageHeader 
        title="Design System Demo"
        subtitle="Ukázka všech UX principů a designových pravidel"
        breadcrumbs={[
          { label: 'Domů', href: '/' },
          { label: 'Komponenty', href: '/components' },
          { label: 'Design System' }
        ]}
        actions={[
          <PrimaryButton key="primary">Hlavní akce</PrimaryButton>,
          <SecondaryButton key="secondary">Vedlejší akce</SecondaryButton>
        ]}
      />

      {/* 🌈 BAREVNOST - smysluplné použití */}
      <ContentCard title="1. Barevnost a Feedback">
        <Typography variant="body1" sx={{ mb: 3, lineHeight: 1.6 }}>
          Barvy používáme jen se smyslem - každá má svůj účel podle UX pravidel:
        </Typography>
        
        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid item xs={12} sm={6} md={3}>
            <SuccessAlert icon={<CheckCircle />}>
              ✅ Úspěch - zelená
            </SuccessAlert>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <WarningAlert icon={<Warning />}>
              ⚠️ Varování - oranžová
            </WarningAlert>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <ErrorAlert icon={<Error />}>
              ❌ Chyba - červená
            </ErrorAlert>
          </Grid>
          <Grid item xs={12} sm={6} md={3}>
            <InfoAlert icon={<Info />}>
              ℹ️ Informace - modrá
            </InfoAlert>
          </Grid>
        </Grid>

        <Typography variant="body2" color="text.secondary">
          💡 Barva není jediný nositel významu - kombinujeme s ikonami a textem (WCAG AA)
        </Typography>
      </ContentCard>

      {/* 🖱️ TLAČÍTKA - konzistentní podle typu akce */}
      <ContentCard title="2. Tlačítka podle typu akce">
        <Typography variant="body1" sx={{ mb: 3, lineHeight: 1.6 }}>
          Každý typ akce má svůj styl tlačítka pro lepší UX:
        </Typography>
        
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mb: 3 }}>
          <PrimaryButton>Uložit změny</PrimaryButton>
          <SecondaryButton>Zrušit</SecondaryButton>
          <DestructiveButton>Smazat účet</DestructiveButton>
        </Box>

        <Typography variant="body2" color="text.secondary">
          ✅ Primární (výrazné) → Sekundární (méně výrazné) → Destruktivní (červené)
        </Typography>
      </ContentCard>

      {/* 📝 FORMULÁŘE - podle UX pravidel */}
      <ContentCard title="3. Formuláře podle UX pravidel">
        <Typography variant="body1" sx={{ mb: 3, lineHeight: 1.6 }}>
          Label vždy nahoře (aligned left), placeholder jen jako doplněk, chyby pod polem:
        </Typography>
        
        <Box component="form" onSubmit={handleFormSubmit}>
          <Grid container spacing={3}>
            <Grid item xs={12} md={6}>
              <FormField
                label="Celé jméno"
                placeholder="Jan Novák"
                required
                value={formData.name}
                onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                error={formErrors.name}
                InputProps={{
                  startAdornment: <Person sx={{ mr: 1, color: 'text.secondary' }} />
                }}
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <FormField
                label="E-mailová adresa"
                type="email"
                placeholder="jan@example.com"
                required
                value={formData.email}
                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                error={formErrors.email}
                InputProps={{
                  startAdornment: <Email sx={{ mr: 1, color: 'text.secondary' }} />
                }}
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <FormField
                label="Telefon"
                placeholder="+420 123 456 789"
                helperText="Volitelné - pro důležitá upozornění"
                value={formData.phone}  
                onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                InputProps={{
                  startAdornment: <Phone sx={{ mr: 1, color: 'text.secondary' }} />
                }}
              />
            </Grid>
            
            <Grid item xs={12} md={6}>
              <FormField
                label="Role v organizaci"
                select
                helperText="Vyberte svou hlavní roli"
                value={formData.role}
                onChange={(e) => setFormData({ ...formData, role: e.target.value })}
                SelectProps={{
                  native: true
                }}
              >
                <option value="">Vyberte roli...</option>
                <option value="admin">Administrátor</option>
                <option value="manager">Manažer</option>
                <option value="user">Uživatel</option>
              </FormField>
            </Grid>
          </Grid>
          
          <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end', mt: 3 }}>
            <SecondaryButton type="button">Zrušit</SecondaryButton>
            <PrimaryButton type="submit">Uložit profil</PrimaryButton>
          </Box>
        </Box>
      </ContentCard>

      {/* 🔄 LOADING STATES - skeleton nebo spinner, ne „nic" */}
      <ContentCard title="4. Loading States - skeleton nebo spinner">
        <Typography variant="body1" sx={{ mb: 3, lineHeight: 1.6 }}>
          Nikdy neukazujeme prázdnou stránku - vždy skeleton nebo spinner:
        </Typography>
        
        <Box sx={{ mb: 3 }}>
          <PrimaryButton onClick={handleLoadingDemo} disabled={loading}>
            {loading ? 'Načítání...' : 'Spustit demo loading'}
          </PrimaryButton>
        </Box>

        {loading ? (
          <Box>
            <Typography variant="h6" sx={{ mb: 2 }}>Loading s popisem:</Typography>
            <LoadingSpinner message="Načítám uživatelská data..." />
            
            <Divider sx={{ my: 3 }} />
            
            <Typography variant="h6" sx={{ mb: 2 }}>Skeleton loading:</Typography>
            <LoadingSkeleton variant="text" count={3} />
            <LoadingSkeleton variant="rectangular" height={120} />
          </Box>
        ) : (
          <Alert severity="info">
            Klikněte na tlačítko výše pro ukázku loading stavů
          </Alert>
        )}
      </ContentCard>

      {/* 🖼️ LAYOUT - Grid systém (12 columns) */}
      <ContentCard title="5. Responsive Grid - max 2-3 sloupce">
        <Typography variant="body1" sx={{ mb: 3, lineHeight: 1.6 }}>
          Grid systém s max 2-3 sloupci na desktopu, aby uživatel neztrácel přehled:
        </Typography>
        
        <ResponsiveGrid columns={{ xs: 1, sm: 2, md: 3 }}>
          <ContentCard>
            <Box sx={{ textAlign: 'center', py: 2 }}>
              <Person sx={{ fontSize: 48, color: 'primary.main', mb: 1 }} />
              <Typography variant="h6">Uživatelé</Typography>
              <Typography variant="body2" color="text.secondary">
                Správa uživatelských účtů
              </Typography>
            </Box>
          </ContentCard>
          
          <ContentCard>
            <Box sx={{ textAlign: 'center', py: 2 }}>
              <LocationOn sx={{ fontSize: 48, color: 'secondary.main', mb: 1 }} />
              <Typography variant="h6">Pobočky</Typography>
              <Typography variant="body2" color="text.secondary">
                Geografické lokace
              </Typography>
            </Box>
          </ContentCard>
          
          <ContentCard>
            <Box sx={{ textAlign: 'center', py: 2 }}>
              <Email sx={{ fontSize: 48, color: 'success.main', mb: 1 }} />
              <Typography variant="h6">Komunikace</Typography>
              <Typography variant="body2" color="text.secondary">
                E-maily a notifikace
              </Typography>
            </Box>
          </ContentCard>
        </ResponsiveGrid>
      </ContentCard>

      {/* 🎯 EMPTY STATES */}
      <ContentCard title="6. Empty States - lepší než prázdná stránka">
        <Typography variant="body1" sx={{ mb: 3, lineHeight: 1.6 }}>
          Místo prázdné stránky ukážeme užitečné empty state:
        </Typography>
        
        <FormControlLabel
          control={
            <Switch 
              checked={showEmptyState}
              onChange={(e) => setShowEmptyState(e.target.checked)}
            />
          }
          label="Zobrazit empty state"
        />
        
        {showEmptyState ? (
          <EmptyState
            icon={<Person sx={{ fontSize: 64 }} />}
            title="Žádní uživatelé"
            description="Zatím zde nejsou žádní uživatelé. Začněte přidáním prvního uživatelského účtu."
            action={
              <PrimaryButton onClick={() => setShowEmptyState(false)}>
                Přidat prvního uživatele
              </PrimaryButton>
            }
          />
        ) : (
          <Alert severity="success">
            ✅ Máte {Math.floor(Math.random() * 50) + 1} aktivních uživatelů
          </Alert>
        )}
      </ContentCard>

      {/* ♿ ACCESSIBILITY - focus outline, ARIA labels */}
      <ContentCard title="7. Přístupnost (A11y) - WCAG AA">
        <Typography variant="body1" sx={{ mb: 3, lineHeight: 1.6 }}>
          Focus outline nikdy neskrýváme, ARIA labely pro lepší UX:
        </Typography>
        
        <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', mb: 3 }}>
          <AccessibleButton
            variant="contained"
            ariaLabel="Uložit dokument"
            tooltip="Uloží současné změny v dokumentu"
          >
            Uložit
          </AccessibleButton>
          
          <AccessibleButton
            variant="outlined"
            ariaLabel="Zavřít dialog"
            tooltip="Zavře aktuální dialog bez uložení"
          >
            ×
          </AccessibleButton>
        </Box>

        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          <Chip label="Klávesová navigace ✓" color="success" size="small" />
          <Chip label="WCAG AA kontrast ✓" color="success" size="small" />
          <Chip label="ARIA labels ✓" color="success" size="small" />
          <Chip label="Focus management ✓" color="success" size="small" />
        </Box>
      </ContentCard>

      {/* ✅ CHECKLIST */}
      <ContentCard title="✅ Design System Checklist">
        <Typography variant="body1" sx={{ mb: 3, lineHeight: 1.6 }}>
          Kontrolní seznam pro vývojáře:
        </Typography>
        
        <Grid container spacing={2}>
          <Grid item xs={12} md={6}>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <CheckCircle sx={{ color: 'success.main', mr: 1 }} />
              <Typography variant="body2">Max 2 fonty (Inter + monospace)</Typography>
            </Box>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <CheckCircle sx={{ color: 'success.main', mr: 1 }} />
              <Typography variant="body2">Barevná hierarchie (primary, feedback)</Typography>
            </Box>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <CheckCircle sx={{ color: 'success.main', mr: 1 }} />
              <Typography variant="body2">WCAG AA kontrast min. 4.5:1</Typography>
            </Box>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <CheckCircle sx={{ color: 'success.main', mr: 1 }} />
              <Typography variant="body2">Labels nad formulářovými poli</Typography>
            </Box>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <CheckCircle sx={{ color: 'success.main', mr: 1 }} />
              <Typography variant="body2">Loading states místo prázdných stránek</Typography>
            </Box>
          </Grid>
          <Grid item xs={12} md={6}>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <CheckCircle sx={{ color: 'success.main', mr: 1 }} />
              <Typography variant="body2">Focus outline nikdy neskrývám</Typography>
            </Box>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <CheckCircle sx={{ color: 'success.main', mr: 1 }} />
              <Typography variant="body2">Breadcrumbs pro hierarchické části</Typography>
            </Box>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <CheckCircle sx={{ color: 'success.main', mr: 1 }} />
              <Typography variant="body2">Max 2-3 sloupce na desktopu</Typography>
            </Box>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <CheckCircle sx={{ color: 'success.main', mr: 1 }} />
              <Typography variant="body2">Konzistentní spacing (4/8/16px)</Typography>
            </Box>
            <Box sx={{ display: 'flex', alignItems: 'center', mb: 1 }}>
              <CheckCircle sx={{ color: 'success.main', mr: 1 }} />
              <Typography variant="body2">Smysluplné použití barev</Typography>
            </Box>
          </Grid>
        </Grid>
      </ContentCard>
    </PageContainer>
  );
};

export default DesignSystemDemo;