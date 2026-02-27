import { Check, ClipboardList, PlayCircle, CheckCircle2 } from 'lucide-react';
import { cn } from '../../utils';

type TriageStatus = 'PREPARING' | 'ACTIVE' | 'COMPLETED' | 'CANCELLED';

interface TriageWorkflowStepperProps {
  currentStatus: TriageStatus;
  preparationsCount?: number;
  decisionsCount?: number;
}

const steps = [
  {
    id: 'PREPARING',
    name: 'Preparation',
    description: 'Add vulnerabilities to review',
    icon: ClipboardList
  },
  {
    id: 'ACTIVE',
    name: 'Active Session',
    description: 'Make triage decisions',
    icon: PlayCircle
  },
  {
    id: 'COMPLETED',
    name: 'Completed',
    description: 'Session finished',
    icon: CheckCircle2
  }
];

function getStepState(stepId: string, currentStatus: TriageStatus): 'completed' | 'current' | 'upcoming' {
  const stepOrder = ['PREPARING', 'ACTIVE', 'COMPLETED'];
  const currentIndex = stepOrder.indexOf(currentStatus);
  const stepIndex = stepOrder.indexOf(stepId);

  if (currentStatus === 'CANCELLED') {
    return 'upcoming';
  }

  if (stepIndex < currentIndex) {
    return 'completed';
  } else if (stepIndex === currentIndex) {
    return 'current';
  }
  return 'upcoming';
}

export function TriageWorkflowStepper({
  currentStatus,
  preparationsCount = 0,
  decisionsCount = 0
}: TriageWorkflowStepperProps) {
  return (
    <nav aria-label="Triage workflow progress" className="mb-6">
      <ol className="flex items-center">
        {steps.map((step, stepIdx) => {
          const state = getStepState(step.id, currentStatus);
          const Icon = step.icon;

          return (
            <li
              key={step.id}
              className={cn(
                'relative flex-1',
                stepIdx !== steps.length - 1 ? 'pr-8' : ''
              )}
            >
              {/* Connector line */}
              {stepIdx !== steps.length - 1 && (
                <div
                  className="absolute top-5 left-[calc(50%+20px)] w-[calc(100%-40px)] h-0.5"
                  aria-hidden="true"
                >
                  <div
                    className={cn(
                      'h-full transition-colors duration-300',
                      state === 'completed' ? 'bg-success/100' : 'bg-bg-tertiary'
                    )}
                  />
                </div>
              )}

              <div className="flex flex-col items-center group">
                {/* Step circle */}
                <span
                  className={cn(
                    'relative z-10 flex h-10 w-10 items-center justify-center rounded-full border-2 transition-all duration-300',
                    state === 'completed' && 'bg-success/100 border-success',
                    state === 'current' && 'bg-accent/100 border-accent ring-4 ring-accent/20',
                    state === 'upcoming' && 'bg-bg-card border-border'
                  )}
                >
                  {state === 'completed' ? (
                    <Check className="h-5 w-5 text-white" aria-hidden="true" />
                  ) : (
                    <Icon
                      className={cn(
                        'h-5 w-5',
                        state === 'current' ? 'text-white' : 'text-text-tertiary'
                      )}
                      aria-hidden="true"
                    />
                  )}
                </span>

                {/* Step label */}
                <span
                  className={cn(
                    'mt-2 text-sm font-medium transition-colors',
                    state === 'completed' && 'text-success',
                    state === 'current' && 'text-accent',
                    state === 'upcoming' && 'text-text-tertiary'
                  )}
                >
                  {step.name}
                </span>

                {/* Step description */}
                <span
                  className={cn(
                    'mt-0.5 text-xs text-center max-w-[120px]',
                    state === 'current' ? 'text-accent' : 'text-text-tertiary'
                  )}
                >
                  {step.description}
                </span>

                {/* Progress indicator */}
                {step.id === 'PREPARING' && state === 'current' && preparationsCount > 0 && (
                  <span className="mt-1 px-2 py-0.5 text-xs font-semibold bg-accent/15 text-accent rounded-full">
                    {preparationsCount} added
                  </span>
                )}
                {step.id === 'ACTIVE' && state === 'current' && (
                  <span className="mt-1 px-2 py-0.5 text-xs font-semibold bg-accent/15 text-accent rounded-full">
                    {decisionsCount} decisions
                  </span>
                )}
                {step.id === 'PREPARING' && state === 'completed' && preparationsCount > 0 && (
                  <span className="mt-1 px-2 py-0.5 text-xs font-semibold bg-success/15 text-success rounded-full">
                    {preparationsCount} prepared
                  </span>
                )}
              </div>
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
