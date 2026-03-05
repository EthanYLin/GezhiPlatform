"use client";

import { useRouter, usePathname } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import { ChevronDown, User, LogOut } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { clearAuthToken } from "@/lib/auth";
import { useUser } from "@/contexts/user-context";

const navItems = [
  { label: "工作台", href: "/dashboard", roles: [] }, // 所有用户都可见
  { label: "档案查询与更新", href: "/records", roles: [] }, // 所有用户都可见
  { label: "学生维护", href: "/students", roles: ["超级管理员"] },
  { label: "用户维护", href: "/users", roles: ["超级管理员"] },
  { label: "权限组维护", href: "/roles", roles: ["超级管理员"] },
  { label: "审计日志", href: "/audit-logs", roles: ["超级管理员"] },
];

export function Navbar() {
  const router = useRouter();
  const pathname = usePathname();
  const { profile, loading, refreshProfile } = useUser();

  const handleLogout = async () => {
    clearAuthToken();
    await refreshProfile(); // 清除 Context 中的用户信息
    router.push("/auth/login");
  };

  const handleProfile = () => {
    router.push("/auth/me");
  };

  // 检查用户是否有超级管理员角色
  const isSuperAdmin = profile?.roles.includes("超级管理员");

  // 过滤导航项
  const visibleNavItems = navItems.filter((item) => {
    if (item.roles.length === 0) return true; // 所有用户都可见
    return isSuperAdmin; // 仅超级管理员可见
  });

  return (
    <nav className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="flex h-16 items-center px-4 w-full">
        {/* Logo + 标题 */}
        <Link href="/" className="flex items-center gap-3 hover:opacity-80 transition-opacity flex-shrink-0">
          <div className="relative w-10 h-10">
            <Image
              src="/logo.png"
              alt="Logo"
              fill
              className="object-contain"
              priority
            />
          </div>
          <span className="text-xl font-semibold hidden sm:inline-block">
            应急协同平台
          </span>
        </Link>

        {/* 导航菜单（居中） */}
        <div className="flex-1 flex items-center justify-center gap-1 md:gap-2">
          {visibleNavItems.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={`px-3 py-2 text-sm font-medium rounded-md transition-colors hover:bg-accent hover:text-accent-foreground ${
                pathname === item.href
                  ? "bg-accent text-accent-foreground"
                  : "text-muted-foreground"
              }`}
            >
              {item.label}
            </Link>
          ))}
        </div>

        {/* 用户下拉菜单 */}
        <DropdownMenu>
          <DropdownMenuTrigger className="flex items-center gap-2 px-3 py-2 text-sm font-medium rounded-md hover:bg-accent transition-colors outline-none flex-shrink-0">
            {loading ? (
              <span className="h-5 w-16 bg-muted animate-pulse rounded" />
            ) : (
              <span className="max-w-[120px] truncate">
                {profile?.name || "未登录"}
              </span>
            )}
            <ChevronDown className="h-4 w-4" />
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-auto">
            <DropdownMenuItem onClick={handleProfile} className="cursor-pointer">
              <User className="mr-2 h-4 w-4" />
              <span>个人中心</span>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={handleLogout} className="cursor-pointer text-destructive">
              <LogOut className="mr-2 h-4 w-4" />
              <span>退出登录</span>
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </nav>
  );
}

