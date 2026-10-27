import React from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
}

export const Input: React.FC<InputProps> = ({ label, error, style, id, ...props }) => {
  const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', width: '100%' }}>
      {label && (
        <label htmlFor={inputId} style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)' }}>
          {label}
        </label>
      )}
      <input
        id={inputId}
        style={{
          background: 'var(--bg-glass-input)',
          border: error ? '1px solid var(--accent-rose)' : '1px solid var(--border-subtle)',
          borderRadius: 'var(--radius-md)',
          padding: '9px 12px',
          color: 'var(--text-primary)',
          fontSize: '13px',
          outline: 'none',
          transition: 'border-color var(--transition-fast), box-shadow var(--transition-fast)',
          width: '100%',
          ...style,
        }}
        {...props}
      />
      {error && <span style={{ fontSize: '11px', color: 'var(--accent-rose)' }}>{error}</span>}
    </div>
  );
};

interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
}

export const Select: React.FC<SelectProps> = ({ label, error, children, style, id, ...props }) => {
  const selectId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', width: '100%' }}>
      {label && (
        <label htmlFor={selectId} style={{ fontSize: '12px', fontWeight: 600, color: 'var(--text-secondary)' }}>
          {label}
        </label>
      )}
      <select
        id={selectId}
        style={{
          background: 'var(--bg-glass-input)',
          border: error ? '1px solid var(--accent-rose)' : '1px solid var(--border-subtle)',
          borderRadius: 'var(--radius-md)',
          padding: '9px 12px',
          color: 'var(--text-primary)',
          fontSize: '13px',
          outline: 'none',
          transition: 'border-color var(--transition-fast), box-shadow var(--transition-fast)',
          width: '100%',
          ...style,
        }}
        {...props}
      >
        {children}
      </select>
      {error && <span style={{ fontSize: '11px', color: 'var(--accent-rose)' }}>{error}</span>}
    </div>
  );
};
