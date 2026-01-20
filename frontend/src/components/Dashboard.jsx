import { useEffect, useState, lazy, Suspense, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { gsap } from 'gsap';
import apiService from '../services/api';

// Lazy load Three.js background
const ThreeBackground = lazy(() => import('./ThreeBackground'));

const Dashboard = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [file, setFile] = useState(null);
  const [summary, setSummary] = useState('');
  const [analyzing, setAnalyzing] = useState(false);
  const thinkingRef = useRef(null);
  const dotsRef = useRef([]);

  useEffect(() => {
    // Check if user is authenticated
    const isAuth = apiService.isAuthenticated();
    if (!isAuth) {
      navigate('/login');
      return;
    }

    // Test the JWT token
    apiService
      .testProtectedEndpoint()
      .then(() => setLoading(false))
      .catch(() => {
        apiService.clearToken();
        navigate('/login');
      });
  }, [navigate]);

  useEffect(() => {
    if (analyzing && thinkingRef.current) {
      // Animate the thinking text
      gsap.fromTo(
        thinkingRef.current,
        { scale: 0.8, opacity: 0 },
        { scale: 1, opacity: 1, duration: 0.5, ease: 'back.out(1.7)' }
      );

      // Animate dots sequentially
      dotsRef.current.forEach((dot, index) => {
        gsap.fromTo(
          dot,
          { y: 0 },
          {
            y: -15,
            duration: 0.5,
            repeat: -1,
            yoyo: true,
            ease: 'power1.inOut',
            delay: index * 0.15,
          }
        );
      });
    }
  }, [analyzing]);

  const handleLogout = () => {
    apiService.logout();
    navigate('/login');
  };

  const handleFileChange = (e) => {
    setFile(e.target.files[0]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!file) return;

    try {
      setAnalyzing(true);
      setSummary('');
      
      const formData = new FormData();
      formData.append('file', file);

      const token = apiService.getToken(); // get JWT from localStorage/session

      const response = await fetch(
        `${import.meta.env.VITE_API_BASE_URL}/api/documents/analyze`,
        {
          method: 'POST',
          body: formData,
          headers: {
            Authorization: `Bearer ${token}`, // <--- important!
          },
        }
      );

      if (!response.ok) {
        throw new Error('Failed to analyze document');
      }

      const text = await response.text();
      setSummary(text);
    } catch (err) {
      console.error('Error uploading document:', err);
      setSummary('Failed to analyze document.');
    } finally {
      setAnalyzing(false);
    }
  };


  if (loading) {
    return (
      <div className="relative min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-900 via-gray-800 to-black overflow-hidden">
        <div className="absolute inset-0 opacity-70">
          <Suspense fallback={null}>
            <ThreeBackground />
          </Suspense>
        </div>
        <div className="text-center relative z-10">
          <div className="w-16 h-16 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-white text-xl">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="relative min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-black px-4 py-8 overflow-hidden">
      {/* Three.js background */}
      <div className="absolute inset-0 opacity-70">
        <Suspense fallback={null}>
          <ThreeBackground />
        </Suspense>
      </div>

      {/* Grid overlay */}
      <div className="fixed inset-0 bg-[linear-gradient(to_right,#80808018_1px,transparent_1px),linear-gradient(to_bottom,#80808018_1px,transparent_1px)] bg-[size:24px_24px] pointer-events-none"></div>

      {/* Animated blobs */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -right-40 w-96 h-96 bg-blue-500/20 rounded-full blur-3xl animate-pulse"></div>
        <div className="absolute top-1/2 -left-40 w-96 h-96 bg-purple-500/20 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '1s' }}></div>
        <div className="absolute -bottom-40 right-1/3 w-96 h-96 bg-indigo-500/15 rounded-full blur-3xl animate-pulse" style={{ animationDelay: '2s' }}></div>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="relative z-10 max-w-4xl mx-auto"
      >
        {/* Header */}
        <div className="bg-gray-800/60 backdrop-blur-2xl rounded-2xl shadow-2xl border border-gray-600/60 p-8 mb-8 hover:border-gray-500/60 transition-all duration-300">
          <div className="flex justify-between items-center">
            <div>
              <h1 className="text-4xl font-bold bg-gradient-to-r from-blue-400 via-purple-400 to-pink-400 bg-clip-text text-transparent mb-2">
                Dashboard
              </h1>
              <p className="text-gray-300">Welcome to AI-Powered Legal Docs Assistant</p>
            </div>
            <div className="flex gap-3">
              <button
                onClick={() => navigate('/settings')}
                className="bg-gradient-to-r from-gray-700 to-gray-800 hover:from-gray-600 hover:to-gray-700 text-white font-semibold py-3 px-6 rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg flex items-center gap-2"
              >
                Settings
              </button>
              <button
                onClick={handleLogout}
                className="bg-gradient-to-r from-red-600 to-red-700 hover:from-red-700 hover:to-red-800 text-white font-semibold py-3 px-8 rounded-lg transition-all duration-300 transform hover:scale-105 shadow-lg hover:shadow-red-500/50"
              >
                Logout
              </button>
            </div>
          </div>
        </div>

        {/* Document Upload */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2, duration: 0.6 }}
          className="bg-gray-800/60 backdrop-blur-2xl rounded-2xl shadow-2xl border border-gray-600/60 p-8 mb-8 hover:border-green-400/60 transition-all duration-300"
        >
          <form onSubmit={handleSubmit} encType="multipart/form-data" className="space-y-6">
            <div>
              <h2 className="text-3xl font-bold bg-gradient-to-r from-green-400 via-emerald-400 to-teal-400 bg-clip-text text-transparent mb-3">
                Document Analysis
              </h2>
              <p className="text-gray-300 mb-6">Upload your legal document for AI-powered analysis</p>
            </div>

            <div className="space-y-4">
              <div className="relative">
                <input
                  type="file"
                  name="file"
                  onChange={handleFileChange}
                  required
                  className="block w-full text-gray-300 file:mr-4 file:py-3 file:px-6 file:rounded-lg file:border-0 file:text-sm file:font-semibold file:bg-gradient-to-r file:from-green-600 file:to-emerald-600 file:text-white hover:file:from-green-700 hover:file:to-emerald-700 file:cursor-pointer file:transition-all file:duration-300 file:shadow-lg hover:file:shadow-green-500/50 bg-gray-700/50 border border-gray-600 rounded-lg cursor-pointer hover:border-green-400/60 transition-all duration-300 backdrop-blur-xl p-3"
                />
              </div>

              <button
                type="submit"
                disabled={!file || analyzing}
                className="w-full bg-gradient-to-r from-green-600 via-emerald-600 to-teal-600 hover:from-green-700 hover:via-emerald-700 hover:to-teal-700 disabled:from-gray-600 disabled:to-gray-700 disabled:cursor-not-allowed text-white font-bold py-4 px-8 rounded-lg transition-all duration-300 transform hover:scale-[1.02] disabled:scale-100 shadow-xl hover:shadow-green-500/50 disabled:shadow-none flex items-center justify-center gap-3"
              >
                {analyzing ? (
                  <>
                    <svg className="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    Analyzing...
                  </>
                ) : (
                  <>
                    <svg
                      className="w-5 h-5"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"
                      />
                    </svg>
                    Analyze Document
                  </>
                )}
              </button>
            </div>
          </form>

          {analyzing && (
            <div className="mt-6 p-8 bg-gradient-to-br from-gray-800/90 via-blue-900/30 to-purple-900/30 backdrop-blur-xl rounded-xl border border-blue-500/50 shadow-2xl shadow-blue-500/20">
              <div className="flex flex-col items-center justify-center space-y-6">
                <div className="relative w-24 h-24">
                  <div className="absolute inset-0 rounded-full bg-gradient-to-r from-blue-500 to-purple-500 opacity-20 animate-pulse"></div>
                  <svg className="w-24 h-24 animate-spin" viewBox="0 0 100 100">
                    <circle
                      cx="50"
                      cy="50"
                      r="40"
                      stroke="url(#gradient)"
                      strokeWidth="6"
                      fill="none"
                      strokeLinecap="round"
                      strokeDasharray="60 200"
                    />
                    <defs>
                      <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="100%">
                        <stop offset="0%" stopColor="#3B82F6" />
                        <stop offset="50%" stopColor="#8B5CF6" />
                        <stop offset="100%" stopColor="#EC4899" />
                      </linearGradient>
                    </defs>
                  </svg>
                </div>
                <div ref={thinkingRef} className="text-center">
                  <h3 className="text-3xl font-bold bg-gradient-to-r from-blue-400 via-purple-400 to-pink-400 bg-clip-text text-transparent mb-3 flex items-center justify-center gap-2">
                    Thinking
                    <span className="flex gap-1">
                      <span ref={(el) => (dotsRef.current[0] = el)} className="text-blue-400">.</span>
                      <span ref={(el) => (dotsRef.current[1] = el)} className="text-purple-400">.</span>
                      <span ref={(el) => (dotsRef.current[2] = el)} className="text-pink-400">.</span>
                    </span>
                  </h3>
                  <p className="text-gray-400 text-lg">AI is analyzing your document</p>
                </div>
              </div>
            </div>
          )}

          {summary && !analyzing && (
            <div className="mt-6 p-6 bg-gray-800/80 backdrop-blur-xl rounded-xl border border-gray-600/50 shadow-xl">
              <h3 className="text-2xl font-bold bg-gradient-to-r from-green-400 to-emerald-400 bg-clip-text text-transparent mb-4">
                Analysis Results
              </h3>
              <div className="text-gray-200 space-y-3 whitespace-pre-wrap leading-relaxed">
                {summary.split('\n').map((line, index) => {
                  // Bold text formatting
                  if (line.trim().startsWith('**') && line.trim().endsWith('**')) {
                    return (
                      <div key={index} className="font-bold text-xl text-blue-300 mt-4 mb-2">
                        {line.trim().replace(/\*\*/g, '')}
                      </div>
                    );
                  }
                  // Main bullet points (•)
                  if (line.includes('•')) {
                    return (
                      <div key={index} className="flex items-start gap-3 ml-2">
                        <span className="text-green-400 text-xl mt-0.5">•</span>
                        <span className="flex-1">{line.replace('•', '').trim()}</span>
                      </div>
                    );
                  }
                  // Secondary bullet points (●)
                  if (line.includes('●')) {
                    return (
                      <div key={index} className="flex items-start gap-3 ml-6">
                        <span className="text-blue-400 text-lg mt-0.5">●</span>
                        <span className="flex-1">{line.replace('●', '').trim()}</span>
                      </div>
                    );
                  }
                  // Tertiary bullet points (○)
                  if (line.includes('○')) {
                    return (
                      <div key={index} className="flex items-start gap-3 ml-10">
                        <span className="text-purple-400 text-lg mt-0.5">○</span>
                        <span className="flex-1 text-gray-300">{line.replace('○', '').trim()}</span>
                      </div>
                    );
                  }
                  // Dash bullet points (-)
                  if (line.trim().startsWith('-')) {
                    return (
                      <div key={index} className="flex items-start gap-3 ml-2">
                        <span className="text-yellow-400 mt-0.5">−</span>
                        <span className="flex-1 text-gray-300">{line.replace('-', '').trim()}</span>
                      </div>
                    );
                  }
                  // Regular text
                  return line.trim() ? (
                    <p key={index} className="text-gray-300">{line}</p>
                  ) : (
                    <div key={index} className="h-2"></div>
                  );
                })}
              </div>
            </div>
          )}
        </motion.div>

        {/* You can keep your Features Grid here as before */}
      </motion.div>

              {/* Features Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.3, duration: 0.6 }}
            className="bg-gray-800/60 backdrop-blur-2xl rounded-xl shadow-xl border border-gray-600/60 p-6 hover:border-blue-400/60 hover:shadow-blue-500/20 transition-all duration-300 transform hover:scale-105"
          >
            <div className="w-12 h-12 bg-gradient-to-br from-blue-500 to-blue-600 rounded-lg flex items-center justify-center mb-4 shadow-lg shadow-blue-500/50">
              <svg
                className="w-6 h-6 text-white"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                />
              </svg>
            </div>
            <h3 className="text-xl font-semibold text-white mb-2">Document Analysis</h3>
            <p className="text-gray-300">
              Upload and analyze legal documents with AI-powered insights.
            </p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.4, duration: 0.6 }}
            className="bg-gray-800/60 backdrop-blur-2xl rounded-xl shadow-xl border border-gray-600/60 p-6 hover:border-purple-400/60 hover:shadow-purple-500/20 transition-all duration-300 transform hover:scale-105"
          >
            <div className="w-12 h-12 bg-gradient-to-br from-purple-500 to-purple-600 rounded-lg flex items-center justify-center mb-4 shadow-lg shadow-purple-500/50">
              <svg
                className="w-6 h-6 text-purple-400"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"
                />
              </svg>
            </div>
            <h3 className="text-xl font-semibold text-white mb-2">Smart Contracts</h3>
            <p className="text-gray-400">
              Generate and review smart contracts with AI assistance.
            </p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: -20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.5, duration: 0.6 }}
            className="bg-gray-800/50 backdrop-blur-xl rounded-xl shadow-xl border border-gray-700/50 p-6"
          >
            <div className="w-12 h-12 bg-green-500/20 rounded-lg flex items-center justify-center mb-4">
              <svg
                className="w-6 h-6 text-green-400"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"
                />
              </svg>
            </div>
            <h3 className="text-xl font-semibold text-white mb-2">Compliance Check</h3>
            <p className="text-gray-400">
              Ensure your documents meet legal compliance requirements.
            </p>
          </motion.div>

          <motion.div
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.6, duration: 0.6 }}
            className="bg-gray-800/60 backdrop-blur-2xl rounded-xl shadow-xl border border-gray-600/60 p-6 hover:border-yellow-400/60 hover:shadow-yellow-500/20 transition-all duration-300 transform hover:scale-105"
          >
            <div className="w-12 h-12 bg-gradient-to-br from-yellow-500 to-orange-600 rounded-lg flex items-center justify-center mb-4 shadow-lg shadow-yellow-500/50">
              <svg
                className="w-6 h-6 text-yellow-400"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M13 10V3L4 14h7v7l9-11h-7z"
                />
              </svg>
            </div>
            <h3 className="text-xl font-semibold text-white mb-2">Quick Actions</h3>
            <p className="text-gray-400">
              Access frequently used tools and templates instantly.
            </p>
          </motion.div>
        </div>
    </div>
  );
};

export default Dashboard;
