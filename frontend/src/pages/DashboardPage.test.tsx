import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { render } from '@/test/test-utils'
import { DashboardPage } from '@/pages/DashboardPage'
import type { UserProfile, Transaction } from '@/types'

const mockUser: UserProfile = {
  userId: 'user-1',
  username: 'testuser',
  email: 'test@example.com',
  accounts: [
    {
      id: 'acc-1',
      userId: 'user-1',
      accountNumber: 'ACC-001',
      balance: 5000,
      currency: 'USD',
      status: 'ACTIVE',
      createdAt: '2026-01-01T00:00:00Z',
    },
  ],
}

const mockTransactions: Transaction[] = [
  {
    id: 'txn-1',
    type: 'TRANSFER',
    status: 'COMPLETED',
    sourceAccountId: 'acc-1',
    targetAccountId: 'acc-2',
    amount: 100,
    currency: 'USD',
    description: 'Lunch money',
    createdAt: '2026-02-15T10:00:00Z',
    completedAt: '2026-02-15T10:00:01Z',
  },
  {
    id: 'txn-2',
    type: 'TRANSFER',
    status: 'COMPLETED',
    sourceAccountId: 'acc-2',
    targetAccountId: 'acc-1',
    amount: 250,
    currency: 'USD',
    description: 'Payment',
    createdAt: '2026-02-14T09:00:00Z',
    completedAt: '2026-02-14T09:00:01Z',
  },
]

// Mock useAuth
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    user: mockUser,
    isAuthenticated: true,
    isLoading: false,
    token: 'test-token',
    login: vi.fn(),
    logout: vi.fn(),
    refreshProfile: vi.fn(),
  }),
}))

// Mock api — cannot reference mockTransactions here because vi.mock is hoisted
vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
}))

// Mock sonner
vi.mock('sonner', () => ({
  toast: { success: vi.fn(), error: vi.fn(), warning: vi.fn() },
  Toaster: () => null,
}))

describe('DashboardPage', () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    const api = await import('@/lib/api')
    vi.mocked(api.default.get).mockResolvedValue({ data: mockTransactions })
  })

  it('displays welcome message with username', async () => {
    render(<DashboardPage />)
    expect(screen.getByText(/welcome back, testuser/i)).toBeInTheDocument()
  })

  it('displays account balance', async () => {
    render(<DashboardPage />)
    await waitFor(() => {
      expect(screen.getByText('$5,000.00')).toBeInTheDocument()
    })
  })

  it('displays account number', async () => {
    render(<DashboardPage />)
    await waitFor(() => {
      expect(screen.getByText(/ACC-001/)).toBeInTheDocument()
    })
  })

  it('displays account status', async () => {
    render(<DashboardPage />)
    await waitFor(() => {
      expect(screen.getByText('active')).toBeInTheDocument()
    })
  })

  it('shows quick action links', () => {
    render(<DashboardPage />)
    expect(screen.getByText('Send Money')).toBeInTheDocument()
    expect(screen.getByText('View Activity')).toBeInTheDocument()
  })

  it('displays recent transactions', async () => {
    render(<DashboardPage />)
    await waitFor(() => {
      expect(screen.getByText('Lunch money')).toBeInTheDocument()
      expect(screen.getByText('Payment')).toBeInTheDocument()
    })
  })

  it('shows Sent label for outgoing transactions', async () => {
    render(<DashboardPage />)
    await waitFor(() => {
      // txn-1 is sent from acc-1
      expect(screen.getByText('Sent')).toBeInTheDocument()
    })
  })

  it('shows Received label for incoming transactions', async () => {
    render(<DashboardPage />)
    await waitFor(() => {
      // txn-2 is received at acc-1
      expect(screen.getByText('Received')).toBeInTheDocument()
    })
  })
})
