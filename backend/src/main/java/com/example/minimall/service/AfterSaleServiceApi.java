package com.example.minimall.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.minimall.model.AfterSaleService;
import com.example.minimall.model.ServiceRecord;

import java.math.BigDecimal;
import java.util.List;

/** 售后服务 API 接口定义 */
public interface AfterSaleServiceApi {

    /** 创建售后申请 */
    AfterSaleService createAfterSale(AfterSaleService afterSale);

    /** 根据 ID 获取售后申请 */
    AfterSaleService getAfterSaleById(Long id);

    /** 获取指定用户的售后申请 */
    List<AfterSaleService> getAfterSalesByUserId(Long userId);

    /** 获取指定订单的售后申请 */
    List<AfterSaleService> getAfterSalesByOrderId(Long orderId);

    /** 分页查询售后申请 */
    IPage<AfterSaleService> getAfterSalesPage(int page, int size, Integer status);

    /** 分页获取指定用户的售后申请 */
    IPage<AfterSaleService> getAfterSalesByUserIdWithPage(Long userId, int page, int size, Integer status);

    /** 分页获取指定商家的售后申请 */
    IPage<AfterSaleService> getAfterSalesBySellerIdWithPage(Long sellerId, int page, int size, Integer status);

    /**
     * 处理售后申请（**事务**，含状态流转和退款记录）
     *
     * @param id             售后单 ID
     * @param status         目标状态
     * @param serviceResult  处理结果描述
     * @param operatorId     操作人 ID（商家/管理员）
     * @param refundAmount   退款金额（可空，部分退款场景）
     * @return 处理后的售后单
     */
    AfterSaleService processAfterSale(Long id, Integer status, String serviceResult, Long operatorId, BigDecimal refundAmount);

    /** 获取售后流转记录（**审计日志**） */
    List<ServiceRecord> getServiceRecords(Long afterSaleId);

    /** 更新售后申请（**事务**，部分字段更新） */
    AfterSaleService updateAfterSale(Long id, AfterSaleService afterSale);

    /** 删除售后申请（**事务**） */
    void deleteAfterSale(Long id);

    /**
     * 补充售后证据（**事务**）
     * <p>追加 evidence 字段并写流转记录</p>
     */
    AfterSaleService addSupplementaryEvidence(Long id, String evidence, Long userId);

    /**
     * 撤销售后申请（**事务**）
     * <p>仅申请人本人、且未进入处理环节时可撤销</p>
     */
    AfterSaleService cancelAfterSale(Long id, Long userId);

    /**
     * 更新退货物流信息（**事务**）
     * <p>用户寄回商品时填写</p>
     */
    AfterSaleService updateReturnLogistics(Long id, String logisticsCompany, String logisticsNo, Long userId);
}