import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow,} from "@/components/ui/table";
import {Checkbox} from "@/components/ui/checkbox";
import {Badge} from "@/components/ui/badge";
import {Button} from "@/components/ui/button";
import {ArrowDown, ArrowUp, ArrowUpDown, Eye, Loader2} from "lucide-react";
import type {PageResult, SortField, SortState, User} from "../_types";

interface UserTableProps {
  users: PageResult<User> | null;
  searching: boolean;
  sort: SortState;
  selectedRows: Set<number>;
  onSortChange: (field: SortField) => void;
  onToggleRow: (userId: number) => void;
  onToggleAll: () => void;
  onViewDetail: (userId: number) => void;
}

function SortIcon({ field, sort }: { field: SortField; sort: SortState }) {
  if (sort.field !== field) {
    return <ArrowUpDown className="h-4 w-4 ml-1 opacity-30" />;
  }
  return sort.direction === "asc" ? (
    <ArrowUp className="h-4 w-4 ml-1" />
  ) : (
    <ArrowDown className="h-4 w-4 ml-1" />
  );
}

function SortableHead({
  label,
  field,
  sort,
  onSortChange,
  className,
}: {
  label: string;
  field: SortField;
  sort: SortState;
  onSortChange: (field: SortField) => void;
  className?: string;
}) {
  return (
    <TableHead className={className}>
      <button
        type="button"
        className="inline-flex items-center hover:text-foreground transition-colors cursor-pointer"
        onClick={() => onSortChange(field)}
      >
        {label}
        <SortIcon field={field} sort={sort} />
      </button>
    </TableHead>
  );
}

function UserStatusBadges({ user }: { user: User }) {
  if (user.isLocked) {
    return <Badge variant="destructive" className="text-xs">已锁定</Badge>;
  }
  if (!user.isEnabled) {
    return <Badge variant="outline" className="text-xs">未启用</Badge>;
  }
  return <Badge variant="default" className="text-xs">正常</Badge>;
}

export function UserTable({
  users,
  searching,
  sort,
  selectedRows,
  onSortChange,
  onToggleRow,
  onToggleAll,
  onViewDetail,
}: UserTableProps) {
  const allOnPage = users?.content.map((u) => u.id) ?? [];
  const allSelected =
    allOnPage.length > 0 && allOnPage.every((id) => selectedRows.has(id));
  const someSelected =
    !allSelected && allOnPage.some((id) => selectedRows.has(id));

  return (
    <div className="overflow-x-auto">
      <Table className="[&_tr>*:first-child]:pl-6 [&_tr>*:last-child]:pr-6">
        <TableHeader>
          <TableRow>
            <TableHead className="w-12">
              <Checkbox
                checked={allSelected ? true : someSelected ? "indeterminate" : false}
                onCheckedChange={onToggleAll}
                aria-label="全选"
              />
            </TableHead>
            <SortableHead label="ID" field="id" sort={sort} onSortChange={onSortChange} className="w-20" />
            <SortableHead label="姓名" field="name" sort={sort} onSortChange={onSortChange} />
            <SortableHead label="用户名" field="username" sort={sort} onSortChange={onSortChange} />
            <TableHead>角色</TableHead>
            <TableHead>状态</TableHead>
            <SortableHead label="最后登录" field="lastLoginTime" sort={sort} onSortChange={onSortChange} />
            <TableHead className="w-20">操作</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {searching ? (
            <TableRow>
              <TableCell colSpan={8} className="text-center py-8">
                <Loader2 className="h-6 w-6 animate-spin mx-auto" />
              </TableCell>
            </TableRow>
          ) : users && users.content.length > 0 ? (
            users.content.map((user) => (
              <TableRow
                key={user.id}
                data-state={selectedRows.has(user.id) ? "selected" : undefined}
                className="cursor-pointer"
                onClick={() => onToggleRow(user.id)}
              >
                <TableCell onClick={(e) => e.stopPropagation()}>
                  <Checkbox
                    checked={selectedRows.has(user.id)}
                    onCheckedChange={() => onToggleRow(user.id)}
                    aria-label={`选择 ${user.name ?? user.username}`}
                  />
                </TableCell>
                <TableCell className="font-mono text-sm">{user.id}</TableCell>
                <TableCell className="font-medium">{user.name ?? "—"}</TableCell>
                <TableCell className="text-muted-foreground text-sm">
                  {user.username ?? "—"}
                </TableCell>
                <TableCell>
                  <div className="flex flex-wrap gap-1">
                    {user.roles.map((role, i) => (
                      <Badge key={i} variant="secondary" className="text-xs">
                        {role.roleAndScope}
                      </Badge>
                    ))}
                  </div>
                </TableCell>
                <TableCell>
                  <UserStatusBadges user={user} />
                </TableCell>
                <TableCell className="text-sm text-muted-foreground">
                  {user.lastLoginTime
                    ? new Date(user.lastLoginTime).toLocaleString("zh-CN")
                    : "从未登录"}
                </TableCell>
                <TableCell onClick={(e) => e.stopPropagation()}>
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => onViewDetail(user.id)}
                  >
                    <Eye className="h-4 w-4" />
                  </Button>
                </TableCell>
              </TableRow>
            ))
          ) : (
            <TableRow>
              <TableCell
                colSpan={8}
                className="text-center py-8 text-muted-foreground"
              >
                暂无数据
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </div>
  );
}
