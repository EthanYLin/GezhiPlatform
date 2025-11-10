"use client";

import { Suspense, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ShieldX, User } from "lucide-react";

function ForbiddenContent() {
  const searchParams = useSearchParams();
  const errorMessage = searchParams.get("error") || "您没有权限访问此资源";

  useEffect(() => {
    document.title = "无权限访问 - 应急协同平台";
  }, []);

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <div className="flex justify-center mb-4">
            <div className="rounded-full bg-destructive/10 p-4">
              <ShieldX className="h-12 w-12 text-destructive" />
            </div>
          </div>
          <CardTitle className="text-2xl">您没有操作权限</CardTitle>
          <CardDescription className="text-base">
            {errorMessage}
          </CardDescription>
        </CardHeader>

        <CardContent>
          <Button asChild className="w-full">
            <Link href="/auth/me">
              <User className="mr-2 h-4 w-4" />
              前往个人中心
            </Link>
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}

export default function ForbiddenPage() {
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
      <ForbiddenContent />
    </Suspense>
  );
}

