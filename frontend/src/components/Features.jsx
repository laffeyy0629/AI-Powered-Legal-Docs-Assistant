import { useEffect, useRef } from 'react';
import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);

export default function Features() {
  const sectionRef = useRef(null);
  const titleRef = useRef(null);
  const cardsRef = useRef([]);
  const iconRefs = useRef([]);

  useEffect(() => {
    // Check for reduced motion preference
    const prefersReducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    
    if (prefersReducedMotion) {
      // Skip animations for users who prefer reduced motion
      gsap.set([titleRef.current, cardsRef.current], { opacity: 1, y: 0, scale: 1 });
      return;
    }
    const ctx = gsap.context(() => {
      // Set initial states to ensure elements start hidden
      gsap.set(titleRef.current, { y: 50, opacity: 0 });
      gsap.set(cardsRef.current, { y: 80, opacity: 0, scale: 0.9 });
      gsap.set(iconRefs.current, { scale: 0, rotation: -180 });

      // Animate title with smooth effect
      gsap.to(titleRef.current, {
        scrollTrigger: {
          trigger: titleRef.current,
          start: 'top 80%',
          toggleActions: 'play none none reverse',
        },
        y: 0,
        opacity: 1,
        duration: 1,
        ease: 'power3.out',
      });

      // Stagger animate cards with smooth entrance
      cardsRef.current.forEach((card, index) => {
        if (card) {
          gsap.to(card, {
            scrollTrigger: {
              trigger: card,
              start: 'top 85%',
              toggleActions: 'play none none reverse',
            },
            y: 0,
            opacity: 1,
            scale: 1,
            duration: 0.8,
            delay: index * 0.15,
            ease: 'back.out(1.2)',
          });
        }
      });

      // Animate icons with spin and bounce effect
      iconRefs.current.forEach((icon, index) => {
        if (icon) {
          gsap.to(icon, {
            scrollTrigger: {
              trigger: icon,
              start: 'top 80%',
              toggleActions: 'play none none reverse',
            },
            scale: 1,
            rotation: 0,
            duration: 1,
            delay: index * 0.15 + 0.2,
            ease: 'elastic.out(1, 0.6)',
          });
        }
      });

      // Refresh ScrollTrigger after a short delay to ensure DOM is ready
      setTimeout(() => {
        ScrollTrigger.refresh();
      }, 100);
    });

    return () => ctx.revert();
  }, []);

  const features = [
    {
      icon: '🤖',
      title: 'AI-Powered',
      description: 'Leverage advanced AI to analyze and generate legal documents with precision.',
    },
    {
      icon: '📄',
      title: 'Document Management',
      description: 'Organize, search, and manage all your legal documents in one secure place.',
    },
    {
      icon: '⚡',
      title: 'Lightning Fast',
      description: 'Get instant results with our optimized processing powered by React and modern tech.',
    },
    {
      icon: '🔒',
      title: 'Secure & Private',
      description: 'Your data is encrypted and protected with enterprise-grade security.',
    },
  ];

  const handleCardHover = (index, isHovering) => {
    const card = cardsRef.current[index];

    if (card) {
      gsap.to(card, {
        y: isHovering ? -12 : 0,
        scale: isHovering ? 1.03 : 1,
        duration: 0.4,
        ease: 'power2.out',
      });
    }
  };

  return (
    <section
      ref={sectionRef}
      className="relative min-h-screen py-20 px-4 flex items-center justify-center overflow-hidden"
    >
      <div className="max-w-6xl mx-auto relative z-10">
        <h2
          ref={titleRef}
          className="text-5xl md:text-6xl font-bold text-center mb-16 bg-gradient-to-r from-indigo-400 to-purple-400 bg-clip-text text-transparent"
        >
          Powerful Features
        </h2>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          {features.map((feature, index) => (
            <div
              key={index}
              ref={(el) => (cardsRef.current[index] = el)}
              onMouseEnter={() => handleCardHover(index, true)}
              onMouseLeave={() => handleCardHover(index, false)}
              className="group relative p-8 bg-gray-800/50 backdrop-blur-sm rounded-2xl border border-gray-700 hover:border-indigo-500 transition-all duration-500 hover:shadow-xl hover:shadow-indigo-500/30 will-change-transform"
            >
              {/* Icon with ref for GSAP animation */}
              <div
                ref={(el) => (iconRefs.current[index] = el)}
                className="text-6xl mb-4"
              >
                {feature.icon}
              </div>

              {/* Title */}
              <h3 className="text-2xl font-bold text-white mb-3 group-hover:text-indigo-400 transition-colors">
                {feature.title}
              </h3>

              {/* Description */}
              <p className="text-gray-400 leading-relaxed">
                {feature.description}
              </p>

              {/* Animated corner accents */}
              <div className="absolute top-0 left-0 w-16 h-16 border-t-2 border-l-2 border-indigo-500/0 group-hover:border-indigo-500/100 transition-all duration-300 rounded-tl-2xl"></div>
              <div className="absolute bottom-0 right-0 w-16 h-16 border-b-2 border-r-2 border-purple-500/0 group-hover:border-purple-500/100 transition-all duration-300 rounded-br-2xl"></div>

              {/* Hover effect glow */}
              <div className="absolute inset-0 bg-gradient-to-r from-indigo-500/10 to-purple-500/10 rounded-2xl opacity-0 group-hover:opacity-100 transition-opacity duration-300 -z-10"></div>

              {/* Particle effect overlay */}
              <div className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity duration-500">
                <div className="absolute top-1/4 left-1/4 w-1 h-1 bg-indigo-400 rounded-full animate-ping"></div>
                <div className="absolute top-3/4 right-1/4 w-1 h-1 bg-purple-400 rounded-full animate-ping" style={{ animationDelay: '0.2s' }}></div>
                <div className="absolute top-1/2 right-1/3 w-1 h-1 bg-pink-400 rounded-full animate-ping" style={{ animationDelay: '0.4s' }}></div>
              </div>
            </div>
          ))}
        </div>

        {/* Enhanced decorative elements - optimized blur */}
        <div className="absolute top-20 left-10 w-72 h-72 bg-indigo-500 rounded-full mix-blend-screen filter blur-2xl opacity-15"></div>
        <div className="absolute bottom-20 right-10 w-72 h-72 bg-purple-500 rounded-full mix-blend-screen filter blur-2xl opacity-15"></div>
      </div>
    </section>
  );
}

