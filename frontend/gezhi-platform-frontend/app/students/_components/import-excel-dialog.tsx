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
import { createStudents } from "../_api";
import type { NewStudentRequest } from "../_types";

interface ImportExcelDialogProps {
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
  stuNo: string;
  stuName: string;
  campus: string;
  gradeNo: string;
  classNo: string;
}

const EXPECTED_HEADERS = ["学号", "姓名", "校区", "年级", "班级"];
const MAX_ROWS = 1000;

export function ImportExcelDialog({
  open,
  onOpenChange,
  onSuccess,
}: ImportExcelDialogProps) {
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
    if (!nextOpen) {
      resetState();
    }
    onOpenChange(nextOpen);
  };

  // ── Download template ──
  const handleDownloadTemplate = () => {
    const wb = XLSX.utils.book_new();
    const ws = XLSX.utils.aoa_to_sheet([EXPECTED_HEADERS]);
    // Set column widths for readability
    ws["!cols"] = [
      { wch: 15 },
      { wch: 15 },
      { wch: 15 },
      { wch: 10 },
      { wch: 10 },
    ];
    XLSX.utils.book_append_sheet(wb, ws, "学生导入模板");
    XLSX.writeFile(wb, "学生导入模板.xlsx");
  };

  // ── Validate all rows ──
  const validateRows = (rows: ParsedRow[]): ValidationError[] => {
    const errs: ValidationError[] = [];
    const stuNoSet = new Set<string>();

    rows.forEach((row, idx) => {
      const rowNum = idx + 2; // Excel row number (1-based header + 1-based data)

      // stuNo: required, digits only
      const stuNo = (row.stuNo ?? "").toString().trim();
      if (!stuNo) {
        errs.push({ row: rowNum, field: "学号", message: "学号不能为空" });
      } else if (!/^[0-9]+$/.test(stuNo)) {
        errs.push({
          row: rowNum,
          field: "学号",
          message: "学号只能由数字组成",
        });
      } else if (stuNoSet.has(stuNo)) {
        errs.push({
          row: rowNum,
          field: "学号",
          message: `学号 "${stuNo}" 在文件中重复`,
        });
      } else {
        stuNoSet.add(stuNo);
      }

      // stuName: required, Chinese/English/space only
      const stuName = (row.stuName ?? "").toString().trim();
      if (!stuName) {
        errs.push({ row: rowNum, field: "姓名", message: "姓名不能为空" });
      } else if (!/^[\p{L} ]+$/u.test(stuName)) {
        errs.push({
          row: rowNum,
          field: "姓名",
          message: "姓名只能包含中文、英文和空格",
        });
      }

      // campus: optional, must be one of the valid values
      const campus = (row.campus ?? "").toString().trim();
      if (campus && campus !== "黄浦校区" && campus !== "奉贤校区") {
        errs.push({
          row: rowNum,
          field: "校区",
          message: "校区只能为「黄浦校区」、「奉贤校区」或不填写",
        });
      }

      // gradeNo: optional, must be number in range 1900-2100
      const gradeRaw = (row.gradeNo ?? "").toString().trim();
      if (gradeRaw) {
        const gradeNum = Number(gradeRaw);
        if (!Number.isInteger(gradeNum)) {
          errs.push({
            row: rowNum,
            field: "年级",
            message: "年级必须为整数",
          });
        } else if (gradeNum < 1900 || gradeNum > 2100) {
          errs.push({
            row: rowNum,
            field: "年级",
            message: "年级范围为 1900–2100",
          });
        }
      }

      // classNo: optional, must be number in range 1-100
      const classRaw = (row.classNo ?? "").toString().trim();
      if (classRaw) {
        const classNum = Number(classRaw);
        if (!Number.isInteger(classNum)) {
          errs.push({
            row: rowNum,
            field: "班级",
            message: "班级必须为整数",
          });
        } else if (classNum < 1 || classNum > 100) {
          errs.push({
            row: rowNum,
            field: "班级",
            message: "班级范围为 1–100",
          });
        }
      }
    });

    return errs;
  };

  // ── Parse file ──
  const processFile = (file: File) => {
    if (
      !file.name.endsWith(".xlsx") &&
      !file.name.endsWith(".xls")
    ) {
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

        // Check row count
        if (jsonData.length > MAX_ROWS) {
          setTooManyRows(true);
          // Still parse for preview but block import
        } else {
          setTooManyRows(false);
        }

        // Map to our format
        const rows: ParsedRow[] = jsonData.map((row) => ({
          stuNo: String(row["学号"] ?? "").trim(),
          stuName: String(row["姓名"] ?? "").trim(),
          campus: String(row["校区"] ?? "").trim(),
          gradeNo: String(row["年级"] ?? "").trim(),
          classNo: String(row["班级"] ?? "").trim(),
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

  // ── File input handler ──
  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) processFile(file);
  };

  // ── Drag & drop ──
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
      const payload: NewStudentRequest[] = parsedRows.map((row) => {
        const req: NewStudentRequest = {
          stuNo: row.stuNo,
          stuName: row.stuName,
        };
        if (row.campus) req.campus = row.campus;
        if (row.gradeNo) req.gradeNo = Number(row.gradeNo);
        if (row.classNo) req.classNo = Number(row.classNo);
        return req;
      });

      const res = await createStudents(payload);
      if (res.error) {
        toast.error(`导入失败：${res.error}`);
      } else {
        toast.success(`成功导入 ${parsedRows.length} 名学生`);
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

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <FileSpreadsheet className="h-5 w-5" />
            从 Excel 导入学生
          </DialogTitle>
          <DialogDescription>
            {stage === "upload"
              ? "上传包含学生信息的 Excel 文件，或下载模板后填写再上传。"
              : `已解析文件「${fileName}」，请确认数据无误后导入。`}
          </DialogDescription>
        </DialogHeader>

        {/* ── Upload Stage ── */}
        {stage === "upload" && (
          <div className="space-y-4">
            {/* Action buttons */}
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

            {/* Format reference table */}
            <div className="space-y-2">
              <p className="text-sm font-medium text-foreground">
                文件格式要求
              </p>
              <div className="rounded-md border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      {EXPECTED_HEADERS.map((h) => (
                        <TableHead key={h}>{h}</TableHead>
                      ))}
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    <TableRow>
                      <TableCell className="text-xs text-muted-foreground whitespace-normal">
                        不能为空，且只能由数字组成
                      </TableCell>
                      <TableCell className="text-xs text-muted-foreground whitespace-normal">
                        不能为空，只能包含中文、英文和空格
                      </TableCell>
                      <TableCell className="text-xs text-muted-foreground whitespace-normal">
                        黄浦校区、奉贤校区或不填写
                      </TableCell>
                      <TableCell className="text-xs text-muted-foreground whitespace-normal">
                        届别，如 2026
                      </TableCell>
                      <TableCell className="text-xs text-muted-foreground whitespace-normal">
                        班级编号，如 1
                      </TableCell>
                    </TableRow>
                  </TableBody>
                </Table>
              </div>
            </div>

            {/* Drag & drop zone */}
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
                className={`h-8 w-8 ${
                  dragging ? "text-primary" : "text-muted-foreground"
                }`}
              />
              <p className="text-sm text-muted-foreground">
                将 xlsx 文件拖拽到此处，或点击选择文件
              </p>
            </div>
          </div>
        )}

        {/* ── Confirm Stage ── */}
        {stage === "confirm" && (
          <div className="space-y-4">
            {/* Summary */}
            <div className="flex items-center gap-2 text-sm">
              <FileSpreadsheet className="h-4 w-4 text-muted-foreground" />
              <span className="text-muted-foreground">
                共解析 <span className="font-medium text-foreground">{parsedRows.length}</span> 条记录
              </span>
            </div>

            {/* Too many rows alert */}
            {tooManyRows && (
              <Alert variant="destructive">
                <AlertCircle className="h-4 w-4" />
                <AlertTitle>数据量超出限制</AlertTitle>
                <AlertDescription>
                  文件包含 {parsedRows.length} 行数据，超过单次导入上限（{MAX_ROWS} 行）。请拆分文件后重新上传。
                </AlertDescription>
              </Alert>
            )}

            {/* Preview table */}
            <div className="space-y-2">
              <p className="text-sm font-medium text-foreground">数据预览</p>
              <div className="rounded-md border">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-10">#</TableHead>
                      {EXPECTED_HEADERS.map((h) => (
                        <TableHead key={h}>{h}</TableHead>
                      ))}
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {previewRows.map((row, idx) => (
                      <TableRow key={idx}>
                        <TableCell className="text-muted-foreground">
                          {idx + 1}
                        </TableCell>
                        <TableCell>{row.stuNo || "—"}</TableCell>
                        <TableCell>{row.stuName || "—"}</TableCell>
                        <TableCell>{row.campus || "—"}</TableCell>
                        <TableCell>{row.gradeNo || "—"}</TableCell>
                        <TableCell>{row.classNo || "—"}</TableCell>
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

            {/* Validation errors */}
            {hasErrors && (
              <div className="space-y-2">
                <Alert variant="destructive">
                  <AlertCircle className="h-4 w-4" />
                  <AlertTitle>
                    发现 {errors.length} 个数据问题
                  </AlertTitle>
                  <AlertDescription>
                    请修正以下问题后重新上传文件。
                  </AlertDescription>
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

            {/* No errors */}
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

        {/* ── Footer ── */}
        {stage === "confirm" && (
          <DialogFooter>
            <Button
              variant="outline"
              onClick={resetState}
              disabled={importing}
            >
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
