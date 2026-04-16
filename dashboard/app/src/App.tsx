import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { Layout } from './components/Layout';
import { Dashboard } from './pages/Dashboard';
import { Vulnerabilities } from './pages/Vulnerabilities';
import { VulnerabilityDetail } from './pages/VulnerabilityDetail';
import { Escalations } from './pages/Escalations';
import { Triage } from './pages/Triage';
import { MyItems } from './pages/MyItems';
import { Inventory } from './pages/Inventory';
import { UserProvider } from './contexts/UserContext';
import { UserDirectoryProvider } from './contexts/UserDirectoryContext';
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
        <UserDirectoryProvider>
        <VulnerabilityListProvider>
          <Router>
            <Layout>
              <Routes>
                <Route path="/" element={<Dashboard />} />
                <Route path="/vulnerabilities" element={<Vulnerabilities />} />
                <Route path="/vulnerabilities/:id" element={<VulnerabilityDetail />} />
                <Route path="/escalations" element={<Escalations />} />
                <Route path="/triage" element={<Triage />} />
                <Route path="/my-items" element={<MyItems />} />
                <Route path="/inventory" element={<Inventory />} />
              </Routes>
            </Layout>
          </Router>
        </VulnerabilityListProvider>
        </UserDirectoryProvider>
      </UserProvider>
    </QueryClientProvider>
  );
}

export default App;