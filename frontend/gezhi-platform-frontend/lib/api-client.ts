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
      return {
        data: undefined,
        error: data?.error || data?.message || `请求失败: ${response.status}`,
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

