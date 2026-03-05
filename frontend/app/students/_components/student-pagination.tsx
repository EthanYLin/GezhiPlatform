import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { ChevronLeft, ChevronRight } from "lucide-react";
import type { PageResult, Student } from "../_types";

interface StudentPaginationProps {
  students: PageResult<Student>;
  currentPage: number;
  pageSize: number;
  onPageChange: (page: number) => void;
  onPageSizeChange: (value: string) => void;
  onPageInputChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onPageInputBlur: () => void;
}

export function StudentPagination({
  students,
  currentPage,
  pageSize,
  onPageChange,
  onPageSizeChange,
  onPageInputChange,
  onPageInputBlur,
}: StudentPaginationProps) {
  return (
    <div className="flex items-center justify-between px-6 py-4 border-t">
      <div className="flex items-center gap-2">
        <span className="text-sm text-muted-foreground">每页显示</span>
        <Select value={pageSize.toString()} onValueChange={onPageSizeChange}>
          <SelectTrigger className="w-[100px]">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="20">20条/页</SelectItem>
            <SelectItem value="50">50条/页</SelectItem>
            <SelectItem value="100">100条/页</SelectItem>
            <SelectItem value="200">200条/页</SelectItem>
          </SelectContent>
        </Select>
        <span className="text-sm text-muted-foreground">
          共 {students.totalElements} 条
        </span>
      </div>

      <div className="flex items-center gap-2">
        <Button
          variant="outline"
          size="icon"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage <= 1}
        >
          <ChevronLeft className="h-4 w-4" />
        </Button>

        <div className="flex items-center gap-1">
          <Input
            type="number"
            min={1}
            max={students.totalPages}
            value={currentPage}
            onChange={onPageInputChange}
            onBlur={onPageInputBlur}
            className="w-16 text-center [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none [-moz-appearance:textfield]"
          />
          <span className="text-sm text-muted-foreground">
            / {students.totalPages}
          </span>
        </div>

        <Button
          variant="outline"
          size="icon"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage >= students.totalPages}
        >
          <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
