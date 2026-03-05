import type {Metadata} from "next";
import "./globals.css";
import {Toaster} from "@/components/ui/sonner";
import {UserProvider} from "@/contexts/user-context";
import {TooltipProvider} from "@/components/ui/tooltip";

export const metadata: Metadata = {
  title: "格致中学应急事件处置协同平台",
  description: "格致中学应急事件处置协同平台",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN">
      <body className="antialiased">
        <UserProvider>
          <TooltipProvider>
            {children}
            <Toaster position="top-center" />
          </TooltipProvider>
        </UserProvider>
      </body>
    </html>
  );
}
