import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { renderWithProviders } from '../test/utils';
import { LanguageToggle } from '../ui/Layout';

describe('i18n + RTL', () => {
  it('switching to Hebrew sets dir=rtl on <html>', async () => {
    renderWithProviders(<LanguageToggle />);
    expect(document.documentElement.dir).toBe('ltr');

    await userEvent.click(screen.getByRole('button', { name: 'עברית' }));

    expect(document.documentElement.dir).toBe('rtl');
    expect(document.documentElement.lang).toBe('he');
    expect(screen.getByRole('button', { name: 'English' })).toBeInTheDocument();
  });
});
