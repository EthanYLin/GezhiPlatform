/**
 * API 客户端工具
 * 通过 Next.js rewrites 转发请求到后端
 */

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "/api";

export interface ApiResponse<T = any> {
  data?: T;
  error?: string;
  status: number;
}

/**
 * 获取存储的认证 token
 */
function getAuthToken(): string | null {
  if (typeof window !== "undefined") {
    return localStorage.getItem("authToken");
  }
  return null;
}

/**
 * 清除认证 token
 */
function clearAuthToken(): void {
  if (typeof window !== "undefined") {
    localStorage.removeItem("authToken");
    document.cookie = "authToken=; path=/; max-age=0";
  }
}

/**
 * 处理 401 未认证错误
 */
function handleUnauthorized(errorMessage: string): void {
  if (typeof window !== "undefined") {
    // 清除本地登录状态
    clearAuthToken();
    // 跳转到未授权页面
    const currentPath = window.location.pathname;
    // 避免在登录页、首页、未授权页面重复跳转
    if (
      currentPath !== "/auth/login" &&
      currentPath !== "/" &&
      currentPath !== "/auth/unauthorized"
    ) {
      // 记录完整的来源 URL（包括路径和查询参数）
      const returnUrl = window.location.pathname + window.location.search;
      window.location.href = `/auth/unauthorized?error=${encodeURIComponent(errorMessage)}&from=${encodeURIComponent(returnUrl)}`;
    }
  }
}

/**
 * 处理 403 无权限错误
 */
function handleForbidden(errorMessage: string): void {
  if (typeof window !== "undefined") {
    // 不清除登录状态，仅跳转到禁止访问页面
    const currentPath = window.location.pathname;
    // 避免在禁止访问页面重复跳转
    if (currentPath !== "/auth/forbidden") {
      window.location.href = `/auth/forbidden?error=${encodeURIComponent(errorMessage)}`;
    }
  }
}

/**
 * 通用请求函数
 */
async function request<T = any>(
  endpoint: string,
  options: RequestInit = {}
): Promise<ApiResponse<T>> {
  try {
    const url = `${API_BASE_URL}${endpoint}`;
    const token = getAuthToken();
    
    const headers: Record<string, string> = {
      "Content-Type": "application/json",
    };
    
    // 如果有 token，添加到请求头
    if (token) {
      headers["authToken"] = token;
    }
    
    // 合并用户提供的 headers
    if (options.headers) {
      Object.assign(headers, options.headers);
    }
    
    const response = await fetch(url, {
      ...options,
      headers,
    });

    const data = await response.json().catch(() => null);

    if (!response.ok) {
      const errorMessage = data?.error || data?.message || `请求失败: ${response.status}`;
      
      // 分别处理 401 和 403 错误
      if (response.status === 401) {
        handleUnauthorized(errorMessage); // 清除登录状态并跳转
      } else if (response.status === 403) {
        handleForbidden(errorMessage); // 仅跳转，不清除登录状态
      }
      
      return {
        data: undefined,
        error: errorMessage,
        status: response.status,
      };
    }

    return {
      data,
      status: response.status,
    };
  } catch (error) {
    return {
      data: undefined,
      error: error instanceof Error ? error.message : "网络请求失败",
      status: 0,
    };
  }
}

/**
 * GET 请求
 */
export async function get<T = any>(endpoint: string): Promise<ApiResponse<T>> {
  return request<T>(endpoint, { method: "GET" });
}

/**
 * POST 请求
 */
export async function post<T = any>(
  endpoint: string,
  body?: any
): Promise<ApiResponse<T>> {
  return request<T>(endpoint, {
    method: "POST",
    body: body ? JSON.stringify(body) : undefined,
  });
}

/**
 * PUT 请求
 */
export async function put<T = any>(
  endpoint: string,
  body?: any
): Promise<ApiResponse<T>> {
  return request<T>(endpoint, {
    method: "PUT",
    body: body ? JSON.stringify(body) : undefined,
  });
}

/**
 * DELETE 请求
 */
export async function del<T = any>(
  endpoint: string
): Promise<ApiResponse<T>> {
  return request<T>(endpoint, { method: "DELETE" });
}

