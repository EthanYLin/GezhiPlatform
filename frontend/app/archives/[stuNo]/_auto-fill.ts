// ==================== 自动填充规则系统 ====================

import type {ArchiveFormData, JsonSchema, PermissionData} from "./_types";
import {isFieldReadable, parsePath} from "./_utils";

/** 自动填充规则定义 */
interface AutoFillRule {
  name: string;
  sourcePath: string;
  targetPaths: string[];
  trigger: "change" | "blur";
  execute: (sourceValue: any, formData: ArchiveFormData) => Record<string, any>;
  enabled: boolean;
}

// ---- 解析工具函数 ----

function parseGenderFromRin(rin: string): string | null {
  if (!rin || rin.length !== 18) return null;
  const genderBit = parseInt(rin.substring(16, 17));
  if (isNaN(genderBit)) return null;
  return genderBit % 2 === 0 ? "女" : "男";
}

function parseBirthDateFromRin(rin: string): string | null {
  if (!rin || rin.length !== 18) return null;
  const year = parseInt(rin.substring(6, 10));
  const month = parseInt(rin.substring(10, 12));
  const day = parseInt(rin.substring(12, 14));
  if (isNaN(year) || isNaN(month) || isNaN(day)) return null;
  if (month < 1 || month > 12 || day < 1 || day > 31) return null;
  return `${year}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
}

function calculateAgeFromBirthYear(birthYear: number): number | null {
  if (!birthYear || isNaN(birthYear)) return null;
  const currentYear = new Date().getFullYear();
  const age = currentYear - birthYear;
  return age >= 0 && age <= 150 ? age : null;
}

// ---- 规则配置 ----

const autoFillRules: AutoFillRule[] = [
  {
    name: "身份证号 → 性别",
    sourcePath: "personalPart.rin",
    targetPaths: ["personalPart.gender"],
    trigger: "change",
    enabled: true,
    execute: (rin: string) => {
      const gender = parseGenderFromRin(rin);
      return gender ? { "personalPart.gender": gender } : {};
    },
  },
  {
    name: "身份证号 → 出生日期",
    sourcePath: "personalPart.rin",
    targetPaths: ["personalPart.birthDate"],
    trigger: "change",
    enabled: true,
    execute: (rin: string) => {
      const birthDate = parseBirthDateFromRin(rin);
      return birthDate ? { "personalPart.birthDate": birthDate } : {};
    },
  },
  {
    name: "出生年份 → 年龄",
    sourcePath: "familyPart.otherRelatives[*].birthYear",
    targetPaths: ["familyPart.otherRelatives[*].age"],
    trigger: "blur",
    enabled: true,
    execute: (birthYear: number) => {
      const age = calculateAgeFromBirthYear(birthYear);
      return age !== null ? { age } : {};
    },
  },
];

// ---- 内部辅助 ----

/** 检查字段路径是否匹配规则路径（支持 [*] 通配符） */
function matchesRulePath(fieldPath: string, rulePath: string): boolean {
  const pattern = rulePath.replace(/\[\*\]/g, "\\[\\d+\\]");
  return new RegExp(`^${pattern}$`).test(fieldPath);
}

/** 检查目标字段是否可读 */
function isTargetFieldReadable(
  targetPath: string,
  schema: JsonSchema | null,
  permissions: PermissionData | null
): boolean {
  if (!schema) return false;
  const segments = parsePath(targetPath);
  let currentSchema: any = schema.properties;

  for (const segment of segments) {
    if (!currentSchema) return false;
    if (typeof segment === "string") {
      currentSchema = currentSchema[segment];
    } else {
      currentSchema = currentSchema?.items;
    }
    if (!currentSchema) return false;
    if (currentSchema.properties) {
      currentSchema = currentSchema.properties;
    }
  }

  return isFieldReadable(currentSchema, permissions);
}

// ---- 对外接口 ----

/**
 * 应用自动填充规则
 *
 * @param fieldPath   当前变更的字段路径
 * @param value       变更后的值
 * @param trigger     触发时机
 * @param formData    当前表单数据
 * @param schema      JSON Schema
 * @param permissions 权限数据
 * @param onFieldChange 字段变更回调（用于更新目标字段）
 */
export function applyAutoFillRules(
  fieldPath: string,
  value: any,
  trigger: "change" | "blur",
  formData: ArchiveFormData,
  schema: JsonSchema | null,
  permissions: PermissionData | null,
  onFieldChange: (path: string, value: any) => void
): void {
  const applicable = autoFillRules.filter(
    (rule) =>
      rule.enabled &&
      rule.trigger === trigger &&
      matchesRulePath(fieldPath, rule.sourcePath)
  );

  if (applicable.length === 0) return;

  for (const rule of applicable) {
    try {
      const updates = rule.execute(value, formData);

      if (rule.sourcePath.includes("[*]")) {
        // 数组字段：提取当前索引，替换目标路径中的通配符
        const indexMatch = fieldPath.match(/\[(\d+)\]/);
        if (!indexMatch) continue;
        const index = parseInt(indexMatch[1]);

        for (const [, val] of Object.entries(updates)) {
          const targetPath = rule.targetPaths[0].replace(
            "[*]",
            `[${index}]`
          );
          if (isTargetFieldReadable(targetPath, schema, permissions)) {
            onFieldChange(targetPath, val);
          }
        }
      } else {
        // 普通字段：直接更新目标
        for (const [targetPath, val] of Object.entries(updates)) {
          if (isTargetFieldReadable(targetPath, schema, permissions)) {
            onFieldChange(targetPath, val);
          }
        }
      }
    } catch (error) {
      console.error(`自动填充规则 "${rule.name}" 执行失败:`, error);
    }
  }
}
