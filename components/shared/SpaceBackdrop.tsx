'use client';

import { useEffect } from 'react';

export function SpaceBackdrop() {
  useEffect(() => {
    const root = document.documentElement;

    let raf = 0;
    const onMove = (event: MouseEvent) => {
      if (raf) return;
      raf = window.requestAnimationFrame(() => {
        const x = (event.clientX / window.innerWidth) * 100;
        const y = (event.clientY / window.innerHeight) * 100;
        root.style.setProperty('--mouse-x', `${x}%`);
        root.style.setProperty('--mouse-y', `${y}%`);
        raf = 0;
      });
    };

    const onLeave = () => {
      root.style.setProperty('--mouse-x', '50%');
      root.style.setProperty('--mouse-y', '50%');
    };

    window.addEventListener('mousemove', onMove, { passive: true });
    window.addEventListener('mouseleave', onLeave, { passive: true });

    return () => {
      window.removeEventListener('mousemove', onMove);
      window.removeEventListener('mouseleave', onLeave);
      if (raf) {
        window.cancelAnimationFrame(raf);
      }
    };
  }, []);

  return (
    <div aria-hidden className="space-bg">
      <div className="space-stars space-stars-back" />
      <div className="space-stars space-stars-front" />
      <div className="space-grid" />
      <div className="space-nebula" />
      <div className="space-cursor-glow" />
      <div className="space-vignette" />
    </div>
  );
}

