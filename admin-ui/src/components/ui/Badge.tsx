import React from 'react';

export type BadgeVariant = 'brand' | 'success' | 'warning' | 'danger' | 'neutral' | 'info';

interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant;
  size?: 'sm' | 'md';
}

const variantStyles: Record<BadgeVariant, { bg: string; color: string; border: string }> = {
  brand: {
    bg: 'rgba(99, 102, 241, 0.16)',
    color: 'var(--brand-light)',
    border: 'rgba(99, 102, 241, 0.35)',
  },
  success: {
    bg: 'rgba(16, 185, 129, 0.16)',
    color: '#34D399',
    border: 'rgba(16, 185, 129, 0.35)',
  },
  warning: {
    bg: 'rgba(245, 158, 11, 0.16)',
    color: '#FBBF24',
    border: 'rgba(245, 158, 11, 0.35)',
  },
  danger: {
    bg: 'rgba(244, 63, 94, 0.16)',
    color: '#FB7185',
    border: 'rgba(244, 63, 94, 0.35)',
  },
  neutral: {
    bg: 'rgba(148, 163, 184, 0.12)',
    color: 'var(--text-secondary)',
    border: 'var(--border-subtle)',
  },
  info: {
    bg: 'rgba(6, 182, 212, 0.16)',
    color: '#38BDF8',
    border: 'rgba(6, 182, 212, 0.35)',
  },
};

export const Badge: React.FC<BadgeProps> = ({
  variant = 'neutral',
  size = 'md',
  children,
  style,
  ...props
}) => {
  const v = variantStyles[variant];
  return (
    <span
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '4px',
        padding: size === 'sm' ? '2px 8px' : '4px 10px',
        fontSize: size === 'sm' ? '11px' : '12px',
        fontWeight: 600,
        borderRadius: 'var(--radius-full)',
        backgroundColor: v.bg,
        color: v.color,
        border: `1px solid ${v.border}`,
        whiteSpace: 'nowrap',
        ...style,
      }}
      {...props}
    >
      {children}
    </span>
  );
};
