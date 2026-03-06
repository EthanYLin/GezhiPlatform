import {Button} from "@/components/ui/button";
import {Trash2, UserPlus, FileSpreadsheet} from "lucide-react";

interface UserToolbarProps {
  selectedCount: number;
  onAdd: () => void;
  onImport: () => void;
  onBatchDelete: () => void;
}

export function UserToolbar({
  selectedCount,
  onAdd,
  onImport,
  onBatchDelete,
}: UserToolbarProps) {
  return (
    <div className="flex items-center gap-2 flex-wrap">
      <Button variant="outline" onClick={onImport}>
        <FileSpreadsheet className="h-4 w-4" />
        从 Excel 导入
      </Button>
      <Button onClick={onAdd}>
        <UserPlus className="h-4 w-4" />
        新增用户
      </Button>

      {selectedCount >= 1 && (
        <Button variant="destructive" onClick={onBatchDelete}>
          <Trash2 className="h-4 w-4" />
          删除
        </Button>
      )}

      {selectedCount > 0 && (
        <span className="text-sm text-muted-foreground ml-2">
          已选择 {selectedCount} 项
        </span>
      )}
    </div>
  );
}
