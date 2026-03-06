import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Checkbox } from "@/components/ui/checkbox";
import { ArrowUp, ArrowDown, ArrowUpDown, Loader2 } from "lucide-react";
import type { Student, PageResult, SortState, SortField } from "../_types";

interface StudentTableProps {
  students: PageResult<Student> | null;
  searching: boolean;
  sort: SortState;
  selectedRows: Set<string>;
  onSortChange: (field: SortField) => void;
  onToggleRow: (stuNo: string) => void;
  onToggleAll: () => void;
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

export function StudentTable({
  students,
  searching,
  sort,
  selectedRows,
  onSortChange,
  onToggleRow,
  onToggleAll,
}: StudentTableProps) {
  const allOnPage = students?.content.map((s) => s.stuNo) ?? [];
  const allSelected =
    allOnPage.length > 0 && allOnPage.every((no) => selectedRows.has(no));
  const someSelected =
    !allSelected && allOnPage.some((no) => selectedRows.has(no));

  return (
    <div className="overflow-x-auto">
      <Table className="min-w-[500px] [&_tr>*:first-child]:pl-4 sm:[&_tr>*:first-child]:pl-6 [&_tr>*:last-child]:pr-4 sm:[&_tr>*:last-child]:pr-6">
        <TableHeader>
          <TableRow>
            <TableHead className="w-12">
              <Checkbox
                checked={allSelected ? true : someSelected ? "indeterminate" : false}
                onCheckedChange={onToggleAll}
                aria-label="全选"
              />
            </TableHead>
            <SortableHead label="学号" field="stuNo" sort={sort} onSortChange={onSortChange} />
            <SortableHead label="姓名" field="stuName" sort={sort} onSortChange={onSortChange} />
            <SortableHead label="校区" field="campus" sort={sort} onSortChange={onSortChange} />
            <SortableHead label="年级班级" field="gradeClass" sort={sort} onSortChange={onSortChange} />
          </TableRow>
        </TableHeader>
        <TableBody>
          {searching ? (
            <TableRow>
              <TableCell colSpan={5} className="text-center py-8">
                <Loader2 className="h-6 w-6 animate-spin mx-auto" />
              </TableCell>
            </TableRow>
          ) : students && students.content.length > 0 ? (
            students.content.map((student) => (
              <TableRow
                key={student.stuNo}
                data-state={selectedRows.has(student.stuNo) ? "selected" : undefined}
                className="cursor-pointer"
                onClick={() => onToggleRow(student.stuNo)}
              >
                <TableCell onClick={(e) => e.stopPropagation()}>
                  <Checkbox
                    checked={selectedRows.has(student.stuNo)}
                    onCheckedChange={() => onToggleRow(student.stuNo)}
                    aria-label={`选择 ${student.stuName}`}
                  />
                </TableCell>
                <TableCell className="font-mono">{student.stuNo}</TableCell>
                <TableCell>{student.stuName}</TableCell>
                <TableCell>{student.campus}</TableCell>
                <TableCell>{student.gradeClassName}</TableCell>
              </TableRow>
            ))
          ) : (
            <TableRow>
              <TableCell
                colSpan={5}
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
