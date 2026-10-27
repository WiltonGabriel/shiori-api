import React, { useState, useEffect } from 'react';
import { CheckCircle2, AlertCircle, Info, X } from 'lucide-react';

export interface ToastItem {
  id: string;
  type: 'success' | 'error' | 'info';
  message: string;
}

let toastListeners: Array<(toasts: ToastItem[]) => void> = [];
let toastsState: ToastItem[] = [];

export const toast = {
  success(message: string) {
    this.add('success', message);
  },
  error(message: string) {
    this.add('error', message);
  },
  info(message: string) {
    this.add('info', message);
  },
  add(type: ToastItem['type'], message: string) {
    const id = Math.random().toString(36).substring(2, 9);
    toastsState = [...toastsState, { id, type, message }];
    toastListeners.forEach((l) => l(toastsState));

    setTimeout(() => {
      this.remove(id);
    }, 4500);
  },
  remove(id: string) {
    toastsState = toastsState.filter((t) => t.id !== id);
    toastListeners.forEach((l) => l(toastsState));
  },
};

export const ToastContainer: React.FC = () => {
  const [toasts, setToasts] = useState<ToastItem[]>(toastsState);

  useEffect(() => {
    toastListeners.push(setToasts);
    return () => {
      toastListeners = toastListeners.filter((l) => l !== setToasts);
    };
  }, []);

  if (toasts.length === 0) return null;

  return (
    <div
      style={{
        position: 'fixed',
        bottom: '24px',
        right: '24px',
        zIndex: 2000,
        display: 'flex',
        flexDirection: 'column',
        gap: '8px',
        maxWidth: '380px',
      }}
    >
      {toasts.map((t) => {
        const getBg = () => {
          if (t.type === 'success') return 'rgba(16, 185, 129, 0.92)';
          if (t.type === 'error') return 'rgba(244, 63, 94, 0.92)';
          return 'rgba(99, 102, 241, 0.92)';
        };

        const getIcon = () => {
          if (t.type === 'success') return <CheckCircle2 size={18} />;
          if (t.type === 'error') return <AlertCircle size={18} />;
          return <Info size={18} />;
        };

        return (
          <div
            key={t.id}
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: '12px',
              padding: '12px 16px',
              borderRadius: 'var(--radius-md)',
              background: getBg(),
              color: '#FFFFFF',
              boxShadow: '0 8px 20px rgba(0, 0, 0, 0.35)',
              fontSize: '13px',
              fontWeight: 500,
              animation: 'slideUp 0.2s cubic-bezier(0.16, 1, 0.3, 1)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              {getIcon()}
              <span>{t.message}</span>
            </div>
            <button
              onClick={() => toast.remove(t.id)}
              style={{
                background: 'transparent',
                border: 'none',
                color: '#FFFFFF',
                cursor: 'pointer',
                padding: '2px',
                display: 'flex',
                opacity: 0.8,
              }}
            >
              <X size={14} />
            </button>
          </div>
        );
      })}
    </div>
  );
};
