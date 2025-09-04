package org.example.gezhiplatform.repository;

import org.example.gezhiplatform.entity.GradeClass;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.archive.Archive;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
    @Query("SELECT DISTINCT s.gradeClass.gradeNo FROM Student s WHERE s.gradeClass.gradeNo IS NOT NULL")
    List<Integer> findAllGrades(); // 查询所有年级

    @Query("SELECT DISTINCT s.gradeClass FROM Student s WHERE s.gradeClass.gradeNo = :gradeNo AND s.gradeClass.classNo IS NOT NULL")
    List<GradeClass> findGradeClassesByGrade(@NotNull @Param("gradeNo") Integer gradeNo); // 查询指定年级的所有班级

    @Query("SELECT DISTINCT s.gradeClass FROM Student s WHERE s.gradeClass IS NOT NULL")
    List<GradeClass> findAllGradeClasses(); // 查询所有年级-班级

    Optional<Student> findByStuNo(@NotNull String stuNo);

    Page<Student> findByGradeClass_GradeNo(@NotNull Integer gradeClassGradeNo, @NotNull Pageable pageable);

    Page<Student> findByGradeClass(@NotNull GradeClass gradeClass, @NotNull Pageable pageable);

    boolean existsByStuNo(@NotNull String stuNo);

    List<Student> findStudentsByStuNoIn(@NotNull Collection<String> stuNos);

    interface StuNoOnly {@NotNull String getStuNo();}  // 仅返回学号的投影接口
    List<StuNoOnly> findByStuNoIn(@NotNull Collection<String> stuNos);

    interface ArchiveOnly { Archive getArchive(); }
    Optional<ArchiveOnly> findArchiveByStuNo(String stuNo);
}
