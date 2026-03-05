import {Input} from "@/components/ui/input";
import {Label} from "@/components/ui/label";

interface UserInfoSectionProps {
  name: string;
  username: string;
  defaultPassword?: string;
  errors: Record<string, string>;
  onChange: (field: string, value: string) => void;
  showPassword?: boolean;
}

export function UserInfoSection({
  name,
  username,
  defaultPassword = "",
  errors,
  onChange,
  showPassword = false,
}: UserInfoSectionProps) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div className="space-y-2">
        <Label htmlFor="user-name">姓名</Label>
        <Input
          id="user-name"
          placeholder="(选填)"
          value={name}
          onChange={(e) => onChange("name", e.target.value)}
        />
        {errors.name && (
          <p className="text-sm text-destructive">{errors.name}</p>
        )}
      </div>

      <div className="space-y-2">
        <Label htmlFor="user-username">登录时用户名</Label>
        <Input
          id="user-username"
          placeholder="(选填，不设置则无法登录)"
          value={username}
          onChange={(e) => onChange("username", e.target.value)}
        />
        {errors.username && (
          <p className="text-sm text-destructive">{errors.username}</p>
        )}
      </div>

      {showPassword && (
        <div className="space-y-2 md:col-span-2">
          <Label htmlFor="user-password">初始密码</Label>
          <Input
            id="user-password"
            placeholder="(选填，不设置则无法登录)"
            value={defaultPassword}
            onChange={(e) => onChange("defaultPassword", e.target.value)}
          />
          {errors.defaultPassword && (
            <p className="text-sm text-destructive">{errors.defaultPassword}</p>
          )}
        </div>
      )}
    </div>
  );
}
