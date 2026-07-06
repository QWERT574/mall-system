package com.example.minimall.model;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 售后服务单实体，对应 after_sale_service 表，存储用户退款/退货/换货等服务申请
 */
@TableName("after_sale_service")
public class AfterSaleService {
    @TableId
    private Long id;

    private Integer deleted;

    @NotNull(message = "订单ID不能为空")
    private Long orderId;
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotNull(message = "商品ID不能为空")
    private Long productId;
    
    @NotNull(message = "服务类型不能为空")
    @Min(value = 1, message = "服务类型必须在1-3之间")
    private Integer serviceType;
    
    @NotBlank(message = "售后原因不能为空")
    private String reason;
    
    private String images;
    
    private Integer status;
    
    private String serviceResult;

    private BigDecimal refundAmount;

    private String returnLogistics;

    private String returnLogisticsCompany;

    private LocalDateTime expectCompleteDate;

    private String closeReason;

    private String supplementaryEvidence;

    private String contactPhone;

    private Long processedBy;

    private LocalDateTime processedAt;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getServiceType() {
        return serviceType;
    }

    public void setServiceType(Integer serviceType) {
        this.serviceType = serviceType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getServiceResult() {
        return serviceResult;
    }

    public void setServiceResult(String serviceResult) {
        this.serviceResult = serviceResult;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getReturnLogistics() {
        return returnLogistics;
    }

    public void setReturnLogistics(String returnLogistics) {
        this.returnLogistics = returnLogistics;
    }

    public String getReturnLogisticsCompany() {
        return returnLogisticsCompany;
    }

    public void setReturnLogisticsCompany(String returnLogisticsCompany) {
        this.returnLogisticsCompany = returnLogisticsCompany;
    }

    public LocalDateTime getExpectCompleteDate() {
        return expectCompleteDate;
    }

    public void setExpectCompleteDate(LocalDateTime expectCompleteDate) {
        this.expectCompleteDate = expectCompleteDate;
    }

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
    }

    public String getSupplementaryEvidence() {
        return supplementaryEvidence;
    }

    public void setSupplementaryEvidence(String supplementaryEvidence) {
        this.supplementaryEvidence = supplementaryEvidence;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public Long getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(Long processedBy) {
        this.processedBy = processedBy;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}