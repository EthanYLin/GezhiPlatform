import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Loader2, Search, RotateCcw } from "lucide-react";
import type { GradeClass } from "../_types";

interface StudentFiltersProps {
  grades: number[];
  classes: GradeClass[];
  selectedGrade: string;
  selectedClass: string;
  searching: boolean;
  onGradeChange: (value: string) => void;
  onClassChange: (value: string) => void;
  onSearch: () => void;
  onReset: () => void;
}

export function StudentFilters({
  grades,
  classes,
  selectedGrade,
  selectedClass,
  searching,
  onGradeChange,
  onClassChange,
  onSearch,
  onReset,
}: StudentFiltersProps) {
  const availableClasses = selectedGrade && selectedGrade !== "all"
    ? classes.map((gc) => gc.classNo).sort((a, b) => a - b)
    : [];

  return (
    <Card className="p-6">
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
        <div className="space-y-2">
          <Label htmlFor="grade">年级</Label>
          <Select value={selectedGrade} onValueChange={onGradeChange}>
            <SelectTrigger id="grade">
              <SelectValue placeholder="请选择年级" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部年级</SelectItem>
              {grades.map((grade) => (
                <SelectItem key={grade} value={grade.toString()}>
                  {grade}届
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-2">
          <Label htmlFor="class">班级</Label>
          <Select
            value={selectedClass}
            onValueChange={onClassChange}
            disabled={!selectedGrade || selectedGrade === "all"}
          >
            <SelectTrigger id="class">
              <SelectValue placeholder="请选择班级" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">全部班级</SelectItem>
              {availableClasses.map((classNo) => (
                <SelectItem key={classNo} value={classNo.toString()}>
                  {classNo}班
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        <div className="flex gap-2 items-end md:col-span-2">
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
