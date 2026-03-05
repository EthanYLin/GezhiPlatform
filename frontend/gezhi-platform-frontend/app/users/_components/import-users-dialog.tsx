"use client";

import { useState, useRef, useCallback, type DragEvent } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableHead,
  TableCell,
} from "@/components/ui/table";
import { Alert, AlertTitle, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import {
  Upload,
  Download,
  Loader2,
  FileSpreadsheet,
  ArrowLeft,
  AlertCircle,
  CheckCircle2,
} from "lucide-react";
import { toast } from "sonner";
import * as XLSX from "xlsx";
import { importUsers } from "../_api";
import type { NewUserRequest, UserRoleDetailsDTO, RoleType } from "../_types";

interface ImportUsersDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess: () => void;
}

interface ValidationError {
  row: number;
  field: string;
  message: string;
}

interface ParsedRow {
  name: string;
  username: string;
  defaultPassword: string;
  校级领导: string;
  年级组长: string;
  班主任: string;
  学生用户: string;
  家长用户: string;
  新生家长: string;
  多班级观察员: string;
  协作用户: string;
  超级管理员: string;
}

const EXPECTED_HEADERS = [
  "姓名",
  "用户名",
  "初始密码",
  "校级领导",
  "年级组长",
  "班主任",
  "学生用户",
  "家长用户",
  "新生家长",
  "多班级观察员",
  "协作用户",
  "超级管理员",
];

const HINT_ROW = [
  "选填",
  "选填，不设置则无法登录",
  "选填，不设置则无法登录",
  "若要设置该角色，输入1",
  "若要设置该角色，输入年级(如2027)",
  "若要设置该角色，输入年级-班级(如2027-1)",
  "若要设置该角色，输入学号(如270101)",
  "若要设置该角色，输入孩子学号，多个学号用逗号隔开(如270101,270102)",
  "若要设置该角色，输入孩子学号，多个学号用逗号隔开(如270101,270102)",
  "若要设置该角色，输入年级-班级，多个班级用逗号隔开(如2027-1,2027-2)",
  "若要设置该角色，输入学号，多个学号用逗号隔开(如270101,270102)",
  "若要设置该角色，输入1",
];

const MAX_ROWS = 500;

// ── Helpers ──

function parseGradeClass(s: string): { gradeNo: number; classNo: number } | null {
  const m = s.match(/^(\d+)-(\d+)$/);
  if (!m) return null;
  return { gradeNo: parseInt(m[1]), classNo: parseInt(m[2]) };
}

function isValidGrade(n: number) {
  return Number.isInteger(n) && n >= 1900 && n <= 2100;
}

function isValidClass(n: number) {
  return Number.isInteger(n) && n >= 1 && n <= 100;
}

function isDigitsOnly(s: string) {
  return /^\d+$/.test(s);
}

// ── Validation ──

