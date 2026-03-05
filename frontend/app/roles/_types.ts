export type RoleType =
  | "超级管理员"
  | "校级领导"
  | "年级组长"
  | "班主任"
  | "多班级观察员"
  | "协作用户"
  | "家长用户"
  | "新生家长"
  | "学生用户";

export const ALL_ROLE_TYPES: RoleType[] = [
  "超级管理员",
  "校级领导",
  "年级组长",
  "班主任",
  "多班级观察员",
  "协作用户",
  "家长用户",
  "新生家长",
  "学生用户",
];

export interface ValidationRule {
  spelExpr: string;
  jsExpr: string;
  message: string;
}

export interface PermissionGroup {
  id?: number;
  name: string;
  description?: string | null;
  displayCaption?: string | null;
  enabled: boolean;
  roleTypes: RoleType[];
  allowedReadableJsonPaths: string[];
  allowedWritableJsonPaths: string[];
  allowedAddArrayJsonPaths: string[];
  allowedEditArrayJsonPaths: string[];
  allowedDeleteArrayJsonPaths: string[];
  validations: ValidationRule[];
}

export interface PageResult<T> {
  content: T[];
  pageNo: number;
  pageSize: number;
  totalPages: number;
  totalElements: number;
}

export interface FieldMetadata {
  allowEdit: boolean;
  titles: string[];
  paths: string[];
  ancestorPaths: string[];
  isArray: boolean;
  insideArray: boolean;
  arrayEntryJsonPath: string | null;
}

export interface FieldTreeNode {
  jsonPath: string;
  title: string;
  depth: number;
  allowEdit: boolean;
  isArray: boolean;
  insideArray: boolean;
  ancestorPaths: string[];
  children: FieldTreeNode[];
  isLastChild: boolean;
}
