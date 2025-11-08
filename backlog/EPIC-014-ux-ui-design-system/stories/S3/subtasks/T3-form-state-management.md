# T3: Form State Management

**Parent Story:** S3 - Form Components & Validation  
**LOC:** ~200 | **Effort:** ~2h

## Objective
Implement form state management patterns for loading, submitting, and error handling.

## Implementation

### Form Hook Pattern

```tsx
// frontend/src/hooks/useFormSubmit.ts
import { useState } from 'react';
import { FieldValues, UseFormReturn } from 'react-hook-form';

export const useFormSubmit = <T extends FieldValues>(
  form: UseFormReturn<T>,
  onSubmit: (data: T) => Promise<void>
) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const handleSubmit = form.handleSubmit(async (data) => {
    setIsSubmitting(true);
    setSubmitError(null);
    
    try {
      await onSubmit(data);
    } catch (error) {
      setSubmitError(error instanceof Error ? error.message : 'Submission failed');
    } finally {
      setIsSubmitting(false);
    }
  });

  return {
    handleSubmit,
    isSubmitting,
    submitError,
  };
};
```

### Usage Example

```tsx
const form = useForm<UserFormData>({
  resolver: zodResolver(userFormSchema),
});

const { handleSubmit, isSubmitting, submitError } = useFormSubmit(
  form,
  async (data) => {
    await api.createUser(data);
  }
);
```

## Acceptance Criteria
- [ ] useFormSubmit hook created
- [ ] Loading states managed
- [ ] Error handling consistent
- [ ] Form reset after successful submit
- [ ] Optimistic updates supported

## Files
- `frontend/src/hooks/useFormSubmit.ts`
- `frontend/src/hooks/useFormState.ts`
