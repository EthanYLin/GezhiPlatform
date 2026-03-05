import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { X } from "lucide-react";
import type { RoleDetails, RoleType, UserRoleDetailsDTO } from "../_types";
import { getRoleDetailDefaults, ROLE_TYPE_OPTIONS } from "../_types";
import { RoleDetailFields } from "./role-detail-fields";

interface RoleEditorProps {
  roles: UserRoleDetailsDTO[];
  onChange: (roles: UserRoleDetailsDTO[]) => void;
}

export function RoleEditor({ roles, onChange }: RoleEditorProps) {
  const updateRoleType = (index: number, newType: RoleType) => {
    const next = [...roles];
    next[index] = { roleType: newType, details: getRoleDetailDefaults(newType) };
    onChange(next);
  };

  const updateRoleDetails = (index: number, details: RoleDetails) => {
    const next = [...roles];
    next[index] = { ...next[index], details };
    onChange(next);
  };

  const removeRole = (index: number) => {
    onChange(roles.filter((_, i) => i !== index));
  };

  return (
    <div className="h-full overflow-y-auto space-y-2 pr-1">
      {roles.length === 0 && (
        <p className="text-sm text-muted-foreground py-2">暂无角色，请添加</p>
      )}
      {roles.map((role, index) => (
        <div key={index} className="flex items-center gap-2">
          <Select
            value={role.roleType}
            onValueChange={(v) => updateRoleType(index, v as RoleType)}
          >
            <SelectTrigger className="w-32 shrink-0">
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
          <RoleDetailFields
            roleType={role.roleType}
            details={role.details}
            onChange={(d) => updateRoleDetails(index, d)}
          />
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className="shrink-0 ml-auto text-muted-foreground hover:text-destructive"
            onClick={() => removeRole(index)}
          >
            <X className="h-4 w-4" />
          </Button>
        </div>
      ))}
    </div>
  );
}

