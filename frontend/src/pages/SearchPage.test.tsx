import { fireEvent, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { Availability } from '../sdk';
import { renderWithProviders, users } from '../test/utils';
import { SearchPage } from './SearchPage';

const availability: Availability = {
  hotelId: 1,
  hotelName: 'Grand Hotel',
  checkIn: '2030-01-10',
  checkOut: '2030-01-12',
  nights: 2,
  occupancyPercent: 20,
  currency: 'USD',
  fx: { rateMicros: 270270, asOf: '2030-01-01T00:00:00Z', stale: false },
  rooms: [
    { roomId: 1, roomNumber: '101', roomType: 'STANDARD', totalAmountMinor: 20000, averageNightlyMinor: 10000, currency: 'USD' },
  ],
};

describe('/search', () => {
  it('shows field errors and does not call the API for an invalid stay', async () => {
    const { sdk } = renderWithProviders(<SearchPage />, { user: users.guest });
    fireEvent.change(screen.getByLabelText('Check-out'), { target: { value: '' } });
    await userEvent.click(screen.getByRole('button', { name: 'Search' }));

    expect(await screen.findByText('Required')).toBeInTheDocument();
    expect(sdk.rooms.availability).not.toHaveBeenCalled();
  });

  it('lists rooms with integer-cent prices formatted as currency', async () => {
    const { sdk } = renderWithProviders(<SearchPage />, { user: users.guest });
    vi.mocked(sdk.rooms.availability).mockResolvedValue(availability);

    await userEvent.click(screen.getByRole('button', { name: 'Search' }));

    expect(await screen.findByText('Room 101')).toBeInTheDocument();
    expect(screen.getByText('$200.00')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Book' })).toBeInTheDocument();
    expect(screen.queryByLabelText('Book for guest #')).not.toBeInTheDocument();
  });

  it('staff must choose the guest they book for', async () => {
    const { sdk } = renderWithProviders(<SearchPage />, { user: users.staff });
    vi.mocked(sdk.rooms.availability).mockResolvedValue(availability);

    await userEvent.click(screen.getByRole('button', { name: 'Search' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Book' }));

    expect(await screen.findByText('Enter a guest number')).toBeInTheDocument();
    expect(sdk.bookings.create).not.toHaveBeenCalled();
  });

  it('hides booking for users without a create permission', async () => {
    const { sdk } = renderWithProviders(<SearchPage />, { user: users.admin });
    vi.mocked(sdk.rooms.availability).mockResolvedValue(availability);

    await userEvent.click(screen.getByRole('button', { name: 'Search' }));
    await waitFor(() => expect(screen.getByText('Room 101')).toBeInTheDocument());
    expect(screen.queryByRole('button', { name: 'Book' })).not.toBeInTheDocument();
  });
});
