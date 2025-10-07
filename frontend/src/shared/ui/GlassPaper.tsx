/**
 * 🎨 GlassPaper - Glassmorphic Paper Component
 * 
 * Přepoužitelný Paper komponent s glassmorphic efektem a podporou dark/light mode
 */

import { Paper, PaperProps } from '@mui/material';
import { useTheme } from '@mui/material/styles';

export interface GlassPaperProps extends PaperProps {
  /** Intenzita blur efektu */
  blur?: number;
  /** Průhlednost pozadí (0-1) */
  opacity?: number;
  /** Vlastní pozadí místo výchozího */
  customBackground?: string;
}

export const GlassPaper: React.FC<GlassPaperProps> = ({ 
  blur = 20, 
  opacity = 0.7,
  customBackground,
  sx,
  children,
  ...props 
}) => {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  const defaultBackground = isDark
    ? `rgba(30, 30, 30, ${opacity * 0.85})`
    : `rgba(255, 255, 255, ${opacity})`;

  return (
    <Paper
      {...props}
      sx={{
        background: customBackground || defaultBackground,
        backdropFilter: `blur(${blur}px)`,
        border: `1px solid ${isDark ? 'rgba(255,255,255,0.1)' : 'rgba(0,0,0,0.05)'}`,
        ...sx,
      }}
    >
      {children}
    </Paper>
  );
};

export default GlassPaper;
