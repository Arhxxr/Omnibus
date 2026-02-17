import { describe, it, expect } from 'vitest'
import { render, screen } from '@/test/test-utils'
import { NotFoundPage } from '@/pages/NotFoundPage'

describe('NotFoundPage', () => {
  it('displays 404 heading', () => {
    render(<NotFoundPage />)
    expect(screen.getByText('404')).toBeInTheDocument()
  })

  it('displays "Page not found" message', () => {
    render(<NotFoundPage />)
    expect(screen.getByText('Page not found')).toBeInTheDocument()
  })

  it('has a link back to dashboard', () => {
    render(<NotFoundPage />)
    const link = screen.getByRole('link', { name: /back to dashboard/i })
    expect(link).toBeInTheDocument()
    expect(link).toHaveAttribute('href', '/dashboard')
  })
})
