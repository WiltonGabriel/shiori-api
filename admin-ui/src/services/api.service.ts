export class ApiError extends Error {
  public status: number;
  public problemDetail?: {
    type?: string;
    title?: string;
    status?: number;
    detail?: string;
    instance?: string;
  };

  constructor(status: number, message: string, problemDetail?: any) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.problemDetail = problemDetail;
  }
}

const TOKEN_KEY = 'shiori_admin_token';
const USER_KEY = 'shiori_admin_user';

export const authStorage = {
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY) || sessionStorage.getItem(TOKEN_KEY);
  },
  setToken(token: string, remember: boolean = true) {
    if (remember) {
      localStorage.setItem(TOKEN_KEY, token);
    } else {
      sessionStorage.setItem(TOKEN_KEY, token);
    }
  },
  getUser(): any {
    const raw = localStorage.getItem(USER_KEY) || sessionStorage.getItem(USER_KEY);
    if (!raw) return null;
    try {
      return JSON.parse(raw);
    } catch {
      return null;
    }
  },
  setUser(user: any, remember: boolean = true) {
    const serialized = JSON.stringify(user);
    if (remember) {
      localStorage.setItem(USER_KEY, serialized);
    } else {
      sessionStorage.setItem(USER_KEY, serialized);
    }
  },
  clear() {
    localStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
    sessionStorage.removeItem(USER_KEY);
  },
};

export async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers || {});
  
  if (!headers.has('Content-Type') && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const token = authStorage.getToken();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(endpoint, {
    ...options,
    headers,
  });

  if (response.status === 401 || response.status === 403) {
    // Apenas despacha se a requisição tentava acessar área protegida
    if (endpoint.startsWith('/api/admin')) {
      window.dispatchEvent(new CustomEvent('shiori:auth_expired', { detail: { status: response.status } }));
    }
  }

  if (!response.ok) {
    let errorDetail: any = null;
    let message = `Erro ${response.status}: ${response.statusText}`;
    try {
      errorDetail = await response.json();
      if (errorDetail?.detail) {
        message = errorDetail.detail;
      } else if (errorDetail?.title) {
        message = errorDetail.title;
      } else if (errorDetail?.message) {
        message = errorDetail.message;
      }
    } catch {
      // Ignora erro de parse se o corpo não for JSON
    }
    throw new ApiError(response.status, message, errorDetail);
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}
