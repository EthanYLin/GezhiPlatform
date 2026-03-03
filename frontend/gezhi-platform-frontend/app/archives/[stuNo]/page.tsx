"use client";

import {Navbar} from "@/components/navbar";
import {Card, CardContent, CardHeader, CardTitle} from "@/components/ui/card";
import {Label} from "@/components/ui/label";
import {Button} from "@/components/ui/button";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import {AlertCircle, ArrowLeft, Download, Loader2, Save,} from "lucide-react";
import {Alert, AlertDescription, AlertTitle} from "@/components/ui/alert";
import {useArchiveForm} from "./_use-archive-form";
import {DynamicForm} from "./_components/field-renderer";

export default function StudentArchivePage() {
  const {
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
  } = useArchiveForm();

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

          {/* 公告 */}
          {permissions?.displayCaption && (
            <Alert>
              <AlertCircle className="h-4 w-4" />
              <AlertTitle>公告</AlertTitle>
              <AlertDescription>{permissions.displayCaption}</AlertDescription>
            </Alert>
          )}

          {/* 操作按钮组 */}
          <div className="flex gap-3">
            <Button variant="outline" onClick={handleGoBack}>
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
            <Button onClick={handleSave} disabled={saveLoading}>
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
                    <Label className="text-sm text-muted-foreground">
                      学号
                    </Label>
                    <p className="text-base font-medium">
                      {studentInfo.stuNo}
                    </p>
                  </div>
                  <div className="space-y-1">
                    <Label className="text-sm text-muted-foreground">
                      班级
                    </Label>
                    <p className="text-base font-medium">
                      {studentInfo.gradeClassName}
                    </p>
                  </div>
                  <div className="space-y-1">
                    <Label className="text-sm text-muted-foreground">
                      姓名
                    </Label>
                    <p className="text-base font-medium">
                      {studentInfo.stuName}
                    </p>
                  </div>
                  <div className="space-y-1">
                    <Label className="text-sm text-muted-foreground">
                      校区
                    </Label>
                    <p className="text-base font-medium">
                      {studentInfo.campus}
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}

          {/* 动态表单 */}
          {schema && schema.properties ? (
            <DynamicForm schema={schema} formActions={formActions} />
          ) : (
            <Card>
              <CardContent className="py-12 text-center text-muted-foreground">
                无法加载表单元信息
              </CardContent>
            </Card>
          )}

          {/* 底部保存按钮 */}
          <div className="flex justify-start pb-6">
            <Button onClick={handleSave} disabled={saveLoading} size="lg">
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
        </div>
      </div>

      {/* 错误对话框 */}
      <Dialog open={errorDialogOpen} onOpenChange={closeErrorDialog}>
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
            <Button onClick={closeErrorDialog}>确定</Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
