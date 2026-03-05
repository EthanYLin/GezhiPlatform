"use client";

import {useState} from "react";
import {Navbar} from "@/components/navbar";
import {Card} from "@/components/ui/card";
import {Loader2} from "lucide-react";
import {useUsers} from "../_use-users";
import {UserFilters} from "./user-filters";
import {UserToolbar} from "./user-toolbar";
import {UserTable} from "./user-table";
import {UserPagination} from "./user-pagination";
import {UserDetailSheet} from "./user-detail-sheet";
import {NewUserSheet} from "./new-user-sheet";import { ImportUsersDialog } from "./import-users-dialog";import {DeleteConfirmDialog} from "./delete-confirm-dialog";

export function UsersContent() {
  const {
    loading,
    searching,
    filters,
    users,
    currentPage,
    pageSize,
    sort,
    selectedRows,
    updateFilter,
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
  } = useUsers();

  // detail sheet
  const [detailOpen, setDetailOpen] = useState(false);
  const [detailUserId, setDetailUserId] = useState<number | null>(null);

  // new user sheet
  const [newUserOpen, setNewUserOpen] = useState(false);

  // import dialog
  const [importOpen, setImportOpen] = useState(false);

  // delete confirm dialog
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteTargetIds, setDeleteTargetIds] = useState<number[]>([]);

  const selectedArray = Array.from(selectedRows);

  const handleViewDetail = (userId: number) => {
    setDetailUserId(userId);
    setDetailOpen(true);
  };

  const handleAddUser = () => {
    setNewUserOpen(true);
  };

  const handleImport = () => {
    setImportOpen(true);
  };

  const handleBatchDelete = () => {
    if (selectedArray.length === 0) return;
    setDeleteTargetIds(selectedArray);
    setDeleteOpen(true);
  };

  const handleMutationSuccess = () => {
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
            <h1 className="text-3xl font-bold text-foreground">用户维护</h1>
          </div>

          <UserFilters
            filters={filters}
            searching={searching}
            onFilterChange={updateFilter}
            onSearch={handleSearch}
            onReset={handleResetFilters}
          />

          <Card>
            <div className="px-6 py-4 border-b">
              <UserToolbar
                selectedCount={selectedArray.length}
                onAdd={handleAddUser}
                onImport={handleImport}
                onBatchDelete={handleBatchDelete}
              />
            </div>

            <UserTable
              users={users}
              searching={searching}
              sort={sort}
              selectedRows={selectedRows}
              onSortChange={handleSortChange}
              onToggleRow={toggleRow}
              onToggleAll={toggleAllRows}
              onViewDetail={handleViewDetail}
            />

            {users && users.content.length > 0 && (
              <UserPagination
                users={users}
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

      <UserDetailSheet
        open={detailOpen}
        onOpenChange={setDetailOpen}
        userId={detailUserId}
        onUserUpdated={refreshList}
        onUserDeleted={handleMutationSuccess}
      />

      <NewUserSheet
        open={newUserOpen}
        onOpenChange={setNewUserOpen}
        onSuccess={handleMutationSuccess}
      />

      <ImportUsersDialog
        open={importOpen}
        onOpenChange={setImportOpen}
        onSuccess={handleMutationSuccess}
      />

      <DeleteConfirmDialog
        open={deleteOpen}
        onOpenChange={setDeleteOpen}
        userIds={deleteTargetIds}
        onSuccess={handleMutationSuccess}
      />
    </>
  );
}
