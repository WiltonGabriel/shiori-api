import React from 'react';

export type BadgeVariant = 'brand' | 'success' | 'warning' | 'danger' | 'neutral' | 'info';

interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  variant?: BadgeVariant;
  size?: 'sm' | 'md';
}

const variantStyles: Record<BadgeVariant, { bg: string; color: string; border: string }> = {
  brand: {
    bg: 'var(--badge-brand-bg)',
    color: 'var(--badge-brand-text)',
    border: 'var(--badge-brand-border)',
  },
  success: {
    bg: 'var(--badge-success-bg)',
    color: 'var(--badge-success-text)',
    border: 'var(--badge-success-border)',
  },
  warning: {
    bg: 'var(--badge-warning-bg)',
    color: 'var(--badge-warning-text)',
    border: 'var(--badge-warning-border)',
  },
  danger: {
    bg: 'var(--badge-danger-bg)',
    color: 'var(--badge-danger-text)',
    border: 'var(--badge-danger-border)',
  },
  neutral: {
    bg: 'rgba(148, 163, 184, 0.12)',
    color: 'var(--text-secondary)',
    border: 'var(--border-subtle)',
  },
  info: {
    bg: 'var(--badge-info-bg)',
    color: 'var(--badge-info-text)',
    border: 'var(--badge-info-border)',
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
