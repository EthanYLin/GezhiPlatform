"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { get, put } from "@/lib/api-client";
import { clearAuthToken, isAuthenticated, setAuthToken } from "@/lib/auth";
import { useUser } from "@/contexts/user-context";
import { toast } from "sonner";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { AlertCircle, Check } from "lucide-react";
import { Navbar } from "@/components/navbar";

interface UserProfile {
  token: string;
  id: number;
  name: string;
  username: string;
  isLocked: boolean;
  isEnabled: boolean;
  roles: string[];
}

export default function MePage() {
  const router = useRouter();
  const { refreshProfile: refreshUserContext } = useUser();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [passwordForm, setPasswordForm] = useState({
    oldPassword: "",
    newPassword: "",
    confirmPassword: "",
  });
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [passwordLoading, setPasswordLoading] = useState(false);

  useEffect(() => {
    document.title = "我的信息 - 应急协同平台";
  }, []);

  useEffect(() => {
    // 检查是否已登录
    if (!isAuthenticated()) {
      router.push("/auth/login");
      return;
    }

    fetchProfile();
  }, [router]);

  const fetchProfile = async () => {
    setLoading(true);
    setError(null);

    const response = await get<UserProfile>("/auth/me");

    if (response.error) {
      setError(response.error);
      // 401/403 错误已由 api-client 统一处理，会自动跳转到 /auth/unauthorized
    } else if (response.data) {
      setProfile(response.data);
    }

    setLoading(false);
  };

  const handleLogout = () => {
    // 清除认证信息
    clearAuthToken();
    // 跳转到登录页
    router.push("/auth/login");
  };

  const handleChangePassword = () => {
    // 打开 Dialog
    setIsDialogOpen(true);
    setPasswordError(null);
    setPasswordForm({ oldPassword: "", newPassword: "", confirmPassword: "" });
  };

  const handlePasswordSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordLoading(true);
    setPasswordError(null);

    // 记录修改前的启用状态
    const wasNotEnabled = profile && !profile.isEnabled;

    // 只发送 oldPassword 和 newPassword 到后端
    const response = await put<UserProfile>("/auth/password", {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
    });

    if (response.error) {
      setPasswordError(response.error);
      setPasswordLoading(false);
    } else if (response.data) {
      // 修改密码成功，保存新的 token
      setAuthToken(response.data.token);
      // 刷新全局用户信息到 Context
      await refreshUserContext();
      // 关闭 Dialog
      setIsDialogOpen(false);
      setPasswordLoading(false);
      // 显示成功提示
      toast.success("密码修改成功");
      
      // 检查账户是否从未启用变为已启用
      if (wasNotEnabled && response.data.isEnabled) {
        // 账户已成功启用，跳转到工作台
        toast.success("账户已成功启用，正在跳转到工作台...");
        setTimeout(() => {
          router.push("/dashboard");
        }, 1500);
      } else {
        // 刷新页面本地用户信息
        fetchProfile();
      }
    }
  };

  const handlePasswordInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setPasswordForm({
      ...passwordForm,
      [e.target.name]: e.target.value,
    });
  };

  // 密码验证规则
  const validatePassword = (password: string, confirmPassword: string) => {
    const hasLetter = /[a-zA-Z]/.test(password);
    const hasNumber = /[0-9]/.test(password);
    const hasSpecial = /[!@#$%^&*(),.?":{}|<>]/.test(password);
    const lengthValid = password.length >= 6 && password.length <= 30;
    
    const typeCount = [hasLetter, hasNumber, hasSpecial].filter(Boolean).length;
    const typesValid = typeCount >= 2;
    
    // 检查两次密码是否一致
    const passwordsMatch = password.length > 0 && password === confirmPassword;
    
    return {
      lengthValid,
      typesValid,
      passwordsMatch,
      isValid: lengthValid && typesValid && passwordsMatch,
    };
  };

  const passwordValidation = validatePassword(
    passwordForm.newPassword,
    passwordForm.confirmPassword
  );

  if (loading) {
    return (
      <>
        <Navbar />
        <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center">
          <div className="text-center">
            <div className="text-lg font-medium">加载中...</div>
          </div>
        </div>
      </>
    );
  }

  if (error) {
    return (
      <>
        <Navbar />
        <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center p-4">
          <Card className="w-full max-w-2xl">
            <CardHeader className="text-center">
              <CardTitle className="text-2xl">加载失败</CardTitle>
              <CardDescription className="text-destructive">
                {error}
              </CardDescription>
            </CardHeader>
          </Card>
        </div>
      </>
    );
  }

  if (!profile) {
    return null;
  }

  return (
    <>
      <Navbar />
      <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center px-4 py-4 sm:p-4">
      <div className="w-full max-w-2xl space-y-4 sm:space-y-6">
        {/* 账户状态提示 */}
        {profile.isLocked && (
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertTitle>您的账户已被锁定</AlertTitle>
            <AlertDescription>
              请联系管理员获得具体信息。
            </AlertDescription>
          </Alert>
        )}

        {!profile.isLocked && !profile.isEnabled && (
          <Alert variant="destructive">
            <AlertCircle className="h-4 w-4" />
            <AlertTitle>您的账户尚未启用</AlertTitle>
            <AlertDescription>
              您需要先修改初始密码才能启用账户，新密码不能与初始密码相同。
            </AlertDescription>
          </Alert>
        )}

        {/* 用户信息卡片 */}
        <Card>
          <CardHeader>
            <CardTitle className="text-xl sm:text-2xl">个人信息</CardTitle>
            <CardDescription>您的账户详细信息</CardDescription>
          </CardHeader>

          <CardContent className="space-y-6">
            {/* 基本信息 */}
            <div className="grid gap-4">
              <div className="grid grid-cols-1 sm:grid-cols-[120px_1fr] items-start sm:items-center gap-1 sm:gap-4">
                <span className="text-sm font-medium text-muted-foreground">
                  姓名
                </span>
                <span className="text-base">{profile.name}</span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-[120px_1fr] items-start sm:items-center gap-1 sm:gap-4">
                <span className="text-sm font-medium text-muted-foreground">
                  登录时用户名
                </span>
                <span className="text-base">{profile.username}</span>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-[120px_1fr] items-start sm:items-center gap-1 sm:gap-4">
                <span className="text-sm font-medium text-muted-foreground">
                  角色
                </span>
                <div className="flex flex-wrap gap-2">
                  {profile.roles.map((role, index) => (
                    <span
                      key={index}
                      className="inline-flex items-center rounded-md bg-primary/10 px-2 py-1 text-sm font-medium text-primary"
                    >
                      {role}
                    </span>
                  ))}
                </div>
              </div>
            </div>

            {/* 操作按钮 */}
            <div className="flex gap-3 pt-4 border-t">
              <Button onClick={handleChangePassword} variant="default">
                修改密码
              </Button>
              <Button onClick={handleLogout} variant="outline">
                退出登录
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* 修改密码 Dialog */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>修改密码</DialogTitle>
            <DialogDescription>
              请输入您的原密码和新密码
            </DialogDescription>
          </DialogHeader>

          <form onSubmit={handlePasswordSubmit}>
            <div className="space-y-4 py-4">
              {passwordError && (
                <Alert variant="destructive">
                  <AlertCircle className="h-4 w-4" />
                  <AlertDescription>{passwordError}</AlertDescription>
                </Alert>
              )}

              <div className="space-y-2">
                <label
                  htmlFor="oldPassword"
                  className="text-sm font-medium leading-none"
                >
                  原密码
                </label>
                <Input
                  type="password"
                  id="oldPassword"
                  name="oldPassword"
                  value={passwordForm.oldPassword}
                  onChange={handlePasswordInputChange}
                  required
                  disabled={passwordLoading}
                  placeholder="请输入原密码"
                />
              </div>

              <div className="space-y-2">
                <label
                  htmlFor="newPassword"
                  className="text-sm font-medium leading-none"
                >
                  新密码
                </label>
                <Input
                  type="password"
                  id="newPassword"
                  name="newPassword"
                  value={passwordForm.newPassword}
                  onChange={handlePasswordInputChange}
                  required
                  disabled={passwordLoading}
                  placeholder="请输入新密码"
                />
              </div>

              <div className="space-y-2">
                <label
                  htmlFor="confirmPassword"
                  className="text-sm font-medium leading-none"
                >
                  确认新密码
                </label>
                <Input
                  type="password"
                  id="confirmPassword"
                  name="confirmPassword"
                  value={passwordForm.confirmPassword}
                  onChange={handlePasswordInputChange}
                  required
                  disabled={passwordLoading}
                  placeholder="请再次输入新密码"
                />
              </div>

              {/* 密码验证提示 */}
              <div className="space-y-2 text-sm">
                <div className="flex items-center gap-2">
                  <div
                    className={`flex h-4 w-4 items-center justify-center rounded-full ${
                      passwordValidation.lengthValid
                        ? "bg-green-500"
                        : "bg-gray-300"
                    }`}
                  >
                    {passwordValidation.lengthValid && (
                      <Check className="h-3 w-3 text-white" />
                    )}
                  </div>
                  <span
                    className={
                      passwordValidation.lengthValid
                        ? "text-green-600"
                        : "text-muted-foreground"
                    }
                  >
                    长度在 6~30 个字符之间
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <div
                    className={`flex h-4 w-4 items-center justify-center rounded-full ${
                      passwordValidation.typesValid
                        ? "bg-green-500"
                        : "bg-gray-300"
                    }`}
                  >
                    {passwordValidation.typesValid && (
                      <Check className="h-3 w-3 text-white" />
                    )}
                  </div>
                  <span
                    className={
                      passwordValidation.typesValid
                        ? "text-green-600"
                        : "text-muted-foreground"
                    }
                  >
                    包含字母、数字、特殊符号中的至少两类
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <div
                    className={`flex h-4 w-4 items-center justify-center rounded-full ${
                      passwordValidation.passwordsMatch
                        ? "bg-green-500"
                        : "bg-gray-300"
                    }`}
                  >
                    {passwordValidation.passwordsMatch && (
                      <Check className="h-3 w-3 text-white" />
                    )}
                  </div>
                  <span
                    className={
                      passwordValidation.passwordsMatch
                        ? "text-green-600"
                        : "text-muted-foreground"
                    }
                  >
                    两次密码输入一致
                  </span>
                </div>
              </div>
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsDialogOpen(false)}
                disabled={passwordLoading}
              >
                取消
              </Button>
              <Button
                type="submit"
                disabled={passwordLoading || !passwordValidation.isValid}
              >
                {passwordLoading ? "修改中..." : "确认修改"}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>
      </div>
    </>
  );
}

