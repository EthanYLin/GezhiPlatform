package org.example.gezhiplatform.entity.archive.address_part;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonMerge;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import lombok.Data;
import org.example.gezhiplatform.annotation.JsonTitle;
import org.springframework.lang.Nullable;

/**
 * 学生档案 - 地址信息部分
 */
@Entity
@Data
@JsonPropertyOrder({"domicileAddress", "currentAddress"})
public class AddressPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonIgnore
    private Long id; // ID(由数据库自增)

    @OneToOne(cascade = CascadeType.ALL)
    @Nullable
    @Valid
    @JsonMerge
    @JsonTitle("户籍地址")
    private DomicileAddress domicileAddress; // 户籍地址

    @OneToOne(cascade = CascadeType.ALL)
    @Nullable
    @Valid
    @JsonMerge
    @JsonTitle("现居地址")
    private CurrentAddress currentAddress; // 现居地址

}
