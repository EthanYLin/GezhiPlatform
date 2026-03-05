import { useEffect, useState, useCallback } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import type {
  Student,
  PageResult,
  SortState,
} from "./_types";
import { fetchGrades, fetchClasses, fetchStudents } from "./_api";
import type { GradeClass } from "./_types";

export function useStudents() {
  const router = useRouter();
  const searchParams = useSearchParams();

  const [loading, setLoading] = useState(true);
  const [searching, setSearching] = useState(false);
  const [initialized, setInitialized] = useState(false);

  const [grades, setGrades] = useState<number[]>([]);
  const [classes, setClasses] = useState<GradeClass[]>([]);

  const [selectedGrade, setSelectedGrade] = useState("");
  const [selectedClass, setSelectedClass] = useState("");

  const [students, setStudents] = useState<PageResult<Student> | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize, setPageSize] = useState(20);

  const [sort, setSort] = useState<SortState>({
    field: "stuNo",
    direction: "asc",
  });

  const [selectedRows, setSelectedRows] = useState<Set<string>>(new Set());

  // --- initialization ---
  useEffect(() => {
    document.title = "学生维护 - 应急协同平台";

    const init = async () => {
      setLoading(true);

      const grade = searchParams.get("grade") || "";
      const cls = searchParams.get("class") || "";
      const page = searchParams.get("page");
      const size = searchParams.get("size");
      const sortParam = searchParams.get("sort");

      setSelectedGrade(grade);
      setSelectedClass(cls);
      setCurrentPage(page ? parseInt(page) : 1);
      setPageSize(size ? parseInt(size) : 20);

      if (sortParam) {
        const [field, dir] = sortParam.split(",");
        setSort({
          field: field as SortState["field"],
          direction: (dir || "asc") as SortState["direction"],
        });
      }

      const gradesRes = await fetchGrades();
      if (gradesRes.data) {
        setGrades(gradesRes.data);
      }

      if (grade) {
        const classesRes = await fetchClasses(parseInt(grade));
        if (classesRes.data) {
          setClasses(classesRes.data);
        }
      }

      setInitialized(true);
      setLoading(false);
    };

    init();
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
      grade?: string;
      cls?: string;
    }) => {
      setSearching(true);
      setSelectedRows(new Set());

      const searchPage = overrides?.page ?? currentPage;
      const searchSize = overrides?.size ?? pageSize;
      const searchSort = overrides?.sort ?? sort;
      const searchGrade = overrides?.grade !== undefined ? overrides.grade : selectedGrade;
      const searchCls = overrides?.cls !== undefined ? overrides.cls : selectedClass;

      const gradeNo =
        searchGrade && searchGrade !== "all"
          ? parseInt(searchGrade)
          : undefined;
      const classNo =
        searchCls && searchCls !== "all"
          ? parseInt(searchCls)
          : undefined;

      const res = await fetchStudents({
        gradeNo,
        classNo,
        page: searchPage - 1,
        size: searchSize,
        sort: `${searchSort.field},${searchSort.direction}`,
      });

      if (res.data) {
        setStudents(res.data);
      }

      updateURL(searchPage, searchSize, searchSort, searchGrade, searchCls);
      setSearching(false);
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [selectedGrade, selectedClass, currentPage, pageSize, sort]
  );

  const updateURL = (
    page: number = currentPage,
    size: number = pageSize,
    s: SortState = sort,
    grade: string = selectedGrade,
    cls: string = selectedClass
  ) => {
    const params = new URLSearchParams();
    if (grade && grade !== "all") params.append("grade", grade);
    if (cls && cls !== "all") params.append("class", cls);
    if (page > 1) params.append("page", page.toString());
    if (size !== 20) params.append("size", size.toString());
    if (s.field !== "stuNo" || s.direction !== "asc")
      params.append("sort", `${s.field},${s.direction}`);

    const qs = params.toString();
    router.push(qs ? `/students?${qs}` : "/students", { scroll: false });
  };

  // --- handlers ---
  const handleGradeChange = async (value: string) => {
    setSelectedGrade(value);
    setSelectedClass("");
    setClasses([]);

    if (value && value !== "all") {
      const res = await fetchClasses(parseInt(value));
      if (res.data) {
        setClasses(res.data);
      }
    }
  };

  const handleClassChange = (value: string) => {
    setSelectedClass(value);
  };

  const handleSearch = () => {
    setCurrentPage(1);
    performSearch({ page: 1, grade: selectedGrade, cls: selectedClass });
  };

  const handleResetFilters = () => {
    setSelectedGrade("");
    setSelectedClass("");
    setClasses([]);
    setCurrentPage(1);
    performSearch({ page: 1, grade: "", cls: "" });
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
  const toggleRow = (stuNo: string) => {
    setSelectedRows((prev) => {
      const next = new Set(prev);
      if (next.has(stuNo)) next.delete(stuNo);
      else next.add(stuNo);
      return next;
    });
  };

  const toggleAllRows = () => {
    if (!students) return;
    const allOnPage = students.content.map((s) => s.stuNo);
    const allSelected = allOnPage.every((no) => selectedRows.has(no));
    if (allSelected) {
      setSelectedRows((prev) => {
        const next = new Set(prev);
        allOnPage.forEach((no) => next.delete(no));
        return next;
      });
    } else {
      setSelectedRows((prev) => {
        const next = new Set(prev);
        allOnPage.forEach((no) => next.add(no));
        return next;
      });
    }
  };

  const clearSelection = () => setSelectedRows(new Set());

  const refreshList = () => performSearch();

  return {
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
    setCurrentPage,
  };
}
