import { get, post, put, del } from "@/lib/api-client";
import type {
  Student,
  NewStudentRequest,
  PageResult,
  GradeClass,
} from "./_types";

export async function fetchGrades() {
  return get<number[]>("/admin/grades");
}

export async function fetchClasses(gradeNo: number) {
  return get<GradeClass[]>(`/admin/grades/${gradeNo}/classes`);
}

export async function fetchStudents(params: {
  gradeNo?: number;
  classNo?: number;
  page: number;
  size: number;
  sort?: string;
}) {
  const query = new URLSearchParams();
  if (params.gradeNo) query.append("gradeNo", params.gradeNo.toString());
  if (params.classNo) query.append("classNo", params.classNo.toString());
  query.append("page", params.page.toString());
  query.append("size", params.size.toString());
  if (params.sort) query.append("sort", params.sort);
  return get<PageResult<Student>>(`/admin/students?${query.toString()}`);
}

export async function fetchStudent(stuNo: string) {
  return get<Student>(`/admin/students/${stuNo}`);
}

export async function createStudents(data: NewStudentRequest[]) {
  return post<Student[]>("/admin/students", data);
}

export async function updateStudent(stuNo: string, data: NewStudentRequest) {
  return put<Student>(`/admin/students/${stuNo}`, data);
}

export async function deleteStudents(stuNos: string[]) {
  const query = stuNos.map((s) => `stuNos=${encodeURIComponent(s)}`).join("&");
  return del<Student[]>(`/admin/students?${query}`);
}

export async function resetArchives(stuNos: string[]) {
  const query = stuNos.map((s) => `stuNos=${encodeURIComponent(s)}`).join("&");
  return put<void>(`/admin/archives/reset?${query}`);
}
