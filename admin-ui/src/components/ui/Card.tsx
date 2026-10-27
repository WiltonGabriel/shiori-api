import React from 'react';

interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  glow?: boolean;
}

export const Card: React.FC<CardProps> = ({ glow, children, style, className = '', ...props }) => {
  return (
    <div
      style={{
        background: 'var(--bg-glass-card)',
        backdropFilter: 'blur(16px)',
        WebkitBackdropFilter: 'blur(16px)',
        border: glow ? '1px solid var(--border-focus)' : '1px solid var(--border-subtle)',
        borderRadius: 'var(--radius-lg)',
        padding: '20px',
        boxShadow: glow ? '0 0 25px var(--brand-glow), var(--card-shadow)' : 'var(--card-shadow)',
        transition: 'border-color var(--transition-fast), box-shadow var(--transition-fast)',
        ...style,
      }}
      className={`admin-card ${className}`}
      {...props}
    >
      {children}
    </div>
  );
};
