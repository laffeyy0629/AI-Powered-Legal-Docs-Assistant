import { useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { gsap } from 'gsap';

export default function Hero() {
  const navigate = useNavigate();
  const titleRef = useRef(null);
  const subtitleRef = useRef(null);
  const buttonRef = useRef(null);
  const loginButtonRef = useRef(null);
  const containerRef = useRef(null);
  const scrollIndicatorRef = useRef(null);
  const floatingShapesRef = useRef([]);

  useEffect(() => {
    const ctx = gsap.context(() => {
      // Set initial states
      gsap.set(titleRef.current, { y: 100, opacity: 0 });
      gsap.set(subtitleRef.current, { y: 50, opacity: 0 });
      gsap.set(buttonRef.current, { y: 30, scale: 0.8, opacity: 0 });
      gsap.set(loginButtonRef.current, { y: 30, scale: 0.8, opacity: 0 });

      // Create timeline for sequential animations
      const tl = gsap.timeline({ defaults: { ease: 'power3.out' } });

      tl.to(titleRef.current, {
        y: 0,
        opacity: 1,
        duration: 1.2,
        delay: 0.3,
        ease: 'power4.out',
      })
        .to(
          subtitleRef.current,
          {
            y: 0,
            opacity: 1,
            duration: 1,
            ease: 'power3.out',
          },
          '-=0.6'
        )
        .to(
          buttonRef.current,
          {
            y: 0,
            scale: 1,
            opacity: 1,
            duration: 1,
            ease: 'elastic.out(1, 0.6)',
          },
          '-=0.3'
        )
        .to(
          loginButtonRef.current,
          {
            y: 0,
            scale: 1,
            opacity: 1,
            duration: 1,
            ease: 'elastic.out(1, 0.6)',
          },
          '-=0.8'
        );

      // Floating animation for the entire container (starts after intro animation)
      gsap.to(containerRef.current, {
        y: -20,
        duration: 2.5,
        repeat: -1,
        yoyo: true,
        ease: 'sine.inOut',
        delay: 2,
      });

      // Scroll indicator animation
      if (scrollIndicatorRef.current) {
        gsap.to(scrollIndicatorRef.current, {
          y: 10,
          opacity: 1,
          duration: 1.5,
          repeat: -1,
          yoyo: true,
          ease: 'sine.inOut',
          delay: 2.5,
        });
      }

      // Floating shapes animation
      floatingShapesRef.current.forEach((shape, index) => {
        if (shape) {
          gsap.to(shape, {
            y: -30 + index * 10,
            x: index % 2 === 0 ? 20 : -20,
            duration: 3 + index * 0.5,
            repeat: -1,
            yoyo: true,
            ease: 'sine.inOut',
            delay: index * 0.3,
          });
        }
      });
    });

    return () => ctx.revert();
  }, []);

  const handleGetStarted = () => {
    gsap.to(buttonRef.current, {
      scale: 0.95,
      duration: 0.1,
      yoyo: true,
      repeat: 1,
    });
    // Smooth scroll to features section
    scrollToFeatures();
  };

  const scrollToFeatures = () => {
    const featuresSection = document.querySelector('section');
    if (featuresSection) {
      featuresSection.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <div className="relative min-h-screen flex items-center justify-center px-4 z-10">
      <div ref={containerRef} className="text-center max-w-4xl mx-auto relative z-20">
        <h1
          ref={titleRef}
          className="text-6xl md:text-8xl font-bold mb-6 bg-gradient-to-r from-indigo-400 via-purple-400 to-pink-400 bg-clip-text text-transparent"
        >
          Welcome
        </h1>

        <p
          ref={subtitleRef}
          className="text-xl md:text-2xl text-gray-300 mb-12 max-w-2xl mx-auto leading-relaxed"
        >
          AI-Powered Legal Documents Assistant
          <br />
          <span className="text-indigo-400">
            Built with React, Three.js, GSAP & Tailwind CSS
          </span>
        </p>

        <div className="relative z-30 flex gap-4 justify-center items-center flex-wrap">
          <button
            ref={buttonRef}
            onClick={handleGetStarted}
            className="px-8 py-4 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-600 hover:to-purple-700 text-white text-lg font-semibold rounded-full shadow-lg hover:shadow-indigo-500/50 transition-all duration-300 cursor-pointer"
          >
            Get Started
          </button>
          <button
            ref={loginButtonRef}
            onClick={() => navigate('/login')}
            className="px-8 py-4 bg-white/10 hover:bg-white/20 backdrop-blur-sm text-white text-lg font-semibold rounded-full border-2 border-white/30 hover:border-white/50 shadow-lg transition-all duration-300 cursor-pointer"
          >
            Login
          </button>
        </div>

        {/* Decorative elements - More visible and animated */}
        <div className="absolute -top-20 -left-20 w-96 h-96 bg-purple-500 rounded-full mix-blend-screen filter blur-3xl opacity-30 animate-pulse -z-10"></div>
        <div className="absolute -top-10 -right-20 w-96 h-96 bg-indigo-500 rounded-full mix-blend-screen filter blur-3xl opacity-30 animate-pulse -z-10" style={{ animationDelay: '1s' }}></div>
        <div className="absolute -bottom-20 left-1/2 -translate-x-1/2 w-96 h-96 bg-pink-500 rounded-full mix-blend-screen filter blur-3xl opacity-20 animate-pulse -z-10" style={{ animationDelay: '2s' }}></div>

        {/* Floating accent shapes */}
        <div ref={el => floatingShapesRef.current[0] = el} className="absolute top-20 left-10 w-4 h-4 bg-indigo-400 rounded-full opacity-40 blur-sm"></div>
        <div ref={el => floatingShapesRef.current[1] = el} className="absolute top-40 right-16 w-3 h-3 bg-purple-400 rounded-full opacity-40 blur-sm"></div>
        <div ref={el => floatingShapesRef.current[2] = el} className="absolute bottom-40 left-20 w-2 h-2 bg-pink-400 rounded-full opacity-40 blur-sm"></div>
        <div ref={el => floatingShapesRef.current[3] = el} className="absolute bottom-32 right-24 w-3 h-3 bg-indigo-300 rounded-full opacity-40 blur-sm"></div>
      </div>

      {/* Scroll indicator */}
      <div
        ref={scrollIndicatorRef}
        onClick={scrollToFeatures}
        className="absolute bottom-10 left-1/2 -translate-x-1/2 flex flex-col items-center gap-2 opacity-0 cursor-pointer group hover:scale-110 transition-transform duration-300"
      >
        <span className="text-gray-400 text-sm font-light group-hover:text-indigo-400 transition-colors">Scroll to explore</span>
        <div className="w-6 h-10 border-2 border-gray-400 group-hover:border-indigo-400 rounded-full flex items-start justify-center p-2 transition-colors">
          <div className="w-1.5 h-3 bg-indigo-400 rounded-full"></div>
        </div>
      </div>
    </div>
  );
}

