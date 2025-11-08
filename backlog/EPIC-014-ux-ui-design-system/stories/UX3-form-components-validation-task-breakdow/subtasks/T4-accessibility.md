# T4: Form Accessibility Implementation

**Parent Story:** S3 - Form Components & Validation  
**LOC:** ~100 | **Effort:** ~2h

## Objective
Ensure all form components meet WCAG 2.1 AA accessibility standards.

## Implementation

### Accessible FormTextField

```tsx
export const FormTextField: React.FC<FormTextFieldProps> = ({
  name,
  control,
  label,
  required,
  ...props
}) => (
  <Controller
    name={name}
    control={control}
    render={({ field, fieldState: { error } }) => (
      <TextField
        {...field}
        {...props}
        label={label}
        required={required}
        error={!!error}
        helperText={error?.message}
        inputProps={{
          'aria-label': label,
          'aria-required': required,
          'aria-invalid': !!error,
          'aria-describedby': error ? `${name}-error` : undefined,
        }}
        FormHelperTextProps={{
          id: error ? `${name}-error` : undefined,
          role: 'alert',
        }}
      />
    )}
  />
);
```

## Acceptance Criteria
- [ ] All form inputs have aria-label
- [ ] Required fields have aria-required
- [ ] Error messages have role="alert"
- [ ] Error states have aria-invalid
- [ ] Form submission disabled during loading
- [ ] Focus management on error (first invalid field)
- [ ] Keyboard navigation works (Tab, Enter)

## Files
- Update all components in `frontend/src/components/forms/`
- Add `frontend/src/utils/accessibility/formA11y.ts`
