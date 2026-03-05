// ==================== 工具函数：路径解析、权限检查、字段验证 ====================

import type {JsonSchemaProperty, PermissionData} from "./_types";

/**
 * 将点号/方括号路径字符串解析为段数组
 * 例如 "familyPart.otherRelatives[0].name" → ["familyPart", "otherRelatives", 0, "name"]
 */
export function parsePath(path: string): (string | number)[] {
  return path
    .split(/\.|\[/)
    .map((seg) => {
      if (seg.endsWith("]")) return parseInt(seg.slice(0, -1));
      return seg;
    })
    .filter((seg) => seg !== "");
}

/** 根据路径从嵌套数据中获取值 */
export function getFieldValueFromData(
  data: Record<string, any>,
  path: string
): any {
  const segments = parsePath(path);
  let current: any = data;
  for (const segment of segments) {
    if (current === null || current === undefined) return undefined;
    if (typeof current !== "object" && !Array.isArray(current))
      return undefined;
    current = current[segment];
  }
  return current;
}

/** 根据路径从嵌套数据中获取数组数据 */
export function getArrayDataFromData(
  data: Record<string, any>,
  path: string
): any[] {
  const segments = parsePath(path);
  let current: any = data;
  for (const segment of segments) {
    if (!current || (typeof current !== "object" && !Array.isArray(current)))
      return [];
    current = current[segment];
  }
  return Array.isArray(current) ? current : [];
}

/** 解析字段的主类型（排除 null） */
export function resolveFieldType(fieldSchema: JsonSchemaProperty): string {
  return Array.isArray(fieldSchema.type)
    ? fieldSchema.type.find((t) => t !== "null") || "string"
    : fieldSchema.type;
}

/** 判断字段是否可空 */
export function isFieldNullable(fieldSchema: JsonSchemaProperty): boolean {
  if (Array.isArray(fieldSchema.type) && fieldSchema.type.includes("null"))
    return true;
  if (fieldSchema.anyOf)
    return fieldSchema.anyOf.some((opt) => opt.type === "null");
  return false;
}

/** 判断字段是否可读（基于权限） */
export function isFieldReadable(
  fieldSchema: JsonSchemaProperty,
  permissions: PermissionData | null
): boolean {
  if (!permissions) return true;
  const jsonPath = fieldSchema["x-jsonpath"];
  if (!jsonPath) return true;
  return permissions.allowedReadableJsonPaths.includes(jsonPath);
}

/**
 * 判断字段是否可写（基于权限）
 *
 * 规则：
 * - 数组字段本身：检查 allowedEditArrayJsonPaths
 * - 数组内的子字段（jsonpath 包含 [*]）：检查父数组是否在 allowedEditArrayJsonPaths
 * - 普通字段：检查 allowedWritableJsonPaths
 */
export function isFieldWritable(
  fieldSchema: JsonSchemaProperty,
  permissions: PermissionData | null
): boolean {
  if (!permissions) return false;
  const jsonPath = fieldSchema["x-jsonpath"];
  if (!jsonPath) return false;

  const fieldType = resolveFieldType(fieldSchema);

  // 数组字段本身
  if (fieldType === "array") {
    return permissions.allowedEditArrayJsonPaths.includes(jsonPath);
  }

  // 数组内的子字段
  if (jsonPath.includes("[*]")) {
    const arrayPath = jsonPath.replace(/\[\*\]\..*$/, "");
    return permissions.allowedEditArrayJsonPaths.includes(arrayPath);
  }

  // 普通字段
  return permissions.allowedWritableJsonPaths.includes(jsonPath);
}

/** 判断数组是否允许添加条目 */
export function isArrayAddable(
  fieldSchema: JsonSchemaProperty,
  permissions: PermissionData | null
): boolean {
  if (!permissions) return false;
  const jsonPath = fieldSchema["x-jsonpath"];
  if (!jsonPath) return false;
  return permissions.allowedAddArrayJsonPaths.includes(jsonPath);
}

/** 判断数组是否允许删除条目 */
export function isArrayDeletable(
  fieldSchema: JsonSchemaProperty,
  permissions: PermissionData | null
): boolean {
  if (!permissions) return false;
  const jsonPath = fieldSchema["x-jsonpath"];
  if (!jsonPath) return false;
  return permissions.allowedDeleteArrayJsonPaths.includes(jsonPath);
}

/** 判断对象是否有可读的子字段 */
export function hasReadableChildren(
  fieldSchema: JsonSchemaProperty,
  permissions: PermissionData | null
): boolean {
  if (!fieldSchema.properties) return false;
  return Object.values(fieldSchema.properties).some((sub) =>
    isFieldReadable(sub, permissions)
  );
}

/** 验证字段值是否符合 schema 约束 */
export function validateField(
  value: string,
  fieldSchema: JsonSchemaProperty
): string | null {
  if (isFieldNullable(fieldSchema) && (!value || value.trim() === ""))
    return null;

  if (fieldSchema.pattern && value) {
    if (!new RegExp(fieldSchema.pattern).test(value)) return "格式不正确";
  }

  if (fieldSchema.maxLength && value && value.length > fieldSchema.maxLength) {
    return `超过最大长度 ${fieldSchema.maxLength}`;
  }

  return null;
}
