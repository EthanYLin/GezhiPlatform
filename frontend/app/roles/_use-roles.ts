import { useEffect, useState, useCallback } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import type { PermissionGroup, PageResult } from "./_types";
import { searchPermissionGroups, batchDeletePermissionGroups } from "./_api";
import { toast } from "sonner";

export function useRoles() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [loading, setLoading] = useState(true);
  const [searching, setSearching] = useState(false);
  const [initialized, setInitialized] = useState(false);

  const [keyword, setKeyword] = useState("");
  const [roleType, setRoleType] = useState("");

  const [permissionGroups, setPermissionGroups] =
    useState<PageResult<PermissionGroup> | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const [selectedRows, setSelectedRows] = useState<Set<number>>(new Set());

  useEffect(() => {
    document.title = "权限组维护 - 应急协同平台";

    const kw = searchParams.get("keyword") || "";
    const rt = searchParams.get("roleType") || "";
    const page = searchParams.get("page");
    const size = searchParams.get("size");

    setKeyword(kw);
    setRoleType(rt);
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

  const performSearch = useCallback(
    async (overrides?: {
      page?: number;
      size?: number;
      kw?: string;
      rt?: string;
    }) => {
      setSearching(true);
      setSelectedRows(new Set());

      const searchPage = overrides?.page ?? currentPage;
      const searchSize = overrides?.size ?? pageSize;
      const searchKw = overrides?.kw !== undefined ? overrides.kw : keyword;
      const searchRt = overrides?.rt !== undefined ? overrides.rt : roleType;

      const res = await searchPermissionGroups({
        keyword: searchKw || undefined,
        roleType: searchRt || undefined,
        page: searchPage - 1,
        size: searchSize,
      });

      if (res.data) {
        setPermissionGroups(res.data);
      }

      updateURL(searchPage, searchSize, searchKw, searchRt);
      setSearching(false);
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [keyword, roleType, currentPage, pageSize]
  );

  const updateURL = (
    page: number = currentPage,
    size: number = pageSize,
    kw: string = keyword,
    rt: string = roleType
  ) => {
    const params = new URLSearchParams();
    if (kw) params.append("keyword", kw);
    if (rt) params.append("roleType", rt);
    if (page > 1) params.append("page", page.toString());
    if (size !== 20) params.append("size", size.toString());

    const qs = params.toString();
    router.push(qs ? `/roles?${qs}` : "/roles", { scroll: false });
  };

  const handleSearch = () => {
    setCurrentPage(1);
    performSearch({ page: 1, kw: keyword, rt: roleType });
  };

  const handleReset = () => {
    setKeyword("");
    setRoleType("");
    setCurrentPage(1);
    performSearch({ page: 1, kw: "", rt: "" });
  };

  const handlePageChange = (newPage: number) => {
    if (newPage < 1 || !permissionGroups || newPage > permissionGroups.totalPages)
      return;
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
    if (permissionGroups && currentPage > permissionGroups.totalPages) {
      setCurrentPage(permissionGroups.totalPages);
      performSearch({ page: permissionGroups.totalPages });
    } else if (currentPage < 1) {
      setCurrentPage(1);
      performSearch({ page: 1 });
    } else {
      performSearch({ page: currentPage });
    }
  };

  const handleBatchDelete = async (ids: number[]) => {
    const res = await batchDeletePermissionGroups(ids);
    if (res.status === 204 || res.status === 200) {
      toast.success(`成功删除 ${ids.length} 个权限组`);
      clearSelection();
      performSearch();
    } else {
      toast.error(res.error || "删除失败");
    }
  };

  const toggleRow = (id: number) => {
    setSelectedRows((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const toggleAllRows = () => {
    if (!permissionGroups) return;
    const allOnPage = permissionGroups.content
      .map((g) => g.id!)
      .filter(Boolean);
    const allSelected = allOnPage.every((id) => selectedRows.has(id));
    if (allSelected) {
      setSelectedRows((prev) => {
        const next = new Set(prev);
        allOnPage.forEach((id) => next.delete(id));
        return next;
      });
    } else {
      setSelectedRows((prev) => {
        const next = new Set(prev);
        allOnPage.forEach((id) => next.add(id));
        return next;
      });
    }
  };

  const clearSelection = () => setSelectedRows(new Set());

  const refreshList = () => performSearch();

  return {
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
    setCurrentPage,
  };
}
