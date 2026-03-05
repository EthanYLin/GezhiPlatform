"use client";

import { Suspense, useEffect, useState } from "react";
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
import { BackButton } from "@/components/back-button";
import { AlertCircle, Home } from "lucide-react";

const DEFAULT_TITLE = "您访问的页面或数据不存在";
const DEFAULT_DESCRIPTION = "404 该页面或数据可能已被删除";

function NotFoundContent() {
  const searchParams = useSearchParams();
  const [errorTitle, setErrorTitle] = useState(DEFAULT_TITLE);
  const [errorDescription, setErrorDescription] = useState(DEFAULT_DESCRIPTION);

  useEffect(() => {
    // 从 URL 参数读取错误信息
    const message = searchParams.get("message");
    const description = searchParams.get("description");
    
    if (message) {
      setErrorTitle(decodeURIComponent(message));
    }
    if (description) {
      setErrorDescription(decodeURIComponent(description));
    }

    // 设置页面标题
    document.title = "页面未找到 - 应急协同平台";
  }, [searchParams]);

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <div className="flex justify-center mb-4">
            <div className="rounded-full bg-destructive/10 p-4">
              <AlertCircle className="h-12 w-12 text-destructive" />
            </div>
          </div>
          <CardTitle className="text-2xl">{errorTitle}</CardTitle>
          <CardDescription className="text-base">
            {errorDescription}
          </CardDescription>
        </CardHeader>

        <CardContent>
          <div className="flex flex-col gap-3">
            <BackButton />
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

export default function NotFound() {
  return (
    <Suspense>
      <NotFoundContent />
    </Suspense>
  );
}

