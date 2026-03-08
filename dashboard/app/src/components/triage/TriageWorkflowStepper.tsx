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
    icon: ClipboardList,
  },
  {
    id: 'ACTIVE',
    name: 'Active Session',
    icon: PlayCircle,
  },
  {
    id: 'COMPLETED',
    name: 'Completed',
    icon: CheckCircle2,
  },
];

function getStepState(
  stepId: string,
  currentStatus: TriageStatus
): 'completed' | 'current' | 'upcoming' {
  const stepOrder = ['PREPARING', 'ACTIVE', 'COMPLETED'];
  const currentIndex = stepOrder.indexOf(currentStatus);
  const stepIndex = stepOrder.indexOf(stepId);

  if (currentStatus === 'CANCELLED') return 'upcoming';
  if (stepIndex < currentIndex) return 'completed';
  if (stepIndex === currentIndex) return 'current';
  return 'upcoming';
}

export function TriageWorkflowStepper({
  currentStatus,
  preparationsCount = 0,
  decisionsCount = 0,
}: TriageWorkflowStepperProps) {
  return (
    <nav aria-label="Triage workflow progress" className="mb-6">
      <ol className="flex items-start">
        {steps.map((step, stepIdx) => {
          const state = getStepState(step.id, currentStatus);
          const Icon = step.icon;
          const isLast = stepIdx === steps.length - 1;

          // Build the counter pill content
          let counterText: string | null = null;
          if (step.id === 'PREPARING' && preparationsCount > 0) {
            counterText =
              state === 'current'
                ? `${preparationsCount} added`
                : state === 'completed'
                  ? `${preparationsCount} prepared`
                  : null;
          }
          if (step.id === 'ACTIVE' && state === 'current') {
            counterText = `${decisionsCount} decisions`;
          }

          return (
            <li
              key={step.id}
              className={cn('flex items-start', isLast ? '' : 'flex-1')}
            >
              {/* Step indicator + label */}
              <div className="flex flex-col items-center min-w-[80px]">
                {/* Circle */}
                <div
                  className={cn(
                    'relative flex h-9 w-9 items-center justify-center rounded-full transition-all duration-300',
                    state === 'completed' &&
                      'bg-success text-white',
                    state === 'current' &&
                      'bg-accent text-white shadow-[0_0_0_4px_rgba(196,151,59,0.15)]',
                    state === 'upcoming' &&
                      'bg-bg-tertiary text-text-tertiary border border-border'
                  )}
                >
                  {state === 'completed' ? (
                    <Check className="h-4 w-4" strokeWidth={2.5} />
                  ) : (
                    <Icon className="h-4 w-4" />
                  )}
                </div>

                {/* Label */}
                <span
                  className={cn(
                    'mt-2 text-xs font-semibold tracking-wide text-center leading-tight',
                    state === 'completed' && 'text-success',
                    state === 'current' && 'text-text-primary',
                    state === 'upcoming' && 'text-text-tertiary'
                  )}
                >
                  {step.name}
                </span>

                {/* Counter pill */}
                {counterText && (
                  <span
                    className={cn(
                      'mt-1.5 px-2 py-0.5 text-[11px] font-medium rounded-full',
                      state === 'current'
                        ? 'bg-accent/15 text-accent'
                        : 'bg-success/15 text-success'
                    )}
                  >
                    {counterText}
                  </span>
                )}
              </div>

              {/* Connector line */}
              {!isLast && (
                <div className="flex-1 flex items-center pt-[18px] px-3">
                  <div
                    className={cn(
                      'h-[2px] w-full rounded-full transition-colors duration-500',
                      state === 'completed'
                        ? 'bg-success'
                        : 'bg-border'
                    )}
                  />
                </div>
              )}
            </li>
          );
        })}
      </ol>
    </nav>
  );
}
