import {useState} from "react";
import {Button} from "@/components/ui/button";
import {Popover, PopoverContent, PopoverTrigger,} from "@/components/ui/popover";
import {Loader2} from "lucide-react";

interface ConfirmPopoverProps {
  trigger: React.ReactNode;
  title: string;
  description?: string;
  confirmText?: string;
  confirmVariant?: "default" | "destructive" | "outline";
  onConfirm: () => Promise<void>;
  disabled?: boolean;
}

export function ConfirmPopover({
  trigger,
  title,
  description,
  confirmText = "确认",
  confirmVariant = "default",
  onConfirm,
  disabled,
}: ConfirmPopoverProps) {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleConfirm = async () => {
    setLoading(true);
    try {
      await onConfirm();
      setOpen(false);
    } finally {
      setLoading(false);
    }
  };

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger asChild disabled={disabled}>
        {trigger}
      </PopoverTrigger>
      <PopoverContent className="w-72">
        <div className="space-y-3">
          <p className="font-medium text-sm">{title}</p>
          {description && (
            <p className="text-sm text-muted-foreground">{description}</p>
          )}
          <div className="flex justify-end gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setOpen(false)}
              disabled={loading}
            >
              取消
            </Button>
            <Button
              variant={confirmVariant}
              size="sm"
              onClick={handleConfirm}
              disabled={loading}
            >
              {loading && <Loader2 className="h-3 w-3 animate-spin mr-1" />}
              {confirmText}
            </Button>
          </div>
        </div>
      </PopoverContent>
    </Popover>
  );
}
