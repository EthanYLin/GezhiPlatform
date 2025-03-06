package org.example.gezhiplatform.entity.archive;

import jakarta.persistence.*;
import org.springframework.lang.Nullable;

@Entity
public class AddressPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID(由数据库自增)

    @Embedded
    @Nullable
    private Address domicileAddress; // 户籍地址

    @Embedded
    @Nullable
    private Address currentAddress; // 现居地址

}
