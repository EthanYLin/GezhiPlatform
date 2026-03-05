import {Button} from "@/components/ui/button";
import {Card} from "@/components/ui/card";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue,} from "@/components/ui/select";
import {ScrollArea} from "@/components/ui/scroll-area";
import {Plus, X} from "lucide-react";
import type {RoleDetails, RoleType, UserRoleDetailsDTO} from "../_types";
import {getRoleDetailDefaults, ROLE_TYPE_OPTIONS} from "../_types";
import {RoleDetailForm} from "./role-detail-form";

interface RoleEditorProps {
  roles: UserRoleDetailsDTO[];
  onChange: (roles: UserRoleDetailsDTO[]) => void;
}

export function RoleEditor({ roles, onChange }: RoleEditorProps) {
  const updateRole = (index: number, updates: Partial<UserRoleDetailsDTO>) => {
    const next = [...roles];
    next[index] = { ...next[index], ...updates };
    onChange(next);
  };

  const updateRoleType = (index: number, newType: RoleType) => {
    const next = [...roles];
    next[index] = { roleType: newType, details: getRoleDetailDefaults(newType) };
    onChange(next);
  };

  const updateRoleDetails = (index: number, details: RoleDetails) => {
    updateRole(index, { details });
  };

  const removeRole = (index: number) => {
    onChange(roles.filter((_, i) => i !== index));
  };

  const addRole = () => {
    onChange([
      ...roles,
      { roleType: "协作用户", details: getRoleDetailDefaults("协作用户") },
    ]);
  };

  return (
    <div className="space-y-3">
      <ScrollArea className={roles.length > 3 ? "max-h-80" : ""}>
        <div className="space-y-3 pr-1">
          {roles.length === 0 && (
            <p className="text-sm text-muted-foreground py-2">暂无角色，请添加</p>
          )}
          {roles.map((role, index) => (
            <Card key={index} className="p-3 space-y-2">
              <div className="flex items-center gap-2">
                <Select
                  value={role.roleType}
                  onValueChange={(v) => updateRoleType(index, v as RoleType)}
                >
                  <SelectTrigger className="flex-1">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {ROLE_TYPE_OPTIONS.map((opt) => (
                      <SelectItem key={opt} value={opt}>
                        {opt}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <Button
                  type="button"
                  variant="ghost"
                  size="icon"
                  className="shrink-0"
                  onClick={() => removeRole(index)}
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
              <RoleDetailForm
                roleType={role.roleType}
                details={role.details}
                onChange={(d) => updateRoleDetails(index, d)}
              />
            </Card>
          ))}
        </div>
      </ScrollArea>

      <Button type="button" variant="outline" size="sm" onClick={addRole}>
        <Plus className="h-4 w-4 mr-1" />
        添加角色
      </Button>
    </div>
  );
}
