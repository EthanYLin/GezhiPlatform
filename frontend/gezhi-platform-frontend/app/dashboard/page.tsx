"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Navbar } from "@/components/navbar";
import { useUser } from "@/contexts/user-context";
import { get } from "@/lib/api-client";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { 
  GraduationCap,
  Users, 
  FileText, 
  Settings,
  Search,
  Loader2,
  AlertCircle,
  ShieldAlert
} from "lucide-react";

interface StudentData {
  content: Array<{
    stuNo: string;
    stuName: string;
    campus: string;
    gradeClassName: string;
  }>;
  totalElements: number;
}

interface GradeClass {
  gradeNo: number;
  classNo: number;
}

export default function DashboardPage() {
  const router = useRouter();
  const { profile } = useUser();
  const [loading, setLoading] = useState(true);
  const [studentCount, setStudentCount] = useState<number>(0);
  const [classCount, setClassCount] = useState<number>(0);
  const [singleStudent, setSingleStudent] = useState<{
    stuNo: string;
    stuName: string;
  } | null>(null);
  const [quickSearchKeyword, setQuickSearchKeyword] = useState("");

  const isSuperAdmin = profile?.roles.includes("超级管理员");

  useEffect(() => {
    document.title = "工作台 - 应急协同平台";
    fetchDashboardData();
  }, []);

  const fetchDashboardData = async () => {
    setLoading(true);

    // 获取学生档案数量
    const studentsResponse = await get<StudentData>("/students?size=1");
    if (studentsResponse.data) {
      setStudentCount(studentsResponse.data.totalElements);
      
      // 如果只有一个学生，保存学生信息
      if (studentsResponse.data.totalElements === 1 && studentsResponse.data.content.length > 0) {
        setSingleStudent({
          stuNo: studentsResponse.data.content[0].stuNo,
          stuName: studentsResponse.data.content[0].stuName,
        });
      }
      
      // 如果档案数量大于1，获取班级数量
      if (studentsResponse.data.totalElements > 1) {
        const classesResponse = await get<GradeClass[]>("/students/classes");
        if (classesResponse.data) {
          setClassCount(classesResponse.data.length);
        }
      }
    }

    setLoading(false);
  };

  // 处理快速查询
  const handleQuickSearch = () => {
    const keyword = quickSearchKeyword.trim();
    if (keyword) {
      router.push(`/records?keyword=${encodeURIComponent(keyword)}`);
    } else {
      router.push("/records");
    }
  };

  // 欢迎卡片（始终显示）
  const WelcomeCard = ({ count }: { count: number }) => (
    <Card
      className={
        `row-span-2 bg-gradient-to-br from-blue-500 to-cyan-600 text-white border-0 shadow-xl flex flex-col justify-center ` +
        (count === 1
          ? "col-span-2 lg:col-span-4"
          : "col-span-2 lg:col-span-3")
      }
    >
      <CardHeader>
        <CardTitle className="text-3xl">
          欢迎回来，{profile?.name || "用户"}
        </CardTitle>
        <CardDescription className="text-blue-50/90">
          格致中学应急事件处置协同平台
        </CardDescription>
      </CardHeader>
      <CardContent>
        {count > 1 && (
          <Button asChild variant="secondary" size="lg" className="bg-white text-blue-600 hover:bg-blue-50">
            <Link href="/records">
              <span className="font-bold text-md">档案查询与更新</span>
            </Link>
          </Button>
        )}
        {count === 1 && (
          <Button asChild variant="secondary" size="lg" className="bg-white text-blue-600 hover:bg-blue-50">
            <Link href="/records">
              <span className="font-bold text-md">维护我的档案</span>
            </Link>
          </Button>
        )}
      </CardContent>
    </Card>
  );

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
      <div className="min-h-[calc(100vh-4rem)] bg-gradient-to-br from-slate-50 to-slate-100 dark:from-slate-900 dark:to-slate-800">
        <div className="container mx-auto p-6">
          
          {/* 档案数为 0 的布局 */}
          {studentCount === 0 && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 auto-rows-[140px]">
              <WelcomeCard count={studentCount} />
              
              {/* 无权限提示卡片 */}
              <Card className="col-span-2 lg:col-span-1 row-span-2 flex flex-col items-center justify-center text-center hover:shadow-lg transition-shadow">
                <CardContent className="pt-6">
                  <ShieldAlert className="h-12 w-12 text-muted-foreground mx-auto mb-4" />
                  <CardTitle className="text-xl mb-2">您无法查看学生档案</CardTitle>
                  <p className="text-sm text-muted-foreground">
                    管理员未给您配置角色，请联系管理员。
                  </p>
                </CardContent>
              </Card>
            </div>
          )}

          {/* 档案数为 1 的布局 */}
          {studentCount === 1 && singleStudent && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 auto-rows-[140px]">
              <WelcomeCard count={studentCount} />
            </div>
          )}

          {/* 档案数大于 1 的布局 */}
          {studentCount > 1 && (
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 auto-rows-[140px]">
              <WelcomeCard count={studentCount} />
              
              {/* 可查看的班级数 */}
              <Card className="col-span-1 hover:shadow-lg transition-shadow">
                <CardHeader className="pb-2">
                  <CardTitle className="text-sm flex items-center gap-2">
                    <GraduationCap className="h-4 w-4" />
                    可查看的班级数
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="text-3xl font-bold">{classCount}</div>
                </CardContent>
              </Card>
              
              {/* 可查看的档案数 */}
              <Card className="col-span-1 hover:shadow-lg transition-shadow">
                <CardHeader className="pb-2">
                  <CardTitle className="text-sm flex items-center gap-2">
                    <Users className="h-4 w-4" />
                    可查看的档案数
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="text-3xl font-bold">{studentCount}</div>
                </CardContent>
              </Card>

              {/* 快速查询卡片 */}
              <Card className={`row-span-1 hover:shadow-lg transition-shadow flex flex-col justify-center ${isSuperAdmin ? 'col-span-2' : 'col-span-2 lg:col-span-4'}`}>
                <CardHeader className="pb-1">
                  <CardTitle className="text-base">快速查询</CardTitle>
                </CardHeader>
                <CardContent className="pt-0">
                  <div className="flex gap-2">
                    <div className="flex-1 relative">
                      <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                      {/* 小屏幕显示简短文本 */}
                      <Input
                        placeholder="学号、姓名、手机号、父母姓名、父母手机号"
                        className="pl-9 lg:hidden"
                        value={quickSearchKeyword}
                        onChange={(e) => setQuickSearchKeyword(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === "Enter") {
                            handleQuickSearch();
                          }
                        }}
                      />
                      {/* 大屏幕显示完整文本 */}
                      <Input
                        placeholder="支持通过学号、姓名、手机号、父母姓名、父母手机号搜索"
                        className="pl-9 hidden lg:block"
                        value={quickSearchKeyword}
                        onChange={(e) => setQuickSearchKeyword(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === "Enter") {
                            handleQuickSearch();
                          }
                        }}
                      />
                    </div>
                    <Button onClick={handleQuickSearch}>查询</Button>
                  </div>
                </CardContent>
              </Card>

              {/* 数据维护卡片（仅超级管理员） */}
              {isSuperAdmin && (
                <Card className="col-span-2 row-span-1 hover:shadow-lg transition-shadow flex flex-col justify-center">
                  <CardHeader className="pb-1">
                    <CardTitle className="text-base flex items-center gap-2">
                      <Settings className="h-4 w-4" />
                      数据维护
                    </CardTitle>
                  </CardHeader>
                  <CardContent className="pt-0">
                    <div className="flex flex-wrap gap-2">
                      <Button asChild variant="outline" size="sm">
                        <Link href="/students">学生维护</Link>
                      </Button>
                      <Button asChild variant="outline" size="sm">
                        <Link href="/users">用户维护</Link>
                      </Button>
                      <Button asChild variant="outline" size="sm">
                        <Link href="/roles">权限组维护</Link>
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              )}
            </div>
          )}

        </div>
      </div>
    </>
  );
}

