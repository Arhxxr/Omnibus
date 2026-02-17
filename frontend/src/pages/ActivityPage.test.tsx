import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { render } from '@/test/test-utils'
import { ActivityPage } from '@/pages/ActivityPage'
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
    description: 'Groceries',
    createdAt: '2026-02-15T10:00:00Z',
    completedAt: '2026-02-15T10:00:01Z',
  },
  {
    id: 'txn-2',
    type: 'TRANSFER',
    status: 'FAILED',
    sourceAccountId: 'acc-2',
    targetAccountId: 'acc-1',
    amount: 50,
    currency: 'USD',
    description: 'Failed payment',
    createdAt: '2026-02-14T09:00:00Z',
    completedAt: null,
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

describe('ActivityPage', () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    const api = await import('@/lib/api')
    vi.mocked(api.default.get).mockResolvedValue({ data: mockTransactions })
  })

  it('renders activity page header', () => {
    render(<ActivityPage />)
    expect(screen.getByText('Activity')).toBeInTheDocument()
    expect(screen.getByText('Your complete transaction history')).toBeInTheDocument()
  })

  it('displays transaction descriptions', async () => {
    render(<ActivityPage />)
    await waitFor(() => {
      expect(screen.getByText('Groceries')).toBeInTheDocument()
      expect(screen.getByText('Failed payment')).toBeInTheDocument()
    })
  })

  it('displays transaction count badge', async () => {
    render(<ActivityPage />)
    await waitFor(() => {
      expect(screen.getByText('2 transactions')).toBeInTheDocument()
    })
  })

  it('shows Money Sent for outgoing transactions', async () => {
    render(<ActivityPage />)
    await waitFor(() => {
      expect(screen.getByText('Money Sent')).toBeInTheDocument()
    })
  })

  it('shows Money Received for incoming transactions', async () => {
    render(<ActivityPage />)
    await waitFor(() => {
      expect(screen.getByText('Money Received')).toBeInTheDocument()
    })
  })

  it('shows status badges', async () => {
    render(<ActivityPage />)
    await waitFor(() => {
      expect(screen.getByText('COMPLETED')).toBeInTheDocument()
      expect(screen.getByText('FAILED')).toBeInTheDocument()
    })
  })

  it('shows formatted amounts', async () => {
    render(<ActivityPage />)
    await waitFor(() => {
      expect(screen.getByText('-$100.00')).toBeInTheDocument()
      expect(screen.getByText('+$50.00')).toBeInTheDocument()
    })
  })
})

describe('ActivityPage - empty state', () => {
  beforeEach(async () => {
    vi.clearAllMocks()
    const api = await import('@/lib/api')
    vi.mocked(api.default.get).mockResolvedValue({ data: [] })
  })

  it('shows empty state when no transactions', async () => {
    render(<ActivityPage />)
    await waitFor(() => {
      expect(screen.getByText('No transactions yet')).toBeInTheDocument()
    })
  })
})
