"use client";

import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { ArrowLeft } from "lucide-react";

export function BackButton() {
  const router = useRouter();

  return (
    <Button onClick={() => router.back()} variant="default" className="w-full">
      <ArrowLeft className="mr-2 h-4 w-4" />
      返回上一页
    </Button>
  );
}

