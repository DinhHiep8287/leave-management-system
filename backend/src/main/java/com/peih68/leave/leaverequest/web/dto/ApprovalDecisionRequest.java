package com.peih68.leave.leaverequest.web.dto;

import jakarta.validation.constraints.Size;

/** Optional comment for approve/cancel; required (non-blank) for reject — enforced in the service. */
public record ApprovalDecisionRequest(
        @Size(max = 2000) String comment) {}
