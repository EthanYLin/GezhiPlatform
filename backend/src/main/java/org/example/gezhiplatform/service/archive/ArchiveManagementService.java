package org.example.gezhiplatform.service.archive;

import jakarta.transaction.Transactional;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.archive.Archive;
import org.example.gezhiplatform.exception.NotFoundException;
import org.example.gezhiplatform.repository.StudentRepository;
import org.springframework.stereotype.Service;

/**
 * 档案管理服务
 * 仅用于对档案进行管理员级的操作，如删除档案、重置档案等。
 * 该服务类【只面向管理员权限】
 * 用户查询档案请使用 {@link ArchiveQueryService}，
 * 用户更新档案请使用 {@link ArchiveUpdateService}
 * 用户导出档案请使用 {@link ArchiveExportService}
 */
@Service
public class ArchiveManagementService {

    private final StudentRepository studentRepository;

    public ArchiveManagementService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    /**
     * 重置指定学号学生的档案(可用于初始化学生档案或清空学生档案数据)
     * @param stuNo 学生学号
     * @throws NotFoundException 当学号对应的学生不存在时抛出
     */
    @Transactional
    public void resetArchive(String stuNo) throws NotFoundException {
        Student student = studentRepository.findByStuNo(stuNo).orElseThrow(
            () -> new NotFoundException("学号为: " + stuNo + " 的学生不存在，无法重置档案。")
        );
        Archive emptyArchive = new Archive();
        emptyArchive.setStudent(student);
        student.setArchive(emptyArchive);
        studentRepository.save(student);
    }

    /**
     * 批量重置学生档案
     * @param stuNos 学生学号列表
     * @throws NotFoundException 当任一学号对应的学生不存在时抛出
     * @see ArchiveManagementService#resetArchive(String)
     */
    @Transactional
    public void resetArchive(Iterable<String> stuNos) throws NotFoundException {
        for (String stuNo : stuNos) {
            resetArchive(stuNo);
        }
    }
}
