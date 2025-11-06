import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "工作台 - 应急协同平台",
};

export default function DashboardPage() {
  return (
    <div className="min-h-screen flex items-center justify-center p-4">
      <div className="text-center">
        <h1 className="text-4xl font-bold text-primary">
          欢迎使用应急事件处置协同平台
        </h1>
      </div>
    </div>
  );
}

