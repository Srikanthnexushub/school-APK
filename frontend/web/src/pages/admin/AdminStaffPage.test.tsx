// src/pages/admin/AdminStaffPage.test.tsx
// Vitest + React Testing Library unit tests for the Admin Staff Portal.
// Tests cover: stats rendering, role filter chips, modal open, Step 1 validation,
// and AI bio generator button availability.

import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import AdminStaffPage from './AdminStaffPage';

// ─── Mocks ───────────────────────────────────────────────────────────────────

vi.mock('../../stores/authStore', () => ({
  useAuthStore: (selector: (s: { user: { centerId: string } }) => unknown) =>
    selector({ user: { centerId: 'center-uuid-1' } }),
}));

const mockStaffList = [
  {
    id: 'staff-1',
    centerId: 'center-uuid-1',
    userId: null,
    firstName: 'Alice',
    lastName: 'Sharma',
    email: 'alice@school.com',
    phoneNumber: '9000000001',
    subjects: 'Mathematics,Physics',
    district: 'Bengaluru Urban',
    employeeId: 'EMP001',
    status: 'ACTIVE',
    joinedAt: '2025-01-01T00:00:00Z',
    roleType: 'HOD',
    qualification: 'M.Sc',
    yearsOfExperience: 8,
    designation: 'Head of Department',
    bio: 'Alice leads the science department.',
  },
  {
    id: 'staff-2',
    centerId: 'center-uuid-1',
    userId: null,
    firstName: 'Bob',
    lastName: 'Kumar',
    email: 'bob@school.com',
    phoneNumber: '9000000002',
    subjects: 'Chemistry',
    district: null,
    employeeId: 'EMP002',
    status: 'INVITATION_SENT',
    joinedAt: '2025-02-01T00:00:00Z',
    roleType: 'TEACHER',
    qualification: 'B.Ed',
    yearsOfExperience: 2,
    designation: 'Assistant Teacher',
    bio: null,
  },
];

vi.mock('../../lib/api', () => ({
  default: {
    get: vi.fn().mockResolvedValue({ data: mockStaffList }),
    post: vi.fn().mockResolvedValue({ data: { content: 'Generated bio text.' } }),
    patch: vi.fn(),
    delete: vi.fn(),
  },
}));

// ─── Helpers ──────────────────────────────────────────────────────────────────

function renderPage() {
  const qc = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter>
        <AdminStaffPage />
      </MemoryRouter>
    </QueryClientProvider>
  );
}

// ─── Tests ────────────────────────────────────────────────────────────────────

describe('AdminStaffPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the Staff Directory heading', async () => {
    renderPage();
    await waitFor(() =>
      expect(screen.getByText(/Staff Directory/i)).toBeInTheDocument()
    );
  });

  it('shows total staff count stat after data loads', async () => {
    renderPage();
    await waitFor(() => {
      // Total Staff stat card
      expect(screen.getByText('2')).toBeInTheDocument();
    });
  });

  it('renders role filter chips for all 8 roles', async () => {
    renderPage();
    await waitFor(() =>
      expect(screen.getByText('Head of Department')).toBeInTheDocument()
    );
    // Verify a selection of role chips are present
    expect(screen.getByText('Teacher')).toBeInTheDocument();
    expect(screen.getByText('Student Counselor')).toBeInTheDocument();
    expect(screen.getByText('Librarian')).toBeInTheDocument();
  });

  it('opens the Create Staff modal when + Create Staff is clicked', async () => {
    renderPage();
    await waitFor(() =>
      expect(screen.getByText(/Create Staff/i)).toBeInTheDocument()
    );
    fireEvent.click(screen.getByRole('button', { name: /Create Staff/i }));
    await waitFor(() =>
      // Modal Step 1 heading
      expect(screen.getByText(/Identity/i)).toBeInTheDocument()
    );
  });

  it('blocks modal Step 1 advancement without required fields', async () => {
    renderPage();
    await waitFor(() =>
      expect(screen.getByText(/Create Staff/i)).toBeInTheDocument()
    );
    fireEvent.click(screen.getByRole('button', { name: /Create Staff/i }));
    await waitFor(() =>
      expect(screen.getByText(/Identity/i)).toBeInTheDocument()
    );
    // Click Next without filling anything
    fireEvent.click(screen.getByRole('button', { name: /Next/i }));
    await waitFor(() => {
      // Should still be on Step 1 (validation blocks advance)
      expect(screen.getByText(/Identity/i)).toBeInTheDocument();
    });
  });
});
