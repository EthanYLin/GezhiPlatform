/**
 * 认证工具函数
 */

const TOKEN_KEY = "authToken";

/**
 * 设置认证 token
 * 注意：cookie 由后端通过 Set-Cookie 自动设置，这里只设置 localStorage
 */
export function setAuthToken(token: string): void {
  if (typeof window === "undefined") return;

  // 设置 localStorage（用于 API 请求的 authToken header）
  localStorage.setItem(TOKEN_KEY, token);
  
  // cookie 由后端的 Set-Cookie 响应头自动设置，无需手动设置
}

/**
 * 获取认证 token
 */
export function getAuthToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

/**
 * 清除认证 token
 * 注意：这里同时清除 localStorage 和 cookie
 * cookie 需要手动清除，因为退出登录时后端可能不会主动删除 cookie
 */
export function clearAuthToken(): void {
  if (typeof window === "undefined") return;

  // 清除 localStorage
  localStorage.removeItem(TOKEN_KEY);

  // 清除 cookie（需要手动删除）
  document.cookie = `${TOKEN_KEY}=; path=/; max-age=0`;
}

/**
 * 检查是否已登录
 */
export function isAuthenticated(): boolean {
  return getAuthToken() !== null;
}

