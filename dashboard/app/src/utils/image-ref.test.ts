import { describe, it, expect } from 'vitest';
import { parseImageRef, formatImageTag } from './index';

describe('parseImageRef', () => {
  it('keeps the tag when the registry carries a port', () => {
    const ref = parseImageRef('artifactory.corp:8081/docker-local/platform/payments-api:2.14.3');

    expect(ref.registry).toBe('artifactory.corp:8081');
    expect(ref.repository).toBe('docker-local/platform/payments-api');
    expect(ref.name).toBe('payments-api');
    expect(ref.namespace).toBe('platform');
    expect(ref.tag).toBe('2.14.3');
  });

  it('parses a fully qualified reference without a port', () => {
    const ref = parseImageRef('artifactory.corp.example.com/docker-virtual/platform-team/payments-api:2.14.3');

    expect(ref.registry).toBe('artifactory.corp.example.com');
    expect(ref.name).toBe('payments-api');
    expect(ref.namespace).toBe('platform-team');
    expect(ref.tag).toBe('2.14.3');
  });

  it('separates a digest from the repository', () => {
    const ref = parseImageRef('registry.io/team/app@sha256:abc123');

    expect(ref.name).toBe('app');
    expect(ref.digest).toBe('sha256:abc123');
    expect(ref.tag).toBeUndefined();
  });

  it('handles a reference carrying both a tag and a digest', () => {
    const ref = parseImageRef('registry.io/team/app:v1@sha256:abc123');

    expect(ref.tag).toBe('v1');
    expect(ref.digest).toBe('sha256:abc123');
    expect(ref.name).toBe('app');
  });

  it('treats a dotless leading segment as a namespace, not a registry', () => {
    const ref = parseImageRef('library/nginx:1.27');

    expect(ref.registry).toBeUndefined();
    expect(ref.repository).toBe('library/nginx');
    expect(ref.name).toBe('nginx');
    expect(ref.tag).toBe('1.27');
  });

  it('handles a bare image name', () => {
    const ref = parseImageRef('nginx');

    expect(ref.registry).toBeUndefined();
    expect(ref.name).toBe('nginx');
    expect(ref.tag).toBeUndefined();
  });

  it('recognises localhost as a registry', () => {
    const ref = parseImageRef('localhost:5000/app:dev');

    expect(ref.registry).toBe('localhost:5000');
    expect(ref.name).toBe('app');
    expect(ref.tag).toBe('dev');
  });
});

describe('formatImageTag', () => {
  it('abbreviates a digest tag', () => {
    expect(formatImageTag('sha256:abcdef1234567890')).toBe('sha:abcdef12');
  });

  it('abbreviates a commit sha tag', () => {
    expect(formatImageTag('a'.repeat(40))).toBe('aaaaaaaa');
  });

  it('leaves a semantic version untouched', () => {
    expect(formatImageTag('2.14.3')).toBe('2.14.3');
  });
});
