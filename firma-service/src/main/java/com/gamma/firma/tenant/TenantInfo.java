package com.gamma.firma.tenant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantInfo {
    private String tenantId;
    private String userId;
}
