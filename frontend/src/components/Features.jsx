import { useEffect, useRef, useState } from 'react';
import { gsap } from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';
import { Canvas, useFrame } from '@react-three/fiber';
import { Float, MeshDistortMaterial, Sphere, Box, Torus } from '@react-three/drei';

gsap.registerPlugin(ScrollTrigger);

// 3D Floating Shape Component
function FloatingShape({ position, shape = 'sphere', color = '#6366f1' }) {
  const meshRef = useRef();
  const [hovered, setHovered] = useState(false);

  useFrame((state) => {
    if (meshRef.current) {
      meshRef.current.rotation.x = state.clock.elapsedTime * 0.2;
      meshRef.current.rotation.y = state.clock.elapsedTime * 0.3;
      if (hovered) {
        meshRef.current.scale.lerp({ x: 1.3, y: 1.3, z: 1.3 }, 0.1);
      } else {
        meshRef.current.scale.lerp({ x: 1, y: 1, z: 1 }, 0.1);
      }
    }
  });

  const shapes = {
    sphere: <Sphere args={[1, 32, 32]} />,
    box: <Box args={[1.5, 1.5, 1.5]} />,
    torus: <Torus args={[1, 0.4, 16, 32]} />,
  };

  return (
    <Float speed={2} rotationIntensity={1} floatIntensity={2}>
      <mesh
        ref={meshRef}
        position={position}
        onPointerEnter={() => setHovered(true)}
        onPointerLeave={() => setHovered(false)}
      >
        {shapes[shape]}
        <MeshDistortMaterial
          color={color}
          attach="material"
          distort={0.3}
          speed={2}
          roughness={0.2}
          metalness={0.8}
        />
      </mesh>
    </Float>
  );
}

// 3D Scene Component
function ThreeScene() {
  return (
    <Canvas camera={{ position: [0, 0, 10], fov: 50 }}>
      <ambientLight intensity={0.5} />
      <directionalLight position={[10, 10, 5]} intensity={1} />
      <pointLight position={[-10, -10, -5]} intensity={0.5} color="#a855f7" />

      <FloatingShape position={[-6, 3, 0]} shape="sphere" color="#6366f1" />
      <FloatingShape position={[6, 3, 0]} shape="box" color="#a855f7" />
      <FloatingShape position={[-6, -3, 0]} shape="torus" color="#ec4899" />
      <FloatingShape position={[6, -3, 0]} shape="sphere" color="#8b5cf6" />
    </Canvas>
  );
}

export default function Features() {
  const sectionRef = useRef(null);
  const titleRef = useRef(null);
  const cardsRef = useRef([]);
  const iconRefs = useRef([]);
  const threeContainerRef = useRef(null);

  useEffect(() => {
    const ctx = gsap.context(() => {
      // Set initial states to ensure elements start hidden
      gsap.set(titleRef.current, { y: 100, opacity: 0, rotationX: -90 });
      gsap.set(cardsRef.current, { y: 100, opacity: 0, scale: 0.8, rotation: -5 });
      gsap.set(iconRefs.current, { scale: 0 });

      // Animate title with split text effect
      gsap.to(titleRef.current, {
        scrollTrigger: {
          trigger: titleRef.current,
          start: 'top 80%',
          end: 'top 50%',
          scrub: 1,
        },
        y: 0,
        opacity: 1,
        rotationX: 0,
        transformOrigin: 'center bottom',
      });

      // Stagger animate cards with advanced effects
      gsap.to(cardsRef.current, {
        scrollTrigger: {
          trigger: sectionRef.current,
          start: 'top 70%',
          end: 'top 30%',
          scrub: 1,
        },
        y: 0,
        opacity: 1,
        scale: 1,
        rotation: 0,
        stagger: 0.2,
        ease: 'power3.out',
      });

      // Animate icons with bounce effect
      iconRefs.current.forEach((icon, index) => {
        if (icon) {
          gsap.to(icon, {
            scrollTrigger: {
              trigger: icon,
              start: 'top 80%',
              toggleActions: 'play none none reverse',
            },
            scale: 1,
            rotation: 360,
            duration: 1,
            delay: index * 0.1,
            ease: 'elastic.out(1, 0.5)',
          });

          // Continuous floating animation for icons
          gsap.to(icon, {
            y: -10,
            duration: 2,
            repeat: -1,
            yoyo: true,
            ease: 'sine.inOut',
            delay: index * 0.2,
          });
        }
      });

      // 3D container parallax effect
      if (threeContainerRef.current) {
        gsap.to(threeContainerRef.current, {
          scrollTrigger: {
            trigger: sectionRef.current,
            start: 'top bottom',
            end: 'bottom top',
            scrub: 1,
          },
          y: -100,
          rotation: 5,
          ease: 'none',
        });
      }

      // Mouse move parallax effect
      const handleMouseMove = (e) => {
        const { clientX, clientY } = e;
        const xPos = (clientX / window.innerWidth - 0.5) * 30;
        const yPos = (clientY / window.innerHeight - 0.5) * 30;

        gsap.to(threeContainerRef.current, {
          x: xPos,
          y: yPos,
          duration: 1,
          ease: 'power2.out',
        });
      };

      window.addEventListener('mousemove', handleMouseMove);

      // Refresh ScrollTrigger after a short delay to ensure DOM is ready
      setTimeout(() => {
        ScrollTrigger.refresh();
      }, 100);

      return () => {
        window.removeEventListener('mousemove', handleMouseMove);
      };
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
    const icon = iconRefs.current[index];

    if (card && icon) {
      gsap.to(card, {
        y: isHovering ? -10 : 0,
        scale: isHovering ? 1.05 : 1,
        duration: 0.3,
        ease: 'power2.out',
      });

      gsap.to(icon, {
        scale: isHovering ? 1.3 : 1,
        rotation: isHovering ? 360 : 0,
        duration: 0.5,
        ease: 'back.out(2)',
      });
    }
  };

  return (
    <section
      ref={sectionRef}
      className="relative min-h-screen py-20 px-4 flex items-center justify-center overflow-hidden"
    >
      {/* Three.js 3D Background */}
      <div
        ref={threeContainerRef}
        className="absolute inset-0 pointer-events-none opacity-30"
      >
        <ThreeScene />
      </div>

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
              className="group relative p-8 bg-gray-800/50 backdrop-blur-sm rounded-2xl border border-gray-700 hover:border-indigo-500 transition-all duration-300 hover:shadow-lg hover:shadow-indigo-500/20 cursor-pointer"
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

        {/* Enhanced decorative elements */}
        <div className="absolute top-20 left-10 w-72 h-72 bg-indigo-500 rounded-full mix-blend-screen filter blur-3xl opacity-20 animate-pulse"></div>
        <div className="absolute bottom-20 right-10 w-72 h-72 bg-purple-500 rounded-full mix-blend-screen filter blur-3xl opacity-20 animate-pulse" style={{ animationDelay: '1s' }}></div>
        <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-pink-500 rounded-full mix-blend-screen filter blur-3xl opacity-10 animate-pulse" style={{ animationDelay: '2s' }}></div>
      </div>
    </section>
  );
}

