import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Checkbox } from "@/components/ui/checkbox";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { ChevronsUpDown } from "lucide-react";
import type { RoleType } from "../_types";
import { ALL_ROLE_TYPES } from "../_types";

interface BasicInfoSectionProps {
  name: string;
  enabled: boolean;
  description: string;
  displayCaption: string;
  roleTypes: RoleType[];
  onUpdate: (partial: {
    name?: string;
    enabled?: boolean;
    description?: string;
    displayCaption?: string;
    roleTypes?: RoleType[];
  }) => void;
}

export function BasicInfoSection({
  name,
  enabled,
  description,
  displayCaption,
  roleTypes,
  onUpdate,
}: BasicInfoSectionProps) {
  const toggleRoleType = (rt: RoleType) => {
    const next = roleTypes.includes(rt)
      ? roleTypes.filter((r) => r !== rt)
      : [...roleTypes, rt];
    onUpdate({ roleTypes: next });
  };

  return (
    <div className="space-y-3">
      <h3 className="text-sm font-semibold text-foreground">基本信息</h3>

      <div className="flex items-center gap-4">
        <div className="flex-1 space-y-1">
          <Label htmlFor="pg-name" className="text-xs">
            权限组名称
          </Label>
          <Input
            id="pg-name"
            value={name}
            onChange={(e) => onUpdate({ name: e.target.value })}
            placeholder="请输入权限组名称"
          />
        </div>
        <div className="flex items-center gap-2 pt-5">
          <Checkbox
            id="pg-enabled"
            checked={enabled}
            onCheckedChange={(c) => onUpdate({ enabled: !!c })}
          />
          <Label htmlFor="pg-enabled" className="text-sm cursor-pointer">
            启用
          </Label>
        </div>
      </div>

      <div className="space-y-1">
        <Label htmlFor="pg-desc" className="text-xs">
          描述
        </Label>
        <Textarea
          id="pg-desc"
          rows={2}
          value={description}
          onChange={(e) => onUpdate({ description: e.target.value })}
          placeholder="权限组描述..."
        />
      </div>

      <div className="space-y-1">
        <Label htmlFor="pg-caption" className="text-xs">
          公告
        </Label>
        <Textarea
          id="pg-caption"
          rows={2}
          value={displayCaption}
          onChange={(e) => onUpdate({ displayCaption: e.target.value })}
          placeholder="页面上给用户的文本提示..."
        />
      </div>

      <div className="space-y-1">
        <Label className="text-xs">角色类型</Label>
        <Popover>
          <PopoverTrigger asChild>
            <Button
              variant="outline"
              className="w-full justify-between font-normal"
            >
              <span className="flex flex-wrap gap-1 overflow-hidden">
                {roleTypes.length === 0 ? (
                  <span className="text-muted-foreground">选择角色类型...</span>
                ) : (
                  roleTypes.map((rt) => (
                    <Badge key={rt} variant="secondary" className="text-xs">
                      {rt}
                    </Badge>
                  ))
                )}
              </span>
              <ChevronsUpDown className="h-4 w-4 shrink-0 opacity-50" />
            </Button>
          </PopoverTrigger>
          <PopoverContent className="w-[--radix-popover-trigger-width] p-2" align="start">
            <div className="grid grid-cols-2 gap-1">
              {ALL_ROLE_TYPES.map((rt) => (
                <label
                  key={rt}
                  className="flex items-center gap-2 rounded-md px-2 py-1.5 hover:bg-accent cursor-pointer"
                >
                  <Checkbox
                    checked={roleTypes.includes(rt)}
                    onCheckedChange={() => toggleRoleType(rt)}
                  />
                  <span className="text-sm">{rt}</span>
                </label>
              ))}
            </div>
          </PopoverContent>
        </Popover>
      </div>
    </div>
  );
}
