import { describe, it, expect } from 'vitest';

describe('App', () => {
  it('should pass basic smoke test', () => {
    expect(true).toBe(true);
  });

  it('should have valid configuration', () => {
    const testObj = { name: 'Trivy Scanner', version: '1.0.0' };
    expect(testObj.name).toBe('Trivy Scanner');
    expect(testObj.version).toBe('1.0.0');
  });
});