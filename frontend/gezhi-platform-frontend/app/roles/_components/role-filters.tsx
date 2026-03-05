import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Loader2, Search, RotateCcw } from "lucide-react";
import { ALL_ROLE_TYPES } from "../_types";

interface RoleFiltersProps {
  keyword: string;
  roleType: string;
  searching: boolean;
  onKeywordChange: (value: string) => void;
  onRoleTypeChange: (value: string) => void;
  onSearch: () => void;
  onReset: () => void;
}

export function RoleFilters({
  keyword,
  roleType,
  searching,
  onKeywordChange,
  onRoleTypeChange,
  onSearch,
  onReset,
}: RoleFiltersProps) {
  return (
    <Card className="p-6">
      <div className="flex flex-col sm:flex-row items-stretch sm:items-end gap-3">
        <div className="flex-1 min-w-0">
          <Input
            placeholder="搜索权限组名称..."
            value={keyword}
            onChange={(e) => onKeywordChange(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") onSearch();
            }}
          />
        </div>

        <div className="w-full sm:w-[180px]">
          <Select value={roleType || "all"} onValueChange={(v) => onRoleTypeChange(v === "all" ? "" : v)}>
            <SelectTrigger>
              <SelectValue placeholder="全部角色类型" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部角色类型</SelectItem>
              {ALL_ROLE_TYPES.map((rt) => (
                <SelectItem key={rt} value={rt}>
                  {rt}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="flex gap-2">
          <Button onClick={onSearch} disabled={searching}>
            {searching ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <>
                <Search className="h-4 w-4" />
                查询
              </>
            )}
          </Button>
          <Button variant="outline" onClick={onReset} disabled={searching}>
            <RotateCcw className="h-4 w-4" />
            重置
          </Button>
        </div>
      </div>
    </Card>
  );
}
