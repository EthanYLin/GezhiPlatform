"use client";

// ==================== 学生档案表单 Hook ====================
// 管理数据获取、表单状态、字段操作、保存/导出等

import {useCallback, useEffect, useRef, useState} from "react";
import {useParams, useRouter} from "next/navigation";
import {toast} from "sonner";
import {get, put} from "@/lib/api-client";
import type {
    ArchiveFormData,
    FormActions,
    JsonSchema,
    JsonSchemaProperty,
    PermissionData,
    StudentBasicInfo,
} from "./_types";
import {
    getArrayDataFromData,
    getFieldValueFromData,
    isFieldNullable,
    isFieldWritable,
    parsePath,
    resolveFieldType,
    validateField,
} from "./_utils";
import {applyAutoFillRules} from "./_auto-fill";

export function useArchiveForm() {
  const params = useParams();
  const router = useRouter();
  const stuNo = params.stuNo as string;

  // ---- 状态 ----
  const [loading, setLoading] = useState(true);
  const [exportLoading, setExportLoading] = useState(false);
  const [saveLoading, setSaveLoading] = useState(false);
  const [schema, setSchema] = useState<JsonSchema | null>(null);
  const [formData, setFormData] = useState<ArchiveFormData>({});
  const [validationErrors, setValidationErrors] = useState<
    Record<string, string>
  >({});
  const [permissions, setPermissions] = useState<PermissionData | null>(null);
  const [studentInfo, setStudentInfo] = useState<StudentBasicInfo | null>(null);

  // 错误对话框
  const [errorDialogOpen, setErrorDialogOpen] = useState(false);
  const [errorDialogTitle, setErrorDialogTitle] = useState("");
  const [errorDialogMessage, setErrorDialogMessage] = useState("");

  // Ref 用于在回调中获取最新值（避免闭包过期）
  const formDataRef = useRef(formData);
  formDataRef.current = formData;
  const schemaRef = useRef(schema);
  schemaRef.current = schema;
  const permissionsRef = useRef(permissions);
  permissionsRef.current = permissions;

  // ---- 数据获取 ----

  const fetchMetadata = useCallback(async () => {
    const response = await get<JsonSchema>("/archive/metadata");
    if (response.data) setSchema(response.data);
  }, []);

  const fetchPermissions = useCallback(async () => {
    const response = await get<PermissionData>(
      `/archive/students/${stuNo}/permission`
    );
    if (response.data) setPermissions(response.data);
  }, [stuNo]);

  const fetchStudentInfo = useCallback(async () => {
    const response = await get<StudentBasicInfo>(`/students/${stuNo}`);
    if (response.data) {
      setStudentInfo(response.data);
    } else if (response.status === 404) {
      const desc = response.error || `学生不存在 (学号:${stuNo})`;
      router.push(`/not-found?description=${encodeURIComponent(desc)}`);
    }
  }, [stuNo, router]);

  const fetchArchiveData = useCallback(async () => {
    try {
      const response = await get<any>(`/archive/students/${stuNo}`);
      if (response.data) setFormData(response.data);
    } catch (error: any) {
      if (error?.response?.status !== 404) {
        console.error("获取档案数据失败:", error);
      }
    }
  }, [stuNo]);

  useEffect(() => {
    document.title = `学生档案 ${stuNo} - 应急协同平台`;
    const init = async () => {
      setLoading(true);
      await Promise.all([
        fetchMetadata(),
        fetchPermissions(),
        fetchStudentInfo(),
      ]);
      await fetchArchiveData();
      setLoading(false);
    };
    init();
  }, [stuNo, fetchMetadata, fetchPermissions, fetchStudentInfo, fetchArchiveData]);

  // ---- 错误对话框 ----

  const showErrorDialog = useCallback((title: string, message: string) => {
    setErrorDialogTitle(title);
    setErrorDialogMessage(message);
    setErrorDialogOpen(true);
  }, []);

  const closeErrorDialog = useCallback(() => setErrorDialogOpen(false), []);

  // ---- 表单字段操作 ----

  /** 在嵌套数据中按路径设置值 */
  const setFieldInData = useCallback((path: string, value: any) => {
    setFormData((prev) => {
      const newData = { ...prev };
      const segments = parsePath(path);
      let current: any = newData;

      for (let i = 0; i < segments.length - 1; i++) {
        const seg = segments[i];
        const nextSeg = segments[i + 1];
        if (!current[seg]) {
          current[seg] = typeof nextSeg === "number" ? [] : {};
        }
        current = current[seg];
      }

      current[segments[segments.length - 1]] = value;
      return newData;
    });
  }, []);

  // 用 Ref 保存 handleFieldChange 以便自动填充递归调用
  const handleFieldChangeRef = useRef<(path: string, value: any) => void>(
    () => {}
  );

  const handleFieldChange = useCallback(
    (path: string, value: any) => {
      setFieldInData(path, value);
      applyAutoFillRules(
        path,
        value,
        "change",
        formDataRef.current,
        schemaRef.current,
        permissionsRef.current,
        handleFieldChangeRef.current
      );
    },
    [setFieldInData]
  );

  handleFieldChangeRef.current = handleFieldChange;

  const handleFieldBlur = useCallback(
    (path: string, value: string, fieldSchema: JsonSchemaProperty) => {
      const error = validateField(value, fieldSchema);
      setValidationErrors((prev) => {
        const newErrors = { ...prev };
        if (error) newErrors[path] = error;
        else delete newErrors[path];
        return newErrors;
      });

      applyAutoFillRules(
        path,
        value,
        "blur",
        formDataRef.current,
        schemaRef.current,
        permissionsRef.current,
        handleFieldChangeRef.current
      );
    },
    []
  );

  const getFieldValue = useCallback(
    (path: string) => getFieldValueFromData(formData, path),
    [formData]
  );

  const getArrayData = useCallback(
    (path: string) => getArrayDataFromData(formData, path),
    [formData]
  );

  const addArrayItem = useCallback((path: string) => {
    setFormData((prev) => {
      const newData = JSON.parse(JSON.stringify(prev));
      const segments = parsePath(path);
      let current: any = newData;

      for (let i = 0; i < segments.length - 1; i++) {
        const seg = segments[i];
        const nextSeg = segments[i + 1];
        if (!current[seg])
          current[seg] = typeof nextSeg === "number" ? [] : {};
        current = current[seg];
      }

      const key = segments[segments.length - 1];
      if (!Array.isArray(current[key])) current[key] = [];
      current[key] = [...current[key], {}];
      return newData;
    });
  }, []);

  const removeArrayItem = useCallback((path: string, index: number) => {
    setFormData((prev) => {
      const newData = JSON.parse(JSON.stringify(prev));
      const segments = parsePath(path);
      let current: any = newData;

      for (let i = 0; i < segments.length - 1; i++) {
        if (!current[segments[i]]) return prev;
        current = current[segments[i]];
      }

      const key = segments[segments.length - 1];
      if (Array.isArray(current[key])) current[key].splice(index, 1);
      return newData;
    });
  }, []);

  const moveArrayItem = useCallback(
    (path: string, fromIndex: number, toIndex: number) => {
      setFormData((prev) => {
        const newData = JSON.parse(JSON.stringify(prev));
        const segments = parsePath(path);
        let current: any = newData;

        for (let i = 0; i < segments.length - 1; i++) {
          if (!current[segments[i]]) return prev;
          current = current[segments[i]];
        }

        const key = segments[segments.length - 1];
        if (Array.isArray(current[key]) && current[key].length > fromIndex) {
          const arr = current[key];
          const [moved] = arr.splice(fromIndex, 1);
          arr.splice(toIndex, 0, moved);
        }
        return newData;
      });
    },
    []
  );

  // ---- 页面动作 ----

  const handleGoBack = useCallback(() => router.back(), [router]);

  const handleExport = useCallback(async () => {
    setExportLoading(true);
    try {
      const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || "/api";
      const token = localStorage.getItem("authToken");

      const response = await fetch(
        `${API_BASE_URL}/archive/students/${stuNo}/export`,
        {
          method: "POST",
          headers: {
            ...(token && { Authorization: `Bearer ${token}` }),
          },
        }
      );

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || "导出失败");
      }

      // 解析文件名
      const contentDisposition = response.headers.get("Content-Disposition");
      let filename = `学生档案_${stuNo}.xlsx`;
      if (contentDisposition) {
        const starMatch = contentDisposition.match(
          /filename\*=([^']+)'([^']*)'(.+)/
        );
        if (starMatch?.[3]) {
          filename = decodeURIComponent(starMatch[3]);
        } else {
          const basicMatch = contentDisposition.match(
            /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/
          );
          if (basicMatch?.[1]) {
            filename = decodeURIComponent(
              basicMatch[1].replace(/['"]/g, "")
            );
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
  }, [stuNo, showErrorDialog]);

  const handleSave = useCallback(async () => {
    const currentSchema = schemaRef.current;
    if (!currentSchema) {
      showErrorDialog("无法保存", "表单结构未加载，请刷新页面后重试");
      return;
    }

    const currentPermissions = permissionsRef.current;
    const currentFormData = formDataRef.current;
    const errors: Record<string, string> = {};

    const validateAll = (
      properties: Record<string, JsonSchemaProperty>,
      parentPath = ""
    ) => {
      Object.entries(properties).forEach(([key, fs]) => {
        const fp = parentPath ? `${parentPath}.${key}` : key;

        if (!isFieldWritable(fs, currentPermissions)) return;

        const val = getFieldValueFromData(currentFormData, fp);
        if (
          isFieldNullable(fs) &&
          (val === null || val === undefined || val === "")
        )
          return;

        if (fs.pattern && val && !new RegExp(fs.pattern).test(val)) {
          errors[fp] = "格式不正确";
        }
        if (fs.maxLength && val && String(val).length > fs.maxLength) {
          errors[fp] = `超过最大长度 ${fs.maxLength}`;
        }

        const fieldType = resolveFieldType(fs);
        if (fieldType === "object" && fs.properties) {
          validateAll(fs.properties, fp);
        }
        if (fieldType === "array" && fs.items?.properties) {
          const arr = getArrayDataFromData(currentFormData, fp);
          arr.forEach((_, idx) => {
            validateAll(fs.items!.properties!, `${fp}[${idx}]`);
          });
        }
      });
    };

    validateAll(currentSchema.properties);

    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors);
      showErrorDialog(
        "表单验证失败",
        `请修正表单错误后再保存：${Object.values(errors)[0]}`
      );
      return;
    }

    setSaveLoading(true);
    try {
      const response = await put(
        `/archive/students/${stuNo}`,
        currentFormData
      );
      if (response.error) throw new Error(response.error);
      toast.success("保存成功！");
      await fetchArchiveData();
    } catch (error: any) {
      showErrorDialog("保存失败", error.message || "未知错误");
    } finally {
      setSaveLoading(false);
    }
  }, [stuNo, showErrorDialog, fetchArchiveData]);

  // ---- 组装 FormActions ----

  const formActions: FormActions = {
    getFieldValue,
    getArrayData,
    handleFieldChange,
    handleFieldBlur,
    addArrayItem,
    removeArrayItem,
    moveArrayItem,
    permissions,
    validationErrors,
  };

  return {
    stuNo,
    loading,
    exportLoading,
    saveLoading,
    schema,
    permissions,
    studentInfo,
    errorDialogOpen,
    errorDialogTitle,
    errorDialogMessage,
    closeErrorDialog,
    handleGoBack,
    handleExport,
    handleSave,
    formActions,
  };
}
