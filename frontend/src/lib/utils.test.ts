import { describe, it, expect } from 'vitest'
import { cn, formatCurrency, formatDate, getInitials } from '@/lib/utils'

// ── cn() ─────────────────────────────────────────────
describe('cn()', () => {
  it('merges class names', () => {
    expect(cn('foo', 'bar')).toBe('foo bar')
  })

  it('handles conditional classes', () => {
    const hidden = false as boolean
    expect(cn('base', hidden && 'hidden', 'visible')).toBe('base visible')
  })

  it('deduplicates conflicting tailwind classes', () => {
    // tailwind-merge should keep the last conflicting utility
    expect(cn('px-4', 'px-6')).toBe('px-6')
  })

  it('handles undefined and null gracefully', () => {
    expect(cn('base', undefined, null, 'end')).toBe('base end')
  })

  it('returns empty string for no arguments', () => {
    expect(cn()).toBe('')
  })
})

// ── formatCurrency() ─────────────────────────────────
describe('formatCurrency()', () => {
  it('formats USD by default', () => {
    expect(formatCurrency(1234.56)).toBe('$1,234.56')
  })

  it('formats zero', () => {
    expect(formatCurrency(0)).toBe('$0.00')
  })

  it('formats negative amounts', () => {
    expect(formatCurrency(-50)).toBe('-$50.00')
  })

  it('formats with explicit currency', () => {
    const result = formatCurrency(1000, 'EUR')
    // Intl formats vary by locale but should contain 1,000.00 and €
    expect(result).toContain('1,000.00')
  })

  it('pads to two decimal places', () => {
    expect(formatCurrency(5)).toBe('$5.00')
  })

  it('formats large amounts with commas', () => {
    expect(formatCurrency(1000000)).toBe('$1,000,000.00')
  })
})

// ── formatDate() ─────────────────────────────────────
describe('formatDate()', () => {
  it('formats an ISO string', () => {
    const result = formatDate('2026-01-15T10:30:00Z')
    // Should contain "Jan 15, 2026" and a time component
    expect(result).toContain('Jan')
    expect(result).toContain('15')
    expect(result).toContain('2026')
  })

  it('formats a Date object', () => {
    const result = formatDate(new Date('2026-06-01T14:00:00Z'))
    expect(result).toContain('Jun')
    expect(result).toContain('2026')
  })

  it('includes time in the output', () => {
    const result = formatDate('2026-03-10T08:45:00Z')
    // Should have some time indication (hour:minute)
    expect(result).toMatch(/\d{1,2}:\d{2}/)
  })
})

// ── getInitials() ────────────────────────────────────
describe('getInitials()', () => {
  it('returns first two initials of multi-word name', () => {
    expect(getInitials('John Doe')).toBe('JD')
  })

  it('returns single initial for single word', () => {
    expect(getInitials('Alice')).toBe('A')
  })

  it('uppercases initials', () => {
    expect(getInitials('jane smith')).toBe('JS')
  })

  it('handles hyphenated names', () => {
    expect(getInitials('Mary-Jane Watson')).toBe('MJ')
  })

  it('handles underscore-separated names', () => {
    expect(getInitials('test_user')).toBe('TU')
  })

  it('limits to 2 characters max', () => {
    expect(getInitials('Alpha Beta Gamma Delta')).toBe('AB')
  })
})