function validateRows(rows: ParsedRow[]): ValidationError[] {
  const errs: ValidationError[] = [];

  rows.forEach((row, idx) => {
    const rowNum = idx + 2; // 1-based header + 1-based data

    const roleFields: (keyof ParsedRow)[] = [
      "校级领导", "年级组长", "班主任", "学生用户",
      "家长用户", "新生家长", "多班级观察员", "协作用户", "超级管理员",
    ];
    const hasAnyRole = roleFields.some((f) => row[f].trim() !== "");
    if (!hasAnyRole) {
      errs.push({ row: rowNum, field: "角色", message: "至少需要设置一个角色" });
    }

    // 校级领导：必须为 1
    const leader = row.校级领导.trim();
    if (leader && leader !== "1") {
      errs.push({ row: rowNum, field: "校级领导", message: "值必须为 1" });
    }

    // 超级管理员：必须为 1
    const admin = row.超级管理员.trim();
    if (admin && admin !== "1") {
      errs.push({ row: rowNum, field: "超级管理员", message: "值必须为 1" });
    }

    // 年级组长：合法年级整数
    const gradeDean = row.年级组长.trim();
    if (gradeDean) {
      const n = Number(gradeDean);
      if (!isValidGrade(n)) {
        errs.push({ row: rowNum, field: "年级组长", message: "请输入合法年级（1900-2100之间的整数）" });
      }
    }

    // 班主任：年级-班级
    const advisor = row.班主任.trim();
    if (advisor) {
      const gc = parseGradeClass(advisor);
      if (!gc) {
        errs.push({ row: rowNum, field: "班主任", message: "格式应为 年级-班级（如2027-1）" });
      } else {
        if (!isValidGrade(gc.gradeNo))
          errs.push({ row: rowNum, field: "班主任", message: "年级不合法（1900-2100）" });
        if (!isValidClass(gc.classNo))
          errs.push({ row: rowNum, field: "班主任", message: "班级不合法（1-100）" });
      }
    }

    // 学生用户：纯数字学号
    const student = row.学生用户.trim();
    if (student) {
      if (!isDigitsOnly(student)) {
        errs.push({ row: rowNum, field: "学生用户", message: "学号只能由数字组成" });
      }
    }

    // 家长用户：逗号分隔的学号
    const parent = row.家长用户.trim();
    if (parent) {
      const nos = parent.split(/[,，]/).map((s) => s.trim()).filter(Boolean);
      if (nos.length === 0) {
        errs.push({ row: rowNum, field: "家长用户", message: "学号不能为空" });
      } else {
        nos.forEach((no) => {
          if (!isDigitsOnly(no))
            errs.push({ row: rowNum, field: "家长用户", message: `"${no}" 不是合法学号` });
        });
      }
    }

    // 新生家长：同家长用户
    const newParent = row.新生家长.trim();
    if (newParent) {
      const nos = newParent.split(/[,，]/).map((s) => s.trim()).filter(Boolean);
      if (nos.length === 0) {
        errs.push({ row: rowNum, field: "新生家长", message: "学号不能为空" });
      } else {
        nos.forEach((no) => {
          if (!isDigitsOnly(no))
            errs.push({ row: rowNum, field: "新生家长", message: `"${no}" 不是合法学号` });
        });
      }
    }

    // 协作用户：逗号分隔的学号
    const cu = row.协作用户.trim();
    if (cu) {
      const nos = cu.split(/[,，]/).map((s) => s.trim()).filter(Boolean);
      if (nos.length === 0) {
        errs.push({ row: rowNum, field: "协作用户", message: "学号不能为空" });
      } else {
        nos.forEach((no) => {
          if (!isDigitsOnly(no))
            errs.push({ row: rowNum, field: "协作用户", message: `"${no}" 不是合法学号` });
        });
      }
    }

    // 多班级观察员：逗号分隔的 年级-班级
    const observer = row.多班级观察员.trim();
    if (observer) {
      const items = observer.split(/[,，]/).map((s) => s.trim()).filter(Boolean);
      if (items.length === 0) {
        errs.push({ row: rowNum, field: "多班级观察员", message: "班级列表不能为空" });
      } else {
        items.forEach((item) => {
          const gc = parseGradeClass(item);
          if (!gc) {
            errs.push({ row: rowNum, field: "多班级观察员", message: `"${item}" 格式应为 年级-班级` });
          } else {
            if (!isValidGrade(gc.gradeNo))
              errs.push({ row: rowNum, field: "多班级观察员", message: `"${item}" 年级不合法` });
            if (!isValidClass(gc.classNo))
              errs.push({ row: rowNum, field: "多班级观察员", message: `"${item}" 班级不合法` });
          }
        });
      }
    }
  });

  return errs;
}

// ── Convert parsed row to API request ──

