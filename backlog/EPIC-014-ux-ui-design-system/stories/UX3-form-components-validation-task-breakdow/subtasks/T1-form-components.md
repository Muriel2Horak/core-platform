# T1: Form Component Library

**Parent Story:** S3 - Form Components & Validation  
**LOC:** ~300 | **Effort:** ~4h

## Objective
Create reusable form components based on Material-UI with consistent styling and behavior.

## Implementation

### Components to Create

1. **FormTextField** (`frontend/src/components/forms/FormTextField.tsx`)
```tsx
import { Controller } from 'react-hook-form';
import { TextField, TextFieldProps } from '@mui/material';

interface FormTextFieldProps extends Omit<TextFieldProps, 'name'> {
  name: string;
  control: any;
  rules?: any;
}

export const FormTextField: React.FC<FormTextFieldProps> = ({
  name,
  control,
  rules,
  ...textFieldProps
}) => (
  <Controller
    name={name}
    control={control}
    rules={rules}
    render={({ field, fieldState: { error } }) => (
      <TextField
        {...field}
        {...textFieldProps}
        error={!!error}
        helperText={error?.message || textFieldProps.helperText}
      />
    )}
  />
);
```

2. **FormSelect** (`frontend/src/components/forms/FormSelect.tsx`)
3. **FormCheckbox** (`frontend/src/components/forms/FormCheckbox.tsx`)
4. **FormDatePicker** (`frontend/src/components/forms/FormDatePicker.tsx`)
5. **FormFileUpload** (`frontend/src/components/forms/FormFileUpload.tsx`)

## Acceptance Criteria
- [ ] All 5 form components created
- [ ] Components integrate with react-hook-form Controller
- [ ] Error states display consistently
- [ ] Components accept all standard MUI props
- [ ] TypeScript interfaces defined

## Files
- `frontend/src/components/forms/FormTextField.tsx`
- `frontend/src/components/forms/FormSelect.tsx`
- `frontend/src/components/forms/FormCheckbox.tsx`
- `frontend/src/components/forms/FormDatePicker.tsx`
- `frontend/src/components/forms/FormFileUpload.tsx`
- `frontend/src/components/forms/index.ts`
