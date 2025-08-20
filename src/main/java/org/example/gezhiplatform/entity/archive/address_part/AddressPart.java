package org.example.gezhiplatform.entity.archive.address_part;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.lang.Nullable;

/**
 * 学生档案 - 地址信息部分
 */
@Entity
@Data
public class AddressPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // ID(由数据库自增)

    @OneToOne(cascade = CascadeType.ALL)
    @Nullable
    private Address domicileAddress; // 户籍地址

    @OneToOne(cascade = CascadeType.ALL)
    @Nullable
    private Address currentAddress; // 现居地址

}
