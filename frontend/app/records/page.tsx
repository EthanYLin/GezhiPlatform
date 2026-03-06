"use client";

import {Suspense, useEffect, useState} from "react";
import {useRouter, useSearchParams} from "next/navigation";
import {Navbar} from "@/components/navbar";
import {get} from "@/lib/api-client";
import {Button} from "@/components/ui/button";
import {Input} from "@/components/ui/input";
import {Label} from "@/components/ui/label";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue,} from "@/components/ui/select";
import {Table, TableBody, TableCell, TableHead, TableHeader, TableRow,} from "@/components/ui/table";
import {Card} from "@/components/ui/card";
import {ChevronLeft, ChevronRight, Loader2} from "lucide-react";

interface GradeClass {
  gradeNo: number;
  classNo: number;
}

interface Student {
  stuNo: string;
  stuName: string;
  campus: string;
  gradeClassName: string;
}

interface PageResult {
  content: Student[];
  pageNo: number;
  pageSize: number;
  totalPages: number;
  totalElements: number;
}

function RecordsContent() {
  const router = useRouter();
  const searchParams = useSearchParams();

  // 状态管理
  const [loading, setLoading] = useState(true);
  const [searching, setSearching] = useState(false);
  const [gradeClasses, setGradeClasses] = useState<GradeClass[]>([]);
  const [students, setStudents] = useState<PageResult | null>(null);
  const [initialized, setInitialized] = useState(false);

  // 表单状态
  const [selectedGrade, setSelectedGrade] = useState<string>("");
  const [selectedClass, setSelectedClass] = useState<string>("");
  const [keyword, setKeyword] = useState("");

  // 分页状态
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  // 计算可用的年级和班级
  const availableGrades = Array.from(
    new Set(gradeClasses.map((gc) => gc.gradeNo))
  ).sort((a, b) => b - a);

  const availableClasses = selectedGrade
    ? gradeClasses
        .filter((gc) => gc.gradeNo === parseInt(selectedGrade))
        .map((gc) => gc.classNo)
        .sort((a, b) => a - b)
    : [];

  // 初始化：加载班级列表和URL参数（只在组件挂载时执行一次）
  useEffect(() => {
    document.title = "学生查询 - 应急协同平台";
    
    const initializePage = async () => {
      setLoading(true);

      // 先从 URL 读取参数
      const grade = searchParams.get("grade");
      const classNo = searchParams.get("class");
      const kw = searchParams.get("keyword");
      const page = searchParams.get("page");
      const size = searchParams.get("size");

      let initialGrade = grade || "";
      let initialClass = classNo || "";
      let initialKeyword = kw || "";
      let initialPage = page ? parseInt(page) : 1;
      let initialSize = size ? parseInt(size) : 20;

      // 获取可访问的班级列表
      const classesResponse = await get<GradeClass[]>("/students/classes");
      if (classesResponse.data) {
        setGradeClasses(classesResponse.data);

        // 如果 URL 没有参数，且只有一个年级，自动选中
        if (!grade) {
          const grades = Array.from(new Set(classesResponse.data.map((gc) => gc.gradeNo)));
          if (grades.length === 1) {
            initialGrade = grades[0].toString();

            // 如果该年级只有一个班级，自动选中
            const classes = classesResponse.data
              .filter((gc) => gc.gradeNo === grades[0])
              .map((gc) => gc.classNo);
            if (classes.length === 1) {
              initialClass = classes[0].toString();
            }
          }
        }
      }

      // 设置状态
      setSelectedGrade(initialGrade);
      setSelectedClass(initialClass);
      setKeyword(initialKeyword);
      setCurrentPage(initialPage);
      setPageSize(initialSize);

      // 标记初始化完成
      setInitialized(true);
      setLoading(false);
    };

    initializePage();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // 只在组件挂载时执行一次

  // 当初始化完成后执行搜索
  useEffect(() => {
    if (initialized) {
      performSearch();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialized]);

  const performSearch = async (options?: {
    page?: number;
    size?: number;
  }) => {
    setSearching(true);

    const params = new URLSearchParams();

    const searchPage = options?.page ?? currentPage;
    const searchSize = options?.size ?? pageSize;

    if (selectedGrade && selectedGrade !== "all") params.append("gradeNo", selectedGrade);
    if (selectedClass && selectedClass !== "all") params.append("classNo", selectedClass);
    if (keyword.trim()) params.append("keyword", keyword.trim());
    params.append("page", (searchPage - 1).toString());
    params.append("size", searchSize.toString());
    params.append("sort", "stuNo,asc");

    const response = await get<PageResult>(`/students?${params.toString()}`);
    if (response.data) {
      setStudents(response.data);
    }

    // 更新 URL，传入实际使用的值
    updateURL(searchPage, searchSize);

    setSearching(false);
  };

  const updateURL = (
    page: number = currentPage,
    size: number = pageSize
  ) => {
    const params = new URLSearchParams();
    if (selectedGrade && selectedGrade !== "all") params.append("grade", selectedGrade);
    if (selectedClass && selectedClass !== "all") params.append("class", selectedClass);
    if (keyword.trim()) params.append("keyword", keyword.trim());
    if (page > 1) params.append("page", page.toString());
    if (size !== 20) params.append("size", size.toString());

    const queryString = params.toString();
    const newUrl = queryString ? `/records?${queryString}` : "/records";
    router.push(newUrl, { scroll: false });
  };

  const handleGradeChange = (value: string) => {
    setSelectedGrade(value);
    setSelectedClass(""); // 清空班级选择

    // 如果该年级只有一个班级，自动选中
    const classes = gradeClasses
      .filter((gc) => gc.gradeNo === parseInt(value))
      .map((gc) => gc.classNo);
    if (classes.length === 1) {
      setSelectedClass(classes[0].toString());
    }
  };

  const handlePageChange = (newPage: number) => {
    if (newPage < 1 || !students || newPage > students.totalPages) return;
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
    if (students && currentPage > students.totalPages) {
      setCurrentPage(students.totalPages);
      performSearch({ page: students.totalPages });
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
        <div className="container mx-auto px-4 py-4 sm:p-6 space-y-4 sm:space-y-6">
          {/* 页面标题 */}
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold text-foreground">学生查询</h1>
          </div>

          {/* 查询表单 */}
          <Card className="p-4 sm:p-6">
            <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
              {/* 年级选择 */}
              <div className="space-y-2">
                <Label htmlFor="grade">年级</Label>
                <Select value={selectedGrade} onValueChange={handleGradeChange}>
                  <SelectTrigger id="grade">
                    <SelectValue placeholder="请选择年级" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">全部年级</SelectItem>
                    {availableGrades.map((grade) => (
                      <SelectItem key={grade} value={grade.toString()}>
                        {grade}届
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* 班级选择 */}
              <div className="space-y-2">
                <Label htmlFor="class">班级</Label>
                <Select
                  value={selectedClass}
                  onValueChange={setSelectedClass}
                  disabled={!selectedGrade || selectedGrade === "all"}
                >
                  <SelectTrigger id="class">
                    <SelectValue placeholder="请选择班级" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">全部班级</SelectItem>
                    {availableClasses.map((classNo) => (
                      <SelectItem key={classNo} value={classNo.toString()}>
                        {classNo}班
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* 关键词输入 */}
              <div className="space-y-2 md:col-span-2">
                <Label htmlFor="keyword">关键词</Label>
                <div className="flex gap-2">
                  <Input
                    id="keyword"
                    placeholder="支持通过学号、姓名、手机号、父母姓名、父母手机号搜索"
                    className="placeholder:text-xs"
                    value={keyword}
                    onChange={(e) => setKeyword(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === "Enter") {
                        setCurrentPage(1);
                        performSearch({ page: 1 });
                      }
                    }}
                  />
                  <Button onClick={() => { setCurrentPage(1); performSearch({ page: 1 }); }} disabled={searching}>
                    {searching ? <Loader2 className="h-4 w-4 animate-spin" /> : "查询"}
                  </Button>
                </div>
              </div>
            </div>
          </Card>

          {/* 数据表格 */}
          <Card>
            <div className="overflow-x-auto">
              <Table className="min-w-[500px] [&_tr>*:first-child]:pl-4 sm:[&_tr>*:first-child]:pl-6 [&_tr>*:last-child]:pr-4 sm:[&_tr>*:last-child]:pr-6">
                <TableHeader>
                  <TableRow>
                    <TableHead>学号</TableHead>
                    <TableHead>班级</TableHead>
                    <TableHead>姓名</TableHead>
                    <TableHead>校区</TableHead>
                    <TableHead className="text-center"></TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {searching ? (
                    <TableRow>
                      <TableCell colSpan={5} className="text-center py-8">
                        <Loader2 className="h-6 w-6 animate-spin mx-auto" />
                      </TableCell>
                    </TableRow>
                  ) : students && students.content.length > 0 ? (
                    students.content.map((student) => (
                      <TableRow key={student.stuNo}>
                        <TableCell className="font-mono">{student.stuNo}</TableCell>
                        <TableCell>{student.gradeClassName}</TableCell>
                        <TableCell>{student.stuName}</TableCell>
                        <TableCell>{student.campus}</TableCell>
                        <TableCell className="text-center">
                          <Button 
                            variant="outline" 
                            size="sm"
                            onClick={() => router.push(`/archives/${student.stuNo}`)}
                          >
                            详情
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))
                  ) : (
                    <TableRow>
                      <TableCell colSpan={5} className="text-center py-8 text-muted-foreground">
                        暂无数据
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </div>

            {/* 分页组件 */}
            {students && students.content.length > 0 && (
              <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 px-4 sm:px-6 py-4 border-t">
                <div className="flex items-center gap-2">
                  <span className="text-sm text-muted-foreground">每页显示</span>
                  <Select value={pageSize.toString()} onValueChange={handlePageSizeChange}>
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
                    共 {students.totalElements} 条
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
                      max={students.totalPages}
                      value={currentPage}
                      onChange={handlePageInputChange}
                      onBlur={handlePageInputBlur}
                      className="w-16 text-center [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none [-moz-appearance:textfield]"
                    />
                    <span className="text-sm text-muted-foreground">
                      / {students.totalPages}
                    </span>
                  </div>

                  <Button
                    variant="outline"
                    size="icon"
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage >= students.totalPages}
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

export default function RecordsPage() {
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
      <RecordsContent />
    </Suspense>
  );
}
