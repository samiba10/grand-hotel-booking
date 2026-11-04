import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { renderWithProviders, users } from '../test/utils';
import { Can } from './Can';

describe('<Can>', () => {
  it('renders children only with the permission', () => {
    renderWithProviders(<Can permission="audit:read">secret</Can>, { user: users.guest });
    expect(screen.queryByText('secret')).not.toBeInTheDocument();
  });

  it('supports scoped own/any checks and a fallback', () => {
    renderWithProviders(
      <Can action="booking:update" ownerGuestId={2} fallback={<span>read only</span>}>
        edit
      </Can>,
      { user: users.guest },
    );
    expect(screen.getByText('read only')).toBeInTheDocument();
  });
});
