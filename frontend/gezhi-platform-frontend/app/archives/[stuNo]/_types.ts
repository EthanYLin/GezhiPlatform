// ==================== 学生档案页面类型定义 ====================

/** JSON Schema 属性定义 */
export interface JsonSchemaProperty {
  type: string | string[];
  title?: string;
  description?: string;
  pattern?: string;
  maxLength?: number;
  enum?: string[];
  anyOf?: Array<{ type: string; enum?: string[]; const?: string }>;
  properties?: Record<string, JsonSchemaProperty>;
  items?: JsonSchemaProperty;
  readOnly?: boolean;
  "x-jsonpath"?: string;
  const?: string;
}

/** JSON Schema 根定义 */
export interface JsonSchema {
  type: string;
  properties: Record<string, JsonSchemaProperty>;
}

/** 表单数据 */
export interface ArchiveFormData {
  [key: string]: any;
}

/** SpEL 校验表达式 */
export interface ValidationSpEL {
  spelExpr?: string;
  jsExpr?: string;
  message: string;
}

/** 档案权限详情（与后端 ArchivePermissionDetails 对应） */
export interface PermissionData {
  grantedRoleAndScopes: string[];
  ownedPermissionGroups: string[];
  displayCaption: string;
  allowedReadableJsonPaths: string[];
  allowedWritableJsonPaths: string[];
  allowedAddArrayJsonPaths: string[];
  allowedEditArrayJsonPaths: string[];
  allowedDeleteArrayJsonPaths: string[];
  validationSpELs: ValidationSpEL[];
}

/** 学生基本信息 */
export interface StudentBasicInfo {
  stuNo: string;
  stuName: string;
  campus: string;
  gradeClassName: string;
}

/** 传递给表单字段组件的操作集合 */
export interface FormActions {
  getFieldValue: (path: string) => any;
  getArrayData: (path: string) => any[];
  handleFieldChange: (path: string, value: any) => void;
  handleFieldBlur: (
    path: string,
    value: string,
    fieldSchema: JsonSchemaProperty
  ) => void;
  addArrayItem: (path: string) => void;
  removeArrayItem: (path: string, index: number) => void;
  moveArrayItem: (path: string, fromIndex: number, toIndex: number) => void;
  permissions: PermissionData | null;
  validationErrors: Record<string, string>;
}
