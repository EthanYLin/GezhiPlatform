import {useEffect, useState} from "react";
import {Sheet, SheetContent, SheetHeader, SheetTitle,} from "@/components/ui/sheet";
import {Button} from "@/components/ui/button";
import {Separator} from "@/components/ui/separator";
import {Tooltip, TooltipContent, TooltipTrigger,} from "@/components/ui/tooltip";
import {Popover, PopoverContent, PopoverTrigger,} from "@/components/ui/popover";
import {Check, Copy, KeyRound, Loader2, Lock, LockOpen, LogOut, Trash2} from "lucide-react";
import {toast} from "sonner";
import type {User, UserRoleDetailsDTO} from "../_types";
import {
  deleteUsers,
  getUserDetail,
  getUserRoles,
  kickoutUser,
  lockUser,
  resetPassword,
  unlockUser,
  updateUserInfo,
  updateUserRoles,
} from "../_api";
import {UserInfoSection} from "./user-info-section";
import {RoleEditor} from "./role-editor";
import {ConfirmPopover} from "./confirm-popover";

interface UserDetailSheetProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  userId: number | null;
  onUserUpdated: () => void;
  onUserDeleted: () => void;
}

export function UserDetailSheet({
  open,
  onOpenChange,
  userId,
  onUserUpdated,
  onUserDeleted,
}: UserDetailSheetProps) {
  const [loadingDetail, setLoadingDetail] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const [roles, setRoles] = useState<UserRoleDetailsDTO[]>([]);

  // info form
  const [infoName, setInfoName] = useState("");
  const [infoUsername, setInfoUsername] = useState("");
  const [infoErrors, setInfoErrors] = useState<Record<string, string>>({});
  const [savingInfo, setSavingInfo] = useState(false);

  // roles
  const [savingRoles, setSavingRoles] = useState(false);

  // password reset popover
  const [resetPopoverOpen, setResetPopoverOpen] = useState(false);
  const [resettingPassword, setResettingPassword] = useState(false);
  const [resetResult, setResetResult] = useState<{ password: string } | null>(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (open && userId) {
      loadData(userId);
    }
    if (!open) {
      setUser(null);
      setRoles([]);
      setResetResult(null);
      setResetPopoverOpen(false);
    }
  }, [open, userId]);

  const loadData = async (id: number) => {
    setLoadingDetail(true);
    const [detailRes, rolesRes] = await Promise.all([
      getUserDetail(id),
      getUserRoles(id),
    ]);
    if (detailRes.data) {
      setUser(detailRes.data);
      setInfoName(detailRes.data.name ?? "");
      setInfoUsername(detailRes.data.username ?? "");
      setInfoErrors({});
    }
    if (rolesRes.data) {
      setRoles(rolesRes.data);
    }
    setLoadingDetail(false);
  };

  // --- Save info ---
  const handleSaveInfo = async () => {
    if (!userId) return;
    setSavingInfo(true);
    try {
      const res = await updateUserInfo(userId, {
        name: infoName || null,
        username: infoUsername || null,
      });
      if (res.error) {
        toast.error(res.error);
      } else {
        toast.success("个人信息已更新");
        if (res.data) setUser(res.data);
        onUserUpdated();
      }
    } finally {
      setSavingInfo(false);
    }
  };

  // --- Save roles ---
  const handleSaveRoles = async () => {
    if (!userId) return;
    setSavingRoles(true);
    try {
      const res = await updateUserRoles(userId, roles);
      if (res.error) {
        toast.error(res.error);
      } else {
        toast.success("角色配置已更新");
        if (res.data) setRoles(res.data);
        onUserUpdated();
      }
    } finally {
      setSavingRoles(false);
    }
  };

  // --- Action bar handlers ---
  const handleResetPassword = async () => {
    if (!userId) return;
    setResettingPassword(true);
    try {
      const res = await resetPassword(userId);
      if (res.error) {
        toast.error(res.error);
        setResetPopoverOpen(false);
      } else if (res.data) {
        setResetResult({ password: res.data.defaultPassword });
        onUserUpdated();
      }
    } finally {
      setResettingPassword(false);
    }
  };

  const handleCopyPassword = async () => {
    if (!resetResult) return;
    await navigator.clipboard.writeText(resetResult.password);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleLockToggle = async () => {
    if (!userId || !user) return;
    const action = user.isLocked ? unlockUser : lockUser;
    const res = await action(userId);
    if (res.error) {
      toast.error(res.error);
    } else {
      toast.success(user.isLocked ? "已解锁用户" : "已锁定用户");
      await loadData(userId);
      onUserUpdated();
    }
  };

  const handleKickout = async () => {
    if (!userId) return;
    const res = await kickoutUser(userId);
    if (res.error) {
      toast.error(res.error);
    } else {
      toast.success("已强制用户下线");
    }
  };

  const handleDelete = async () => {
    if (!userId) return;
    const res = await deleteUsers([userId]);
    if (res.error) {
      toast.error(res.error);
    } else {
      toast.success("用户已删除");
      onOpenChange(false);
      onUserDeleted();
    }
  };

  const handleInfoChange = (field: string, value: string) => {
    if (field === "name") setInfoName(value);
    if (field === "username") setInfoUsername(value);
    setInfoErrors((prev) => {
      const next = { ...prev };
      delete next[field];
      return next;
    });
  };

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="sm:max-w-xl overflow-y-auto px-6">
        <SheetHeader className="px-0">
          <SheetTitle>用户详情</SheetTitle>
        </SheetHeader>

        {loadingDetail ? (
          <div className="flex items-center justify-center py-20">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        ) : user ? (
          <div className="space-y-4">
            {/* Action Bar */}
            <div className="flex items-center gap-1">
              {/* Reset password */}
              <Popover
                open={resetPopoverOpen}
                onOpenChange={(o) => {
                  setResetPopoverOpen(o);
                  if (!o) {
                    setResetResult(null);
                    setCopied(false);
                  }
                }}
              >
                <Tooltip>
                  <TooltipTrigger asChild>
                    <PopoverTrigger asChild>
                      <Button variant="outline" size="icon">
                        <KeyRound className="h-4 w-4" />
                      </Button>
                    </PopoverTrigger>
                  </TooltipTrigger>
                  <TooltipContent>重置密码</TooltipContent>
                </Tooltip>
                <PopoverContent className="w-72">
                  {resetResult ? (
                    <div className="space-y-3">
                      <p className="font-medium text-sm">密码已重置</p>
                      <div className="flex items-center gap-2 bg-muted rounded px-3 py-2">
                        <code className="flex-1 text-sm font-mono">
                          {resetResult.password}
                        </code>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-7 w-7"
                          onClick={handleCopyPassword}
                        >
                          {copied ? (
                            <Check className="h-3 w-3" />
                          ) : (
                            <Copy className="h-3 w-3" />
                          )}
                        </Button>
                      </div>
                      <p className="text-xs text-muted-foreground">
                        请将新密码告知用户，此密码仅显示一次
                      </p>
                    </div>
                  ) : (
                    <div className="space-y-3">
                      <p className="font-medium text-sm">重置密码</p>
                      <p className="text-sm text-muted-foreground">
                        将为用户生成新的临时密码，用户将被强制下线。
                      </p>
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => setResetPopoverOpen(false)}
                          disabled={resettingPassword}
                        >
                          取消
                        </Button>
                        <Button
                          size="sm"
                          onClick={handleResetPassword}
                          disabled={resettingPassword}
                        >
                          {resettingPassword && (
                            <Loader2 className="h-3 w-3 animate-spin mr-1" />
                          )}
                          确认重置
                        </Button>
                      </div>
                    </div>
                  )}
                </PopoverContent>
              </Popover>

              {/* Lock/Unlock */}
              <Tooltip>
                <TooltipTrigger asChild>
                  <div>
                    <ConfirmPopover
                      trigger={
                        <Button variant="outline" size="icon">
                          {user.isLocked ? (
                            <LockOpen className="h-4 w-4" />
                          ) : (
                            <Lock className="h-4 w-4" />
                          )}
                        </Button>
                      }
                      title={user.isLocked ? "解锁用户" : "锁定用户"}
                      description={
                        user.isLocked
                          ? "解锁后用户可重新登录系统。"
                          : "锁定后用户将被强制下线，无法使用系统。"
                      }
                      confirmText={user.isLocked ? "解锁" : "锁定"}
                      confirmVariant={user.isLocked ? "default" : "destructive"}
                      onConfirm={handleLockToggle}
                    />
                  </div>
                </TooltipTrigger>
                <TooltipContent>{user.isLocked ? "解锁" : "锁定"}</TooltipContent>
              </Tooltip>

              {/* Kickout */}
              <Tooltip>
                <TooltipTrigger asChild>
                  <div>
                    <ConfirmPopover
                      trigger={
                        <Button variant="outline" size="icon">
                          <LogOut className="h-4 w-4" />
                        </Button>
                      }
                      title="强制下线"
                      description="将立即终止该用户的所有会话。"
                      confirmText="强制下线"
                      onConfirm={handleKickout}
                    />
                  </div>
                </TooltipTrigger>
                <TooltipContent>强制下线</TooltipContent>
              </Tooltip>

              {/* Delete */}
              <Tooltip>
                <TooltipTrigger asChild>
                  <div>
                    <ConfirmPopover
                      trigger={
                        <Button
                          variant="outline"
                          size="icon"
                          className="text-destructive hover:text-destructive"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      }
                      title="删除用户"
                      description="删除后无法恢复，确定要删除该用户吗？"
                      confirmText="删除"
                      confirmVariant="destructive"
                      onConfirm={handleDelete}
                    />
                  </div>
                </TooltipTrigger>
                <TooltipContent>删除用户</TooltipContent>
              </Tooltip>
            </div>

            <Separator />

            {/* Personal info */}
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold">个人信息</h3>
                <Button
                  size="sm"
                  onClick={handleSaveInfo}
                  disabled={savingInfo}
                >
                  {savingInfo && <Loader2 className="h-3 w-3 animate-spin mr-1" />}
                  保存信息
                </Button>
              </div>
              <UserInfoSection
                name={infoName}
                username={infoUsername}
                errors={infoErrors}
                onChange={handleInfoChange}
              />
            </div>

            <Separator />

            {/* Roles */}
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-semibold">角色配置</h3>
                <Button
                  size="sm"
                  onClick={handleSaveRoles}
                  disabled={savingRoles}
                >
                  {savingRoles && <Loader2 className="h-3 w-3 animate-spin mr-1" />}
                  保存角色
                </Button>
              </div>
              <RoleEditor roles={roles} onChange={setRoles} />
            </div>
          </div>
        ) : null}
      </SheetContent>
    </Sheet>
  );
}
