import { describe, it, expect } from 'vitest'
import { render } from '@/test/test-utils'
import { Skeleton, CardSkeleton, TransactionSkeleton } from '@/components/ui/Skeleton'

describe('Skeleton', () => {
  it('renders with default class', () => {
    const { container } = render(<Skeleton />)
    const el = container.firstChild as HTMLElement
    expect(el).toBeInTheDocument()
    expect(el).toHaveClass('animate-pulse')
    expect(el).toHaveAttribute('aria-hidden', 'true')
  })

  it('accepts custom className', () => {
    const { container } = render(<Skeleton className="h-4 w-28" />)
    const el = container.firstChild as HTMLElement
    expect(el).toHaveClass('h-4')
    expect(el).toHaveClass('w-28')
  })
})

describe('CardSkeleton', () => {
  it('renders skeleton card structure', () => {
    const { container } = render(<CardSkeleton />)
    // Should have the card wrapper with border
    const card = container.firstChild as HTMLElement
    expect(card).toBeInTheDocument()
    expect(card).toHaveClass('rounded-2xl')
    // Should contain multiple skeleton elements (aria-hidden)
    const skeletons = container.querySelectorAll('[aria-hidden="true"]')
    expect(skeletons.length).toBeGreaterThanOrEqual(3)
  })
})

describe('TransactionSkeleton', () => {
  it('renders skeleton transaction row', () => {
    const { container } = render(<TransactionSkeleton />)
    const wrapper = container.firstChild as HTMLElement
    expect(wrapper).toBeInTheDocument()
    // Should contain multiple skeleton placeholders
    const skeletons = container.querySelectorAll('[aria-hidden="true"]')
    expect(skeletons.length).toBeGreaterThanOrEqual(3)
  })
})
