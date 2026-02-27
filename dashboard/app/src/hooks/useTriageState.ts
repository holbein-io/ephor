import { useReducer } from 'react';
import { TriageSession } from '../types';

// State interface for the triage feature
export interface TriageState {
  currentSession: TriageSession | null;
  selectedDays: number;
  selectedNamespace: string;
  prepLead: string;
  prepNotes: string;
  sessionNotes: string;
  attendees: string[];
  selectedSeverities: string[];
  selectedVulns: Set<number>;
  showCommentModal: { vulnId: number; vulnCve: string } | null;
  commentText: string;
  currentTriageIndex: number;
  expandedSections: Record<string, boolean>;
  showRemediateForm: boolean;
  remediateFormData: {
    assigned_to: string;
    target_date: string;
    notes: string;
  };
}

// Action types for the reducer
export type TriageAction =
  | { type: 'SET_CURRENT_SESSION'; payload: TriageSession | null }
  | { type: 'SET_SELECTED_DAYS'; payload: number }
  | { type: 'SET_SELECTED_NAMESPACE'; payload: string }
  | { type: 'SET_PREP_LEAD'; payload: string }
  | { type: 'SET_PREP_NOTES'; payload: string }
  | { type: 'SET_SESSION_NOTES'; payload: string }
  | { type: 'SET_ATTENDEES'; payload: string[] }
  | { type: 'ADD_ATTENDEE'; payload: string }
  | { type: 'REMOVE_ATTENDEE'; payload: number }
  | { type: 'UPDATE_ATTENDEE'; payload: { index: number; value: string } }
  | { type: 'SET_SELECTED_SEVERITIES'; payload: string[] }
  | { type: 'TOGGLE_SEVERITY'; payload: string }
  | { type: 'ADD_SELECTED_VULN'; payload: number }
  | { type: 'REMOVE_SELECTED_VULN'; payload: number }
  | { type: 'CLEAR_SELECTED_VULNS' }
  | { type: 'SET_COMMENT_MODAL'; payload: { vulnId: number; vulnCve: string } | null }
  | { type: 'SET_COMMENT_TEXT'; payload: string }
  | { type: 'SET_CURRENT_TRIAGE_INDEX'; payload: number }
  | { type: 'INCREMENT_TRIAGE_INDEX' }
  | { type: 'DECREMENT_TRIAGE_INDEX' }
  | { type: 'TOGGLE_EXPANDED_SECTION'; payload: string }
  | { type: 'SET_EXPANDED_SECTIONS'; payload: Record<string, boolean> }
  | { type: 'SET_SHOW_REMEDIATE_FORM'; payload: boolean }
  | { type: 'SET_REMEDIATE_FORM_DATA'; payload: Partial<TriageState['remediateFormData']> }
  | { type: 'RESET_REMEDIATE_FORM' }
  | { type: 'RESET_SESSION_STATE' };

// Initial state
export const initialTriageState: TriageState = {
  currentSession: null,
  selectedDays: 7,
  selectedNamespace: '',
  prepLead: '',
  prepNotes: '',
  sessionNotes: '',
  attendees: [''],
  selectedSeverities: ['CRITICAL', 'HIGH', 'MEDIUM', 'LOW'],
  selectedVulns: new Set(),
  showCommentModal: null,
  commentText: '',
  currentTriageIndex: 0,
  expandedSections: {
    remediate: true,
    escalate: true,
    accept_risk: false,
    false_positive: false
  },
  showRemediateForm: false,
  remediateFormData: {
    assigned_to: '',
    target_date: '',
    notes: ''
  }
};

// Reducer function
export function triageReducer(state: TriageState, action: TriageAction): TriageState {
  switch (action.type) {
    case 'SET_CURRENT_SESSION':
      return { ...state, currentSession: action.payload };

    case 'SET_SELECTED_DAYS':
      return { ...state, selectedDays: action.payload };

    case 'SET_SELECTED_NAMESPACE':
      return { ...state, selectedNamespace: action.payload };

    case 'SET_PREP_LEAD':
      return { ...state, prepLead: action.payload };

    case 'SET_PREP_NOTES':
      return { ...state, prepNotes: action.payload };

    case 'SET_SESSION_NOTES':
      return { ...state, sessionNotes: action.payload };

    case 'SET_ATTENDEES':
      return { ...state, attendees: action.payload };

    case 'ADD_ATTENDEE':
      return { ...state, attendees: [...state.attendees, action.payload] };

    case 'REMOVE_ATTENDEE':
      return { ...state, attendees: state.attendees.filter((_, i) => i !== action.payload) };

    case 'UPDATE_ATTENDEE':
      return {
        ...state,
        attendees: state.attendees.map((a, i) =>
          i === action.payload.index ? action.payload.value : a
        )
      };

    case 'SET_SELECTED_SEVERITIES':
      return { ...state, selectedSeverities: action.payload };

    case 'TOGGLE_SEVERITY':
      return {
        ...state,
        selectedSeverities: state.selectedSeverities.includes(action.payload)
          ? state.selectedSeverities.filter(s => s !== action.payload)
          : [...state.selectedSeverities, action.payload]
      };

    case 'ADD_SELECTED_VULN':
      return {
        ...state,
        selectedVulns: new Set([...state.selectedVulns, action.payload])
      };

    case 'REMOVE_SELECTED_VULN':
      const newSelectedVulns = new Set(state.selectedVulns);
      newSelectedVulns.delete(action.payload);
      return { ...state, selectedVulns: newSelectedVulns };

    case 'CLEAR_SELECTED_VULNS':
      return { ...state, selectedVulns: new Set() };

    case 'SET_COMMENT_MODAL':
      return { ...state, showCommentModal: action.payload };

    case 'SET_COMMENT_TEXT':
      return { ...state, commentText: action.payload };

    case 'SET_CURRENT_TRIAGE_INDEX':
      return { ...state, currentTriageIndex: action.payload };

    case 'INCREMENT_TRIAGE_INDEX':
      return { ...state, currentTriageIndex: state.currentTriageIndex + 1 };

    case 'DECREMENT_TRIAGE_INDEX':
      return { ...state, currentTriageIndex: Math.max(0, state.currentTriageIndex - 1) };

    case 'TOGGLE_EXPANDED_SECTION':
      return {
        ...state,
        expandedSections: {
          ...state.expandedSections,
          [action.payload]: !state.expandedSections[action.payload]
        }
      };

    case 'SET_EXPANDED_SECTIONS':
      return { ...state, expandedSections: action.payload };

    case 'SET_SHOW_REMEDIATE_FORM':
      return { ...state, showRemediateForm: action.payload };

    case 'SET_REMEDIATE_FORM_DATA':
      return {
        ...state,
        remediateFormData: {
          ...state.remediateFormData,
          ...action.payload
        }
      };

    case 'RESET_REMEDIATE_FORM':
      return {
        ...state,
        showRemediateForm: false,
        remediateFormData: {
          assigned_to: '',
          target_date: '',
          notes: ''
        }
      };

    case 'RESET_SESSION_STATE':
      return {
        ...initialTriageState,
        currentSession: state.currentSession
      };

    default:
      return state;
  }
}

// Custom hook to use the triage state
export function useTriageState(initialState?: Partial<TriageState>) {
  const [state, dispatch] = useReducer(triageReducer, {
    ...initialTriageState,
    ...initialState
  });

  return { state, dispatch };
}