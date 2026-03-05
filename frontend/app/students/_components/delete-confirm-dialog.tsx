import {useState} from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {Button} from "@/components/ui/button";
import {Loader2} from "lucide-react";
import {toast} from "sonner";
import {deleteStudents} from "../_api";

interface DeleteConfirmDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  stuNos: string[];
  onSuccess: () => void;
}

export function DeleteConfirmDialog({
  open,
  onOpenChange,
  stuNos,
  onSuccess,
}: DeleteConfirmDialogProps) {
  const [deleting, setDeleting] = useState(false);

  const handleConfirm = async () => {
    setDeleting(true);
    try {
      const res = await deleteStudents(stuNos);
      if (res.error) {
        toast.error(res.error);
      } else {
        toast.success(`成功删除 ${stuNos.length} 名学生`);
        onOpenChange(false);
        onSuccess();
      }
    } finally {
      setDeleting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>确认删除</DialogTitle>
          <DialogDescription>
            此操作不可撤销，删除后学生数据将被永久移除。
          </DialogDescription>
        </DialogHeader>

        <div className="py-2">
          <p className="text-sm text-muted-foreground mb-2">
            即将删除以下 {stuNos.length} 名学生：
          </p>
          <div className="max-h-48 overflow-y-auto rounded border p-2">
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
        </div>

        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={deleting}
          >
            取消
          </Button>
          <Button
            variant="destructive"
            onClick={handleConfirm}
            disabled={deleting}
          >
            {deleting && <Loader2 className="h-4 w-4 animate-spin mr-1" />}
            确认删除
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
