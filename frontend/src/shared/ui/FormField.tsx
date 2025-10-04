/**
 * 📝 FormField - Core Platform Form Field Component
 * 
 * Wrapper kolem MUI TextField s always-controlled behavior.
 * Automaticky zobrazuje "—" pro prázdné hodnoty, má readonly styling.
 */

import React from 'react';
import { TextField, TextFieldProps, InputAdornment } from '@mui/material';
import { tokens } from '../theme/tokens';

export interface FormFieldProps extends Omit<TextFieldProps, 'value' | 'defaultValue'> {
  /** Field value - always controlled */
  value?: string | number;
  /** Field label */
  label: string;
  /** Help text below field */
  helperText?: string;
  /** Error state */
  error?: boolean;
  /** Error message */
  errorMessage?: string;
  /** Readonly state */
  readonly?: boolean;
  /** Show empty dash for empty values */
  showEmptyDash?: boolean;
  /** Start icon */
  startIcon?: React.ReactNode;
  /** End icon */
  endIcon?: React.ReactNode;
}

/**
 * FormField komponent s Core Platform design systémem
 */
export const FormField: React.FC<FormFieldProps> = ({
  value = '',
  label,
  helperText,
  error = false,
  errorMessage,
  readonly = false,
  showEmptyDash = false,
  startIcon,
  endIcon,
  required,
  disabled,
  multiline,
  rows,
  type = 'text',
  placeholder,
  onChange,
  onBlur,
  onFocus,
  sx,
  ...props
}) => {
  // 🔄 Always controlled value with fallback
  const controlledValue = value ?? '';

  // 📝 Display value - show dash for empty readonly fields
  const displayValue = (() => {
    if (readonly && showEmptyDash && !controlledValue) {
      return '—'; // Em dash pro prázdné readonly hodnoty
    }
    return controlledValue;
  })();

  // 🎨 Readonly styling
  const readonlyStyles = readonly ? {
    '& .MuiOutlinedInput-root': {
      backgroundColor: tokens.colors.grey[50],
      cursor: 'not-allowed',

      '& .MuiOutlinedInput-input': {
        cursor: 'not-allowed',
        color: showEmptyDash && !controlledValue ? tokens.colors.grey[500] : 'inherit',
        fontStyle: showEmptyDash && !controlledValue ? 'italic' : 'normal',
      },

      '& fieldset': {
        borderColor: tokens.colors.grey[300],
      },

      '&:hover fieldset': {
        borderColor: tokens.colors.grey[300], // No hover effect for readonly
      },
    },
  } : {};

  // 🎯 Input adornments
  const getInputProps = () => {
    const inputProps: {
      startAdornment?: React.ReactNode;
      endAdornment?: React.ReactNode;
    } = {};

    if (startIcon || endIcon) {
      inputProps.startAdornment = startIcon ? (
        <InputAdornment position="start" sx={{ color: tokens.colors.grey[500] }}>
          {startIcon}
        </InputAdornment>
      ) : undefined;

      inputProps.endAdornment = endIcon ? (
        <InputAdornment position="end" sx={{ color: tokens.colors.grey[500] }}>
          {endIcon}
        </InputAdornment>
      ) : undefined;
    }

    return inputProps;
  };

  // 📱 ARIA attributes
  const ariaProps = {
    'aria-label': readonly && showEmptyDash && !controlledValue ? 'Hodnota není uvedena' : undefined,
    'aria-readonly': readonly,
    'aria-required': required,
    'aria-invalid': error,
    'aria-describedby': helperText || errorMessage ? `${props.id || label}-helper-text` : undefined,
  };

  return (
    <TextField
      {...props}
      label={label}
      value={displayValue}
      error={error}
      helperText={error && errorMessage ? errorMessage : helperText}
      required={required}
      disabled={disabled || readonly}
      multiline={multiline}
      rows={rows}
      type={type}
      placeholder={readonly ? undefined : placeholder}
      onChange={readonly ? undefined : onChange}
      onBlur={readonly ? undefined : onBlur}
      onFocus={readonly ? undefined : onFocus}
      InputProps={{
        readOnly: readonly,
        ...getInputProps(),
        ...ariaProps,
      }}
      FormHelperTextProps={{
        id: `${props.id || label}-helper-text`,
      }}
      sx={{
        // Base styling
        '& .MuiOutlinedInput-root': {
          borderRadius: tokens.radius.md,
          backgroundColor: tokens.colors.white,
          minHeight: tokens.components.formField.height,
          transition: `all ${tokens.components.animation.normal} ${tokens.components.animation.easing}`,

          '& fieldset': {
            borderColor: tokens.colors.grey[300],
            transition: `border-color ${tokens.components.animation.normal} ${tokens.components.animation.easing}`,
          },

          '&:hover fieldset': {
            borderColor: tokens.colors.primary[400],
          },

          '&.Mui-focused fieldset': {
            borderColor: tokens.colors.primary[500],
            borderWidth: '2px',
          },

          '&.Mui-error fieldset': {
            borderColor: tokens.colors.error[500],
          },

          // Focus styling integrated here
          '&:focus-within': {
            outline: readonly ? 'none' : `${tokens.a11y.focusRing.width} ${tokens.a11y.focusRing.style} ${tokens.a11y.focusRing.color}`,
            outlineOffset: tokens.a11y.focusRing.offset,
          },
        },

        // Label styling
        '& .MuiInputLabel-root': {
          fontSize: tokens.typography.fontSize.base,
          fontWeight: tokens.typography.fontWeight.medium,
          color: tokens.colors.grey[600],

          '&.Mui-focused': {
            color: tokens.colors.primary[600],
          },

          '&.Mui-error': {
            color: tokens.colors.error[600],
          },
        },

        // Helper text styling
        '& .MuiFormHelperText-root': {
          fontSize: tokens.typography.fontSize.sm,
          marginTop: tokens.spacing.xs,

          '&.Mui-error': {
            color: tokens.colors.error[600],
          },
        },

        // Input text styling
        '& .MuiOutlinedInput-input': {
          fontSize: tokens.typography.fontSize.base,
          padding: `${tokens.spacing.md} ${tokens.spacing.md}`,
        },

        // Readonly specific styles
        ...readonlyStyles,

        // Custom styles
        ...sx,
      }}
    />
  );
};

export default FormField;