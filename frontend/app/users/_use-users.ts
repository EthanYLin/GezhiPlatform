import {useCallback, useEffect, useState} from "react";
import {useRouter, useSearchParams} from "next/navigation";
import type {PageResult, SortState, User, UserFilters} from "./_types";
import {searchUsers} from "./_api";

const DEFAULT_FILTERS: UserFilters = {
  keyword: "",
  isLocked: "all",
  isEnabled: "all",
  roleType: "all",
};

export function useUsers() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [loading, setLoading] = useState(true);
  const [searching, setSearching] = useState(false);
  const [initialized, setInitialized] = useState(false);

  const [filters, setFilters] = useState<UserFilters>({ ...DEFAULT_FILTERS });
  const [users, setUsers] = useState<PageResult<User> | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const [sort, setSort] = useState<SortState>({
    field: "id",
    direction: "asc",
  });

  const [selectedRows, setSelectedRows] = useState<Set<number>>(new Set());

  // --- initialization ---
  useEffect(() => {
    document.title = "用户维护 - 应急协同平台";

    const keyword = searchParams.get("keyword") || "";
    const isLocked = searchParams.get("isLocked") || "all";
    const isEnabled = searchParams.get("isEnabled") || "all";
    const roleType = searchParams.get("roleType") || "all";
    const page = searchParams.get("page");
    const size = searchParams.get("size");
    const sortParam = searchParams.get("sort");

    setFilters({ keyword, isLocked, isEnabled, roleType });
    setCurrentPage(page ? parseInt(page) : 1);
    setPageSize(size ? parseInt(size) : 20);

    if (sortParam) {
      const [field, dir] = sortParam.split(",");
      setSort({
        field: field as SortState["field"],
        direction: (dir || "asc") as SortState["direction"],
      });
    }

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

  // --- core search ---
  const performSearch = useCallback(
    async (overrides?: {
      page?: number;
      size?: number;
      sort?: SortState;
      filters?: UserFilters;
    }) => {
      setSearching(true);
      setSelectedRows(new Set());

      const searchPage = overrides?.page ?? currentPage;
      const searchSize = overrides?.size ?? pageSize;
      const searchSort = overrides?.sort ?? sort;
      const searchFilters = overrides?.filters ?? filters;

      const res = await searchUsers({
        filters: searchFilters,
        page: searchPage - 1,
        size: searchSize,
        sort: searchSort,
      });

      if (res.data) {
        setUsers(res.data);
      }

      updateURL(searchPage, searchSize, searchSort, searchFilters);
      setSearching(false);
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [filters, currentPage, pageSize, sort],
  );

  const updateURL = (
    page: number = currentPage,
    size: number = pageSize,
    s: SortState = sort,
    f: UserFilters = filters,
  ) => {
    const params = new URLSearchParams();
    if (f.keyword) params.append("keyword", f.keyword);
    if (f.isLocked !== "all") params.append("isLocked", f.isLocked);
    if (f.isEnabled !== "all") params.append("isEnabled", f.isEnabled);
    if (f.roleType !== "all") params.append("roleType", f.roleType);
    if (page > 1) params.append("page", page.toString());
    if (size !== 20) params.append("size", size.toString());
    if (s.field !== "id" || s.direction !== "asc")
      params.append("sort", `${s.field},${s.direction}`);

    const qs = params.toString();
    router.push(qs ? `/users?${qs}` : "/users", { scroll: false });
  };

  // --- filter handlers ---
  const updateFilter = <K extends keyof UserFilters>(key: K, value: UserFilters[K]) => {
    setFilters((prev) => ({ ...prev, [key]: value }));
  };

  const handleSearch = () => {
    setCurrentPage(1);
    performSearch({ page: 1, filters });
  };

  const handleResetFilters = () => {
    const reset = { ...DEFAULT_FILTERS };
    setFilters(reset);
    setCurrentPage(1);
    performSearch({ page: 1, filters: reset });
  };

  // --- pagination handlers ---
  const handlePageChange = (newPage: number) => {
    if (newPage < 1 || !users || newPage > users.totalPages) return;
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
    if (users && currentPage > users.totalPages) {
      setCurrentPage(users.totalPages);
      performSearch({ page: users.totalPages });
    } else if (currentPage < 1) {
      setCurrentPage(1);
      performSearch({ page: 1 });
    } else {
      performSearch({ page: currentPage });
    }
  };

  // --- sort ---
  const handleSortChange = (field: SortState["field"]) => {
    const newSort: SortState =
      sort.field === field
        ? { field, direction: sort.direction === "asc" ? "desc" : "asc" }
        : { field, direction: "asc" };
    setSort(newSort);
    setCurrentPage(1);
    performSearch({ page: 1, sort: newSort });
  };

  // --- selection ---
  const toggleRow = (userId: number) => {
    setSelectedRows((prev) => {
      const next = new Set(prev);
      if (next.has(userId)) next.delete(userId);
      else next.add(userId);
      return next;
    });
  };

  const toggleAllRows = () => {
    if (!users) return;
    const allOnPage = users.content.map((u) => u.id);
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
    setCurrentPage,
  };
}
