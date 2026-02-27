import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { Layout } from './components/Layout';
import { Dashboard } from './pages/Dashboard';
import { Vulnerabilities } from './pages/Vulnerabilities';
import { VulnerabilityDetail } from './pages/VulnerabilityDetail';
import { Escalations } from './pages/Escalations';
import { Triage } from './pages/Triage';
import { UserProvider } from './contexts/UserContext';
import { VulnerabilityListProvider } from './contexts/VulnerabilityListContext';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
});

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <ReactQueryDevtools initialIsOpen={false} />
      <UserProvider>
        <VulnerabilityListProvider>
          <Router>
            <Layout>
              <Routes>
                <Route path="/" element={<Dashboard />} />
                <Route path="/vulnerabilities" element={<Vulnerabilities />} />
                <Route path="/vulnerabilities/:id" element={<VulnerabilityDetail />} />
                <Route path="/escalations" element={<Escalations />} />
                <Route path="/triage" element={<Triage />} />
              </Routes>
            </Layout>
          </Router>
        </VulnerabilityListProvider>
      </UserProvider>
    </QueryClientProvider>
  );
}

export default App;