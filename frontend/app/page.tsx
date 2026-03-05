import Link from "next/link";
import Image from "next/image";
import type { Metadata } from "next";
import { Button } from "@/components/ui/button";
import { ArrowRight } from "lucide-react";

export const metadata: Metadata = {
  title: "格致中学应急事件处置协同平台",
};

export default function Home() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 via-white to-slate-50 dark:from-slate-900 dark:via-slate-800 dark:to-slate-900">
      <main className="container px-4 py-16 mx-auto">
        <div className="max-w-4xl mx-auto text-center space-y-8">
          {/* Logo */}
          <div className="flex justify-center mb-8">
            <div className="relative w-32 h-32 md:w-40 md:h-40">
              <Image
                src="/logo.png"
                alt="格致中学校徽"
                fill
                className="object-contain"
                priority
              />
            </div>
          </div>

          {/* 主标题 */}
          <div className="space-y-4">
            <h1 className="text-5xl md:text-6xl lg:text-7xl font-bold tracking-tight text-slate-900 dark:text-white">
              格致中学
              <span className="block mt-2 bg-gradient-to-r from-blue-600 to-cyan-600 bg-clip-text text-transparent">
                应急事件处置协同平台
              </span>
            </h1>
          </div>

          {/* 副标题/介绍文字 */}
          <p className="text-xl md:text-2xl text-slate-600 dark:text-slate-300 max-w-2xl mx-auto leading-relaxed">
            高效协同，快速响应，为校园安全保驾护航
          </p>

          {/* CTA 按钮 */}
          <div className="pt-4">
            <Button asChild size="lg" className="text-lg px-8 py-6 rounded-full shadow-lg hover:shadow-xl transition-all">
              <Link href="/dashboard" className="flex items-center gap-2">
                前往工作台
                <ArrowRight className="h-5 w-5" />
              </Link>
            </Button>
          </div>

          {/* 装饰性元素 */}
          <div className="pt-12 grid grid-cols-3 gap-8 max-w-3xl mx-auto text-center">
            <div className="space-y-2">
              <div className="text-3xl font-bold text-primary">高效</div>
              <p className="text-sm text-muted-foreground">快速响应机制</p>
            </div>
            <div className="space-y-2">
              <div className="text-3xl font-bold text-primary">协同</div>
              <p className="text-sm text-muted-foreground">多部门联动</p>
            </div>
            <div className="space-y-2">
              <div className="text-3xl font-bold text-primary">安全</div>
              <p className="text-sm text-muted-foreground">全面风险防控</p>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