function rowToRequest(row: ParsedRow): NewUserRequest {
  const roles: UserRoleDetailsDTO[] = [];

  if (row.超级管理员.trim() === "1") {
    roles.push({ roleType: "超级管理员", details: {} });
  }
  if (row.校级领导.trim() === "1") {
    roles.push({ roleType: "校级领导", details: {} });
  }
  const gradeDean = row.年级组长.trim();
  if (gradeDean) {
    roles.push({ roleType: "年级组长", details: { gradeNo: parseInt(gradeDean) } });
  }
  const advisor = row.班主任.trim();
  if (advisor) {
    const gc = parseGradeClass(advisor)!;
    roles.push({ roleType: "班主任", details: { gradeClass: gc } });
  }
  const student = row.学生用户.trim();
  if (student) {
    roles.push({ roleType: "学生用户", details: { stuNo: student } });
  }
  const parent = row.家长用户.trim();
  if (parent) {
    const stuNos = parent.split(/[,，]/).map((s) => s.trim()).filter(Boolean);
    roles.push({ roleType: "家长用户", details: { stuNos } });
  }
  const newParent = row.新生家长.trim();
  if (newParent) {
    const stuNos = newParent.split(/[,，]/).map((s) => s.trim()).filter(Boolean);
    roles.push({ roleType: "新生家长", details: { stuNos } });
  }
  const observer = row.多班级观察员.trim();
  if (observer) {
    const gradeClasses = observer.split(/[,，]/).map((s) => s.trim()).filter(Boolean).map((item) => {
      const gc = parseGradeClass(item)!;
      return gc;
    });
    roles.push({ roleType: "多班级观察员", details: { gradeClasses } });
  }
  const cu = row.协作用户.trim();
  if (cu) {
    const stuNos = cu.split(/[,，]/).map((s) => s.trim()).filter(Boolean);
    roles.push({ roleType: "协作用户", details: { stuNos } });
  }

  return {
    name: row.name.trim() || null,
    username: row.username.trim() || null,
    defaultPassword: row.defaultPassword.trim() || null,
    roles,
  };
}

// ── Component ──

