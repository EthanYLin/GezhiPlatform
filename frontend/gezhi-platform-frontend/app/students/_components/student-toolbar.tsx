import { Button } from "@/components/ui/button";
import { Plus, Pencil, Trash2, RotateCcw, FileSpreadsheet } from "lucide-react";

interface StudentToolbarProps {
  selectedCount: number;
  onAdd: () => void;
  onImport: () => void;
  onEdit: () => void;
  onDelete: () => void;
  onReset: () => void;
}

export function StudentToolbar({
  selectedCount,
  onAdd,
  onImport,
  onEdit,
  onDelete,
  onReset,
}: StudentToolbarProps) {
  return (
    <div className="flex items-center gap-2">
      <Button variant="outline" onClick={onImport}>
        <FileSpreadsheet className="h-4 w-4" />
        从 Excel 导入
      </Button>
      <Button onClick={onAdd}>
        <Plus className="h-4 w-4" />
        添加学生
      </Button>

      {selectedCount === 1 && (
        <Button variant="outline" onClick={onEdit}>
          <Pencil className="h-4 w-4" />
          编辑
        </Button>
      )}

      {selectedCount >= 1 && (
        <>
          <Button variant="destructive" onClick={onDelete}>
            <Trash2 className="h-4 w-4" />
            删除
          </Button>
          <Button variant="destructive" onClick={onReset}>
            <RotateCcw className="h-4 w-4" />
            重置档案
          </Button>
        </>
      )}

      {selectedCount > 0 && (
        <span className="text-sm text-muted-foreground ml-2">
          已选择 {selectedCount} 项
        </span>
      )}
    </div>
  );
}
