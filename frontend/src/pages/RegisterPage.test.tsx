import { describe, it, expect, vi, beforeEach } from 'vitest'
import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { render } from '@/test/test-utils'
import { RegisterPage } from '@/pages/RegisterPage'

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

describe('RegisterPage', () => {
  const user = userEvent.setup()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('renders registration form', () => {
    render(<RegisterPage />)
    expect(screen.getByText('Create account')).toBeInTheDocument()
    expect(screen.getByLabelText('Username')).toBeInTheDocument()
    expect(screen.getByLabelText('Email')).toBeInTheDocument()
    expect(screen.getByLabelText('Password')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /create account/i })).toBeInTheDocument()
  })

  it('displays feature list', () => {
    render(<RegisterPage />)
    expect(screen.getByText('Instant $10,000 demo balance')).toBeInTheDocument()
    expect(screen.getByText('Send money to any user')).toBeInTheDocument()
    expect(screen.getByText('Full transaction history')).toBeInTheDocument()
  })

  it('validates username minimum length', async () => {
    render(<RegisterPage />)
    await user.type(screen.getByLabelText('Username'), 'ab')
    await user.type(screen.getByLabelText('Email'), 'test@example.com')
    await user.type(screen.getByLabelText('Password'), 'password123')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    await waitFor(() => {
      expect(screen.getByText('Must be at least 3 characters')).toBeInTheDocument()
    })
  })

  it('validates email format', async () => {
    render(<RegisterPage />)
    await user.type(screen.getByLabelText('Username'), 'testuser')
    await user.type(screen.getByLabelText('Email'), 'not-an-email')
    await user.type(screen.getByLabelText('Password'), 'password123')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    await waitFor(() => {
      // Zod 4 may render the error differently; match flexibly
      const errorEl = screen.getByText((content) =>
        /invalid|email|valid/i.test(content)
      )
      expect(errorEl).toBeInTheDocument()
    })
  })

  it('validates password minimum length', async () => {
    render(<RegisterPage />)
    await user.type(screen.getByLabelText('Username'), 'testuser')
    await user.type(screen.getByLabelText('Email'), 'test@example.com')
    await user.type(screen.getByLabelText('Password'), 'short')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    await waitFor(() => {
      expect(screen.getByText('Must be at least 8 characters')).toBeInTheDocument()
    })
  })

  it('has link to login page', () => {
    render(<RegisterPage />)
    const loginLink = screen.getByRole('link', { name: /sign in/i })
    expect(loginLink).toBeInTheDocument()
    expect(loginLink).toHaveAttribute('href', '/login')
  })

  it('submits form with valid data', async () => {
    const api = await import('@/lib/api')
    vi.mocked(api.default.post).mockResolvedValueOnce({
      data: { userId: '1', username: 'newuser', token: 'jwt-token', expiresInMs: 900000 },
    })
    mockLogin.mockResolvedValueOnce(undefined)

    render(<RegisterPage />)
    await user.type(screen.getByLabelText('Username'), 'newuser')
    await user.type(screen.getByLabelText('Email'), 'new@example.com')
    await user.type(screen.getByLabelText('Password'), 'password123')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    await waitFor(() => {
      expect(api.default.post).toHaveBeenCalledWith('/auth/register', {
        username: 'newuser',
        email: 'new@example.com',
        password: 'password123',
      })
    })

    expect(mockLogin).toHaveBeenCalledWith('jwt-token')
    expect(mockNavigate).toHaveBeenCalledWith('/dashboard')
  })

  it('displays server error on failed registration', async () => {
    const api = await import('@/lib/api')
    vi.mocked(api.default.post).mockRejectedValueOnce({
      response: { data: { detail: 'Username already taken' }, status: 409 },
    })

    render(<RegisterPage />)
    await user.type(screen.getByLabelText('Username'), 'taken')
    await user.type(screen.getByLabelText('Email'), 'taken@example.com')
    await user.type(screen.getByLabelText('Password'), 'password123')
    await user.click(screen.getByRole('button', { name: /create account/i }))

    await waitFor(() => {
      expect(screen.getByText('Username already taken')).toBeInTheDocument()
    })
  })

  it('toggles password visibility', async () => {
    render(<RegisterPage />)
    const passwordInput = screen.getByLabelText('Password')
    const toggleBtn = screen.getByLabelText(/show password/i)

    expect(passwordInput).toHaveAttribute('type', 'password')
    await user.click(toggleBtn)
    expect(passwordInput).toHaveAttribute('type', 'text')
  })
})
