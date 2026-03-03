"use client";

import {useEffect, useState} from "react";
import {useParams, useRouter} from "next/navigation";
import {toast} from "sonner";
import {Navbar} from "@/components/navbar";
import {get, put} from "@/lib/api-client";
import {Card, CardContent, CardHeader, CardTitle} from "@/components/ui/card";
import {Input} from "@/components/ui/input";
import {Label} from "@/components/ui/label";
import {Textarea} from "@/components/ui/textarea";
import {Button} from "@/components/ui/button";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue,} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {AlertCircle, ArrowLeft, ChevronDown, ChevronUp, Download, Loader2, Plus, Save, Trash2} from "lucide-react";

interface JsonSchemaProperty {
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

interface JsonSchema {
  type: string;
  properties: Record<string, JsonSchemaProperty>;
}

interface FormData {
  [key: string]: any;
}

interface PermissionData {
  grantedRoleAndScopes: string[];
  ownedPermissionGroups: string[];
  allowedJsonPaths: {
    readableJsonPaths: string[];
    writableJsonPaths: string[];
  };
}

interface StudentBasicInfo {
  stuNo: string;
  stuName: string;
  campus: string;
  gradeClassName: string;
}

export default function StudentArchivePage() {
  const params = useParams();
  const router = useRouter();
  const stuNo = params.stuNo as string;

  const [loading, setLoading] = useState(true);
  const [exportLoading, setExportLoading] = useState(false);
  const [saveLoading, setSaveLoading] = useState(false);
  const [schema, setSchema] = useState<JsonSchema | null>(null);
  const [formData, setFormData] = useState<FormData>({});
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({});
  const [permissions, setPermissions] = useState<PermissionData | null>(null);
  const [studentInfo, setStudentInfo] = useState<StudentBasicInfo | null>(null);
  
  // 错误对话框状态
  const [errorDialogOpen, setErrorDialogOpen] = useState(false);
  const [errorDialogTitle, setErrorDialogTitle] = useState("");
  const [errorDialogMessage, setErrorDialogMessage] = useState("");

  useEffect(() => {
    document.title = `学生档案 ${stuNo} - 应急协同平台`;
    const initData = async () => {
      setLoading(true);
      await Promise.all([
        fetchMetadata(),
        fetchPermissions(),
        fetchStudentInfo(),
      ]);
      // 在权限和元数据加载完成后，再加载档案数据
      await fetchArchiveData();
      setLoading(false);
    };
    initData();
  }, [stuNo]);

  const fetchMetadata = async () => {
    const response = await get<JsonSchema>("/archive/metadata");
    if (response.data) {
      setSchema(response.data);
    }
  };

  const fetchPermissions = async () => {
    const response = await get<PermissionData>(`/archive/students/${stuNo}/permission`);
    if (response.data) {
      setPermissions(response.data);
    }
  };

  const fetchStudentInfo = async () => {
    const response = await get<StudentBasicInfo>(`/students/${stuNo}`);
    if (response.data) {
      setStudentInfo(response.data);
    } else if (response.status === 404) {
      // 学生不存在，通过 URL 参数跳转到 404 页面
      const errorDescription = response.error || `学生不存在 (学号:${stuNo})`;
      router.push(`/not-found?description=${encodeURIComponent(errorDescription)}`);
    }
  };

  const fetchArchiveData = async () => {
    try {
      const response = await get<any>(`/archive/students/${stuNo}`);
      if (response.data) {
        // 后端已经根据权限过滤了数据，直接使用
        setFormData(response.data);
      }
    } catch (error: any) {
      // 如果返回 404，说明学生尚无档案，保持空表单
      if (error?.response?.status === 404) {
        console.log('学生尚无档案数据，显示空表单');
      } else {
        console.error('获取档案数据失败:', error);
      }
    }
  };

  // 显示错误对话框
  const showErrorDialog = (title: string, message: string) => {
    setErrorDialogTitle(title);
    setErrorDialogMessage(message);
    setErrorDialogOpen(true);
  };

  const handleGoBack = () => {
    router.back();
  };

  const handleExport = async () => {
    setExportLoading(true);
    try {
      const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "/api";
      const token = localStorage.getItem("authToken");
      
      const response = await fetch(`${API_BASE_URL}/archive/students/${stuNo}/export`, {
        method: "POST",
        headers: {
          ...(token && { Authorization: `Bearer ${token}` }),
        },
      });

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || "导出失败");
      }

      // 获取文件名，如果响应头中没有，使用默认值
      const contentDisposition = response.headers.get("Content-Disposition");
      let filename = `学生档案_${stuNo}.xlsx`;
      if (contentDisposition) {
        // 优先尝试 filename*= (RFC 5987 格式)
        const filenameStarMatch = contentDisposition.match(/filename\*=([^']+)'([^']*)'(.+)/);
        if (filenameStarMatch && filenameStarMatch[3]) {
          // filename*= 格式: charset'lang'encoded-value
          filename = decodeURIComponent(filenameStarMatch[3]);
        } else {
          // 回退到 filename=
          const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
          if (filenameMatch && filenameMatch[1]) {
            filename = filenameMatch[1].replace(/['"]/g, '');
            // 解码 URL 编码的文件名
            filename = decodeURIComponent(filename);
          }
        }
      }

      // 下载文件
      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (error: any) {
      console.error("导出失败:", error);
      showErrorDialog("导出失败", error.message || "未知错误");
    } finally {
      setExportLoading(false);
    }
  };

  const handleSave = async () => {
    // 先验证所有字段
    if (!schema) {
      showErrorDialog("无法保存", "表单结构未加载，请刷新页面后重试");
      return;
    }

    // 收集所有验证错误
    const errors: Record<string, string> = {};
    const validateAllFields = (properties: Record<string, JsonSchemaProperty>, parentPath: string = "") => {
      Object.entries(properties).forEach(([key, fieldSchema]) => {
        const fieldPath = parentPath ? `${parentPath}.${key}` : key;
        
        // 只验证可写字段
        if (!isFieldWritable(fieldSchema)) {
          return;
        }

        const value = getFieldValue(fieldPath);

        // 如果字段可空且值为空，跳过验证
        if (isFieldNullable(fieldSchema) && (value === null || value === undefined || value === "")) {
          return;
        }

        // Pattern 验证
        if (fieldSchema.pattern && value) {
          const regex = new RegExp(fieldSchema.pattern);
          if (!regex.test(value)) {
            errors[fieldPath] = `格式不正确`;
          }
        }

        // MaxLength 验证
        if (fieldSchema.maxLength && value && String(value).length > fieldSchema.maxLength) {
          errors[fieldPath] = `超过最大长度 ${fieldSchema.maxLength}`;
        }

        // 递归验证对象字段
        if (fieldSchema.type === "object" && fieldSchema.properties) {
          validateAllFields(fieldSchema.properties, fieldPath);
        }

        // 验证数组字段
        if (fieldSchema.type === "array" && fieldSchema.items?.properties) {
          const arrayData = getArrayData(fieldPath);
          arrayData.forEach((_, index) => {
            const itemPath = `${fieldPath}[${index}]`;
            validateAllFields(fieldSchema.items!.properties!, itemPath);
          });
        }
      });
    };

    validateAllFields(schema.properties);

    // 如果有验证错误，显示第一个错误并停止保存
    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      const firstError = Object.values(errors)[0];
      showErrorDialog("表单验证失败", `请修正表单错误后再保存：${firstError}`);
      return;
    }

    // 执行保存
    setSaveLoading(true);
    try {
      const response = await put(`/archive/students/${stuNo}`, formData);
      
      if (response.error) {
        throw new Error(response.error);
      }

      toast.success("保存成功！");
      // 重新加载档案数据
      await fetchArchiveData();
    } catch (error: any) {
      showErrorDialog("保存失败", error.message || "未知错误");
    } finally {
      setSaveLoading(false);
    }
  };

  // ==================== 自动填充规则系统 ====================
  
  // 自动填充规则接口
  interface AutoFillRule {
    // 规则名称
    name: string;
    // 源字段路径
    sourcePath: string;
    // 目标字段路径数组（可以同时更新多个字段）
    targetPaths: string[];
    // 触发时机：'change' | 'blur'
    trigger: 'change' | 'blur';
    // 规则执行函数
    execute: (sourceValue: any, formData: FormData) => Record<string, any>;
    // 是否启用该规则（可用于开关）
    enabled: boolean;
  }

  // 从身份证号解析性别
  const parseGenderFromRin = (rin: string): string | null => {
    if (!rin || rin.length !== 18) {
      return null;
    }
    try {
      const genderBit = parseInt(rin.substring(16, 17));
      if (isNaN(genderBit)) return null;
      return genderBit % 2 === 0 ? '女' : '男';
    } catch (e) {
      return null;
    }
  };

  // 从身份证号解析出生日期
  const parseBirthDateFromRin = (rin: string): string | null => {
    if (!rin || rin.length !== 18) {
      return null;
    }
    try {
      const year = parseInt(rin.substring(6, 10));
      const month = parseInt(rin.substring(10, 12));
      const day = parseInt(rin.substring(12, 14));
      
      if (isNaN(year) || isNaN(month) || isNaN(day)) return null;
      if (month < 1 || month > 12 || day < 1 || day > 31) return null;
      
      // 返回 YYYY-MM-DD 格式
      return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
    } catch (e) {
      return null;
    }
  };

  // 从出生年份计算年龄
  const calculateAgeFromBirthYear = (birthYear: number): number | null => {
    if (!birthYear || isNaN(birthYear)) {
      return null;
    }
    const currentYear = new Date().getFullYear();
    const age = currentYear - birthYear;
    return age >= 0 && age <= 150 ? age : null;
  };

  // 自动填充规则配置
  const autoFillRules: AutoFillRule[] = [
    {
      name: '身份证号 → 性别',
      sourcePath: 'personalPart.rin',
      targetPaths: ['personalPart.gender'],
      trigger: 'change',
      enabled: true,
      execute: (rin: string) => {
        const gender = parseGenderFromRin(rin);
        return gender ? { 'personalPart.gender': gender } : {};
      }
    },
    {
      name: '身份证号 → 出生日期',
      sourcePath: 'personalPart.rin',
      targetPaths: ['personalPart.birthDate'],
      trigger: 'change',
      enabled: true,
      execute: (rin: string) => {
        const birthDate = parseBirthDateFromRin(rin);
        return birthDate ? { 'personalPart.birthDate': birthDate } : {};
      }
    },
    {
      name: '出生年份 → 年龄',
      sourcePath: 'familyPart.otherRelatives[*].birthYear',
      targetPaths: ['familyPart.otherRelatives[*].age'],
      trigger: 'blur',
      enabled: true,
      execute: (birthYear: number) => {
        const age = calculateAgeFromBirthYear(birthYear);
        return age !== null ? { age } : {};
      }
    }
  ];

  // 检查字段路径是否匹配规则（支持数组通配符）
  const matchesRulePath = (fieldPath: string, rulePath: string): boolean => {
    // 替换数组通配符为正则表达式
    const pattern = rulePath.replace(/\[\*\]/g, '\\[\\d+\\]');
    const regex = new RegExp(`^${pattern}$`);
    return regex.test(fieldPath);
  };

  // 检查目标字段是否可读
  const isTargetFieldReadable = (targetPath: string, sourcePath: string, schema: JsonSchema | null): boolean => {
    if (!schema) return false;
    
    // 解析路径并查找字段定义
    const segments = parsePath(targetPath);
    let currentSchema: any = schema.properties;
    
    for (const segment of segments) {
      if (!currentSchema) return false;
      
      if (typeof segment === 'string') {
        currentSchema = currentSchema[segment];
      } else {
        // 数组索引，获取 items 定义
        currentSchema = currentSchema?.items;
      }
      
      if (!currentSchema) return false;
      
      // 如果是对象，进入 properties
      if (currentSchema.properties) {
        currentSchema = currentSchema.properties;
      }
    }
    
    // 检查是否可读
    return isFieldReadable(currentSchema);
  };

  // 应用自动填充规则
  const applyAutoFillRules = (fieldPath: string, value: any, trigger: 'change' | 'blur') => {
    const applicableRules = autoFillRules.filter(rule => 
      rule.enabled && 
      rule.trigger === trigger &&
      matchesRulePath(fieldPath, rule.sourcePath)
    );

    if (applicableRules.length === 0) return;

    // 对于每个匹配的规则
    applicableRules.forEach(rule => {
      try {
        // 执行规则获取更新值
        const updates = rule.execute(value, formData);
        
        // 处理数组字段的特殊情况
        if (rule.sourcePath.includes('[*]')) {
          // 提取数组索引
          const indexMatch = fieldPath.match(/\[(\d+)\]/);
          if (!indexMatch) return;
          const index = parseInt(indexMatch[1]);
          
          // 更新同一数组项中的目标字段
          Object.entries(updates).forEach(([key, val]) => {
            const targetPath = rule.targetPaths[0].replace('[*]', `[${index}]`);
            
            // 检查目标字段是否可读
            if (isTargetFieldReadable(targetPath, fieldPath, schema)) {
              handleFieldChange(targetPath, val);
            }
          });
        } else {
          // 普通字段：直接更新目标字段
          Object.entries(updates).forEach(([targetPath, val]) => {
            // 检查目标字段是否可读
            if (isTargetFieldReadable(targetPath, fieldPath, schema)) {
              handleFieldChange(targetPath, val);
            }
          });
        }
      } catch (error) {
        console.error(`自动填充规则 "${rule.name}" 执行失败:`, error);
      }
    });
  };

  // ==================== 自动填充规则系统结束 ====================

  const handleFieldChange = (path: string, value: any) => {
    setFormData((prev) => {
      const newData = { ...prev };
      
      // 将路径拆分为段，例如 "familyPart.otherRelatives[0].name" 
      // 拆分为 ["familyPart", "otherRelatives", "[0]", "name"]
      const segments = path.split(/\.|\[/).map((seg) => {
        if (seg.endsWith(']')) {
          return parseInt(seg.slice(0, -1)); // 数组索引
        }
        return seg; // 对象键名
      }).filter((seg) => seg !== '');
      
      let current: any = newData;

      // 遍历到倒数第二个段
      for (let i = 0; i < segments.length - 1; i++) {
        const segment = segments[i];
        const nextSegment = segments[i + 1];
        
        if (!current[segment]) {
          // 如果下一个段是数字，创建数组，否则创建对象
          current[segment] = typeof nextSegment === 'number' ? [] : {};
        }
        current = current[segment];
      }

      // 设置最后一个段的值
      current[segments[segments.length - 1]] = value;
      return newData;
    });

    // 应用自动填充规则（onChange 触发）
    applyAutoFillRules(path, value, 'change');
  };

  // 解析路径为段数组
  const parsePath = (path: string): (string | number)[] => {
    return path.split(/\.|\[/).map((seg) => {
      if (seg.endsWith(']')) {
        return parseInt(seg.slice(0, -1)); // 数组索引
      }
      return seg; // 对象键名
    }).filter((seg) => seg !== '');
  };

  // 获取数组数据
  const getArrayData = (path: string): any[] => {
    const segments = parsePath(path);
    let current: any = formData;
    
    for (const segment of segments) {
      if (!current || (typeof current !== "object" && !Array.isArray(current))) {
        return [];
      }
      current = current[segment];
    }
    
    return Array.isArray(current) ? current : [];
  };

  // 添加数组项
  const addArrayItem = (path: string) => {
    setFormData((prev) => {
      const newData = JSON.parse(JSON.stringify(prev)); // 深拷贝
      const segments = parsePath(path);
      let current: any = newData;

      // 遍历到倒数第二个段
      for (let i = 0; i < segments.length - 1; i++) {
        const segment = segments[i];
        const nextSegment = segments[i + 1];
        
        if (!current[segment]) {
          current[segment] = typeof nextSegment === 'number' ? [] : {};
        }
        current = current[segment];
      }

      const arrayKey = segments[segments.length - 1];
      if (!Array.isArray(current[arrayKey])) {
        current[arrayKey] = [];
      }
      current[arrayKey] = [...current[arrayKey], {}];
      return newData;
    });
  };

  // 删除数组项
  const removeArrayItem = (path: string, index: number) => {
    setFormData((prev) => {
      const newData = JSON.parse(JSON.stringify(prev)); // 深拷贝
      const segments = parsePath(path);
      let current: any = newData;

      // 遍历到倒数第二个段
      for (let i = 0; i < segments.length - 1; i++) {
        const segment = segments[i];
        if (!current[segment]) {
          return prev;
        }
        current = current[segment];
      }

      const arrayKey = segments[segments.length - 1];
      if (Array.isArray(current[arrayKey])) {
        current[arrayKey].splice(index, 1);
      }
      return newData;
    });
  };

  // 移动数组项
  const moveArrayItem = (path: string, fromIndex: number, toIndex: number) => {
    setFormData((prev) => {
      const newData = JSON.parse(JSON.stringify(prev)); // 深拷贝
      const segments = parsePath(path);
      let current: any = newData;

      // 遍历到倒数第二个段
      for (let i = 0; i < segments.length - 1; i++) {
        const segment = segments[i];
        if (!current[segment]) {
          return prev;
        }
        current = current[segment];
      }

      const arrayKey = segments[segments.length - 1];
      if (Array.isArray(current[arrayKey]) && current[arrayKey].length > fromIndex) {
        const arr = current[arrayKey];
        const [movedItem] = arr.splice(fromIndex, 1);
        arr.splice(toIndex, 0, movedItem);
      }
      return newData;
    });
  };

  // 检查字段是否可空（anyOf 包含 null 或 type 包含 null）
  const isFieldNullable = (fieldSchema: JsonSchemaProperty): boolean => {
    if (Array.isArray(fieldSchema.type) && fieldSchema.type.includes("null")) {
      return true;
    }
    if (fieldSchema.anyOf) {
      return fieldSchema.anyOf.some((opt) => opt.type === "null");
    }
    return false;
  };

  // 检查字段是否可读
  const isFieldReadable = (fieldSchema: JsonSchemaProperty): boolean => {
    if (!permissions) return true; // 权限未加载时默认可读
    const jsonPath = fieldSchema["x-jsonpath"];
    if (!jsonPath) return true; // 没有 jsonpath 的字段默认可读
    return permissions.allowedJsonPaths.readableJsonPaths.includes(jsonPath);
  };

  // 检查字段是否可写
  const isFieldWritable = (fieldSchema: JsonSchemaProperty): boolean => {
    if (!permissions) return false; // 权限未加载时默认不可写
    const jsonPath = fieldSchema["x-jsonpath"];
    if (!jsonPath) return false; // 没有 jsonpath 的字段默认不可写
    return permissions.allowedJsonPaths.writableJsonPaths.includes(jsonPath);
  };

  // 检查对象是否有可读的子字段
  const hasReadableChildren = (fieldSchema: JsonSchemaProperty): boolean => {
    if (!fieldSchema.properties) return false;
    return Object.values(fieldSchema.properties).some((subSchema) => 
      isFieldReadable(subSchema)
    );
  };

  // 验证字段
  const validateField = (
    path: string,
    value: string,
    fieldSchema: JsonSchemaProperty
  ): string | null => {
    // 如果字段可空且值为空，则不验证
    const nullable = isFieldNullable(fieldSchema);
    if (nullable && (!value || value.trim() === "")) {
      return null;
    }

    // 验证 pattern
    if (fieldSchema.pattern && value) {
      const regex = new RegExp(fieldSchema.pattern);
      if (!regex.test(value)) {
        return `格式不正确`;
      }
    }

    // 验证 maxLength
    if (fieldSchema.maxLength && value && value.length > fieldSchema.maxLength) {
      return `超过最大长度 ${fieldSchema.maxLength}`;
    }

    return null;
  };

  // 处理字段失焦
  const handleFieldBlur = (
    path: string,
    value: string,
    fieldSchema: JsonSchemaProperty
  ) => {
    const error = validateField(path, value, fieldSchema);
    setValidationErrors((prev) => {
      const newErrors = { ...prev };
      if (error) {
        newErrors[path] = error;
      } else {
        delete newErrors[path];
      }
      return newErrors;
    });

    // 应用自动填充规则（onBlur 触发）
    applyAutoFillRules(path, value, 'blur');
  };

  // 获取字段值
  const getFieldValue = (path: string): any => {
    const segments = parsePath(path);
    let current: any = formData;
    
    for (const segment of segments) {
      if (current === null || current === undefined) {
        return undefined;
      }
      if (typeof current !== "object" && !Array.isArray(current)) {
        return undefined;
      }
      current = current[segment];
    }
    
    // 返回实际值，包括 null、undefined、空字符串等
    return current;
  };

  // 渲染简单字段（用于数组项等）
  const renderSimpleField = (
    fieldName: string,
    fieldSchema: JsonSchemaProperty,
    fieldPath: string,
    context: string = ""
  ) => {
    // 检查权限：如果不可读，直接不渲染
    if (!isFieldReadable(fieldSchema)) {
      return null;
    }

    const fieldTitle = fieldSchema.title || fieldName;
    const isReadOnly = fieldSchema.readOnly || !isFieldWritable(fieldSchema);
    const fieldValue = getFieldValue(fieldPath);

    let fieldType = Array.isArray(fieldSchema.type)
      ? fieldSchema.type.find((t) => t !== "null") || "string"
      : fieldSchema.type;

    let enumValues: string[] | undefined = undefined;

    if (fieldSchema.anyOf) {
      const enumOption = fieldSchema.anyOf.find((opt) => opt.enum);
      if (enumOption && enumOption.enum) {
        enumValues = enumOption.enum;
      }
    } else if (fieldSchema.enum) {
      enumValues = fieldSchema.enum;
    }

    // 渲染枚举字段
    if (enumValues) {
      const nullable = isFieldNullable(fieldSchema);
      // 将 null/undefined/空字符串 转换为 undefined，让 Select 显示为未选中状态
      const selectValue = fieldValue === null || fieldValue === undefined || fieldValue === "" 
        ? undefined 
        : String(fieldValue);
      
      return (
        <div key={fieldPath} className="space-y-2">
          <Label htmlFor={fieldPath}>
            {fieldTitle}
          </Label>
          <Select
            disabled={isReadOnly}
            value={selectValue}
            onValueChange={(value) => handleFieldChange(fieldPath, value === "__empty__" ? "" : value)}
          >
            <SelectTrigger id={fieldPath} className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {nullable && (
                <SelectItem value="__empty__">
                  <span className="text-muted-foreground">(不填写)</span>
                </SelectItem>
              )}
              {enumValues.map((enumValue) => (
                <SelectItem key={enumValue} value={enumValue}>
                  {enumValue}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      );
    }

    // 渲染普通输入字段
    const shouldUseTextarea = fieldSchema.maxLength && fieldSchema.maxLength > 200;
    const nullable = isFieldNullable(fieldSchema);
    // 将 null/undefined 转换为空字符串，确保 Input 受控
    const inputValue = fieldValue ?? "";
    
    return (
      <div key={fieldPath} className="space-y-2">
        <Label htmlFor={fieldPath}>
          {fieldTitle}
        </Label>
        {shouldUseTextarea ? (
          <Textarea
            id={fieldPath}
            value={inputValue}
            disabled={isReadOnly}
            maxLength={fieldSchema.maxLength}
            onChange={(e) => handleFieldChange(fieldPath, e.target.value)}
            onBlur={(e) => handleFieldBlur(fieldPath, e.target.value, fieldSchema)}
            className={isReadOnly ? "bg-muted" : ""}
          />
        ) : (
          <Input
            id={fieldPath}
            type={fieldType === "integer" ? "number" : "text"}
            value={inputValue}
            disabled={isReadOnly}
            maxLength={fieldSchema.maxLength}
            onChange={(e) => handleFieldChange(fieldPath, e.target.value)}
            onBlur={(e) => handleFieldBlur(fieldPath, e.target.value, fieldSchema)}
            className={isReadOnly ? "bg-muted" : ""}
          />
        )}
        {validationErrors[fieldPath] && (
          <p className="text-xs text-destructive">
            {validationErrors[fieldPath]}
          </p>
        )}
      </div>
    );
  };

  const renderField = (
    fieldName: string,
    fieldSchema: JsonSchemaProperty,
    parentPath: string = ""
  ) => {
    // 检查权限：如果不可读，直接不渲染
    if (!isFieldReadable(fieldSchema)) {
      return null;
    }

    const fieldPath = parentPath ? `${parentPath}.${fieldName}` : fieldName;
    const fieldTitle = fieldSchema.title || fieldName;
    const isReadOnly = fieldSchema.readOnly || !isFieldWritable(fieldSchema);

    // 处理 type 可能是数组的情况
    let fieldType = Array.isArray(fieldSchema.type)
      ? fieldSchema.type.find((t) => t !== "null") || "string"
      : fieldSchema.type;

    // 处理 anyOf 的情况（用于enum）
    let enumValues: string[] | undefined = undefined;
    let constValue: string | undefined = undefined;

    if (fieldSchema.anyOf) {
      const enumOption = fieldSchema.anyOf.find((opt) => opt.enum);
      if (enumOption && enumOption.enum) {
        enumValues = enumOption.enum;
      }
      const constOption = fieldSchema.anyOf.find((opt) => opt.const);
      if (constOption && constOption.const) {
        constValue = constOption.const;
      }
    } else if (fieldSchema.enum) {
      enumValues = fieldSchema.enum;
    } else if (fieldSchema.const) {
      constValue = fieldSchema.const;
    }

    // 如果是对象类型，递归渲染子字段
    if (fieldType === "object" && fieldSchema.properties) {
      // 如果对象没有任何可读的子字段，则不渲染
      if (!hasReadableChildren(fieldSchema)) {
        return null;
      }

      return (
        <Card key={fieldPath} className="mb-4">
          <CardHeader>
            <CardTitle className="text-lg">{fieldTitle}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {Object.entries(fieldSchema.properties).map(([subFieldName, subFieldSchema]) =>
              renderField(subFieldName, subFieldSchema, fieldPath)
            )}
          </CardContent>
        </Card>
      );
    }

    // 如果是数组类型
    if (fieldType === "array" && fieldSchema.items) {
      const arrayData = getArrayData(fieldPath);
      const canWriteArray = isFieldWritable(fieldSchema);
      
      return (
        <div key={fieldPath} className="mb-4 col-span-full">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0">
              <CardTitle className="text-base">{fieldTitle}</CardTitle>
              {canWriteArray && (
                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  onClick={() => addArrayItem(fieldPath)}
                >
                  <Plus className="h-4 w-4 mr-1" />
                  添加
                </Button>
              )}
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                {arrayData.length === 0 ? (
                  <div className="text-center py-8 text-muted-foreground">
                    {canWriteArray ? '暂无数据，点击"添加"按钮创建条目' : '暂无数据'}
                  </div>
                ) : (
                  arrayData.map((_, index) => (
                    <div
                      key={`${fieldPath}[${index}]`}
                      className="border rounded-lg p-4 space-y-4"
                    >
                      {/* 数组项标题和操作按钮 */}
                      <div className="flex items-center justify-between mb-3">
                        <h4 className="font-medium text-sm">
                          {fieldTitle} #{index + 1}
                        </h4>
                        {canWriteArray && (
                          <div className="flex gap-1">
                            <Button
                              type="button"
                              size="sm"
                              variant="ghost"
                              disabled={index === 0}
                              onClick={() => moveArrayItem(fieldPath, index, index - 1)}
                            >
                              <ChevronUp className="h-4 w-4" />
                            </Button>
                            <Button
                              type="button"
                              size="sm"
                              variant="ghost"
                              disabled={index === arrayData.length - 1}
                              onClick={() => moveArrayItem(fieldPath, index, index + 1)}
                            >
                              <ChevronDown className="h-4 w-4" />
                            </Button>
                            <Button
                              type="button"
                              size="sm"
                              variant="ghost"
                              className="text-destructive hover:text-destructive"
                              onClick={() => removeArrayItem(fieldPath, index)}
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </div>
                        )}
                      </div>

                      {/* 渲染数组项的字段 */}
                      {fieldSchema.items?.properties && (
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          {Object.entries(fieldSchema.items.properties).map(
                            ([itemFieldName, itemFieldSchema]) => {
                              const itemPath = `${fieldPath}[${index}].${itemFieldName}`;
                              return renderSimpleField(
                                itemFieldName,
                                itemFieldSchema,
                                itemPath,
                                `${fieldTitle}条目`
                              );
                            }
                          )}
                        </div>
                      )}
                    </div>
                  ))
                )}
              </div>
            </CardContent>
          </Card>
        </div>
      );
    }

    // 渲染常量字段（只读，只有一个值）
    if (constValue !== undefined) {
      return (
        <div key={fieldPath} className="space-y-2">
          <Label htmlFor={fieldPath}>
            {fieldTitle}
            {fieldSchema.description && (
              <span className="text-xs text-muted-foreground ml-2">
                {fieldSchema.description}
              </span>
            )}
          </Label>
          <Input
            id={fieldPath}
            value={constValue}
            disabled
            className="bg-muted"
          />
        </div>
      );
    }

    // 渲染枚举字段（下拉选择）
    if (enumValues) {
      const nullable = isFieldNullable(fieldSchema);
      const fieldValue = getFieldValue(fieldPath);
      // 将 null/undefined/空字符串 转换为 undefined，让 Select 显示为未选中状态
      const selectValue = fieldValue === null || fieldValue === undefined || fieldValue === "" 
        ? undefined 
        : String(fieldValue);
      
      return (
        <div key={fieldPath} className="space-y-2">
          <Label htmlFor={fieldPath}>
            {fieldTitle}
            {fieldSchema.description && (
              <span className="text-xs text-muted-foreground ml-2">
                {fieldSchema.description}
              </span>
            )}
          </Label>
          <Select
            disabled={isReadOnly}
            value={selectValue}
            onValueChange={(value) => handleFieldChange(fieldPath, value === "__empty__" ? "" : value)}
          >
            <SelectTrigger id={fieldPath} className="w-full">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {nullable && (
                <SelectItem value="__empty__">
                  <span className="text-muted-foreground">(不填写)</span>
                </SelectItem>
              )}
              {enumValues.map((enumValue) => (
                <SelectItem key={enumValue} value={enumValue}>
                  {enumValue}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>
      );
    }

    // 渲染普通输入字段
    const shouldUseTextarea = fieldSchema.maxLength && fieldSchema.maxLength > 200;
    const fieldValue = getFieldValue(fieldPath);
    // 将 null/undefined 转换为空字符串，确保 Input 受控
    const inputValue = fieldValue ?? "";
    
    return (
      <div key={fieldPath} className="space-y-2">
        <Label htmlFor={fieldPath}>
          {fieldTitle}
          {fieldSchema.description && (
            <span className="text-xs text-muted-foreground ml-2">
              {fieldSchema.description}
            </span>
          )}
        </Label>
        {shouldUseTextarea ? (
          <Textarea
            id={fieldPath}
            value={inputValue}
            disabled={isReadOnly}
            maxLength={fieldSchema.maxLength}
            onChange={(e) => handleFieldChange(fieldPath, e.target.value)}
            onBlur={(e) => handleFieldBlur(fieldPath, e.target.value, fieldSchema)}
            className={isReadOnly ? "bg-muted" : ""}
          />
        ) : (
          <Input
            id={fieldPath}
            type={fieldType === "integer" ? "number" : "text"}
            value={inputValue}
            disabled={isReadOnly}
            maxLength={fieldSchema.maxLength}
            onChange={(e) => handleFieldChange(fieldPath, e.target.value)}
            onBlur={(e) => handleFieldBlur(fieldPath, e.target.value, fieldSchema)}
            className={isReadOnly ? "bg-muted" : ""}
          />
        )}
        {validationErrors[fieldPath] && (
          <p className="text-xs text-destructive">
            {validationErrors[fieldPath]}
          </p>
        )}
      </div>
    );
  };

  if (loading) {
    return (
      <>
        <Navbar />
        <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center">
          <div className="flex flex-col items-center gap-3">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
            <p className="text-muted-foreground">加载表单信息中...</p>
          </div>
        </div>
      </>
    );
  }

  return (
    <>
      <Navbar />
      <div className="min-h-[calc(100vh-4rem)] bg-linear-to-br from-slate-50 to-slate-100 dark:from-slate-900 dark:to-slate-800">
        <div className="container mx-auto p-6 space-y-6">
          {/* 页面标题 */}
          <div>
            <h1 className="text-3xl font-bold text-foreground">学生档案</h1>
            {studentInfo && (
              <p className="text-lg text-muted-foreground mt-2">
                {studentInfo.stuNo} {studentInfo.stuName}
              </p>
            )}
          </div>

          {/* 操作按钮组 */}
          <div className="flex gap-3">
            <Button
              variant="outline"
              onClick={handleGoBack}
            >
              <ArrowLeft className="h-4 w-4 mr-2" />
              返回上一页
            </Button>
            <Button
              variant="outline"
              onClick={handleExport}
              disabled={exportLoading}
            >
              {exportLoading ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  导出中...
                </>
              ) : (
                <>
                  <Download className="h-4 w-4 mr-2" />
                  导出
                </>
              )}
            </Button>
            <Button
              onClick={handleSave}
              disabled={saveLoading}
            >
              {saveLoading ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  保存中...
                </>
              ) : (
                <>
                  <Save className="h-4 w-4 mr-2" />
                  保存修改
                </>
              )}
            </Button>
          </div>

          {/* 基本信息卡片 */}
          {studentInfo && (
            <Card>
              <CardHeader>
                <CardTitle>基本信息</CardTitle>
                <p className="text-xs text-muted-foreground mt-2">
                  基本信息不支持修改，若信息有误请联系管理员。
                </p>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
                  <div className="space-y-1">
                    <Label className="text-sm text-muted-foreground">学号</Label>
                    <p className="text-base font-medium">{studentInfo.stuNo}</p>
                  </div>
                  <div className="space-y-1">
                    <Label className="text-sm text-muted-foreground">班级</Label>
                    <p className="text-base font-medium">{studentInfo.gradeClassName}</p>
                  </div>
                  <div className="space-y-1">
                    <Label className="text-sm text-muted-foreground">姓名</Label>
                    <p className="text-base font-medium">{studentInfo.stuName}</p>
                  </div>
                  <div className="space-y-1">
                    <Label className="text-sm text-muted-foreground">校区</Label>
                    <p className="text-base font-medium">{studentInfo.campus}</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          {/* 动态表单 */}
          {schema && schema.properties && (
            <div className="space-y-6">
              {Object.entries(schema.properties).map(([sectionName, sectionSchema]) => {
                // 如果是对象类型的分段，渲染为独立的Card
                if (
                  sectionSchema.properties &&
                  (Array.isArray(sectionSchema.type)
                    ? sectionSchema.type.includes("object")
                    : sectionSchema.type === "object")
                ) {
                  // 如果该分段没有任何可读的子字段，则不渲染
                  if (!hasReadableChildren(sectionSchema)) {
                    return null;
                  }

                  return (
                    <Card key={sectionName}>
                      <CardHeader>
                        <CardTitle>{sectionSchema.title || sectionName}</CardTitle>
                      </CardHeader>
                      <CardContent>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                          {Object.entries(sectionSchema.properties).map(
                            ([fieldName, fieldSchema]) =>
                              renderField(fieldName, fieldSchema, sectionName)
                          )}
                        </div>
                      </CardContent>
                    </Card>
                  );
                }
                return null;
              })}
            </div>
          )}

          {!schema && (
            <Card>
              <CardContent className="py-12 text-center text-muted-foreground">
                无法加载表单元信息
              </CardContent>
            </Card>
          )}
        </div>
      </div>

      {/* 错误对话框 */}
      <Dialog open={errorDialogOpen} onOpenChange={setErrorDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <div className="flex items-center gap-3">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-destructive/10">
                <AlertCircle className="h-5 w-5 text-destructive" />
              </div>
              <DialogTitle>{errorDialogTitle}</DialogTitle>
            </div>
          </DialogHeader>
          <DialogDescription className="pt-2">
            {errorDialogMessage}
          </DialogDescription>
          <DialogFooter>
            <Button onClick={() => setErrorDialogOpen(false)}>确定</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}

