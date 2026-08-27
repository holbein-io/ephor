import { describe, it, expect } from 'vitest';
import { parseImageRef, formatImageTag, imageRefDisplay } from './index';

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

  it('keeps the version and the commit of a long build tag', () => {
    const shown = formatImageTag('23.0.0-202608110843-11-c806ced');

    expect(shown.startsWith('23.0.0')).toBe(true);
    expect(shown.endsWith('c806ced')).toBe(true);
    expect(shown.length).toBeLessThanOrEqual(18);
  });
});

// Shapes taken from a corporate Artifactory: reverse-DNS repositories where
// several services share a final segment.
describe('imageRefDisplay', () => {
  const ARTIFACTORY = 'artifactory.corp.example.com';

  it('keeps the service name out of the truncated lead', () => {
    const { lead, name } = imageRefDisplay(
      `${ARTIFACTORY}/docker-local/apps-corp/com.example.applications.messageassistant.message-assistant-service:25.2.0.0-202603101034-769081e`
    );

    expect(name).toBe('message-assistant-service');
    expect(lead).not.toContain(ARTIFACTORY);
  });

  it('tells sibling services apart by the qualifier next to the name', () => {
    const broker = imageRefDisplay(
      `${ARTIFACTORY}/payments/com.example.payments.broker.landingpage.landingpage-service:23.0.0-202608110843-11-c806ced`
    );
    const retail = imageRefDisplay(
      `${ARTIFACTORY}/payments/com.example.payments.retail.landingpage.landingpage-service:23.0.0-202608200744-13-1104858`
    );

    expect(broker.name).toBe('landingpage-service');
    expect(retail.name).toBe('landingpage-service');
    // The lead clips from its start, so the last qualifier is what survives.
    expect(broker.lead.endsWith('broker.')).toBe(true);
    expect(retail.lead.endsWith('retail.')).toBe(true);
  });

  it('drops qualifiers the repository or the name already states', () => {
    const { lead, name } = imageRefDisplay(
      `${ARTIFACTORY}/payments/com.example.payments.organisation.organisation-graphql:23.0.0-202608070921-10-c3535ad`
    );

    expect(name).toBe('organisation-graphql');
    expect(lead).toBe('payments/com.example.');
  });

  it('reads a digest appended with a colon instead of an at sign', () => {
    const { name, tag } = imageRefDisplay(
      `${ARTIFACTORY}/payments/solsson/kafka-prometheus-jmx-exporter:latest:sha256:6f82e2b0464f50da8104acd7363fb9b995001ddff77d248379f8788e78946143`
    );

    expect(name).toBe('kafka-prometheus-jmx-exporter');
    expect(tag).toBe('latest');
  });

  it('leaves a plain repository path alone', () => {
    const { lead, name, tag } = imageRefDisplay(`${ARTIFACTORY}/payments/corp-kafka-connect:3.1.0`);

    expect(lead).toBe('payments/');
    expect(name).toBe('corp-kafka-connect');
    expect(tag).toBe('3.1.0');
  });
});
