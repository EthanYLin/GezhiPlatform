import {Button} from "@/components/ui/button";
import {Card} from "@/components/ui/card";
import {Input} from "@/components/ui/input";
import {Label} from "@/components/ui/label";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue,} from "@/components/ui/select";
import {Loader2, RotateCcw, Search} from "lucide-react";
import type {UserFilters} from "../_types";
import {ROLE_TYPE_OPTIONS} from "../_types";

interface UserFiltersProps {
  filters: UserFilters;
  searching: boolean;
  onFilterChange: <K extends keyof UserFilters>(key: K, value: UserFilters[K]) => void;
  onSearch: () => void;
  onReset: () => void;
}

export function UserFilters({
  filters,
  searching,
  onFilterChange,
  onSearch,
  onReset,
}: UserFiltersProps) {
  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") onSearch();
  };

  return (
    <Card className="p-4 sm:p-6">
      <div className="flex flex-wrap gap-4 items-end">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 flex-1 min-w-0">
          <div className="space-y-2">
            <Label htmlFor="keyword">关键词</Label>
            <Input
              id="keyword"
              placeholder="搜索姓名、用户名…"
              value={filters.keyword}
              onChange={(e) => onFilterChange("keyword", e.target.value)}
              onKeyDown={handleKeyDown}
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="isLocked">锁定状态</Label>
            <Select value={filters.isLocked} onValueChange={(v) => onFilterChange("isLocked", v)}>
              <SelectTrigger id="isLocked">
                <SelectValue placeholder="全部" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">全部</SelectItem>
                <SelectItem value="true">已锁定</SelectItem>
                <SelectItem value="false">未锁定</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="isEnabled">启用状态</Label>
            <Select value={filters.isEnabled} onValueChange={(v) => onFilterChange("isEnabled", v)}>
              <SelectTrigger id="isEnabled">
                <SelectValue placeholder="全部" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">全部</SelectItem>
                <SelectItem value="true">已启用</SelectItem>
                <SelectItem value="false">未启用</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <Label htmlFor="roleType">角色类型</Label>
            <Select value={filters.roleType} onValueChange={(v) => onFilterChange("roleType", v)}>
              <SelectTrigger id="roleType">
                <SelectValue placeholder="全部" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">全部</SelectItem>
                {ROLE_TYPE_OPTIONS.map((role) => (
                  <SelectItem key={role} value={role}>
                    {role}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        <div className="flex gap-2 shrink-0">
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
