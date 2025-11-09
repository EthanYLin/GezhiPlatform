import { NextResponse } from "next/server";
import type { NextRequest } from "next/server";

// 不需要登录即可访问的路径
const PUBLIC_PATHS = ["/", "/health", "/auth/login", "/auth/unauthorized", "/auth/forbidden"];

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl;

  // 检查是否是公开路径
  const isPublicPath = PUBLIC_PATHS.some((path) => pathname === path);

  // 如果是公开路径，直接放行
  if (isPublicPath) {
    return NextResponse.next();
  }

  // 检查静态资源和 Next.js 内部路径
  if (
    pathname.startsWith("/_next") ||
    pathname.startsWith("/api") ||
    pathname.includes(".")
  ) {
    return NextResponse.next();
  }

  // 检查是否有 authToken
  const token = request.cookies.get("authToken")?.value;
  
  // 如果没有 token，重定向到登录页，并携带原始 URL
  if (!token) {
    const url = request.nextUrl.clone();
    const returnUrl = request.nextUrl.pathname + request.nextUrl.search;
    url.pathname = "/auth/login";
    url.search = `?redirect=${encodeURIComponent(returnUrl)}`;
    return NextResponse.redirect(url);
  }

  return NextResponse.next();
}

export const config = {
  matcher: [
    /*
     * 匹配所有路径，除了：
     * - _next/static (静态文件)
     * - _next/image (图片优化文件)
     * - favicon.ico (favicon 文件)
     */
    "/((?!_next/static|_next/image|favicon.ico).*)",
  ],
};

