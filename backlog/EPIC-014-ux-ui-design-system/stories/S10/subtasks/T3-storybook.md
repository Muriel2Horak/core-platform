# T3: Storybook Integration

**Parent Story:** S10 - Design Tokens & Documentation  
**LOC:** ~100 | **Effort:** ~1h

## Objective
Integrate Storybook for interactive component documentation.

## Implementation

```bash
# Install Storybook
npx storybook@latest init

# .storybook/main.ts
export default {
  stories: ['../src/**/*.stories.@(js|jsx|ts|tsx)'],
  addons: [
    '@storybook/addon-links',
    '@storybook/addon-essentials',
    '@storybook/addon-interactions',
    '@storybook/addon-a11y',
  ],
  framework: {
    name: '@storybook/react-vite',
    options: {},
  },
};

# .storybook/preview.tsx
import { ThemeProvider } from '@mui/material';
import { theme } from '../src/theme';

export const decorators = [
  (Story) => (
    <ThemeProvider theme={theme}>
      <Story />
    </ThemeProvider>
  ),
];

export const parameters = {
  actions: { argTypesRegex: '^on[A-Z].*' },
  controls: {
    matchers: {
      color: /(background|color)$/i,
      date: /Date$/,
    },
  },
};
```

```tsx
// frontend/src/components/Button.stories.tsx
import type { Meta, StoryObj } from '@storybook/react';
import { Button } from '@mui/material';

const meta: Meta<typeof Button> = {
  title: 'Components/Button',
  component: Button,
  tags: ['autodocs'],
  argTypes: {
    variant: {
      control: 'select',
      options: ['contained', 'outlined', 'text'],
    },
    color: {
      control: 'select',
      options: ['primary', 'secondary', 'success', 'error'],
    },
  },
};

export default meta;
type Story = StoryObj<typeof Button>;

export const Primary: Story = {
  args: {
    variant: 'contained',
    children: 'Button',
  },
};

export const Outlined: Story = {
  args: {
    variant: 'outlined',
    children: 'Button',
  },
};

export const WithIcon: Story = {
  args: {
    variant: 'contained',
    startIcon: <Add />,
    children: 'Add Item',
  },
};
```

## Acceptance Criteria
- [ ] Storybook installed
- [ ] All components have stories
- [ ] Interactive controls
- [ ] Accessibility addon (a11y)
- [ ] Dark/light mode toggle
- [ ] Auto-generated docs
- [ ] npm run storybook

## Files
- `.storybook/main.ts`
- `.storybook/preview.tsx`
- `frontend/src/**/*.stories.tsx`
- `package.json` (storybook scripts)
