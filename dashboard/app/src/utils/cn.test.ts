import { describe, it, expect } from 'vitest';
import { cn } from './index';

describe('cn', () => {
  it('keeps the text colour when combined with a sub-xs scale step', () => {
    expect(cn('font-mono text-text-tertiary', 'text-caption')).toContain('text-text-tertiary');
    expect(cn('text-text-tertiary', 'text-micro')).toContain('text-text-tertiary');
  });

  it('still lets a later size override an earlier one', () => {
    expect(cn('text-caption', 'text-micro')).toBe('text-micro');
    expect(cn('text-sm', 'text-micro')).toBe('text-micro');
  });
});
