// ===== 角色类型枚举 =====

export const ROLE_TYPE_OPTIONS = [
  "超级管理员",
  "校级领导",
  "年级组长",
  "班主任",
  "多班级观察员",
  "协作用户",
  "家长用户",
  "学生用户",
  "新生家长",
] as const;

export type RoleType = (typeof ROLE_TYPE_OPTIONS)[number];

// ===== 角色详情结构 =====

export interface GradeClass {
  gradeNo: number | null;
  classNo: number | null;
}

/** 超级管理员、校级领导 */
export type DetailsEmpty = object

/** 年级组长 */
export interface DetailsForGradeDean {
  gradeNo: number | null;
}

/** 班主任 */
export interface DetailsForClassAdvisor {
  gradeClass: GradeClass | null;
}

/** 多班级观察员 */
export interface DetailsForMultiClassObserver {
  gradeClasses: GradeClass[];
}

/** 协作用户、家长用户、新生家长 */
export interface DetailsForParentOrCU {
  stuNos: string[];
}

/** 学生用户 */
export interface DetailsForStudentUser {
  stuNo: string | null;
}

export type RoleDetails =
  | DetailsEmpty
  | DetailsForGradeDean
  | DetailsForClassAdvisor
  | DetailsForMultiClassObserver
  | DetailsForParentOrCU
  | DetailsForStudentUser;

// ===== 角色 DTO =====

export interface UserRoleDetailsDTO {
  roleType: RoleType;
  details: RoleDetails;
}

/** 列表中的角色展示信息 */
export interface RoleInfo {
  roleType: string;
  roleAndScope: string;
}

// ===== 用户 =====

export interface User {
  id: number;
  name: string | null;
  username: string | null;
  roles: RoleInfo[];
  isLocked: boolean;
  isEnabled: boolean;
  lastLoginTime: string | null;
}

// ===== 分页 =====

export interface PageResult<T> {
  content: T[];
  pageNo: number;
  pageSize: number;
  totalPages: number;
  totalElements: number;
}

// ===== 请求 / 响应 =====

export interface NewUserRequest {
  name?: string | null;
  username?: string | null;
  defaultPassword?: string | null;
  roles: UserRoleDetailsDTO[];
}

export interface UserUpdateRequest {
  name?: string | null;
  username?: string | null;
}

export interface PasswordResetResponse {
  id: number;
  name: string | null;
  username: string | null;
  defaultPassword: string;
}

// ===== 排序 =====

export type SortField = "id" | "name" | "username" | "lastLoginTime";

export type SortDirection = "asc" | "desc";

export interface SortState {
  field: SortField;
  direction: SortDirection;
}

// ===== 筛选 =====

export interface UserFilters {
  keyword: string;
  isLocked: string; // "all" | "true" | "false"
  isEnabled: string; // "all" | "true" | "false"
  roleType: string; // "all" | RoleType
}

// ===== 工具函数 =====

export function getRoleDetailDefaults(roleType: RoleType): RoleDetails {
  switch (roleType) {
    case "超级管理员":
    case "校级领导":
      return {};
    case "年级组长":
      return { gradeNo: null };
    case "班主任":
      return { gradeClass: { gradeNo: null, classNo: null } };
    case "多班级观察员":
      return { gradeClasses: [] };
    case "协作用户":
    case "家长用户":
    case "新生家长":
      return { stuNos: [] };
    case "学生用户":
      return { stuNo: null };
    default:
      return {};
  }
}
