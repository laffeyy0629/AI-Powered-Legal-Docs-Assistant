import { lazy, Suspense } from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Hero from './components/Hero';
import Features from './components/Features';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import OAuthCallback from './components/OAuthCallback';
import OAuthLinkConsent from './components/OAuthLinkConsent';
import AccountSettings from './components/AccountSettings';
import EmailVerification from './components/EmailVerification';
import ForgotPassword from './components/ForgotPassword';
import ResetPassword from './components/ResetPassword';
import ForgotUsername from './components/ForgotUsername';

// Lazy load the Three.js background component for better initial load performance
const ThreeBackground = lazy(() => import('./components/ThreeBackground'));

// Home Page Component
const HomePage = () => (
  <div className="relative w-full bg-gradient-to-b from-gray-900 via-gray-800 to-gray-900">
    {/* Three.js animated background - lazy loaded */}
    <Suspense fallback={null}>
      <ThreeBackground />
    </Suspense>

    {/* Main content */}
    <Hero />

    {/* Features section */}
    <Features />

    {/* Optional: Add a subtle grid overlay */}
    <div className="fixed inset-0 bg-[linear-gradient(to_right,#80808012_1px,transparent_1px),linear-gradient(to_bottom,#80808012_1px,transparent_1px)] bg-[size:24px_24px] pointer-events-none -z-5"></div>
  </div>
);

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/login" element={<Login />} />
        <Route path="/dashboard" element={<Dashboard />} />
        <Route path="/settings" element={<AccountSettings />} />
        <Route path="/auth/callback" element={<OAuthCallback />} />
        <Route path="/oauth-link-consent" element={<OAuthLinkConsent />} />
        <Route path="/verify-email" element={<EmailVerification />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route path="/forgot-username" element={<ForgotUsername />} />
      </Routes>
    </Router>
  );
}

export default App;

