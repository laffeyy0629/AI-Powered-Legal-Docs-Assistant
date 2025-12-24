import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import apiService from '../services/api';

const OAuthCallback = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState('Processing...');

  useEffect(() => {
    console.log('OAuthCallback mounted');
    const token = searchParams.get('token');
    const error = searchParams.get('error');

    console.log('Token from URL:', token ? 'Present' : 'Missing');
    console.log('Error from URL:', error || 'None');

    if (error) {
      console.log('OAuth error, redirecting to login');
      setStatus('Authentication failed. Redirecting...');
      setTimeout(() => navigate('/login'), 2000);
      return;
    }

    if (token) {
      console.log('Saving token to localStorage...');
      // Save the JWT token
      apiService.setToken(token);
      console.log('Token saved, checking localStorage...');
      console.log('Token in localStorage:', localStorage.getItem('jwt_token') ? 'Yes' : 'No');
      setStatus('Authentication successful! Redirecting...');
      setTimeout(() => {
        console.log('Navigating to dashboard...');
        navigate('/dashboard');
      }, 1000);
    } else {
      console.log('No token in URL, redirecting to login');
      setStatus('No token received. Redirecting...');
      setTimeout(() => navigate('/login'), 2000);
    }
  }, [searchParams, navigate]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-gray-900 via-gray-800 to-black">
      <div className="text-center">
        <div className="w-16 h-16 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
        <p className="text-white text-xl">{status}</p>
      </div>
    </div>
  );
};

export default OAuthCallback;

