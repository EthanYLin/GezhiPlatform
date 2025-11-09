"use client";

import { useEffect, useState } from "react";
import { Navbar } from "@/components/navbar";
import { get } from "@/lib/api-client";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Loader2, RefreshCw, Users, AlertCircle } from "lucide-react";

interface Role {
  roleType: string;
  roleAndScope: string;
}

interface User {
  id: number;
  name: string;
  username: string;
  roles: Role[];
  isLocked: boolean;
  isEnabled: boolean;
  lastLoginTime: string | null;
}

interface PageResult {
  content: User[];
  pageNo: number;
  pageSize: number;
  totalPages: number;
  totalElements: number;
}

export default function UsersPage() {
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<PageResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    document.title = "用户维护 - 应急协同平台";
    fetchUsers();
  }, []);

  const fetchUsers = async () => {
    setLoading(true);
    setError(null);

    const response = await get<PageResult>("/admin/users?size=50");

    if (response.error) {
      setError(response.error);
    } else if (response.data) {
      setData(response.data);
    }

    setLoading(false);
  };

  return (
    <>
      <Navbar />
      <div className="min-h-[calc(100vh-4rem)] bg-slate-50 dark:bg-slate-900 p-6">
        <div className="container mx-auto">
          {/* 调试信息头部 */}
          <Card className="mb-6 border-dashed border-2 border-orange-300 bg-orange-50/50">
            <CardHeader>
              <CardTitle className="flex items-center gap-2 text-orange-700">
                <AlertCircle className="h-5 w-5" />
                调试模式 - 用户维护
              </CardTitle>
              <CardDescription className="text-orange-600">
                此页面为开发调试版本，展示从 <code className="bg-orange-100 px-1 rounded">/admin/users</code> 接口获取的数据
              </CardDescription>
            </CardHeader>
          </Card>

          {/* 数据展示区域 */}
          <Card>
            <CardHeader>
              <div className="flex items-center justify-between">
                <div>
                  <CardTitle className="flex items-center gap-2">
                    <Users className="h-5 w-5" />
                    用户列表
                  </CardTitle>
                  {data && (
                    <CardDescription>
                      共 {data.totalElements} 个用户，当前显示第 {data.pageNo + 1} 页（{data.content.length} 条记录）
                    </CardDescription>
                  )}
                </div>
                <Button onClick={fetchUsers} variant="outline" size="sm" disabled={loading}>
                  {loading ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <RefreshCw className="h-4 w-4" />
                  )}
                  <span className="ml-2">刷新</span>
                </Button>
              </div>
            </CardHeader>
            <CardContent>
              {loading && (
                <div className="flex items-center justify-center py-12">
                  <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
                  <span className="ml-3 text-muted-foreground">加载中...</span>
                </div>
              )}

              {error && (
                <div className="rounded-lg bg-destructive/10 border border-destructive/20 p-4 text-center">
                  <p className="text-destructive font-medium">加载失败</p>
                  <p className="text-sm text-destructive/80 mt-1">{error}</p>
                </div>
              )}

              {!loading && !error && data && (
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead>
                      <tr className="border-b">
                        <th className="text-left py-3 px-4 font-medium text-muted-foreground">ID</th>
                        <th className="text-left py-3 px-4 font-medium text-muted-foreground">姓名</th>
                        <th className="text-left py-3 px-4 font-medium text-muted-foreground">用户名</th>
                        <th className="text-left py-3 px-4 font-medium text-muted-foreground">角色</th>
                        <th className="text-left py-3 px-4 font-medium text-muted-foreground">状态</th>
                        <th className="text-left py-3 px-4 font-medium text-muted-foreground">最后登录</th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.content.map((user) => (
                        <tr key={user.id} className="border-b hover:bg-muted/50 transition-colors">
                          <td className="py-3 px-4 text-sm">{user.id}</td>
                          <td className="py-3 px-4 font-medium">{user.name}</td>
                          <td className="py-3 px-4 text-sm text-muted-foreground">{user.username}</td>
                          <td className="py-3 px-4">
                            <div className="flex flex-wrap gap-1">
                              {user.roles.map((role, index) => (
                                <Badge key={index} variant="secondary" className="text-xs">
                                  {role.roleAndScope}
                                </Badge>
                              ))}
                            </div>
                          </td>
                          <td className="py-3 px-4">
                            <div className="flex gap-1">
                              {user.isLocked && (
                                <Badge variant="destructive" className="text-xs">已锁定</Badge>
                              )}
                              {!user.isEnabled && (
                                <Badge variant="outline" className="text-xs">未启用</Badge>
                              )}
                              {!user.isLocked && user.isEnabled && (
                                <Badge variant="default" className="text-xs bg-green-500">正常</Badge>
                              )}
                            </div>
                          </td>
                          <td className="py-3 px-4 text-sm text-muted-foreground">
                            {user.lastLoginTime 
                              ? new Date(user.lastLoginTime).toLocaleString('zh-CN')
                              : '从未登录'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}

              {!loading && !error && data && data.content.length === 0 && (
                <div className="text-center py-12 text-muted-foreground">
                  暂无用户数据
                </div>
              )}
            </CardContent>
          </Card>

          {/* 原始数据展示（用于调试） */}
          {!loading && !error && data && (
            <Card className="mt-6 border-dashed">
              <CardHeader>
                <CardTitle className="text-sm">原始JSON数据（调试）</CardTitle>
              </CardHeader>
              <CardContent>
                <pre className="bg-slate-100 dark:bg-slate-800 p-4 rounded-lg overflow-x-auto text-xs">
                  {JSON.stringify(data, null, 2)}
                </pre>
              </CardContent>
            </Card>
          )}
        </div>
      </div>
    </>
  );
}

