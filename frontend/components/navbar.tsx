"use client";

import { useState } from "react";
import { useRouter, usePathname } from "next/navigation";
import Link from "next/link";
import Image from "next/image";
import { ChevronDown, User, LogOut, Menu } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
} from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
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
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = async () => {
    clearAuthToken();
    await refreshProfile();
    router.push("/auth/login");
  };

  const handleProfile = () => {
    router.push("/auth/me");
  };

  const isSuperAdmin = profile?.roles.includes("超级管理员");

  const visibleNavItems = navItems.filter((item) => {
    if (item.roles.length === 0) return true;
    return isSuperAdmin;
  });

  return (
    <nav className="sticky top-0 z-50 w-full border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/60">
      <div className="flex h-14 sm:h-16 items-center px-3 sm:px-4 w-full">
        {/* 移动端汉堡菜单按钮 */}
        <Button
          variant="ghost"
          size="icon"
          className="md:hidden mr-1 flex-shrink-0"
          onClick={() => setMobileMenuOpen(true)}
          aria-label="打开菜单"
        >
          <Menu className="h-5 w-5" />
        </Button>

        {/* Logo + 标题 */}
        <Link href="/" className="flex items-center gap-2 sm:gap-3 hover:opacity-80 transition-opacity flex-shrink-0">
          <div className="relative w-8 h-8 sm:w-10 sm:h-10">
            <Image
              src="/logo.png"
              alt="Logo"
              fill
              className="object-contain"
              priority
            />
          </div>
          <span className="text-lg sm:text-xl font-semibold hidden sm:inline-block">
            应急协同平台
          </span>
        </Link>

        {/* 桌面端导航菜单（居中） */}
        <div className="hidden md:flex flex-1 items-center justify-center gap-1 lg:gap-2">
          {visibleNavItems.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={`px-2 lg:px-3 py-2 text-sm font-medium rounded-md transition-colors hover:bg-accent hover:text-accent-foreground whitespace-nowrap ${
                pathname === item.href
                  ? "bg-accent text-accent-foreground"
                  : "text-muted-foreground"
              }`}
            >
              {item.label}
            </Link>
          ))}
        </div>

        {/* 移动端占位，让用户菜单靠右 */}
        <div className="flex-1 md:hidden" />

        {/* 用户下拉菜单 */}
        <DropdownMenu>
          <DropdownMenuTrigger className="flex items-center gap-1 sm:gap-2 px-2 sm:px-3 py-2 text-sm font-medium rounded-md hover:bg-accent transition-colors outline-none flex-shrink-0">
            {loading ? (
              <span className="h-5 w-12 sm:w-16 bg-muted animate-pulse rounded" />
            ) : (
              <span className="max-w-[80px] sm:max-w-[120px] truncate">
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

      {/* 移动端侧边导航菜单 */}
      <Sheet open={mobileMenuOpen} onOpenChange={setMobileMenuOpen}>
        <SheetContent side="left" className="w-64 p-0">
          <SheetHeader className="px-4 py-4 border-b">
            <SheetTitle className="flex items-center gap-2">
              <div className="relative w-8 h-8">
                <Image src="/logo.png" alt="Logo" fill className="object-contain" />
              </div>
              应急协同平台
            </SheetTitle>
          </SheetHeader>
          <div className="flex flex-col py-2">
            {visibleNavItems.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                onClick={() => setMobileMenuOpen(false)}
                className={`px-4 py-3 text-sm font-medium transition-colors hover:bg-accent ${
                  pathname === item.href
                    ? "bg-accent text-accent-foreground"
                    : "text-muted-foreground"
                }`}
              >
                {item.label}
              </Link>
            ))}
            <Separator className="my-2" />
            <button
              onClick={() => { setMobileMenuOpen(false); handleProfile(); }}
              className="flex items-center px-4 py-3 text-sm font-medium text-muted-foreground hover:bg-accent transition-colors text-left"
            >
              <User className="mr-2 h-4 w-4" />
              个人中心
            </button>
            <button
              onClick={() => { setMobileMenuOpen(false); handleLogout(); }}
              className="flex items-center px-4 py-3 text-sm font-medium text-destructive hover:bg-accent transition-colors text-left"
            >
              <LogOut className="mr-2 h-4 w-4" />
              退出登录
            </button>
          </div>
        </SheetContent>
      </Sheet>
    </nav>
  );
}

