import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";
import { resetArchives } from "../_api";

interface ResetConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  stuNos: string[];
  onSuccess: () => void;
}

export function ResetConfirmDialog({
  open,
  onOpenChange,
  stuNos,
  onSuccess,
}: ResetConfirmDialogProps) {
  const [resetting, setResetting] = useState(false);

  const handleConfirm = async () => {
    setResetting(true);
    try {
      const res = await resetArchives(stuNos);
      if (res.error) {
        toast.error(res.error);
      } else {
        toast.success(`成功重置 ${stuNos.length} 名学生的档案`);
        onOpenChange(false);
        onSuccess();
      }
    } finally {
      setResetting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>确认重置档案</DialogTitle>
          <DialogDescription>
            此操作将把选中学生的档案清空为初始状态，不可撤销。
          </DialogDescription>
        </DialogHeader>

        <div className="py-2">
          <p className="text-sm text-muted-foreground mb-2">
            即将重置以下 {stuNos.length} 名学生的档案：
          </p>
          <div className="flex flex-wrap gap-1.5">
            {stuNos.map((no) => (
              <span
                key={no}
                className="inline-block bg-muted px-2 py-0.5 rounded text-sm font-mono"
              >
                {no}
              </span>
            ))}
          </div>
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={resetting}
          >
            取消
          </Button>
          <Button
            variant="destructive"
            onClick={handleConfirm}
            disabled={resetting}
          >
            {resetting && <Loader2 className="h-4 w-4 animate-spin mr-1" />}
            确认重置
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
