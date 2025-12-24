import { useState, useEffect, lazy, Suspense } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';

// Lazy load Three.js background
const ThreeBackground = lazy(() => import('./ThreeBackground'));

export default function OAuthLinkConsent() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [accountInfo, setAccountInfo] = useState(null);

  // Extract OAuth data from URL params
  const email = searchParams.get('email');
  const provider = searchParams.get('provider');
  const sessionId = searchParams.get('session');

  useEffect(() => {
    if (!email || !provider || !sessionId) {
      setError('Invalid OAuth linking request. Missing required parameters.');
      return;
    }

    // Fetch existing account info
    const fetchAccountInfo = async () => {
      try {
        const response = await api.post('/user/oauth-link-info', {
          email,
          provider,
          session_id: sessionId
        });

        if (response.success) {
          setAccountInfo(response.account);
        } else {
          setError(response.message || 'Failed to fetch account information.');
        }
      } catch (err) {
        setError(err.message || 'An error occurred while fetching account information.');
      }
    };

    fetchAccountInfo();
  }, [email, provider, sessionId]);

  const handleConfirmLink = async () => {
    setError('');
    setLoading(true);

    try {
      const response = await api.post('/user/confirm-oauth-link', {
        email,
        provider,
        session_id: sessionId,
        consent: true
      });

      if (response.success) {
        // Store tokens if provided
        if (response.access_token) {
          api.setToken(response.access_token);
          if (response.refresh_token) {
            localStorage.setItem('refresh_token', response.refresh_token);
          }
        }

        // Redirect to dashboard
        navigate('/dashboard');
      } else {
        setError(response.message || 'Failed to link OAuth account.');
      }
    } catch (err) {
      setError(err.message || 'An error occurred. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    // Clear OAuth session and go back to login
    navigate('/login');
  };

  return (
    <div className="relative min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-900 via-gray-800 to-black px-4 overflow-hidden">
      {/* Three.js animated background */}
      <div className="absolute inset-0 opacity-40">
        <Suspense fallback={null}>
          <ThreeBackground />
        </Suspense>
      </div>

      {/* Subtle grid overlay */}
      <div className="fixed inset-0 bg-[linear-gradient(to_right,#80808008_1px,transparent_1px),linear-gradient(to_bottom,#80808008_1px,transparent_1px)] bg-[size:24px_24px] pointer-events-none"></div>

      {/* Card */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="w-full max-w-lg relative z-10"
      >
        <div className="bg-gray-800/50 backdrop-blur-xl rounded-2xl shadow-2xl border border-gray-700/50 p-8">
          {/* Header */}
          <div className="text-center mb-8">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.2 }}
              className="mb-4"
            >
              <div className="w-16 h-16 mx-auto bg-blue-500/10 rounded-full flex items-center justify-center">
                <svg className="w-8 h-8 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                </svg>
              </div>
            </motion.div>
            <motion.h2
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.3 }}
              className="text-3xl font-bold text-white mb-2"
            >
              Link {provider ? provider.charAt(0).toUpperCase() + provider.slice(1) : 'OAuth'} Account?
            </motion.h2>
            <motion.p
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.4 }}
              className="text-gray-400"
            >
              An existing account was found with this email
            </motion.p>
          </div>

          {/* Error Message */}
          {error && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              className="mb-4 p-3 bg-red-500/10 border border-red-500/50 rounded-lg text-red-400 text-sm"
            >
              {error}
            </motion.div>
          )}

          {/* Account Info */}
          {accountInfo && (
            <div className="mb-6">
              <div className="bg-gray-900/50 rounded-lg p-4 border border-gray-700">
                <p className="text-gray-400 text-sm mb-3">We found an existing account:</p>

                <div className="space-y-2">
                  <div className="flex items-center gap-2">
                    <svg className="w-5 h-5 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                    </svg>
                    <span className="text-white font-semibold">{accountInfo.username}</span>
                  </div>

                  <div className="flex items-center gap-2">
                    <svg className="w-5 h-5 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                    </svg>
                    <span className="text-gray-300">{email}</span>
                  </div>

                  {accountInfo.email_verified && (
                    <div className="flex items-center gap-2">
                      <svg className="w-5 h-5 text-green-400" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                      </svg>
                      <span className="text-green-400 text-sm">Email verified</span>
                    </div>
                  )}
                </div>
              </div>

              {/* Benefits */}
              <div className="mt-4 bg-blue-500/5 border border-blue-500/20 rounded-lg p-4">
                <p className="text-blue-400 font-semibold mb-2 text-sm">By linking your {provider} account, you'll be able to:</p>
                <ul className="space-y-1 text-gray-300 text-sm">
                  <li className="flex items-start gap-2">
                    <span className="text-blue-400 mt-1">•</span>
                    <span>Sign in with your username & password</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-blue-400 mt-1">•</span>
                    <span>Sign in with {provider} (one-click login)</span>
                  </li>
                  <li className="flex items-start gap-2">
                    <span className="text-blue-400 mt-1">•</span>
                    <span>Unlink {provider} anytime in account settings</span>
                  </li>
                </ul>
              </div>
            </div>
          )}

          {/* Action Buttons */}
          <div className="space-y-3">
            <button
              onClick={handleConfirmLink}
              disabled={loading || !accountInfo}
              className="w-full bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white font-semibold py-3 px-4 rounded-lg transition-all duration-300 transform hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
            >
              {loading ? (
                <div className="flex items-center justify-center gap-2">
                  <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  <span>Linking...</span>
                </div>
              ) : (
                <span>Yes, Link {provider ? provider.charAt(0).toUpperCase() + provider.slice(1) : 'OAuth'} Account</span>
              )}
            </button>

            <button
              onClick={handleCancel}
              disabled={loading}
              className="w-full bg-gray-700 hover:bg-gray-600 text-white font-semibold py-3 px-4 rounded-lg transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Cancel
            </button>
          </div>

          {/* Privacy Note */}
          <div className="mt-6 text-center">
            <p className="text-gray-500 text-xs">
              By linking your account, you agree to our Terms of Service and Privacy Policy.
              You can unlink your {provider} account anytime from your account settings.
            </p>
          </div>
        </div>
      </motion.div>
    </div>
  );
}

