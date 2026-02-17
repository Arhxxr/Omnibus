import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { ErrorBoundary } from '@/components/ErrorBoundary'

function ThrowingComponent({ error }: { error: Error }) {
  throw error
}

function GoodComponent() {
  return <div>All good</div>
}

describe('ErrorBoundary', () => {
  // Suppress React error boundary logging during tests
  const originalConsoleError = console.error
  beforeEach(() => {
    console.error = vi.fn()
  })
  afterEach(() => {
    console.error = originalConsoleError
  })

  it('renders children when no error', () => {
    render(
      <ErrorBoundary>
        <GoodComponent />
      </ErrorBoundary>,
    )
    expect(screen.getByText('All good')).toBeInTheDocument()
  })

  it('renders error UI when child throws', () => {
    render(
      <ErrorBoundary>
        <ThrowingComponent error={new Error('Test crash')} />
      </ErrorBoundary>,
    )
    expect(screen.getByText('Something went wrong')).toBeInTheDocument()
    expect(screen.getByText('Test crash')).toBeInTheDocument()
    expect(screen.getByText('Return to Dashboard')).toBeInTheDocument()
  })

  it('renders custom fallback when provided', () => {
    render(
      <ErrorBoundary fallback={<div>Custom error page</div>}>
        <ThrowingComponent error={new Error('Oops')} />
      </ErrorBoundary>,
    )
    expect(screen.getByText('Custom error page')).toBeInTheDocument()
  })

  it('displays generic message when error has no message', () => {
    render(
      <ErrorBoundary>
        <ThrowingComponent error={new Error()} />
      </ErrorBoundary>,
    )
    expect(screen.getByText('An unexpected error occurred')).toBeInTheDocument()
  })
})
