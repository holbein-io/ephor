import { Check } from 'lucide-react';
import { cn } from '../../utils';

type TriageStatus = 'PREPARING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

interface TriageWorkflowStepperProps {
  currentStatus: TriageStatus;
  preparationsCount?: number;
  decisionsCount?: number;
}

const steps = [
  { id: 'PREPARING', name: 'Prepare' },
  { id: 'ACTIVE', name: 'Decide' },
  { id: 'COMPLETED', name: 'Review' },
];

function getStepState(stepId: string, currentStatus: TriageStatus): 'completed' | 'current' | 'upcoming' {
  const order = ['PREPARING', 'ACTIVE', 'COMPLETED'];
  const currentIdx = order.indexOf(currentStatus);
  const stepIdx = order.indexOf(stepId);
  if (currentStatus === 'CANCELLED') return 'upcoming';
  if (stepIdx < currentIdx) return 'completed';
  if (stepIdx === currentIdx) return 'current';
  return 'upcoming';
}

export function TriageWorkflowStepper({
  currentStatus,
  preparationsCount = 0,
  decisionsCount = 0,
}: TriageWorkflowStepperProps) {
  const getSubtext = (stepId: string, state: string) => {
    if (stepId === 'PREPARING') {
      return state === 'completed' ? `${preparationsCount} prepared` : `${preparationsCount} added`;
    }
    if (stepId === 'ACTIVE') {
      return state === 'current' ? `${decisionsCount} decided` : state === 'completed' ? `${decisionsCount} decided` : 'waiting';
    }
    return 'waiting';
  };

  return (
    <div className="bg-bg-secondary border border-border rounded-2xl px-7 py-5 flex items-center justify-center gap-0">
      {steps.map((step, i) => {
        const state = getStepState(step.id, currentStatus);
        const isLast = i === steps.length - 1;
        const subtext = getSubtext(step.id, state);

        return (
          <div key={step.id} className="contents">
            <div className="flex flex-col items-center gap-2 min-w-[100px]">
              <div
                className={cn(
                  'w-9 h-9 rounded-full flex items-center justify-center flex-shrink-0 border-2',
                  state === 'completed' && 'bg-accent-mint-dim border-accent-mint text-accent-mint',
                  state === 'current' && 'bg-accent-dim border-accent text-accent animate-[pulseRing_2s_ease_infinite]',
                  state === 'upcoming' && 'bg-bg-hover border-border text-text-tertiary'
                )}
              >
                {state === 'completed' ? (
                  <Check className="w-4 h-4" strokeWidth={2.5} />
                ) : state === 'current' ? (
                  <div className="w-2.5 h-2.5 rounded-full bg-accent" />
                ) : (
                  <span className="font-mono text-[13px]">{i + 1}</span>
                )}
              </div>
              <div
                className={cn(
                  'text-[13px] font-semibold text-center',
                  state === 'completed' && 'text-accent-mint',
                  state === 'current' && 'text-accent',
                  state === 'upcoming' && 'text-text-tertiary'
                )}
              >
                {step.name}
              </div>
              <div
                className={cn(
                  'text-[11px] font-mono text-center',
                  state === 'completed' && 'text-accent-mint',
                  state === 'current' && 'text-accent',
                  state === 'upcoming' && 'text-text-secondary'
                )}
              >
                {subtext}
              </div>
            </div>

            {!isLast && (
              <div
                className={cn(
                  'flex-1 h-0.5 min-w-[60px] max-w-[80px] -mt-5',
                  state === 'completed' ? 'bg-accent-mint' : 'bg-border'
                )}
              />
            )}
          </div>
        );
      })}
    </div>
  );
}
