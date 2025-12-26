import { useState, useEffect, lazy, Suspense } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';

// Lazy load Three.js background
const ThreeBackground = lazy(() => import('./ThreeBackground'));

export default function AccountSettings() {
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [userInfo, setUserInfo] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  // Unlink OAuth state
  const [showUnlinkConfirm, setShowUnlinkConfirm] = useState(false);
  const [unlinkPassword, setUnlinkPassword] = useState('');
  const [unlinking, setUnlinking] = useState(false);

  useEffect(() => {
    fetchUserInfo();
  }, []);

  const fetchUserInfo = async () => {
    try {
      const response = await api.get('/user/profile');

      if (response.success) {
        setUserInfo(response.user);
      } else {
        setError('Failed to load account information.');
      }
    } catch (err) {
      setError(err.message || 'An error occurred.');
      // If unauthorized, redirect to login
      if (err.message.includes('401') || err.message.includes('Unauthorized')) {
        navigate('/login');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleUnlinkOAuth = async () => {
    if (!unlinkPassword.trim()) {
      setError('Please enter your password to confirm.');
      return;
    }

    setError('');
    setSuccess('');
    setUnlinking(true);

    try {
      const response = await api.post('/user/unlink-oauth', {
        password: unlinkPassword,
        provider: userInfo.oAuthProvider
      });

      if (response.success) {
        setSuccess(response.message || 'OAuth account unlinked successfully!');
        setShowUnlinkConfirm(false);
        setUnlinkPassword('');

        // Refresh user info
        await fetchUserInfo();
      } else {
        setError(response.message || 'Failed to unlink OAuth account.');
      }
    } catch (err) {
      setError(err.message || 'An error occurred. Please try again.');
    } finally {
      setUnlinking(false);
    }
  };

  const handleLogout = () => {
    api.logout();
  };

  // Check if user is OAuth-only (no password set)
  const isOAuthOnly = () => {
    // If user has OAuth but registered via OAuth (no real password)
    // Backend sets a random UUID as password for OAuth users
    // We can't check password directly, but we can infer from the response
    return userInfo?.oAuthProvider && !userInfo?.hasPassword;
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-900 via-gray-800 to-black">
        <div className="w-16 h-16 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  return (
    <div className="relative min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-black px-4 py-8 overflow-hidden">
      {/* Three.js animated background */}
      <div className="absolute inset-0 opacity-40">
        <Suspense fallback={null}>
          <ThreeBackground />
        </Suspense>
      </div>

      {/* Subtle grid overlay */}
      <div className="fixed inset-0 bg-[linear-gradient(to_right,#80808008_1px,transparent_1px),linear-gradient(to_bottom,#80808008_1px,transparent_1px)] bg-[size:24px_24px] pointer-events-none"></div>

      {/* Content */}
      <div className="max-w-4xl mx-auto relative z-10">
        {/* Header */}
        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-8"
        >
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-4xl font-bold text-white mb-2">Account Settings</h1>
              <p className="text-gray-400">Manage your account and OAuth connections</p>
            </div>
            <button
              onClick={() => navigate('/dashboard')}
              className="bg-gray-700 hover:bg-gray-600 text-white px-4 py-2 rounded-lg transition-colors"
            >
              ← Back to Dashboard
            </button>
          </div>
        </motion.div>

        {/* Messages */}
        {error && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mb-4 p-4 bg-red-500/10 border border-red-500/50 rounded-lg text-red-400"
          >
            {error}
          </motion.div>
        )}

        {success && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mb-4 p-4 bg-green-500/10 border border-green-500/50 rounded-lg text-green-400"
          >
            {success}
          </motion.div>
        )}

        <div className="grid gap-6">
          {/* Account Information Card */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.1 }}
            className="bg-gray-800/50 backdrop-blur-xl rounded-2xl shadow-2xl border border-gray-700/50 p-6"
          >
            <h2 className="text-2xl font-bold text-white mb-4 flex items-center gap-2">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
              </svg>
              Account Information
            </h2>

            <div className="space-y-4">
              <div>
                <label className="text-gray-400 text-sm">Username</label>
                <p className="text-white font-semibold text-lg">{userInfo?.username}</p>
              </div>

              <div>
                <label className="text-gray-400 text-sm">Email</label>
                <div className="flex items-center gap-2">
                  <p className="text-white font-semibold">{userInfo?.email}</p>
                  {userInfo?.emailVerified && (
                    <span className="bg-green-500/20 text-green-400 text-xs px-2 py-1 rounded-full flex items-center gap-1">
                      <svg className="w-3 h-3" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zm3.707-9.293a1 1 0 00-1.414-1.414L9 10.586 7.707 9.293a1 1 0 00-1.414 1.414l2 2a1 1 0 001.414 0l4-4z" clipRule="evenodd" />
                      </svg>
                      Verified
                    </span>
                  )}
                </div>
              </div>

              <div>
                <label className="text-gray-400 text-sm">Member Since</label>
                <p className="text-white">{userInfo?.createdAt ? new Date(userInfo.createdAt).toLocaleDateString() : 'N/A'}</p>
              </div>
            </div>
          </motion.div>

          {/* OAuth Connections Card */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.2 }}
            className="bg-gray-800/50 backdrop-blur-xl rounded-2xl shadow-2xl border border-gray-700/50 p-6"
          >
            <h2 className="text-2xl font-bold text-white mb-4 flex items-center gap-2">
              <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
              </svg>
              OAuth Connections
            </h2>

            {userInfo?.oAuthProvider ? (
              <div className="space-y-4">
                <div className="bg-gray-900/50 rounded-lg p-4 border border-gray-700">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="w-12 h-12 bg-blue-500/10 rounded-full flex items-center justify-center">
                        <svg className="w-6 h-6 text-blue-400" fill="currentColor" viewBox="0 0 24 24">
                          <path d="M12.48 10.92v3.28h7.84c-.24 1.84-.853 3.187-1.787 4.133-1.147 1.147-2.933 2.4-6.053 2.4-4.827 0-8.6-3.893-8.6-8.72s3.773-8.72 8.6-8.72c2.6 0 4.507 1.027 5.907 2.347l2.307-2.307C18.747 1.44 16.133 0 12.48 0 5.867 0 .307 5.387.307 12s5.56 12 12.173 12c3.573 0 6.267-1.173 8.373-3.36 2.16-2.16 2.84-5.213 2.84-7.667 0-.76-.053-1.467-.173-2.053H12.48z" />
                        </svg>
                      </div>
                      <div>
                        <p className="text-white font-semibold capitalize">{userInfo.oAuthProvider}</p>
                        <p className="text-gray-400 text-sm">Connected and active</p>
                      </div>
                    </div>

                    <button
                      onClick={() => setShowUnlinkConfirm(true)}
                      disabled={isOAuthOnly()}
                      className="bg-red-500/10 hover:bg-red-500/20 text-red-400 px-4 py-2 rounded-lg transition-colors text-sm font-semibold border border-red-500/30 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:bg-red-500/10"
                      title={isOAuthOnly() ? "Cannot unlink - this is your only login method" : "Unlink OAuth account"}
                    >
                      Unlink
                    </button>
                  </div>
                </div>

                {isOAuthOnly() && (
                  <div className="bg-yellow-500/10 border border-yellow-500/30 rounded-lg p-4">
                    <p className="text-yellow-400 text-sm flex items-start gap-2">
                      <svg className="w-5 h-5 mt-0.5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                      </svg>
                      <span>
                        <strong>Cannot unlink OAuth:</strong> This is your only login method. Please set a password in account settings before unlinking your {userInfo.oAuthProvider} account.
                      </span>
                    </p>
                  </div>
                )}

                <div className="bg-blue-500/5 border border-blue-500/20 rounded-lg p-4">
                  <p className="text-blue-400 text-sm flex items-start gap-2">
                    <svg className="w-5 h-5 mt-0.5 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clipRule="evenodd" />
                    </svg>
                    <span>
                      You can sign in using either your password or {userInfo.oAuthProvider}.
                      Unlinking will remove the OAuth login option but you can still use your password.
                    </span>
                  </p>
                </div>
              </div>
            ) : (
              <div className="text-center py-8">
                <div className="w-16 h-16 mx-auto bg-gray-700/30 rounded-full flex items-center justify-center mb-4">
                  <svg className="w-8 h-8 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                  </svg>
                </div>
                <p className="text-gray-400 mb-2">No OAuth accounts linked</p>
                <p className="text-gray-500 text-sm">You can link an OAuth provider during your next login</p>
              </div>
            )}
          </motion.div>

          {/* Logout Button */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.3 }}
          >
            <button
              onClick={handleLogout}
              className="w-full bg-gray-700 hover:bg-gray-600 text-white font-semibold py-3 px-4 rounded-lg transition-colors"
            >
              Logout
            </button>
          </motion.div>
        </div>
      </div>

      {/* Unlink Confirmation Modal */}
      {showUnlinkConfirm && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm flex items-center justify-center z-50 p-4">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-gray-800 rounded-2xl shadow-2xl border border-gray-700 p-6 max-w-md w-full"
          >
            <div className="text-center mb-6">
              <div className="w-16 h-16 mx-auto bg-red-500/10 rounded-full flex items-center justify-center mb-4">
                <svg className="w-8 h-8 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                </svg>
              </div>
              <h3 className="text-xl font-bold text-white mb-2">Unlink OAuth Account?</h3>
              <p className="text-gray-400 text-sm">
                You'll no longer be able to sign in with {userInfo?.oAuthProvider}.
                You can still use your username and password.
              </p>
            </div>

            <div className="mb-4">
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Enter your password to confirm
              </label>
              <input
                type="password"
                value={unlinkPassword}
                onChange={(e) => setUnlinkPassword(e.target.value)}
                placeholder="Your password"
                className="w-full px-4 py-3 bg-gray-900/50 border border-gray-600 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent"
                disabled={unlinking}
              />
            </div>

            <div className="flex gap-3">
              <button
                onClick={() => {
                  setShowUnlinkConfirm(false);
                  setUnlinkPassword('');
                  setError('');
                }}
                disabled={unlinking}
                className="flex-1 bg-gray-700 hover:bg-gray-600 text-white font-semibold py-3 rounded-lg transition-colors disabled:opacity-50"
              >
                Cancel
              </button>
              <button
                onClick={handleUnlinkOAuth}
                disabled={unlinking}
                className="flex-1 bg-red-500 hover:bg-red-600 text-white font-semibold py-3 rounded-lg transition-colors disabled:opacity-50"
              >
                {unlinking ? (
                  <div className="flex items-center justify-center gap-2">
                    <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                    <span>Unlinking...</span>
                  </div>
                ) : (
                  'Unlink Account'
                )}
              </button>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  );
}

