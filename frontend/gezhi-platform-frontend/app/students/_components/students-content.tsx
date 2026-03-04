"use client";

import { useState } from "react";
import { Navbar } from "@/components/navbar";
import { Card } from "@/components/ui/card";
import { Loader2 } from "lucide-react";
import { useStudents } from "../_use-students";
import { StudentFilters } from "./student-filters";
import { StudentToolbar } from "./student-toolbar";
import { StudentTable } from "./student-table";
import { StudentPagination } from "./student-pagination";
import { StudentDialog } from "./student-dialog";
import { DeleteConfirmDialog } from "./delete-confirm-dialog";
import { ResetConfirmDialog } from "./reset-confirm-dialog";
import { ImportExcelDialog } from "./import-excel-dialog";

export function StudentsContent() {
  const {
    loading,
    searching,
    grades,
    classes,
    selectedGrade,
    selectedClass,
    students,
    currentPage,
    pageSize,
    sort,
    selectedRows,
    handleGradeChange,
    handleClassChange,
    handleSearch,
    handleResetFilters,
    handlePageChange,
    handlePageSizeChange,
    handlePageInputChange,
    handlePageInputBlur,
    handleSortChange,
    toggleRow,
    toggleAllRows,
    clearSelection,
    refreshList,
  } = useStudents();

  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogMode, setDialogMode] = useState<"add" | "edit">("add");
  const [editStuNo, setEditStuNo] = useState<string | undefined>();

  const [deleteOpen, setDeleteOpen] = useState(false);
  const [resetOpen, setResetOpen] = useState(false);
  const [importOpen, setImportOpen] = useState(false);

  const selectedArray = Array.from(selectedRows);

  const handleAdd = () => {
    setDialogMode("add");
    setEditStuNo(undefined);
    setDialogOpen(true);
  };

  const handleEdit = () => {
    if (selectedArray.length !== 1) return;
    setDialogMode("edit");
    setEditStuNo(selectedArray[0]);
    setDialogOpen(true);
  };

  const handleDelete = () => {
    if (selectedArray.length === 0) return;
    setDeleteOpen(true);
  };

  const handleResetArchives = () => {
    if (selectedArray.length === 0) return;
    setResetOpen(true);
  };

  const handleDialogSuccess = () => {
    clearSelection();
    refreshList();
  };

  const handleDeleteSuccess = () => {
    clearSelection();
    refreshList();
  };

  const handleResetSuccess = () => {
    clearSelection();
    refreshList();
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
            <h1 className="text-3xl font-bold text-foreground">学生维护</h1>
          </div>

          <StudentFilters
            grades={grades}
            classes={classes}
            selectedGrade={selectedGrade}
            selectedClass={selectedClass}
            searching={searching}
            onGradeChange={handleGradeChange}
            onClassChange={handleClassChange}
            onSearch={handleSearch}
            onReset={handleResetFilters}
          />

          <Card>
            <div className="px-6 py-4 border-b">
              <StudentToolbar
                selectedCount={selectedArray.length}
                onAdd={handleAdd}
                onImport={() => setImportOpen(true)}
                onEdit={handleEdit}
                onDelete={handleDelete}
                onReset={handleResetArchives}
              />
            </div>

            <StudentTable
              students={students}
              searching={searching}
              sort={sort}
              selectedRows={selectedRows}
              onSortChange={handleSortChange}
              onToggleRow={toggleRow}
              onToggleAll={toggleAllRows}
            />

            {students && students.content.length > 0 && (
              <StudentPagination
                students={students}
                currentPage={currentPage}
                pageSize={pageSize}
                onPageChange={handlePageChange}
                onPageSizeChange={handlePageSizeChange}
                onPageInputChange={handlePageInputChange}
                onPageInputBlur={handlePageInputBlur}
              />
            )}
          </Card>
        </div>
      </div>

      <StudentDialog
        open={dialogOpen}
        onOpenChange={setDialogOpen}
        mode={dialogMode}
        editStuNo={editStuNo}
        onSuccess={handleDialogSuccess}
      />

      <DeleteConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        stuNos={selectedArray}
        onSuccess={handleDeleteSuccess}
      />

      <ResetConfirmDialog
        open={resetOpen}
        onOpenChange={setResetOpen}
        stuNos={selectedArray}
        onSuccess={handleResetSuccess}
      />

      <ImportExcelDialog
        open={importOpen}
        onOpenChange={setImportOpen}
        onSuccess={handleDialogSuccess}
      />
    </>
  );
}
