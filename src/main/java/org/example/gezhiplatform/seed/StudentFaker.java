package org.example.gezhiplatform.seed;

import org.example.gezhiplatform.entity.Student;
import org.example.gezhiplatform.entity.enums.Campus;

public class StudentFaker {
    public static Student of(PersonalInfoFaker faker, Campus campus, int gradeNo, int classNo, int seatNo) {
        String stuNo = String.format("%s%02d%02d", String.valueOf(gradeNo).substring(2), classNo, seatNo);
        String stuName = faker.name();
        return new Student(stuNo, stuName, gradeNo, classNo, campus);
    }
}
