package com.peih68.leave.attachment.service;

import com.peih68.leave.attachment.web.dto.AttachmentResponse;
import org.springframework.core.io.Resource;

public record AttachmentDownload(AttachmentResponse metadata, Resource resource) {}
