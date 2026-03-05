export interface Student {
  stuNo: string;
  stuName: string;
  campus: string;
  gradeClassName: string;
  gradeNo?: number;
  classNo?: number;
}

export interface NewStudentRequest {
  stuNo: string;
  stuName: string;
  campus?: string;
  gradeNo?: number;
  classNo?: number;
}

export interface PageResult<T> {
  content: T[];
  pageNo: number;
  pageSize: number;
  totalPages: number;
  totalElements: number;
}

export interface GradeClass {
  gradeNo: number;
  classNo: number;
}

export type SortField = "stuNo" | "stuName" | "campus" | "gradeClass";

export type SortDirection = "asc" | "desc";

export interface SortState {
  field: SortField;
  direction: SortDirection;
}
