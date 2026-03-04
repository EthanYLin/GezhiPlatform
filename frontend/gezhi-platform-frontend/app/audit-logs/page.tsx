"use client";

import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Navbar } from "@/components/navbar";
import { get } from "@/lib/api-client";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { ChevronLeft, ChevronRight, Loader2, Search, RotateCcw } from "lucide-react";

interface AuditRecord {
  id: number;
  username: string;
  name: string;
  time: string;
  operation: string;
  details: string;
}

interface PageResult {
  content: AuditRecord[];
  pageNo: number;
  pageSize: number;
  totalPages: number;
  totalElements: number;
}

interface Filters {
  startTime: string;
  endTime: string;
  operation: string;
  username: string;
  keyword: string;
}

function toApiDateTime(datetimeLocal: string): string {
  if (!datetimeLocal) return "";
  return datetimeLocal.replace("T", " ") + ":00";
}

function formatDisplayTime(isoTime: string): string {
  const date = new Date(isoTime);
  return date.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
  });
}

function AuditLogsContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [loading, setLoading] = useState(true);
  const [searching, setSearching] = useState(false);
  const [auditLogs, setAuditLogs] = useState<PageResult | null>(null);
  const [initialized, setInitialized] = useState(false);

  const [filters, setFilters] = useState<Filters>({
    startTime: "",
    endTime: "",
    operation: "",
    username: "",
    keyword: "",
  });

  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  useEffect(() => {
    document.title = "审计日志 - 应急协同平台";

    const initialFilters: Filters = {
      startTime: searchParams.get("startTime") || "",
      endTime: searchParams.get("endTime") || "",
      operation: searchParams.get("operation") || "",
      username: searchParams.get("username") || "",
      keyword: searchParams.get("keyword") || "",
    };
    const page = searchParams.get("page");
    const size = searchParams.get("size");

    setFilters(initialFilters);
    setCurrentPage(page ? parseInt(page) : 1);
    setPageSize(size ? parseInt(size) : 20);
    setInitialized(true);
    setLoading(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (initialized) {
      performSearch();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialized]);

  const performSearch = async (overrides?: {
    filters?: Filters;
    page?: number;
    size?: number;
  }) => {
    setSearching(true);

    const f = overrides?.filters ?? filters;
    const searchPage = overrides?.page ?? currentPage;
    const searchSize = overrides?.size ?? pageSize;

    const params = new URLSearchParams();
    if (f.startTime) params.append("startTime", toApiDateTime(f.startTime));
    if (f.endTime) params.append("endTime", toApiDateTime(f.endTime));
    if (f.operation && f.operation !== "all")
      params.append("operation", f.operation);
    if (f.username.trim()) params.append("username", f.username.trim());
    if (f.keyword.trim()) params.append("keyword", f.keyword.trim());
    params.append("page", (searchPage - 1).toString());
    params.append("size", searchSize.toString());
    params.append("sort", "time,desc");

    const response = await get<PageResult>(
      `/admin/audit?${params.toString()}`
    );
    if (response.data) {
      setAuditLogs(response.data);
    }

    updateURL(f, searchPage, searchSize);
    setSearching(false);
  };

  const updateURL = (
    f: Filters = filters,
    page: number = currentPage,
    size: number = pageSize
  ) => {
    const params = new URLSearchParams();
    if (f.startTime) params.append("startTime", f.startTime);
    if (f.endTime) params.append("endTime", f.endTime);
    if (f.operation && f.operation !== "all")
      params.append("operation", f.operation);
    if (f.username.trim()) params.append("username", f.username.trim());
    if (f.keyword.trim()) params.append("keyword", f.keyword.trim());
    if (page > 1) params.append("page", page.toString());
    if (size !== 20) params.append("size", size.toString());

    const queryString = params.toString();
    const newUrl = queryString ? `/audit-logs?${queryString}` : "/audit-logs";
    router.push(newUrl, { scroll: false });
  };

  const updateFilter = <K extends keyof Filters>(key: K, value: Filters[K]) => {
    setFilters((prev) => ({ ...prev, [key]: value }));
  };

  const handleSearch = () => {
    setCurrentPage(1);
    performSearch({ page: 1 });
  };

  const handleReset = () => {
    const emptyFilters: Filters = {
      startTime: "",
      endTime: "",
      operation: "",
      username: "",
      keyword: "",
    };
    setFilters(emptyFilters);
    setCurrentPage(1);
    performSearch({ filters: emptyFilters, page: 1 });
  };

  const handlePageChange = (newPage: number) => {
    if (newPage < 1 || !auditLogs || newPage > auditLogs.totalPages) return;
    setCurrentPage(newPage);
    performSearch({ page: newPage });
  };

  const handlePageSizeChange = (value: string) => {
    const newSize = parseInt(value);
    setPageSize(newSize);
    setCurrentPage(1);
    performSearch({ page: 1, size: newSize });
  };

  const handlePageInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = parseInt(e.target.value);
    if (!isNaN(value)) {
      setCurrentPage(value);
    }
  };

  const handlePageInputBlur = () => {
    if (auditLogs && currentPage > auditLogs.totalPages) {
      setCurrentPage(auditLogs.totalPages);
      performSearch({ page: auditLogs.totalPages });
    } else if (currentPage < 1) {
      setCurrentPage(1);
      performSearch({ page: 1 });
    } else {
      performSearch({ page: currentPage });
    }
  };

  if (loading) {
    return (
      <>
        <Navbar />
        <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center">
          <div className="flex flex-col items-center gap-3">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
            <p className="text-muted-foreground">加载中...</p>
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
          <div>
            <h1 className="text-3xl font-bold text-foreground">审计日志</h1>
          </div>

          {/* 筛选表单 */}
          <Card className="p-6">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 items-end">
              <div className="space-y-2">
                <Label htmlFor="startTime">开始时间</Label>
                <Input
                  id="startTime"
                  type="datetime-local"
                  value={filters.startTime}
                  onChange={(e) => updateFilter("startTime", e.target.value)}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="endTime">结束时间</Label>
                <Input
                  id="endTime"
                  type="datetime-local"
                  value={filters.endTime}
                  onChange={(e) => updateFilter("endTime", e.target.value)}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="operation">操作类型</Label>
                <Select
                  value={filters.operation}
                  onValueChange={(v) => updateFilter("operation", v)}
                >
                  <SelectTrigger id="operation">
                    <SelectValue placeholder="全部类型" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">全部类型</SelectItem>
                    <SelectItem value="档案查询">档案查询</SelectItem>
                    <SelectItem value="档案导出">档案导出</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="username">用户名</Label>
                <Input
                  id="username"
                  placeholder="精确匹配用户名"
                  value={filters.username}
                  onChange={(e) => updateFilter("username", e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") handleSearch();
                  }}
                />
              </div>
            </div>

            <div className="mt-4 flex gap-2 items-end">
              <div className="flex-1 space-y-2">
                <Label htmlFor="keyword">关键词</Label>
                <Input
                  id="keyword"
                  placeholder="在操作详情中模糊搜索"
                  value={filters.keyword}
                  onChange={(e) => updateFilter("keyword", e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") handleSearch();
                  }}
                />
              </div>
              <Button onClick={handleSearch} disabled={searching}>
                {searching ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <>
                    <Search className="h-4 w-4" />
                    查询
                  </>
                )}
              </Button>
              <Button
                variant="outline"
                onClick={handleReset}
                disabled={searching}
              >
                <RotateCcw className="h-4 w-4" />
                重置
              </Button>
            </div>
          </Card>

          {/* 数据表格 */}
          <Card>
            <div className="overflow-x-auto">
              <Table className="[&_tr>*:first-child]:pl-6 [&_tr>*:last-child]:pr-6">
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-16">ID</TableHead>
                    <TableHead>用户名</TableHead>
                    <TableHead>姓名</TableHead>
                    <TableHead>时间</TableHead>
                    <TableHead>操作类型</TableHead>
                    <TableHead>操作详情</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {searching ? (
                    <TableRow>
                      <TableCell colSpan={6} className="text-center py-8">
                        <Loader2 className="h-6 w-6 animate-spin mx-auto" />
                      </TableCell>
                    </TableRow>
                  ) : auditLogs && auditLogs.content.length > 0 ? (
                    auditLogs.content.map((record) => (
                      <TableRow key={record.id}>
                        <TableCell className="font-mono text-muted-foreground">
                          {record.id}
                        </TableCell>
                        <TableCell className="font-mono">
                          {record.username}
                        </TableCell>
                        <TableCell>{record.name}</TableCell>
                        <TableCell className="text-muted-foreground whitespace-nowrap">
                          {formatDisplayTime(record.time)}
                        </TableCell>
                        <TableCell>
                          <Badge
                            variant={
                              record.operation === "档案导出"
                                ? "default"
                                : "secondary"
                            }
                          >
                            {record.operation}
                          </Badge>
                        </TableCell>
                        <TableCell className="max-w-md truncate">
                          {record.details}
                        </TableCell>
                      </TableRow>
                    ))
                  ) : (
                    <TableRow>
                      <TableCell
                        colSpan={6}
                        className="text-center py-8 text-muted-foreground"
                      >
                        暂无数据
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </div>

            {/* 分页 */}
            {auditLogs && auditLogs.content.length > 0 && (
              <div className="flex items-center justify-between px-6 py-4 border-t">
                <div className="flex items-center gap-2">
                  <span className="text-sm text-muted-foreground">
                    每页显示
                  </span>
                  <Select
                    value={pageSize.toString()}
                    onValueChange={handlePageSizeChange}
                  >
                    <SelectTrigger className="w-[100px]">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="20">20条/页</SelectItem>
                      <SelectItem value="50">50条/页</SelectItem>
                      <SelectItem value="100">100条/页</SelectItem>
                      <SelectItem value="200">200条/页</SelectItem>
                    </SelectContent>
                  </Select>
                  <span className="text-sm text-muted-foreground">
                    共 {auditLogs.totalElements} 条
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="icon"
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={currentPage <= 1}
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </Button>

                  <div className="flex items-center gap-1">
                    <Input
                      type="number"
                      min={1}
                      max={auditLogs.totalPages}
                      value={currentPage}
                      onChange={handlePageInputChange}
                      onBlur={handlePageInputBlur}
                      className="w-16 text-center [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none [-moz-appearance:textfield]"
                    />
                    <span className="text-sm text-muted-foreground">
                      / {auditLogs.totalPages}
                    </span>
                  </div>

                  <Button
                    variant="outline"
                    size="icon"
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage >= auditLogs.totalPages}
                  >
                    <ChevronRight className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            )}
          </Card>
        </div>
      </div>
    </>
  );
}

export default function AuditLogsPage() {
  return (
    <Suspense
      fallback={
        <>
          <Navbar />
          <div className="min-h-[calc(100vh-4rem)] flex items-center justify-center">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        </>
      }
    >
      <AuditLogsContent />
    </Suspense>
  );
}
