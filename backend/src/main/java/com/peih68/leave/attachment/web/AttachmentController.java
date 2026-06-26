package com.peih68.leave.attachment.web;

import com.peih68.leave.attachment.service.AttachmentDownload;
import com.peih68.leave.attachment.service.AttachmentService;
import com.peih68.leave.attachment.web.dto.AttachmentResponse;
import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.common.web.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @GetMapping("/leave-requests/{requestId}/attachments")
    public ApiResponse<List<AttachmentResponse>> list(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(attachmentService.list(requestId, actor));
    }

    @PostMapping(value = "/leave-requests/{requestId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<List<AttachmentResponse>> upload(
            @PathVariable Long requestId,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(attachmentService.upload(requestId, files, actor));
    }

    @GetMapping("/leave-requests/{requestId}/attachments/{attachmentId}/download")
    public ResponseEntity<Resource> download(
            @PathVariable Long requestId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal UserPrincipal actor) {
        AttachmentDownload download = attachmentService.download(requestId, attachmentId, actor);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.metadata().contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(download.metadata().originalFilename())
                        .build()
                        .toString())
                .contentLength(download.metadata().sizeBytes())
                .body(download.resource());
    }

    @DeleteMapping("/leave-requests/{requestId}/attachments/{attachmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long requestId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal UserPrincipal actor) {
        attachmentService.delete(requestId, attachmentId, actor);
    }
}
