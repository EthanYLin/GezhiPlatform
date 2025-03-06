package org.example.gezhiplatform.entity.enums;

/**
 * 上海市行政区(枚举类)
 */
public enum District {
    HUANGPU("黄浦区"),
    XUHUI("徐汇区"),
    CHANGNING("长宁区"),
    JINGAN("静安区"),
    PUTUO("普陀区"),
    HONGKOU("虹口区"),
    YANGPU("杨浦区"),
    PUDONG("浦东新区"),
    MINHANG("闵行区"),
    BAOSHAN("宝山区"),
    JIADING("嘉定区"),
    JINSHAN("金山区"),
    SONGJIANG("松江区"),
    QINGPU("青浦区"),
    FENGXIAN("奉贤区"),
    CHONGMING("崇明区"),
    OTHER("其他省市");

    private final String name;

    District(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
