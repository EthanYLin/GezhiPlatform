import {useEffect, useState} from "react";
import {Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle,} from "@/components/ui/sheet";
import {Separator} from "@/components/ui/separator";
import {Button} from "@/components/ui/button";
import {Loader2} from "lucide-react";
import {toast} from "sonner";
import type {UserRoleDetailsDTO} from "../_types";
import {createUser} from "../_api";
import {UserInfoSection} from "./user-info-section";
import {RoleEditor} from "./role-editor";

interface NewUserSheetProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess: () => void;
}

export function NewUserSheet({ open, onOpenChange, onSuccess }: NewUserSheetProps) {
  const [name, setName] = useState("");
  const [username, setUsername] = useState("");
  const [defaultPassword, setDefaultPassword] = useState("");
  const [roles, setRoles] = useState<UserRoleDetailsDTO[]>([]);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (open) {
      setName("");
      setUsername("");
      setDefaultPassword("");
      setRoles([]);
      setErrors({});
    }
  }, [open]);

  const handleInfoChange = (field: string, value: string) => {
    if (field === "name") setName(value);
    if (field === "username") setUsername(value);
    if (field === "defaultPassword") setDefaultPassword(value);
    setErrors((prev) => {
      const next = { ...prev };
      delete next[field];
      return next;
    });
  };

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};
    if (roles.length === 0) {
      newErrors.roles = "至少需要分配一个角色";
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async () => {
    if (!validate()) return;
    setSubmitting(true);
    try {
      const res = await createUser({
        name: name || null,
        username: username || null,
        defaultPassword: defaultPassword || null,
        roles,
      });
      if (res.error) {
        toast.error(res.error);
      } else {
        toast.success("用户创建成功");
        onOpenChange(false);
        onSuccess();
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="sm:max-w-xl overflow-y-auto px-6">
        <SheetHeader className="px-0">
          <SheetTitle>新增用户</SheetTitle>
          <SheetDescription>创建新用户并分配角色</SheetDescription>
        </SheetHeader>

        <div className="space-y-4">
          <div className="space-y-3">
            <h3 className="text-sm font-semibold">个人信息</h3>
            <UserInfoSection
              name={name}
              username={username}
              defaultPassword={defaultPassword}
              errors={errors}
              onChange={handleInfoChange}
              showPassword
            />
          </div>

          <Separator />

          <div className="space-y-3">
            <h3 className="text-sm font-semibold">角色配置</h3>
            {errors.roles && (
              <p className="text-sm text-destructive">{errors.roles}</p>
            )}
            <RoleEditor roles={roles} onChange={setRoles} />
          </div>
        </div>

        <SheetFooter className="px-0 flex-row justify-end gap-2">
          <Button
            variant="outline"
            onClick={() => onOpenChange(false)}
            disabled={submitting}
          >
            取消
          </Button>
          <Button onClick={handleSubmit} disabled={submitting}>
            {submitting && <Loader2 className="h-4 w-4 animate-spin mr-1" />}
            创建用户
          </Button>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}
