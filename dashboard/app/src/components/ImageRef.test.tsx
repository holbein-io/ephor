import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { TooltipProvider } from './ui/tooltip';
import { ImageRef } from './ImageRef';

const REF =
  'artifactory.corp.example.com/payments/com.example.payments.broker.landingpage.landingpage-service:23.0.0-202608110843-11-c806ced';

function renderRef(references: string | string[]) {
  return render(
    <TooltipProvider>
      <ImageRef references={references} />
    </TooltipProvider>
  );
}

describe('ImageRef', () => {
  it('leads with the service name and drops the registry host', () => {
    renderRef(REF);

    expect(screen.getByText('landingpage-service')).toBeInTheDocument();
    expect(screen.queryByText(/artifactory\.corp\.example\.com/)).not.toBeInTheDocument();
  });

  it('keeps the qualifier that separates sibling services', () => {
    renderRef(REF);

    expect(screen.getByText(/broker\.$/)).toBeInTheDocument();
  });

  it('counts the references it does not show', () => {
    renderRef([REF, 'artifactory.corp.example.com/payments/corp-kafka-connect:3.1.0']);

    expect(screen.getByText('+1')).toBeInTheDocument();
  });

  it('renders an em dash when there is nothing to show', () => {
    const { container } = renderRef([]);

    expect(container.textContent).toBe('—');
  });
});
