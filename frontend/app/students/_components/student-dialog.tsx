import { useEffect, useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Loader2 } from "lucide-react";
import { toast } from "sonner";
import type { NewStudentRequest } from "../_types";
import { createStudents, updateStudent, fetchStudent } from "../_api";

interface StudentDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  mode: "add" | "edit";
  editStuNo?: string;
  onSuccess: () => void;
}

const EMPTY_FORM: NewStudentRequest = {
  stuNo: "",
  stuName: "",
  campus: undefined,
  gradeNo: undefined,
  classNo: undefined,
};

export function StudentDialog({
  open,
  onOpenChange,
  mode,
  editStuNo,
  onSuccess,
}: StudentDialogProps) {
  const [form, setForm] = useState<NewStudentRequest>({ ...EMPTY_FORM });
  const [submitting, setSubmitting] = useState(false);
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!open) return;

    if (mode === "edit" && editStuNo) {
      setLoadingDetail(true);
      fetchStudent(editStuNo).then((res) => {
        if (res.data) {
          setForm({
            stuNo: res.data.stuNo,
            stuName: res.data.stuName,
            campus: res.data.campus || undefined,
            gradeNo: res.data.gradeNo,
            classNo: res.data.classNo,
          });
        }
        setLoadingDetail(false);
      });
    } else {
      setForm({ ...EMPTY_FORM });
    }
    setErrors({});
  }, [open, mode, editStuNo]);

  const validate = (): boolean => {
    const errs: Record<string, string> = {};

    if (!form.stuNo.trim()) {
      errs.stuNo = "学号不能为空";
    } else if (!/^[0-9]+$/.test(form.stuNo)) {
      errs.stuNo = "学号只能包含数字";
    } else if (form.stuNo.length > 50) {
      errs.stuNo = "学号不能超过50个字符";
    }

    if (!form.stuName.trim()) {
      errs.stuName = "姓名不能为空";
    } else if (!/^[\p{L} ]+$/u.test(form.stuName)) {
      errs.stuName = "姓名只能包含中文、英文和空格";
    } else if (form.stuName.length > 50) {
      errs.stuName = "姓名不能超过50个字符";
    }

    if (form.gradeNo !== undefined) {
      if (form.gradeNo < 1900 || form.gradeNo > 2100) {
        errs.gradeNo = "年级范围 1900-2100";
      }
    }

    if (form.classNo !== undefined) {
      if (form.classNo < 1 || form.classNo > 100) {
        errs.classNo = "班级范围 1-100";
      }
    }

    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    setSubmitting(true);

    try {
      if (mode === "add") {
        const res = await createStudents([form]);
        if (res.error) {
          toast.error(res.error);
        } else {
          toast.success("添加学生成功");
          onOpenChange(false);
          onSuccess();
        }
      } else {
        const res = await updateStudent(editStuNo!, form);
        if (res.error) {
          toast.error(res.error);
        } else {
          toast.success("更新学生信息成功");
          onOpenChange(false);
          onSuccess();
        }
      }
    } finally {
      setSubmitting(false);
    }
  };

  const updateField = <K extends keyof NewStudentRequest>(
    key: K,
    value: NewStudentRequest[K]
  ) => {
    setForm((prev) => ({ ...prev, [key]: value }));
    if (errors[key]) {
      setErrors((prev) => {
        const next = { ...prev };
        delete next[key];
        return next;
      });
    }
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>{mode === "add" ? "添加学生" : "编辑学生"}</DialogTitle>
          <DialogDescription>
            {mode === "add"
              ? "填写学生信息，带 * 的字段为必填项"
              : "修改学生信息，带 * 的字段为必填项"}
          </DialogDescription>
        </DialogHeader>

        {loadingDetail ? (
          <div className="flex items-center justify-center py-8">
            <Loader2 className="h-6 w-6 animate-spin" />
          </div>
        ) : (
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="stuNo">学号 *</Label>
              <Input
                id="stuNo"
                placeholder="纯数字"
                value={form.stuNo}
                onChange={(e) => updateField("stuNo", e.target.value)}
              />
              {errors.stuNo && (
                <p className="text-sm text-destructive">{errors.stuNo}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="stuName">姓名 *</Label>
              <Input
                id="stuName"
                placeholder="中文、英文或空格"
                value={form.stuName}
                onChange={(e) => updateField("stuName", e.target.value)}
              />
              {errors.stuName && (
                <p className="text-sm text-destructive">{errors.stuName}</p>
              )}
            </div>

            <div className="space-y-2">
              <Label htmlFor="campus">校区</Label>
              <Select
                value={form.campus || ""}
                onValueChange={(v) =>
                  updateField("campus", v === "none" ? undefined : v)
                }
              >
                <SelectTrigger id="campus">
                  <SelectValue placeholder="请选择校区" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="none">不选择</SelectItem>
                  <SelectItem value="黄浦校区">黄浦校区</SelectItem>
                  <SelectItem value="奉贤校区">奉贤校区</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="gradeNo">年级</Label>
                <Input
                  id="gradeNo"
                  type="number"
                  placeholder="如 2025"
                  value={form.gradeNo ?? ""}
                  onChange={(e) =>
                    updateField(
                      "gradeNo",
                      e.target.value ? parseInt(e.target.value) : undefined
                    )
                  }
                />
                {errors.gradeNo && (
                  <p className="text-sm text-destructive">{errors.gradeNo}</p>
                )}
              </div>

              <div className="space-y-2">
                <Label htmlFor="classNo">班级</Label>
                <Input
                  id="classNo"
                  type="number"
                  placeholder="如 1"
                  value={form.classNo ?? ""}
                  onChange={(e) =>
                    updateField(
                      "classNo",
                      e.target.value ? parseInt(e.target.value) : undefined
                    )
                  }
                />
                {errors.classNo && (
                  <p className="text-sm text-destructive">{errors.classNo}</p>
                )}
              </div>
            </div>
          </div>
        )}

        <DialogFooter>
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={submitting}
          >
            取消
          </Button>
          <Button onClick={handleSubmit} disabled={submitting || loadingDetail}>
            {submitting && <Loader2 className="h-4 w-4 animate-spin mr-1" />}
            {mode === "add" ? "添加" : "保存"}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
