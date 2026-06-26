package com.peih68.leave.attachment.web.dto;

import java.time.OffsetDateTime;

public record AttachmentResponse(
        Long id,
        Long leaveRequestId,
        String originalFilename,
        String contentType,
        Long sizeBytes,
        Long uploadedById,
        String uploadedByName,
        OffsetDateTime createdAt) {}
