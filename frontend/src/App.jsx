import { lazy, Suspense } from 'react';
import Hero from './components/Hero';
import Features from './components/Features';

// Lazy load the Three.js background component for better initial load performance
const ThreeBackground = lazy(() => import('./components/ThreeBackground'));

function App() {
  return (
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
}

export default App;

