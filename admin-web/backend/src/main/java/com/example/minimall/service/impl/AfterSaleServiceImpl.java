package com.example.minimall.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.minimall.enums.AfterSaleStatusEnum;
import com.example.minimall.mapper.AfterSaleServiceMapper;
import com.example.minimall.mapper.ProductMapper;
import com.example.minimall.mapper.ServiceRecordMapper;
import com.example.minimall.model.AfterSaleService;
import com.example.minimall.model.Product;
import com.example.minimall.model.ServiceRecord;
import com.example.minimall.service.AfterSaleServiceApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 售后服务业务实现 */
@Service
public class AfterSaleServiceImpl implements AfterSaleServiceApi {
    private static final Logger logger = LoggerFactory.getLogger(AfterSaleServiceImpl.class);
    /** 售后记录Mapper */
    private final AfterSaleServiceMapper afterSaleServiceMapper;
    /** 服务流转记录Mapper */
    private final ServiceRecordMapper serviceRecordMapper;
    /** 商品Mapper */
    private final ProductMapper productMapper;

    public AfterSaleServiceImpl(AfterSaleServiceMapper afterSaleServiceMapper, ServiceRecordMapper serviceRecordMapper, ProductMapper productMapper) {
        this.afterSaleServiceMapper = afterSaleServiceMapper;
        this.serviceRecordMapper = serviceRecordMapper;
        this.productMapper = productMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    /**
     * 创建售后申请（用户提交）
     * <p>
     * 流程：校验服务类型 → 写售后单（status=PENDING）→ 写服务流转记录。
     * 整个流程在事务中，任一失败回滚。
     * </p>
     *
     * @param afterSale 售后申请实体（包含 orderId、userId、productId、serviceType、reason 等）
     * @return 创建成功的售后单（含自增 ID）
     * @throws RuntimeException 服务类型非法 / 插入失败
     */
    public AfterSaleService createAfterSale(AfterSaleService afterSale) {
        logger.info("创建售后服务申请: orderId={}, userId={}, productId={}, serviceType={}, reason={}",
            afterSale.getOrderId(), afterSale.getUserId(), afterSale.getProductId(),
            afterSale.getServiceType(), afterSale.getReason());

        try {
            if (afterSale.getServiceType() == null || afterSale.getServiceType() < 1 || afterSale.getServiceType() > 3) {
                throw new RuntimeException("服务类型必须在1-3之间");
            }

            afterSale.setStatus(AfterSaleStatusEnum.PENDING.getCode());
            afterSale.setCreatedAt(LocalDateTime.now());
            afterSale.setUpdatedAt(LocalDateTime.now());

            logger.info("准备插入售后记录: {}", afterSale);
            int result = afterSaleServiceMapper.insert(afterSale);
            logger.info("插入结果: {}, 生成的ID: {}", result, afterSale.getId());

            if (afterSale.getId() != null && afterSale.getId() > 0) {
                ServiceRecord record = new ServiceRecord();
                record.setAfterSaleId(afterSale.getId());
                record.setOperatorId(afterSale.getUserId());
                record.setOperationType(1);
                record.setOperationContent("用户提交售后服务申请: " + afterSale.getReason());
                record.setCreatedAt(LocalDateTime.now());
                record.setUpdatedAt(LocalDateTime.now());

                serviceRecordMapper.insert(record);
                logger.info("服务记录已创建: afterSaleId={}", afterSale.getId());
            } else {
                throw new RuntimeException("插入售后记录失败：未生成有效ID");
            }
        } catch (Exception e) {
            logger.error("创建售后服务申请失败: orderId={}, error={}", afterSale.getOrderId(), e.getMessage(), e);
            throw new RuntimeException("创建售后服务申请失败: " + e.getMessage(), e);
        }

        return afterSale;
    }

    @Override
    /**
     * 根据 ID 查询售后申请详情
     *
     * @param id 售后单 ID
     * @return 售后单实体（无则返回 null）
     */
    public AfterSaleService getAfterSaleById(Long id) {
        return afterSaleServiceMapper.selectById(id);
    }

    @Override
    /**
     * 查询用户所有售后申请（不分页）
     *
     * @param userId 用户 ID
     * @return 售后单列表
     */
    public List<AfterSaleService> getAfterSalesByUserId(Long userId) {
        return afterSaleServiceMapper.selectByUserId(userId);
    }

    @Override
    /**
     * 查询订单关联的所有售后申请
     *
     * @param orderId 订单 ID
     * @return 售后单列表
     */
    public List<AfterSaleService> getAfterSalesByOrderId(Long orderId) {
        return afterSaleServiceMapper.selectByOrderId(orderId);
    }

    @Override
    /**
     * 分页查询售后列表（管理后台用），支持按状态过滤
     *
     * @param page   页码
     * @param size   每页大小
     * @param status 售后状态（可空）
     * @return 售后分页
     */
    public IPage<AfterSaleService> getAfterSalesPage(int page, int size, Integer status) {
        try {
            Page<AfterSaleService> pageQuery = new Page<>(page, size);
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AfterSaleService> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            if (status != null) {
                qw.eq("status", status);
            }
            qw.orderByDesc("created_at");
            return afterSaleServiceMapper.selectPage(pageQuery, qw);
        } catch (Exception e) {
            System.err.println("查询售后服务列表失败: " + e.getMessage());
            e.printStackTrace();
            Page<AfterSaleService> emptyPage = new Page<>(page, size);
            emptyPage.setRecords(new java.util.ArrayList<>());
            emptyPage.setTotal(0);
            return emptyPage;
        }
    }

    @Override
    /** 分页获取指定用户的售后申请 */
    public IPage<AfterSaleService> getAfterSalesByUserIdWithPage(Long userId, int page, int size, Integer status) {
        try {
            System.out.println("===== [Service] 查询用户售后列表: userId=" + userId + ", page=" + page + ", size=" + size + " =====");

            QueryWrapper<AfterSaleService> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId);
            if (status != null) {
                queryWrapper.eq("status", status);
            }
            queryWrapper.orderByDesc("created_at");

            System.out.println("===== [Service] 执行查询... =====");
            List<AfterSaleService> allRecords = afterSaleServiceMapper.selectList(queryWrapper);
            System.out.println("===== [Service] 查询到总记录数: " + allRecords.size() + " =====");

            int total = allRecords.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);

            List<AfterSaleService> pageRecords;
            if (fromIndex >= total) {
                pageRecords = new java.util.ArrayList<>();
            } else {
                pageRecords = allRecords.subList(fromIndex, toIndex);
            }

            Page<AfterSaleService> resultPage = new Page<>(page, size);
            resultPage.setRecords(pageRecords);
            resultPage.setTotal(total);
            resultPage.setPages((total + size - 1) / size);

            System.out.println("===== [Service] 返回结果: total=" + total + ", 当前页记录数=" + pageRecords.size() + " =====");

            return resultPage;
        } catch (Exception e) {
            System.err.println("查询用户售后服务列表失败: " + e.getMessage());
            e.printStackTrace();
            Page<AfterSaleService> emptyPage = new Page<>(page, size);
            emptyPage.setRecords(new java.util.ArrayList<>());
            emptyPage.setTotal(0);
            return emptyPage;
        }
    }

    @Override
    /**
     * 分页查询商家名下的售后申请
     * <p>
     * 通过 sellerId → 商品集合 → 售后单的二级关联查询
     * </p>
     *
     * @param sellerId 商家 ID
     * @param page     页码
     * @param size     每页大小
     * @param status   售后状态（可空）
     * @return 售后分页
     */
    public IPage<AfterSaleService> getAfterSalesBySellerIdWithPage(Long sellerId, int page, int size, Integer status) {
        try {
            System.out.println("===== [Service] 查询商家售后列表: sellerId=" + sellerId + " =====");

            QueryWrapper<Product> productQuery = new QueryWrapper<>();
            productQuery.eq("seller_id", sellerId);
            List<Product> products = productMapper.selectList(productQuery);

            System.out.println("===== [Service] 商家 " + sellerId + " 有 " + products.size() + " 个商品 =====");

            if (products.isEmpty()) {
                Page<AfterSaleService> emptyPage = new Page<>(page, size);
                emptyPage.setRecords(new java.util.ArrayList<>());
                emptyPage.setTotal(0);
                return emptyPage;
            }

            List<Long> productIds = products.stream()
                    .map(Product::getId)
                    .collect(java.util.stream.Collectors.toList());

            System.out.println("===== [Service] 商品ID列表: " + productIds + " =====");

            QueryWrapper<AfterSaleService> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("product_id", productIds);
            if (status != null) {
                queryWrapper.eq("status", status);
            }
            queryWrapper.orderByDesc("created_at");

            List<AfterSaleService> allRecords = afterSaleServiceMapper.selectList(queryWrapper);
            System.out.println("===== [Service] 查询到总记录数: " + allRecords.size() + " =====");

            int total = allRecords.size();
            int fromIndex = (page - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);
            
            List<AfterSaleService> pageRecords;
            if (fromIndex >= total) {
                pageRecords = new java.util.ArrayList<>();
            } else {
                pageRecords = allRecords.subList(fromIndex, toIndex);
            }

            Page<AfterSaleService> resultPage = new Page<>(page, size);
            resultPage.setRecords(pageRecords);
            resultPage.setTotal(total);
            resultPage.setPages((total + size - 1) / size);

            System.out.println("===== [Service] 返回: total=" + total + ", 当前页记录数=" + pageRecords.size() + " =====");
            return resultPage;
        } catch (Exception e) {
            System.err.println("===== [Service] 异常: " + e.getMessage() + " =====");
            e.printStackTrace();
            Page<AfterSaleService> emptyPage = new Page<>(page, size);
            emptyPage.setRecords(new java.util.ArrayList<>());
            emptyPage.setTotal(0);
            return emptyPage;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AfterSaleService processAfterSale(Long id, Integer status, String serviceResult, Long operatorId, BigDecimal refundAmount) {
        AfterSaleService afterSale = afterSaleServiceMapper.selectById(id);
        if (afterSale == null) {
            throw new IllegalArgumentException("售后服务申请不存在");
        }

        if (!AfterSaleStatusEnum.isValidTransition(afterSale.getStatus(), status)) {
            throw new IllegalArgumentException("无效的状态转换: " + afterSale.getStatus() + " -> " + status);
        }

        afterSale.setStatus(status);
        afterSale.setServiceResult(serviceResult);
        afterSale.setProcessedBy(operatorId);
        afterSale.setProcessedAt(LocalDateTime.now());
        if (refundAmount != null) {
            afterSale.setRefundAmount(refundAmount);
        }
        if (status == 3) {
            afterSale.setCloseReason(serviceResult);
        }
        afterSale.setUpdatedAt(LocalDateTime.now());
        afterSaleServiceMapper.updateById(afterSale);

        ServiceRecord record = new ServiceRecord();
        record.setAfterSaleId(id);
        record.setOperationType(2);
        String statusDesc = AfterSaleStatusEnum.getByCode(status) != null ? AfterSaleStatusEnum.getByCode(status).getDesc() : String.valueOf(status);
        record.setOperationContent("商家处理售后申请，状态更新为：" + statusDesc + "，处理意见：" + (serviceResult != null ? serviceResult : "无"));
        record.setOperatorId(operatorId);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        serviceRecordMapper.insert(record);

        return afterSale;
    }

    @Override
    public List<ServiceRecord> getServiceRecords(Long afterSaleId) {
        return serviceRecordMapper.selectByAfterSaleId(afterSaleId);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AfterSaleService updateAfterSale(Long id, AfterSaleService afterSale) {
        AfterSaleService existing = afterSaleServiceMapper.selectById(id);
        if (existing == null) {
            throw new IllegalArgumentException("售后服务申请不存在");
        }

        if (existing.getStatus() != 0) {
            throw new IllegalArgumentException("只有待处理状态的申请才能修改");
        }

        existing.setReason(afterSale.getReason());
        existing.setImages(afterSale.getImages());
        existing.setContactPhone(afterSale.getContactPhone());
        existing.setUpdatedAt(LocalDateTime.now());
        afterSaleServiceMapper.updateById(existing);

        ServiceRecord record = new ServiceRecord();
        record.setAfterSaleId(id);
        record.setOperationType(3);
        record.setOperationContent("用户修改售后服务申请");
        record.setOperatorId(existing.getUserId());
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        serviceRecordMapper.insert(record);

        return existing;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteAfterSale(Long id) {
        afterSaleServiceMapper.deleteById(id);
        List<ServiceRecord> records = serviceRecordMapper.selectByAfterSaleId(id);
        for (ServiceRecord record : records) {
            serviceRecordMapper.deleteById(record.getId());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public AfterSaleService addSupplementaryEvidence(Long id, String evidence, Long userId) {
        AfterSaleService afterSale = afterSaleServiceMapper.selectById(id);
        if (afterSale == null) {
            throw new IllegalArgumentException("售后服务申请不存在");
        }
        if (!afterSale.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此售后申请");
        }
        if (afterSale.getStatus() != 0 && afterSale.getStatus() != 1) {
            throw new IllegalArgumentException("当前状态不允许补充证据");
        }

        afterSale.setSupplementaryEvidence(evidence);
        afterSale.setUpdatedAt(LocalDateTime.now());
        afterSaleServiceMapper.updateById(afterSale);

        ServiceRecord record = new ServiceRecord();
        record.setAfterSaleId(id);
        record.setOperationType(3);
        record.setOperationContent("用户补充了售后证据");
        record.setOperatorId(userId);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        serviceRecordMapper.insert(record);

        return afterSale;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    /** 撤销售后申请 */
    public AfterSaleService cancelAfterSale(Long id, Long userId) {
        AfterSaleService afterSale = afterSaleServiceMapper.selectById(id);
        if (afterSale == null) {
            throw new IllegalArgumentException("售后服务申请不存在");
        }
        if (!afterSale.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此售后申请");
        }
        if (afterSale.getStatus() != 0 && afterSale.getStatus() != 1) {
            throw new IllegalArgumentException("当前状态不允许取消");
        }

        afterSale.setStatus(AfterSaleStatusEnum.CLOSED.getCode());
        afterSale.setCloseReason("用户主动取消");
        afterSale.setUpdatedAt(LocalDateTime.now());
        afterSaleServiceMapper.updateById(afterSale);

        ServiceRecord record = new ServiceRecord();
        record.setAfterSaleId(id);
        record.setOperationType(3);
        record.setOperationContent("用户主动取消了售后申请");
        record.setOperatorId(userId);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        serviceRecordMapper.insert(record);

        return afterSale;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    /** 更新退货物流信息 */
    public AfterSaleService updateReturnLogistics(Long id, String logisticsCompany, String logisticsNo, Long userId) {
        AfterSaleService afterSale = afterSaleServiceMapper.selectById(id);
        if (afterSale == null) {
            throw new IllegalArgumentException("售后服务申请不存在");
        }
        if (!afterSale.getUserId().equals(userId)) {
            throw new IllegalArgumentException("无权操作此售后申请");
        }
        if (afterSale.getStatus() != 1) {
            throw new IllegalArgumentException("当前状态不允许填写退货物流");
        }

        afterSale.setReturnLogisticsCompany(logisticsCompany);
        afterSale.setReturnLogistics(logisticsNo);
        afterSale.setUpdatedAt(LocalDateTime.now());
        afterSaleServiceMapper.updateById(afterSale);

        ServiceRecord record = new ServiceRecord();
        record.setAfterSaleId(id);
        record.setOperationType(3);
        record.setOperationContent("用户填写了退货物流信息：" + logisticsCompany + " - " + logisticsNo);
        record.setOperatorId(userId);
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        serviceRecordMapper.insert(record);

        return afterSale;
    }
}