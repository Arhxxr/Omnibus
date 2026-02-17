import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import { render } from '@/test/test-utils'
import { AuthProvider, useAuth } from '@/contexts/AuthContext'
import api from '@/lib/api'
import type { UserProfile } from '@/types'

// Mock the api module
vi.mock('@/lib/api', () => ({
  default: {
    get: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
}))

const mockProfile: UserProfile = {
  userId: 'user-1',
  username: 'testuser',
  email: 'test@example.com',
  accounts: [
    {
      id: 'acc-1',
      userId: 'user-1',
      accountNumber: 'ACC-001',
      balance: 10000,
      currency: 'USD',
      status: 'ACTIVE',
      createdAt: '2026-01-01T00:00:00Z',
    },
  ],
}

/**
 * Helper component that exposes auth state for testing.
 */
function AuthConsumer() {
  const { user, isAuthenticated, isLoading } = useAuth()
  return (
    <div>
      <span data-testid="loading">{String(isLoading)}</span>
      <span data-testid="authenticated">{String(isAuthenticated)}</span>
      <span data-testid="username">{user?.username ?? 'none'}</span>
    </div>
  )
}

describe('AuthContext', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('starts unauthenticated when no token in localStorage', async () => {
    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    )

    await waitFor(() => {
      expect(screen.getByTestId('loading').textContent).toBe('false')
    })
    expect(screen.getByTestId('authenticated').textContent).toBe('false')
    expect(screen.getByTestId('username').textContent).toBe('none')
  })

  it('fetches profile when token exists in localStorage', async () => {
    localStorage.setItem('token', 'test-jwt-token')
    vi.mocked(api.get).mockResolvedValueOnce({ data: mockProfile })

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    )

    await waitFor(() => {
      expect(screen.getByTestId('loading').textContent).toBe('false')
    })
    expect(screen.getByTestId('authenticated').textContent).toBe('true')
    expect(screen.getByTestId('username').textContent).toBe('testuser')
  })

  it('clears auth state when profile fetch fails', async () => {
    localStorage.setItem('token', 'expired-token')
    vi.mocked(api.get).mockRejectedValueOnce(new Error('401'))

    render(
      <AuthProvider>
        <AuthConsumer />
      </AuthProvider>,
    )

    await waitFor(() => {
      expect(screen.getByTestId('loading').textContent).toBe('false')
    })
    expect(screen.getByTestId('authenticated').textContent).toBe('false')
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('throws when useAuth is used outside AuthProvider', () => {
    // Suppress React error boundary console output
    const consoleSpy = vi.spyOn(console, 'error').mockImplementation(() => {})
    expect(() => render(<AuthConsumer />)).toThrow(
      'useAuth must be used within an AuthProvider',
    )
    consoleSpy.mockRestore()
  })
})
