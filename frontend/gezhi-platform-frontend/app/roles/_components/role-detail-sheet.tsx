"use client";

import { useEffect } from "react";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Loader2, Save } from "lucide-react";
import { useRoleDetail } from "../_use-role-detail";
import { BasicInfoSection } from "./basic-info-section";
import { FieldPermissionSection } from "./field-permission-section";
import { ValidationSection } from "./validation-section";

interface RoleDetailSheetProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  editId?: number;
  onSuccess: () => void;
}

export function RoleDetailSheet({
  open,
  onOpenChange,
  editId,
  onSuccess,
}: RoleDetailSheetProps) {
  const {
    loading,
    saving,
    mode,
    formData,
    flatFields,
    readableSet,
    writableSet,
    addArraySet,
    editArraySet,
    deleteArraySet,
    validations,
    loadDetail,
    toggleVisible,
    toggleEditable,
    toggleArrayPerm,
    updateFormData,
    addValidation,
    removeValidation,
    updateValidation,
    save,
  } = useRoleDetail();

  useEffect(() => {
    if (open) {
      loadDetail(editId);
    }
  }, [open, editId, loadDetail]);

  const handleSave = async () => {
    const ok = await save();
    if (ok) {
      onOpenChange(false);
      onSuccess();
    }
  };

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent
        side="right"
        showCloseButton={true}
        className="w-full sm:max-w-3xl flex h-full flex-col overflow-hidden p-0 gap-0"
      >
        <SheetHeader className="shrink-0 px-4 pt-4 pb-2 border-b">
          <div className="flex items-start justify-between pr-8">
            <div>
              <SheetTitle>
                {mode === "edit" ? "权限组详情" : "新增权限组"}
              </SheetTitle>
              <SheetDescription>
                {mode === "edit"
                  ? "查看和编辑权限组配置"
                  : "创建一个新的权限组"}
              </SheetDescription>
            </div>
            <Button onClick={handleSave} disabled={saving || loading} size="sm">
              {saving ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Save className="h-4 w-4" />
              )}
              {mode === "edit" ? "保存修改" : "创建"}
            </Button>
          </div>
        </SheetHeader>

        {loading ? (
          <div className="flex-1 flex items-center justify-center">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        ) : (
          <div className="flex-1 min-h-0 overflow-y-auto space-y-4 px-4 py-4 pr-3">
            <BasicInfoSection
              name={formData.name}
              enabled={formData.enabled}
              description={formData.description}
              displayCaption={formData.displayCaption}
              roleTypes={formData.roleTypes}
              onUpdate={updateFormData}
            />

            <Separator />

            <FieldPermissionSection
              flatFields={flatFields}
              readableSet={readableSet}
              writableSet={writableSet}
              addArraySet={addArraySet}
              editArraySet={editArraySet}
              deleteArraySet={deleteArraySet}
              onToggleVisible={toggleVisible}
              onToggleEditable={toggleEditable}
              onToggleArrayPerm={toggleArrayPerm}
            />

            <Separator />

            <ValidationSection
              validations={validations}
              onAdd={addValidation}
              onRemove={removeValidation}
              onUpdate={updateValidation}
            />
          </div>
        )}
      </SheetContent>
    </Sheet>
  );
}
