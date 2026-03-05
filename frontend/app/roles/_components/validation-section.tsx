import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Plus, Trash2 } from "lucide-react";
import type { ValidationRule } from "../_types";

interface ValidationSectionProps {
  validations: ValidationRule[];
  onAdd: () => void;
  onRemove: (index: number) => void;
  onUpdate: (index: number, partial: Partial<ValidationRule>) => void;
}

export function ValidationSection({
  validations,
  onAdd,
  onRemove,
  onUpdate,
}: ValidationSectionProps) {
  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-semibold text-foreground">校验表达式</h3>
        <Button variant="outline" size="sm" onClick={onAdd}>
          <Plus className="h-3.5 w-3.5" />
          添加规则
        </Button>
      </div>

      <div className="max-h-[400px] overflow-y-auto pr-1">
        {validations.length === 0 ? (
          <div className="text-sm text-muted-foreground py-4 text-center border rounded-md">
            暂无校验规则
          </div>
        ) : (
          <div className="space-y-3">
            {validations.map((v, idx) => (
              <div key={idx} className="border rounded-md p-3 space-y-2 relative">
                <Button
                  variant="ghost"
                  size="sm"
                  className="absolute top-1 right-1 h-7 w-7 p-0 text-muted-foreground hover:text-destructive"
                  onClick={() => onRemove(idx)}
                >
                  <Trash2 className="h-3.5 w-3.5" />
                </Button>

                <div className="space-y-1 pr-8">
                  <Label className="text-xs">SpEL 表达式</Label>
                  <Textarea
                    rows={2}
                    value={v.spelExpr}
                    onChange={(e) =>
                      onUpdate(idx, { spelExpr: e.target.value })
                    }
                    placeholder="后端校验表达式..."
                    className="text-xs font-mono"
                  />
                </div>

                <div className="space-y-1">
                  <Label className="text-xs">JS 表达式</Label>
                  <Textarea
                    rows={2}
                    value={v.jsExpr}
                    onChange={(e) =>
                      onUpdate(idx, { jsExpr: e.target.value })
                    }
                    placeholder="前端校验表达式..."
                    className="text-xs font-mono"
                  />
                </div>

                <div className="space-y-1">
                  <Label className="text-xs">提示消息</Label>
                  <Input
                    value={v.message}
                    onChange={(e) =>
                      onUpdate(idx, { message: e.target.value })
                    }
                    placeholder="校验未通过时的提示消息"
                    className="text-sm"
                  />
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
