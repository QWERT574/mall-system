package com.example.minimall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.minimall.mapper.AdminInterventionMapper;
import com.example.minimall.mapper.UserMapper;
import com.example.minimall.mapper.OrdersMapper;
import com.example.minimall.mapper.ProductMapper;
import com.example.minimall.mapper.AfterSaleServiceMapper;
import com.example.minimall.model.AdminIntervention;
import com.example.minimall.model.User;
import com.example.minimall.model.Orders;
import com.example.minimall.model.Product;
import com.example.minimall.model.AfterSaleService;
import com.example.minimall.service.AdminInterventionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 管理员介入申请服务实现 */
@Service
public class AdminInterventionServiceImpl implements AdminInterventionService {

    private static final Logger logger = LoggerFactory.getLogger(AdminInterventionServiceImpl.class);

    /** 介入申请Mapper */
    @Autowired
    private AdminInterventionMapper adminInterventionMapper;

    /** 用户Mapper */
    @Autowired
    private UserMapper userMapper;

    /** 订单Mapper */
    @Autowired
    private OrdersMapper ordersMapper;

    /** 商品Mapper */
    @Autowired
    private ProductMapper productMapper;

    /** 售后服务Mapper */
    @Autowired
    private AfterSaleServiceMapper afterSaleServiceMapper;

