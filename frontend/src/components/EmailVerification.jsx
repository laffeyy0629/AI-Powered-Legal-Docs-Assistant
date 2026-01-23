import { useEffect, useState, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { motion } from 'framer-motion';
import api from '../services/api';

export default function EmailVerification() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState('verifying'); // 'verifying', 'success', 'error', 'expired'
  const [message, setMessage] = useState('Verifying your email...');
  const [email, setEmail] = useState('');
  const [isResending, setIsResending] = useState(false);
  const hasVerified = useRef(false);

  useEffect(() => {
    // Prevent duplicate verification calls
    if (hasVerified.current) {
      return;
    }

    const token = searchParams.get('token');

    if (!token) {
      setStatus('error');
      setMessage('Invalid verification link. Please check your email for the correct link.');
      return;
    }

    hasVerified.current = true;
    verifyEmail(token);
  }, [searchParams]);

  const verifyEmail = async (token) => {
    console.log('Verifying email with token...');
    try {
      const response = await fetch(`http://localhost:8080/user/verify-email?token=${token}`, {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      const data = await response.json();
      console.log('Verification response:', data);

      if (data.success) {
        setStatus('success');
        setMessage(data.message);

        // Redirect to login after 3 seconds
        setTimeout(() => {
          navigate('/login');
        }, 3000);
      } else {
        if (data.expired) {
          setStatus('expired');
          setEmail(data.email);
        } else {
          setStatus('error');
        }
        setMessage(data.message);
      }
    } catch (error) {
      console.error('Email verification error:', error);
      setStatus('error');
      setMessage('Failed to verify email. Please try again later.');
    }
  };

  const handleResendVerification = async () => {
    if (!email) {
      alert('Email address not available. Please go back to login and request a new verification email.');
      return;
    }

    setIsResending(true);

    try {
      const response = await api.post('/user/resend-verification', { email });

      if (response.success) {
        alert('Verification email sent! Please check your inbox.');
      } else {
        alert(response.message || 'Failed to send verification email.');
      }
    } catch (error) {
      console.error('Resend verification error:', error);
      alert('Failed to send verification email. Please try again later.');
    } finally {
      setIsResending(false);
    }
  };

  const getStatusIcon = () => {
    switch (status) {
      case 'verifying':
        return (
          <div className="w-16 h-16 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
        );
      case 'success':
        return (
          <svg className="w-16 h-16 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
          </svg>
        );
      case 'error':
      case 'expired':
        return (
          <svg className="w-16 h-16 text-red-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
          </svg>
        );
      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-indigo-900 to-gray-900 flex items-center justify-center px-4">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.6 }}
        className="bg-white rounded-lg shadow-2xl p-8 max-w-md w-full"
      >
        <div className="flex flex-col items-center text-center">
          {/* Status Icon */}
          <div className="mb-6">
            {getStatusIcon()}
          </div>

          {/* Message */}
          <h2 className={`text-2xl font-bold mb-4 ${
            status === 'success' ? 'text-green-600' : 
            status === 'error' || status === 'expired' ? 'text-red-600' : 
            'text-gray-800'
          }`}>
            {status === 'verifying' && 'Verifying Email'}
            {status === 'success' && 'Email Verified!'}
            {status === 'error' && 'Verification Failed'}
            {status === 'expired' && 'Link Expired'}
          </h2>

          <p className="text-gray-600 mb-6">
            {message}
          </p>

          {/* Action Buttons */}
          {status === 'success' && (
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => navigate('/login')}
              className="bg-indigo-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-indigo-700 transition-colors"
            >
              Go to Login
            </motion.button>
          )}

          {status === 'expired' && (
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={handleResendVerification}
              disabled={isResending}
              className="bg-indigo-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-indigo-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isResending ? 'Sending...' : 'Resend Verification Email'}
            </motion.button>
          )}

          {status === 'error' && (
            <motion.button
              whileHover={{ scale: 1.05 }}
              whileTap={{ scale: 0.95 }}
              onClick={() => navigate('/login')}
              className="bg-gray-600 text-white px-6 py-3 rounded-lg font-semibold hover:bg-gray-700 transition-colors"
            >
              Back to Login
            </motion.button>
          )}
        </div>
      </motion.div>
    </div>
  );
}

