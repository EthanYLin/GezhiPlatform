package org.example.gezhiplatform.seed;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.archive.Archive;
import org.example.gezhiplatform.entity.enums.Campus;
import org.example.gezhiplatform.repository.StudentRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.IntStream;

@Slf4j
@Component
@Profile("seed")
@RequiredArgsConstructor
public class SeedDataRunner implements ApplicationRunner {

    private final ApplicationContext ctx;
    private final Scanner scanner = new Scanner(System.in);
    private final StudentRepository studentRepository;

    private record SeedStudents(int gradeNo, Campus campus, int classNo, int seatNoFrom, int seatNoTo){}
    private final List<SeedStudents> seedStudents = new ArrayList<>();
    @PostConstruct
    private void setSeedStudents() {
        for (int grade = 2026; grade <= 2028; grade++) {
            seedStudents.addAll(List.of(
                new SeedStudents(grade, Campus.HUANGPU, 1, 1, 5),
                new SeedStudents(grade, Campus.HUANGPU, 2, 1, 5),
                new SeedStudents(grade, Campus.HUANGPU, 3, 1, 5),
                new SeedStudents(grade, Campus.FENGXIAN, 9, 1, 5)
            ));
        }
    }

    private void generateStudentAndArchive() {
        seedStudents.forEach(seedStudents -> {
            IntStream.range(seedStudents.seatNoFrom(), seedStudents.seatNoTo() + 1)
                .forEach(seatNo -> {
                    int age = 2025 - seedStudents.gradeNo() + 18;
                    var faker = PersonalInfoFaker.builder().age(age).build();
                    Student student = StudentFaker.of(
                        faker, seedStudents.campus(), seedStudents.gradeNo(), seedStudents.classNo(), seatNo
                    );
                    Archive archive = ArchiveFaker.of(faker);
                    student.setArchive(archive);
                    studentRepository.save(student);
                });
        });
    }

    @Override
    public void run(ApplicationArguments args) {
        log.warn("你正在准备进行模拟数据生成...");

        log.warn("1. 生成以下学生及档案");
        log.warn(seedStudents.toString());

        System.out.println("输入[Y/y]继续...");
        if (!scanner.nextLine().matches("[Yy]")) {
            log.warn("已取消数据生成。");
            return;
        }

        generateStudentAndArchive();
        System.out.println(studentRepository.count());
        
        System.exit(SpringApplication.exit(ctx));
    }


}