export function ImportUsersDialog({
  open,
  onOpenChange,
  onSuccess,
}: ImportUsersDialogProps) {
  const [stage, setStage] = useState<"upload" | "confirm">("upload");
  const [parsedRows, setParsedRows] = useState<ParsedRow[]>([]);
  const [errors, setErrors] = useState<ValidationError[]>([]);
  const [fileName, setFileName] = useState("");
  const [tooManyRows, setTooManyRows] = useState(false);
  const [importing, setImporting] = useState(false);
  const [dragging, setDragging] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const resetState = useCallback(() => {
    setStage("upload");
    setParsedRows([]);
    setErrors([]);
    setFileName("");
    setTooManyRows(false);
    setImporting(false);
    setDragging(false);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  }, []);

  const handleOpenChange = (nextOpen: boolean) => {
    if (!nextOpen) resetState();
    onOpenChange(nextOpen);
  };

  // ── Download template ──
  const handleDownloadTemplate = () => {
    const wb = XLSX.utils.book_new();
    const ws = XLSX.utils.aoa_to_sheet([EXPECTED_HEADERS]);
    ws["!cols"] = EXPECTED_HEADERS.map(() => ({ wch: 16 }));
    XLSX.utils.book_append_sheet(wb, ws, "用户导入模板");
    XLSX.writeFile(wb, "用户导入模板.xlsx");
  };

  // ── Parse file ──
  const processFile = (file: File) => {
    if (!file.name.endsWith(".xlsx") && !file.name.endsWith(".xls")) {
      toast.error("请上传 .xlsx 格式的文件");
      return;
    }

    setFileName(file.name);
    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const data = e.target?.result;
        const workbook = XLSX.read(data, { type: "array" });
        const sheetName = workbook.SheetNames[0];
        if (!sheetName) {
          toast.error("文件中不包含任何工作表");
          return;
        }
        const worksheet = workbook.Sheets[sheetName];
        const jsonData = XLSX.utils.sheet_to_json<Record<string, unknown>>(
          worksheet,
          { defval: "" }
        );

        if (jsonData.length === 0) {
          toast.error("文件中没有数据行，请检查文件内容");
          return;
        }

        if (jsonData.length > MAX_ROWS) {
          setTooManyRows(true);
        } else {
          setTooManyRows(false);
        }

        const rows: ParsedRow[] = jsonData.map((row) => ({
          name: String(row["姓名"] ?? "").trim(),
          username: String(row["用户名"] ?? "").trim(),
          defaultPassword: String(row["初始密码"] ?? "").trim(),
          校级领导: String(row["校级领导"] ?? "").trim(),
          年级组长: String(row["年级组长"] ?? "").trim(),
          班主任: String(row["班主任"] ?? "").trim(),
          学生用户: String(row["学生用户"] ?? "").trim(),
          家长用户: String(row["家长用户"] ?? "").trim(),
          新生家长: String(row["新生家长"] ?? "").trim(),
          多班级观察员: String(row["多班级观察员"] ?? "").trim(),
          协作用户: String(row["协作用户"] ?? "").trim(),
          超级管理员: String(row["超级管理员"] ?? "").trim(),
        }));

        setParsedRows(rows);
        setErrors(validateRows(rows));
        setStage("confirm");
      } catch {
        toast.error("文件解析失败，请确认文件格式正确");
      }
    };
    reader.readAsArrayBuffer(file);
  };

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) processFile(file);
  };

  const handleDragOver = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setDragging(true);
  };

  const handleDragLeave = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setDragging(false);
  };

  const handleDrop = (e: DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    e.stopPropagation();
    setDragging(false);
    const file = e.dataTransfer?.files?.[0];
    if (file) processFile(file);
  };

  // ── Submit import ──
  const handleImport = async () => {
    setImporting(true);
    try {
      const payload: NewUserRequest[] = parsedRows.map(rowToRequest);
      const res = await importUsers(payload);
      if (res.error) {
        toast.error(`导入失败：${res.error}`);
      } else {
        toast.success(`成功导入 ${parsedRows.length} 名用户`);
        handleOpenChange(false);
        onSuccess();
      }
    } catch {
      toast.error("导入请求异常，请稍后重试");
    } finally {
      setImporting(false);
    }
  };

  const previewRows = parsedRows.slice(0, 5);
  const hasErrors = errors.length > 0;
  const canImport = !hasErrors && !tooManyRows && parsedRows.length > 0;

  // Summarize roles for preview
  const summarizeRoles = (row: ParsedRow): string => {
    const parts: string[] = [];
    if (row.超级管理员.trim()) parts.push("超管");
    if (row.校级领导.trim()) parts.push("校领导");
    if (row.年级组长.trim()) parts.push("年级组长");
    if (row.班主任.trim()) parts.push("班主任");
    if (row.学生用户.trim()) parts.push("学生");
    if (row.家长用户.trim()) parts.push("家长");
    if (row.新生家长.trim()) parts.push("新生家长");
    if (row.多班级观察员.trim()) parts.push("观察员");
    if (row.协作用户.trim()) parts.push("协作");
    return parts.join("、") || "—";
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-4xl max-h-[90vh] flex flex-col overflow-hidden">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <FileSpreadsheet className="h-5 w-5" />
            从 Excel 导入用户
          </DialogTitle>
          <DialogDescription>
            {stage === "upload"
              ? "上传包含用户信息的 Excel 文件，或下载模板后填写再上传。"
              : `已解析文件「${fileName}」，请确认数据无误后导入。`}
          </DialogDescription>
        </DialogHeader>

        {/* ── Upload Stage ── */}
        {stage === "upload" && (
          <div className="space-y-4 overflow-y-auto min-h-0">
            <div className="flex items-center gap-2">
              <Button onClick={() => fileInputRef.current?.click()}>
                <Upload className="h-4 w-4" />
                上传文件
              </Button>
              <Button variant="outline" onClick={handleDownloadTemplate}>
                <Download className="h-4 w-4" />
                下载模板
              </Button>
              <input
                ref={fileInputRef}
                type="file"
                accept=".xlsx,.xls"
                className="hidden"
                onChange={handleFileSelect}
              />
            </div>

            <div className="space-y-2">
              <p className="text-sm font-medium text-foreground">文件格式要求</p>
              <div className="rounded-md border overflow-x-auto w-full">
                <Table>
                  <TableHeader>
                    <TableRow>
                      {EXPECTED_HEADERS.map((h) => (
                        <TableHead key={h} className="whitespace-nowrap">{h}</TableHead>
                      ))}
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    <TableRow>
                      {HINT_ROW.map((hint, i) => (
                        <TableCell key={i} className="text-xs text-muted-foreground whitespace-nowrap max-w-[180px]">
                          <span className="whitespace-normal">{hint}</span>
                        </TableCell>
                      ))}
                    </TableRow>
                  </TableBody>
                </Table>
              </div>
            </div>

            <div
              onDragOver={handleDragOver}
              onDragLeave={handleDragLeave}
              onDrop={handleDrop}
              onClick={() => fileInputRef.current?.click()}
              className={`flex flex-col items-center justify-center gap-2 rounded-lg border-2 border-dashed p-8 cursor-pointer transition-colors ${
                dragging
                  ? "border-primary bg-primary/5"
                  : "border-muted-foreground/25 hover:border-primary/50"
              }`}
            >
              <Upload
                className={`h-8 w-8 ${dragging ? "text-primary" : "text-muted-foreground"}`}
              />
              <p className="text-sm text-muted-foreground">
                将 xlsx 文件拖拽到此处，或点击选择文件
              </p>
            </div>
          </div>
        )}

        {/* ── Confirm Stage ── */}
        {stage === "confirm" && (
          <div className="space-y-4 overflow-y-auto min-h-0">
            <div className="flex items-center gap-2 text-sm">
              <FileSpreadsheet className="h-4 w-4 text-muted-foreground" />
              <span className="text-muted-foreground">
                共解析 <span className="font-medium text-foreground">{parsedRows.length}</span> 条记录
              </span>
            </div>

            {tooManyRows && (
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4" />
                <AlertTitle>数据量超出限制</AlertTitle>
                <AlertDescription>
                  文件包含 {parsedRows.length} 行数据，超过单次导入上限（{MAX_ROWS} 行）。请拆分文件后重新上传。
                </AlertDescription>
              </Alert>
            )}

            <div className="space-y-2">
              <p className="text-sm font-medium text-foreground">数据预览</p>
              <div className="rounded-md border overflow-x-auto">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-10">#</TableHead>
                      <TableHead>姓名</TableHead>
                      <TableHead>用户名</TableHead>
                      <TableHead>初始密码</TableHead>
                      <TableHead>角色</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {previewRows.map((row, idx) => (
                      <TableRow key={idx}>
                        <TableCell className="text-muted-foreground">{idx + 1}</TableCell>
                        <TableCell>{row.name || "—"}</TableCell>
                        <TableCell>{row.username || "—"}</TableCell>
                        <TableCell>{row.defaultPassword ? "••••" : "—"}</TableCell>
                        <TableCell className="text-xs">{summarizeRoles(row)}</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>
              {parsedRows.length > 5 && (
                <p className="text-xs text-muted-foreground">
                  仅显示前 5 条，共 {parsedRows.length} 条
                </p>
              )}
            </div>

            {hasErrors && (
              <div className="space-y-2">
                <Alert variant="destructive">
                  <AlertCircle className="h-4 w-4" />
                  <AlertTitle>发现 {errors.length} 个数据问题</AlertTitle>
                  <AlertDescription>请修正以下问题后重新上传文件。</AlertDescription>
                </Alert>
                <div className="max-h-40 overflow-y-auto rounded-md border p-3 space-y-1">
                  {errors.map((err, idx) => (
                    <p key={idx} className="text-sm text-destructive">
                      第 {err.row} 行 · {err.field}：{err.message}
                    </p>
                  ))}
                </div>
              </div>
            )}

            {!hasErrors && !tooManyRows && (
              <Alert>
                <CheckCircle2 className="h-4 w-4" />
                <AlertTitle>数据校验通过</AlertTitle>
                <AlertDescription>
                  共 {parsedRows.length} 条记录，确认无误后点击下方按钮导入。
                </AlertDescription>
              </Alert>
            )}
          </div>
        )}

        {stage === "confirm" && (
          <DialogFooter>
            <Button variant="outline" onClick={resetState} disabled={importing}>
              <ArrowLeft className="h-4 w-4" />
              返回
            </Button>
            <Button onClick={handleImport} disabled={!canImport || importing}>
              {importing ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <Upload className="h-4 w-4" />
              )}
              {importing ? "导入中..." : "确认导入"}
            </Button>
          </DialogFooter>
        )}
      </DialogContent>
    </Dialog>
  );
}
