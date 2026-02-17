import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { render } from '@/test/test-utils'
import { LoginPage } from '@/pages/LoginPage'

// Mock useAuth
const mockLogin = vi.fn()
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: () => ({
    login: mockLogin,
    user: null,
    isAuthenticated: false,
    isLoading: false,
    token: null,
    logout: vi.fn(),
    refreshProfile: vi.fn(),
  }),
}))

// Mock api
vi.mock('@/lib/api', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
    interceptors: {
      request: { use: vi.fn() },
      response: { use: vi.fn() },
    },
  },
}))

// Mock navigate
const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom')
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  }
})

// Mock sonner
vi.mock('sonner', () => ({
  toast: {
    success: vi.fn(),
    error: vi.fn(),
    warning: vi.fn(),
  },
  Toaster: () => null,
}))

describe('LoginPage', () => {
  const user = userEvent.setup()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders login form', () => {
    render(<LoginPage />)
    expect(screen.getByText('Welcome back')).toBeInTheDocument()
    expect(screen.getByLabelText('Username')).toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument()
  })

  it('shows validation errors for empty fields', async () => {
    render(<LoginPage />)
    const submitBtn = screen.getByRole('button', { name: /sign in/i })
    await user.click(submitBtn)

    await waitFor(() => {
      expect(screen.getByText('Username is required')).toBeInTheDocument()
    })
    expect(screen.getByText('Password is required')).toBeInTheDocument()
  })

  it('shows link to register page', () => {
    render(<LoginPage />)
    const registerLink = screen.getByRole('link', { name: /create one/i })
    expect(registerLink).toBeInTheDocument()
    expect(registerLink).toHaveAttribute('href', '/register')
  })

  it('toggles password visibility', async () => {
    render(<LoginPage />)
    const passwordInput = screen.getByLabelText('Password')
    const toggleBtn = screen.getByLabelText(/show password/i)

    expect(passwordInput).toHaveAttribute('type', 'password')
    await user.click(toggleBtn)
    expect(passwordInput).toHaveAttribute('type', 'text')
  })

  it('submits form with valid data', async () => {
    const api = await import('@/lib/api')
    vi.mocked(api.default.post).mockResolvedValueOnce({
      data: { userId: '1', username: 'testuser', token: 'jwt-token', expiresInMs: 900000 },
    })
    mockLogin.mockResolvedValueOnce(undefined)

    render(<LoginPage />)
    await user.type(screen.getByLabelText('Username'), 'testuser')
    await user.type(screen.getByLabelText('Password'), 'password123')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() => {
      expect(api.default.post).toHaveBeenCalledWith('/auth/login', {
        username: 'testuser',
        password: 'password123',
      })
    })

    expect(mockLogin).toHaveBeenCalledWith('jwt-token')
    expect(mockNavigate).toHaveBeenCalledWith('/dashboard')
  })

  it('displays server error on failed login', async () => {
    const api = await import('@/lib/api')
    vi.mocked(api.default.post).mockRejectedValueOnce({
      response: { data: { detail: 'Invalid credentials' }, status: 401 },
    })

    render(<LoginPage />)
    await user.type(screen.getByLabelText('Username'), 'testuser')
    await user.type(screen.getByLabelText('Password'), 'wrong')
    await user.click(screen.getByRole('button', { name: /sign in/i }))

    await waitFor(() => {
      expect(screen.getByText('Invalid credentials')).toBeInTheDocument()
    })
  })
})
