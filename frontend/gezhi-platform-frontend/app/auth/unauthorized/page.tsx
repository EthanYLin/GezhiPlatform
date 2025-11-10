"use client";

import { Suspense, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ShieldAlert, LogIn, Home } from "lucide-react";

function UnauthorizedContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const errorMessage = searchParams.get("error") || "您的登录状态已过期或无效";
  const fromUrl = searchParams.get("from"); // 获取来源 URL

  useEffect(() => {
    document.title = "需要重新登录 - 应急协同平台";
  }, []);

  const handleLogin = () => {
    // 如果有来源 URL，将其作为 redirect 参数传递给登录页
    if (fromUrl) {
      router.push(`/auth/login?redirect=${encodeURIComponent(fromUrl)}`);
    } else {
      router.push("/auth/login");
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <div className="flex justify-center mb-4">
            <div className="rounded-full bg-destructive/10 p-4">
              <ShieldAlert className="h-12 w-12 text-destructive" />
            </div>
          </div>
          <CardTitle className="text-2xl">您需要重新登录</CardTitle>
          <CardDescription className="text-base">
            {errorMessage}
          </CardDescription>
        </CardHeader>

        <CardContent>
          <div className="flex flex-col gap-3">
            <Button onClick={handleLogin} className="w-full">
              <LogIn className="mr-2 h-4 w-4" />
              重新登录
            </Button>
            <Button asChild variant="outline" className="w-full">
              <Link href="/">
                <Home className="mr-2 h-4 w-4" />
                返回首页
              </Link>
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

export default function UnauthorizedPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-screen flex items-center justify-center">
          <div className="text-center">
            <div className="text-lg font-medium">加载中...</div>
          </div>
        </div>
      }
    >
      <UnauthorizedContent />
    </Suspense>
  );
}

