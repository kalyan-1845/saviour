'use client';

import { ReactNode } from 'react';

interface EmergencyCardProps {
  icon?: ReactNode;
  title: string;
  description?: string;
  onClick?: () => void;
  variant?: 'default' | 'highlight' | 'warning';
  className?: string;
}

export function EmergencyCard({
  icon,
  title,
  description,
  onClick,
  variant = 'default',
  className = '',
}: EmergencyCardProps) {
  const variantStyles = {
    default:
      'bg-white/10 border border-white/20 hover:border-white/40 hover:bg-white/20',
    highlight:
      'bg-gradient-to-br from-orange-400/20 to-red-500/20 border-2 border-orange-400 hover:border-orange-300 hover:from-orange-400/30 hover:to-red-500/30',
    warning:
      'bg-gradient-to-br from-yellow-400/20 to-red-600/20 border-2 border-yellow-400 hover:border-yellow-300',
  };

  return (
    <div
      onClick={onClick}
      className={`
        p-6 rounded-xl cursor-pointer
        transition-all duration-300
        transform hover:scale-105 hover:shadow-2xl
        ${variantStyles[variant]}
        ${className}
      `}
    >
      {icon && (
        <div className="text-4xl mb-4 flex justify-center">{icon}</div>
      )}
      <h3 className="text-xl font-bold text-white text-center mb-2">
        {title}
      </h3>
      {description && (
        <p className="text-white/80 text-sm text-center">{description}</p>
      )}
    </div>
  );
}