    /**
     * 为介入申请补全用户/商家/订单/商品/售后单详情（**私有工具方法**）
     * <p>任意子查询失败都会被 catch，仅记录 warn，不影响主流程</p>
     *
     * @param intervention 待补全的实体（可空，方法内判空）
     */
    private void enrichInterventionWithDetails(AdminIntervention intervention) {
        if (intervention == null) return;

        try {
            if (intervention.getUserId() != null) {
                User buyer = userMapper.selectById(intervention.getUserId());
                if (buyer != null) {
                    intervention.setBuyerName(buyer.getNickname() != null ? buyer.getNickname() : buyer.getUsername());
                    intervention.setBuyerPhone(buyer.getPhone());
                }
            }

            if (intervention.getSellerId() != null) {
                User seller = userMapper.selectById(intervention.getSellerId());
                if (seller != null) {
                    intervention.setSellerName(seller.getNickname() != null ? seller.getNickname() : seller.getUsername());
                    intervention.setSellerShopName(seller.getCompanyName());
                }
            }

            if (intervention.getOrderId() != null) {
                Orders order = ordersMapper.selectById(intervention.getOrderId());
                if (order != null) {
                    intervention.setOrderNo(order.getOrderSn());
                    intervention.setOrderAmount(order.getTotalPrice() != null ? order.getTotalPrice().doubleValue() : 0.0);
                }
            }

            if (intervention.getProductId() != null) {
                Product product = productMapper.selectById(intervention.getProductId());
                if (product != null) {
                    intervention.setProductName(product.getName());
                }
            }

            LambdaQueryWrapper<AfterSaleService> afterSaleWrapper = new LambdaQueryWrapper<>();
            afterSaleWrapper.eq(AfterSaleService::getOrderId, intervention.getOrderId())
                          .eq(AfterSaleService::getUserId, intervention.getUserId())
                          .orderByDesc(AfterSaleService::getCreatedAt)
                          .last("LIMIT 1");
            List<AfterSaleService> afterSales = afterSaleServiceMapper.selectList(afterSaleWrapper);
            if (afterSales != null && !afterSales.isEmpty()) {
                AfterSaleService afterSale = afterSales.get(0);
                intervention.setAfterSaleId(afterSale.getId());
                intervention.setServiceType(afterSale.getServiceType());
                intervention.setAmount(afterSale.getRefundAmount() != null ? afterSale.getRefundAmount().doubleValue() : 0.0);
            }

            intervention.setInterventionAt(intervention.getCreatedAt());

        } catch (Exception e) {
            logger.warn("Failed to enrich intervention details for id={}: {}", intervention.getId(), e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /** 创建介入申请 */
    public AdminIntervention createIntervention(AdminIntervention intervention) {
        intervention.setStatus(0);
        intervention.setCreatedAt(LocalDateTime.now());
        intervention.setUpdatedAt(LocalDateTime.now());
        adminInterventionMapper.insert(intervention);
        logger.info("Created intervention request: id={}, userId={}, sellerId={}, issueType={}",
                intervention.getId(), intervention.getUserId(), intervention.getSellerId(), intervention.getIssueType());
        return intervention;
    }

    @Override
    /**
     * 根据 ID 获取介入申请并补全详情
     *
     * @param id 申请主键
     * @return 含详情字段的申请实体
     */
    public AdminIntervention getInterventionById(Long id) {
        AdminIntervention intervention = adminInterventionMapper.selectById(id);
        enrichInterventionWithDetails(intervention);
        return intervention;
    }

    @Override
    /**
     * 获取指定用户发起的全部介入申请（**按时间倒序**）
     *
     * @param userId 用户 ID
     * @return 申请列表
     */
    public List<AdminIntervention> getInterventionsByUserId(Long userId) {
        LambdaQueryWrapper<AdminIntervention> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminIntervention::getUserId, userId)
                .orderByDesc(AdminIntervention::getCreatedAt);
        return adminInterventionMapper.selectList(wrapper);
    }

    @Override
    /**
     * 获取指定商家被投诉的全部介入申请（**按时间倒序**）
     *
     * @param sellerId 商家 ID
     * @return 申请列表
     */
    public List<AdminIntervention> getInterventionsBySellerId(Long sellerId) {
        LambdaQueryWrapper<AdminIntervention> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminIntervention::getSellerId, sellerId)
                .orderByDesc(AdminIntervention::getCreatedAt);
        return adminInterventionMapper.selectList(wrapper);
    }

    @Override
    /**
     * 分页查询介入申请（**支持按状态过滤**）
     * <p>每条记录会自动 enrich 详情</p>
     *
     * @param page   当前页
     * @param size   每页大小
     * @param status 状态（可空，不过滤）
     * @return 分页对象
     */
    public IPage<AdminIntervention> getInterventionsPage(Integer page, Integer size, Integer status) {
        Page<AdminIntervention> mpPage = new Page<>(page, size);
        LambdaQueryWrapper<AdminIntervention> wrapper = new LambdaQueryWrapper<>();

        if (status != null) {
            wrapper.eq(AdminIntervention::getStatus, status);
        }

        wrapper.orderByDesc(AdminIntervention::getCreatedAt);
        IPage<AdminIntervention> result = adminInterventionMapper.selectPage(mpPage, wrapper);

        if (result.getRecords() != null) {
            for (AdminIntervention intervention : result.getRecords()) {
                enrichInterventionWithDetails(intervention);
            }
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminIntervention processIntervention(Long id, Integer status, String remark, Long adminId) {
        AdminIntervention intervention = adminInterventionMapper.selectById(id);
        if (intervention == null) {
            throw new RuntimeException("介入申请不存在");
        }

        intervention.setStatus(status);
        intervention.setAdminId(adminId);
        intervention.setAdminRemark(remark);
        if (status != null && status != 0) {
            intervention.setProcessedAt(LocalDateTime.now());
        }
        intervention.setUpdatedAt(LocalDateTime.now());

        adminInterventionMapper.updateById(intervention);
        logger.info("Processed intervention: id={}, status={}, adminId={}", id, status, adminId);
        // 直接复用已更新的实体 + enrich, 避免一次冗余 selectById
        enrichInterventionWithDetails(intervention);
        return intervention;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 提交仲裁决策（**事务**）
     * <p>
     * params 包含字段：
     * <ul>
     *   <li>status: 1 同意退款 / 2 拒绝 / 3 部分退款</li>
     *   <li>remark: 备注</li>
     *   <li>refundAmount: 退款金额</li>
     *   <li>penalty: 处罚说明</li>
     *   <li>fineAmount: 罚款金额</li>
     *   <li>evidenceImages: 证据图片（String 或 List&lt;String&gt;）</li>
     * </ul>
     * 决策内容会被拼接成 resultDetail 写入 adminRemark / result。
     * </p>
     *
     * @param id     申请主键
     * @param params 决策参数
     * @return 更新后的实体（已 enrich）
     */
    public AdminIntervention submitDecision(Long id, Map<String, Object> params) {
        try {
            AdminIntervention intervention = adminInterventionMapper.selectById(id);
            if (intervention == null) {
                throw new RuntimeException("介入申请不存在，ID: " + id);
            }

            Integer decision = params.get("status") != null ? (Integer) params.get("status") : null;
            String remark = (String) params.get("remark");
            Double refundAmount = params.get("refundAmount") != null ? ((Number) params.get("refundAmount")).doubleValue() : null;
            String penalty = (String) params.get("penalty");
            Double fineAmount = params.get("fineAmount") != null ? ((Number) params.get("fineAmount")).doubleValue() : null;

            Object evidenceImagesObj = params.get("evidenceImages");
            String evidenceImages = null;
            if (evidenceImagesObj != null) {
                if (evidenceImagesObj instanceof String) {
                    evidenceImages = (String) evidenceImagesObj;
                } else if (evidenceImagesObj instanceof List) {
                    List<?> list = (List<?>) evidenceImagesObj;
                    evidenceImages = String.join(",", list.stream().map(Object::toString).toArray(String[]::new));
                }
            }

            StringBuilder resultDetail = new StringBuilder();
            if (decision != null) {
                switch (decision) {
                    case 1: resultDetail.append("同意退款"); break;
                    case 2: resultDetail.append("拒绝退款"); break;
                    case 3: resultDetail.append("部分退款"); break;
                    default: resultDetail.append("其他处理"); break;
                }
            }
            if (refundAmount != null && refundAmount > 0) {
                resultDetail.append(", 退款金额: ¥").append(String.format("%.2f", refundAmount));
            }
            if (penalty != null && !penalty.isEmpty()) {
                resultDetail.append(", 处罚: ").append(penalty);
            }
            if (fineAmount != null && fineAmount > 0) {
                resultDetail.append(", 罚款: ¥").append(String.format("%.2f", fineAmount));
            }
            if (remark != null && !remark.isEmpty()) {
                resultDetail.append(", 备注: ").append(remark);
            }

            intervention.setStatus(2);
            intervention.setAdminRemark(resultDetail.toString());
            intervention.setResult(resultDetail.toString());
            if (evidenceImages != null && !evidenceImages.isEmpty()) {
                intervention.setEvidenceImages(evidenceImages);
            }
            intervention.setProcessedAt(LocalDateTime.now());
            intervention.setUpdatedAt(LocalDateTime.now());

            adminInterventionMapper.updateById(intervention);
            logger.info("Submitted arbitration decision: id={}, decision={}, detail={}", id, decision, resultDetail.toString());
            AdminIntervention updated = adminInterventionMapper.selectById(id);
            enrichInterventionWithDetails(updated);
            return updated;
        } catch (RuntimeException e) {
            logger.error("提交仲裁决策失败: id={}, error={}", id, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            logger.error("提交仲裁决策时发生未知错误: id={}, error={}", id, e.getMessage(), e);
            throw new RuntimeException("提交仲裁决策失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminIntervention updateIntervention(Long id, AdminIntervention intervention) {
        AdminIntervention existing = adminInterventionMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("介入申请不存在");
        }

        if (intervention.getIssueType() != null) {
            existing.setIssueType(intervention.getIssueType());
        }
        if (intervention.getDescription() != null) {
            existing.setDescription(intervention.getDescription());
        }
        if (intervention.getEvidenceImages() != null) {
            existing.setEvidenceImages(intervention.getEvidenceImages());
        }
        if (intervention.getTitle() != null) {
            existing.setTitle(intervention.getTitle());
        }

        existing.setUpdatedAt(LocalDateTime.now());
        adminInterventionMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteIntervention(Long id) {
        adminInterventionMapper.deleteById(id);
        logger.info("Deleted intervention: id={}", id);
    }

    @Override
    public List<AdminIntervention> getPendingInterventions() {
        LambdaQueryWrapper<AdminIntervention> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminIntervention::getStatus, 0)
                .orderByDesc(AdminIntervention::getCreatedAt);
        return adminInterventionMapper.selectList(wrapper);
    }

    @Override
    /** 分页获取待处理的介入申请 */
    public IPage<AdminIntervention> getPendingInterventionsPage(Integer page, Integer size) {
        Page<AdminIntervention> mpPage = new Page<>(page, size);
        LambdaQueryWrapper<AdminIntervention> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminIntervention::getStatus, 0)
                .orderByDesc(AdminIntervention::getCreatedAt);
        IPage<AdminIntervention> result = adminInterventionMapper.selectPage(mpPage, wrapper);

        if (result.getRecords() != null) {
            for (AdminIntervention intervention : result.getRecords()) {
                enrichInterventionWithDetails(intervention);
            }
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 指派管理员处理申请（**事务**）
     * <p>状态为 0（待处理）时同时置为 1（处理中）</p>
     *
     * @param id      申请主键
     * @param adminId 管理员 ID
     * @return 更新后的实体
     */
    public AdminIntervention assignAdmin(Long id, Long adminId) {
        AdminIntervention intervention = adminInterventionMapper.selectById(id);
        if (intervention == null) {
            throw new RuntimeException("介入申请不存在");
        }

        intervention.setAdminId(adminId);
        if (intervention.getStatus() == 0) {
            intervention.setStatus(1);
        }
        intervention.setUpdatedAt(LocalDateTime.now());

        adminInterventionMapper.updateById(intervention);
        logger.info("Assigned admin to intervention: id={}, adminId={}", id, adminId);
        // 直接复用已更新的实体 + enrich, 避免一次冗余 selectById
        enrichInterventionWithDetails(intervention);
        return intervention;
    }

    @Override
    /**
     * 介入申请统计
     * <p>含 pending（status=0）/ processing（status=1）/ completed（status=2,3）</p>
     *
     * @return 统计 Map
     */
    public Map<String, Object> getInterventionStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", getPendingCount());
        stats.put("processing", getProcessingCount());
        stats.put("completed", getCompletedCount());
        return stats;
    }

    @Override
    /** 待处理（status=0）数量 */
    public long getPendingCount() {
        LambdaQueryWrapper<AdminIntervention> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminIntervention::getStatus, 0);
        return adminInterventionMapper.selectCount(wrapper);
    }

    @Override
    /** 处理中（status=1）数量 */
    public long getProcessingCount() {
        LambdaQueryWrapper<AdminIntervention> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AdminIntervention::getStatus, 1);
        return adminInterventionMapper.selectCount(wrapper);
    }

    @Override
    /**
     * 已完成（status=2 或 3）数量
     */
    public long getCompletedCount() {
        LambdaQueryWrapper<AdminIntervention> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(AdminIntervention::getStatus, 2, 3);
        return adminInterventionMapper.selectCount(wrapper);
    }
}
