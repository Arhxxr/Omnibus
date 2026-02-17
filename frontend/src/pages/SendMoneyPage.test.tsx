import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { render } from '@/test/test-utils'
import { SendMoneyPage } from '@/pages/SendMoneyPage'
import type { UserProfile } from '@/types'

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

// Mock useAuth
const mockRefreshProfile = vi.fn()
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    user: mockUser,
    isAuthenticated: true,
    isLoading: false,
    token: 'test-token',
    login: vi.fn(),
    logout: vi.fn(),
    refreshProfile: mockRefreshProfile,
  }),
}))

// Mock api
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
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
  },
  Toaster: () => null,
}))

describe('SendMoneyPage', () => {
  const user = userEvent.setup()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders send money form', () => {
    render(<SendMoneyPage />)
    expect(screen.getByText('Send Money')).toBeInTheDocument()
    expect(screen.getByLabelText('Recipient')).toBeInTheDocument()
    expect(screen.getByLabelText('Amount')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /review transfer/i })).toBeInTheDocument()
  })

  it('shows available balance', () => {
    render(<SendMoneyPage />)
    expect(screen.getByText(/available: \$5,000\.00/i)).toBeInTheDocument()
  })

  it('disables review button when no recipient found', () => {
    render(<SendMoneyPage />)
    const reviewBtn = screen.getByRole('button', { name: /review transfer/i })
    expect(reviewBtn).toBeDisabled()
  })

  it('looks up recipient on search click', async () => {
    const api = await import('@/lib/api')
    vi.mocked(api.default.get).mockResolvedValueOnce({
      data: { accountId: 'acc-2', username: 'recipient', accountNumber: 'ACC-002' },
    })

    render(<SendMoneyPage />)
    await user.type(screen.getByLabelText('Recipient'), 'recipient')
    await user.click(screen.getByLabelText('Search for user'))

    await waitFor(() => {
      expect(api.default.get).toHaveBeenCalledWith(
        '/accounts/lookup?username=recipient',
      )
    })

    await waitFor(() => {
      expect(screen.getByText(/found:/i)).toBeInTheDocument()
      expect(screen.getByText('recipient')).toBeInTheDocument()
    })
  })

  it('shows error when recipient not found', async () => {
    const api = await import('@/lib/api')
    vi.mocked(api.default.get).mockRejectedValueOnce(new Error('Not found'))

    render(<SendMoneyPage />)
    await user.type(screen.getByLabelText('Recipient'), 'nobody')
    await user.click(screen.getByLabelText('Search for user'))

    await waitFor(() => {
      expect(screen.getByText('User not found')).toBeInTheDocument()
    })
  })

  it('prevents sending to self', async () => {
    const api = await import('@/lib/api')
    vi.mocked(api.default.get).mockResolvedValueOnce({
      data: { accountId: 'acc-1', username: 'testuser', accountNumber: 'ACC-001' },
    })

    render(<SendMoneyPage />)
    await user.type(screen.getByLabelText('Recipient'), 'testuser')
    await user.click(screen.getByLabelText('Search for user'))

    await waitFor(() => {
      expect(screen.getByText('You cannot send money to yourself')).toBeInTheDocument()
    })
  })

  it('shows optional description field', () => {
    render(<SendMoneyPage />)
    expect(screen.getByLabelText(/description/i)).toBeInTheDocument()
    expect(screen.getByText('(optional)')).toBeInTheDocument()
  })
})
