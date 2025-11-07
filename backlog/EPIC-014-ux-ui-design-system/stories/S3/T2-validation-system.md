# T2: Validation System Integration

**Parent Story:** S3 - Form Components & Validation  
**LOC:** ~200 | **Effort:** ~2h

## Objective
Integrate Zod validation schema library with react-hook-form for consistent validation.

## Implementation

### Validation Schema Example

```tsx
// frontend/src/validation/schemas/user.schema.ts
import { z } from 'zod';

export const userFormSchema = z.object({
  email: z.string().email('Invalid email format'),
  password: z
    .string()
    .min(8, 'Password must be at least 8 characters')
    .regex(/[A-Z]/, 'Password must contain uppercase letter'),
  confirmPassword: z.string(),
}).refine((data) => data.password === data.confirmPassword, {
  message: "Passwords don't match",
  path: ['confirmPassword'],
});

export type UserFormData = z.infer<typeof userFormSchema>;
```

### Usage in Form

```tsx
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { userFormSchema, UserFormData } from '@/validation/schemas/user.schema';

const { control, handleSubmit } = useForm<UserFormData>({
  resolver: zodResolver(userFormSchema),
  defaultValues: {
    email: '',
    password: '',
    confirmPassword: '',
  },
});
```

## Acceptance Criteria
- [ ] Zod installed and configured
- [ ] @hookform/resolvers/zod installed
- [ ] Common validation schemas created (user, tenant, etc.)
- [ ] Error messages user-friendly
- [ ] Cross-field validation works (e.g., password confirmation)

## Files
- `frontend/src/validation/schemas/user.schema.ts`
- `frontend/src/validation/schemas/tenant.schema.ts`
- `frontend/src/validation/common/validators.ts`
