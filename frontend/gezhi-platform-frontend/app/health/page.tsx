"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";

interface HealthResponse {
  message: string;
  serverTime: string;
}

interface ErrorResponse {
  timestamp?: string;
  status: number;
  error: string;
}

export default function HealthPage() {
  const [status, setStatus] = useState<"loading" | "success" | "error">(
    "loading"
  );
  const [data, setData] = useState<HealthResponse | null>(null);
  const [error, setError] = useState<ErrorResponse | null>(null);

  const apiUrl = process.env.NEXT_PUBLIC_API_URL || "/api";

  useEffect(() => {
    document.title = "心跳检测 - 应急协同平台";
  }, []);

  useEffect(() => {
    checkHealth();
  }, []);

  const checkHealth = async () => {
    setStatus("loading");
    setData(null);
    setError(null);

    try {
      const response = await fetch(`${apiUrl}/`);
      
      if (response.ok) {
        const jsonData = await response.json();
        setData(jsonData);
        setStatus("success");
      } else {
        // 尝试解析后端返回的错误信息
        const errorData = await response.json().catch(() => null);
        
        let errorMessage = "";
        if (errorData?.error) {
          errorMessage = errorData.error;
        } else if (response.status === 500) {
          errorMessage = `服务器内部错误: ${response.statusText}`;
        } else if (response.status === 502 || response.status === 503) {
          errorMessage = `后端服务不可用: ${response.statusText}`;
        } else {
          errorMessage = `未知错误: ${response.statusText}`;
        }
        
        setError({
          status: response.status,
          error: errorMessage,
          timestamp: errorData?.timestamp,
        });
        setStatus("error");
      }
    } catch (err) {
      // 网络错误（如 ECONNREFUSED）
      let errorMessage = "无法连接到服务器";
      
      if (err instanceof TypeError) {
        if (err.message.includes("fetch")) {
          errorMessage = "网络连接失败";
        } else {
          errorMessage = `网络错误: ${err.message}`;
        }
      } else if (err instanceof Error) {
        errorMessage = err.message;
      }
      
      setError({
        status: 0,
        error: errorMessage,
      });
      setStatus("error");
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <Card className="w-full max-w-md">
        <CardHeader className="text-center">
          <CardTitle className="text-2xl">
            {status === "loading" && "正在连接..."}
            {status === "success" && "您已与服务器成功连接"}
            {status === "error" && "连接服务器失败"}
          </CardTitle>
          <CardDescription>
            {status === "loading" && "检测服务器状态中"}
            {status === "success" && data && (
              <div className="space-y-1 mt-2">
                <div className="text-xs">
                  <span className="font-medium">Message:</span> {data.message}
                </div>
                <div className="text-xs">
                  <span className="font-medium">Server Time:</span>{" "}
                  {data.serverTime}
                </div>
                <div className="text-xs">
                  <span className="font-medium">API URL:</span> {apiUrl}
                </div>
              </div>
            )}
            {status === "error" && error && (
              <div className="space-y-1 mt-2">
                <div className="text-xs text-destructive">
                  <span className="font-medium">错误码:</span> {error.status}
                </div>
                <div className="text-xs text-destructive">
                  <span className="font-medium">错误信息:</span> {error.error}
                </div>
                <div className="text-xs">
                  <span className="font-medium">API URL:</span> {apiUrl}
                </div>
              </div>
            )}
          </CardDescription>
        </CardHeader>

        {status !== "loading" && (
          <CardContent>
            <div className="flex flex-col gap-3">
              <button
                onClick={checkHealth}
                className="w-full rounded-lg bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
              >
                重新检测
              </button>
              <Button asChild variant="outline" className="w-full">
                <Link href="/">前往首页</Link>
              </Button>
            </div>
          </CardContent>
        )}
      </Card>
    </div>
  );
}

