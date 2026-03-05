import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Checkbox } from "@/components/ui/checkbox";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Loader2, Eye } from "lucide-react";
import type { PageResult, PermissionGroup } from "../_types";

interface RoleTableProps {
  data: PageResult<PermissionGroup> | null;
  searching: boolean;
  selectedRows: Set<number>;
  onToggleRow: (id: number) => void;
  onToggleAll: () => void;
  onViewDetail: (id: number) => void;
}

export function RoleTable({
  data,
  searching,
  selectedRows,
  onToggleRow,
  onToggleAll,
  onViewDetail,
}: RoleTableProps) {
  const allOnPage = data?.content.map((g) => g.id!).filter(Boolean) ?? [];
  const allSelected =
    allOnPage.length > 0 && allOnPage.every((id) => selectedRows.has(id));

  if (searching) {
    return (
      <div className="flex items-center justify-center py-16">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!data || data.content.length === 0) {
    return (
      <div className="flex items-center justify-center py-16 text-muted-foreground">
        暂无数据
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <Table className="min-w-[620px]">
        <TableHeader>
          <TableRow>
            <TableHead className="w-[40px]">
              <Checkbox
                checked={allSelected}
                onCheckedChange={onToggleAll}
              />
            </TableHead>
            <TableHead className="w-[160px]">名称</TableHead>
            <TableHead className="w-[220px]">描述</TableHead>
            <TableHead className="w-[80px]">启用</TableHead>
            <TableHead className="w-[100px]">涉及角色</TableHead>
            <TableHead className="w-[60px] text-center">操作</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {data.content.map((group) => (
            <TableRow key={group.id}>
              <TableCell>
                <Checkbox
                  checked={selectedRows.has(group.id!)}
                  onCheckedChange={() => onToggleRow(group.id!)}
                />
              </TableCell>
              <TableCell className="font-medium">{group.name}</TableCell>
              <TableCell className="max-w-[220px] text-sm text-muted-foreground">
                <span className="block truncate">{group.description || "-"}</span>
              </TableCell>
              <TableCell>
                <Badge variant={group.enabled ? "default" : "secondary"}>
                  {group.enabled ? "启用" : "停用"}
                </Badge>
              </TableCell>
              <TableCell>
                <div className="flex flex-wrap gap-1">
                  {group.roleTypes.map((rt) => (
                    <Badge key={rt} variant="outline" className="text-xs">
                      {rt}
                    </Badge>
                  ))}
                  {group.roleTypes.length === 0 && (
                    <span className="text-sm text-muted-foreground">-</span>
                  )}
                </div>
              </TableCell>
              <TableCell className="text-center">
                <Button
                  variant="ghost"
                  size="icon"
                  className="h-8 w-8"
                  aria-label="查看详情"
                  onClick={() => onViewDetail(group.id!)}
                >
                  <Eye className="h-4 w-4" />
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
