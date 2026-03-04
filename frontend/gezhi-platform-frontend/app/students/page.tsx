"use client";

import { Suspense } from "react";
import { Navbar } from "@/components/navbar";
import { Loader2 } from "lucide-react";
import { StudentsContent } from "./_components/students-content";

export default function StudentsPage() {
  return (
    <Suspense
      fallback={
        <>
          <Navbar />
          <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        </>
      }
    >
      <StudentsContent />
    </Suspense>
  );
}
