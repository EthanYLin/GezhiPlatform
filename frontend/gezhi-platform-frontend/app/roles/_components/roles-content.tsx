"use client";

import { useState } from "react";
import { Navbar } from "@/components/navbar";
import { Card } from "@/components/ui/card";
import { Loader2 } from "lucide-react";
import { useRoles } from "../_use-roles";
import { RoleFilters } from "./role-filters";
import { RoleToolbar } from "./role-toolbar";
import { RoleTable } from "./role-table";
import { RolePagination } from "./role-pagination";
import { RoleDetailSheet } from "./role-detail-sheet";

export function RolesContent() {
  const {
    loading,
    searching,
    keyword,
    setKeyword,
    roleType,
    setRoleType,
    permissionGroups,
    currentPage,
    pageSize,
    selectedRows,
    handleSearch,
    handleReset,
    handlePageChange,
    handlePageSizeChange,
    handlePageInputChange,
    handlePageInputBlur,
    handleBatchDelete,
    toggleRow,
    toggleAllRows,
    clearSelection,
    refreshList,
  } = useRoles();

  const [sheetOpen, setSheetOpen] = useState(false);
  const [editId, setEditId] = useState<number | undefined>();

  const handleAdd = () => {
    setEditId(undefined);
    setSheetOpen(true);
  };

  const handleViewDetail = (id: number) => {
    setEditId(id);
    setSheetOpen(true);
  };

  const handleDelete = () => {
    const ids = Array.from(selectedRows);
    if (ids.length === 0) return;
    handleBatchDelete(ids);
  };

  const handleSheetSuccess = () => {
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
            <h1 className="text-3xl font-bold text-foreground">权限组维护</h1>
          </div>

          <RoleFilters
            keyword={keyword}
            roleType={roleType}
            searching={searching}
            onKeywordChange={setKeyword}
            onRoleTypeChange={setRoleType}
            onSearch={handleSearch}
            onReset={handleReset}
          />

          <Card>
            <div className="px-6 py-4 border-b">
              <RoleToolbar
                selectedCount={selectedRows.size}
                onAdd={handleAdd}
                onDelete={handleDelete}
              />
            </div>

            <RoleTable
              data={permissionGroups}
              searching={searching}
              selectedRows={selectedRows}
              onToggleRow={toggleRow}
              onToggleAll={toggleAllRows}
              onViewDetail={handleViewDetail}
            />

            {permissionGroups && permissionGroups.content.length > 0 && (
              <RolePagination
                data={permissionGroups}
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

      <RoleDetailSheet
        open={sheetOpen}
        onOpenChange={setSheetOpen}
        editId={editId}
        onSuccess={handleSheetSuccess}
      />
    </>
  );
}
