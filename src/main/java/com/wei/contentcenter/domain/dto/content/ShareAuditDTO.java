package com.wei.contentcenter.domain.dto.content;

import com.wei.contentcenter.domain.enums.AuditStatusEnum;
import lombok.Data;

@Data
public class ShareAuditDTO {
    /**
     * 审核状态
     */
    private AuditStatusEnum auditStatusEnum;
    /**
     * 原因
     */
    private String reason;
}
