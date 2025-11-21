package org.example.gezhiplatform.service.metadata;

/**
 * 可识别接口
 * 用于标识具有唯一标识符的实体
 */
public interface Identifiable {
    
    /**
     * 获取实体的唯一标识符
     * @return 实体ID
     */
    Object getId();
    
}
