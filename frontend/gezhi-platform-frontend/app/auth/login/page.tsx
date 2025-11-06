"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { post } from "@/lib/api-client";
import { setAuthToken, isAuthenticated } from "@/lib/auth";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { AlertCircle } from "lucide-react";

interface LoginRequest {
  username: string;
  password: string;
}

interface LoginResponse {
  token: string;
  id: number;
  name: string;
  username: string;
  isLocked: boolean;
  isEnabled: boolean;
  roles: string[];
}

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [formData, setFormData] = useState<LoginRequest>({
    username: "",
    password: "",
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    document.title = "登录 - 应急协同平台";
  }, []);

  useEffect(() => {
    // 检查是否已登录
    if (isAuthenticated()) {
      // 如果已登录，跳转到重定向 URL 或默认页面
      const redirect = searchParams.get("redirect");
      router.push(redirect || "/auth/me");
    }
  }, [router, searchParams]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    const response = await post<LoginResponse>("/auth/login", formData);

    if (response.error) {
      setError(response.error);
      setLoading(false);
    } else if (response.data) {
      // 保存 token
      setAuthToken(response.data.token);

      // 获取重定向 URL
      const redirect = searchParams.get("redirect");

      // 根据用户状态决定跳转
      if (response.data.isLocked || !response.data.isEnabled) {
        // 账户被锁定或未启用，跳转到个人信息页
        router.push("/auth/me");
      } else if (redirect && redirect !== "/auth/login") {
        // 有重定向 URL 且不是登录页，跳转回原页面
        router.push(redirect);
      } else {
        // 否则跳转到工作台
        router.push("/dashboard");
      }
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl">欢迎登录</CardTitle>
          <CardDescription>请输入您的用户名和密码</CardDescription>
        </CardHeader>

        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-4">
            {error && (
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4" />
                <AlertDescription>{error}</AlertDescription>
              </Alert>
            )}

            <div className="space-y-2">
              <label
                htmlFor="username"
                className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
              >
                用户名
              </label>
              <Input
                type="text"
                id="username"
                name="username"
                value={formData.username}
                onChange={handleChange}
                required
                placeholder="请输入用户名"
                disabled={loading}
              />
            </div>

            <div className="space-y-2">
              <label
                htmlFor="password"
                className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
              >
                密码
              </label>
              <Input
                type="password"
                id="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                required
                placeholder="请输入密码"
                disabled={loading}
              />
            </div>

            <Button type="submit" className="w-full" disabled={loading}>
              {loading ? "登录中..." : "登录"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}

export default function LoginPage() {
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
      <LoginForm />
    </Suspense>
  );
}

