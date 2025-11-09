"use client";

import { useEffect } from "react";
import { Navbar } from "@/components/navbar";

export default function RecordsPage() {
  useEffect(() => {
    document.title = "档案查询与更新 - 应急协同平台";
  }, []);

  return (
    <>
      <Navbar />
      <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center p-4">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-primary">档案查询与更新</h1>
          <p className="mt-4 text-muted-foreground">该功能正在开发中...</p>
        </div>
      </div>
    </>
  );
}

