package com.peih68.leave.attachment.service;

import com.peih68.leave.attachment.config.AttachmentProperties;
import com.peih68.leave.attachment.domain.AttachmentEntity;
import com.peih68.leave.attachment.repository.AttachmentRepository;
import com.peih68.leave.attachment.web.dto.AttachmentResponse;
import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.common.audit.AuditLogWriter;
import com.peih68.leave.common.exception.ApiException;
import com.peih68.leave.common.exception.ErrorCode;
import com.peih68.leave.leaverequest.domain.LeaveRequestEntity;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import com.peih68.leave.leaverequest.repository.LeaveRequestRepository;
import com.peih68.leave.user.domain.Role;
import com.peih68.leave.user.domain.UserEntity;
import com.peih68.leave.user.repository.UserRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final UserRepository userRepository;
    private final AttachmentProperties properties;
    private final AuditLogWriter auditLogWriter;

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Long requestId, UserPrincipal actor) {
        ensureEnabled();
        LeaveRequestEntity request = requireVisibleRequest(requestId, actor);
        return toResponses(attachmentRepository.findByLeaveRequestIdOrderByCreatedAtAsc(request.getId()));
    }

    @Transactional
    public List<AttachmentResponse> upload(Long requestId, List<MultipartFile> files, UserPrincipal actor) {
        ensureEnabled();
        LeaveRequestEntity request = requireRequest(requestId);
        requireRequesterPending(request, actor);
        if (files == null || files.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "select at least one file");
        }
        long existing = attachmentRepository.countByLeaveRequestId(requestId);
        if (existing + files.size() > properties.getMaxFilesPerRequest()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR,
                    "a leave request can have at most %d attachments".formatted(properties.getMaxFilesPerRequest()));
        }

        List<AttachmentEntity> saved = files.stream()
                .map(file -> saveOne(request.getId(), file, actor.getId()))
                .toList();
        auditLogWriter.record(actor.getId(), "ATTACHMENT_UPLOADED", "leave_request", requestId,
                null, "{\"count\":" + saved.size() + "}");
        return toResponses(saved);
    }

    @Transactional(readOnly = true)
    public AttachmentDownload download(Long requestId, Long attachmentId, UserPrincipal actor) {
        ensureEnabled();
        requireVisibleRequest(requestId, actor);
        AttachmentEntity attachment = requireAttachment(requestId, attachmentId);
        Path path = resolveStoredPath(attachment.getStoredKey());
        if (!Files.isRegularFile(path)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Attachment file not found: " + attachmentId);
        }
        return new AttachmentDownload(toResponse(attachment, uploaderNames(List.of(attachment))), new FileSystemResource(path));
    }

    @Transactional
    public void delete(Long requestId, Long attachmentId, UserPrincipal actor) {
        ensureEnabled();
        LeaveRequestEntity request = requireRequest(requestId);
        requireRequesterPending(request, actor);
        AttachmentEntity attachment = requireAttachment(requestId, attachmentId);
        attachmentRepository.delete(attachment);
        try {
            Files.deleteIfExists(resolveStoredPath(attachment.getStoredKey()));
        } catch (IOException ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to delete attachment file");
        }
        auditLogWriter.record(actor.getId(), "ATTACHMENT_DELETED", "leave_request", requestId,
                "{\"attachmentId\":" + attachmentId + "}", null);
    }

    private AttachmentEntity saveOne(Long requestId, MultipartFile file, Long actorId) {
        validate(file);
        String originalName = sanitizeOriginalName(file.getOriginalFilename());
        String storedKey = requestId + "/" + UUID.randomUUID() + extensionFor(file.getContentType(), originalName);
        Path target = resolveStoredPath(storedKey);
        try {
            Files.createDirectories(target.getParent());
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new ApiException(ErrorCode.INTERNAL_ERROR, "Failed to store attachment");
        }
        return attachmentRepository.save(AttachmentEntity.builder()
                .leaveRequestId(requestId)
                .uploadedBy(actorId)
                .originalFilename(originalName)
                .storedKey(storedKey)
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .build());
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "empty files are not allowed");
        }
        if (file.getSize() > properties.getMaxFileSizeBytes()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "file is larger than 5MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !properties.getAllowedContentTypes().contains(contentType)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "only PDF, JPG and PNG files are allowed");
        }
    }

    private LeaveRequestEntity requireVisibleRequest(Long requestId, UserPrincipal actor) {
        LeaveRequestEntity request = requireRequest(requestId);
        boolean privileged = actor.getRole() == Role.ADMIN || actor.getRole() == Role.HR;
        boolean participant = request.getUserId().equals(actor.getId()) || actor.getId().equals(request.getManagerId());
        if (!privileged && !participant) {
            throw new ApiException(ErrorCode.FORBIDDEN, "you cannot access attachments for this request");
        }
        return request;
    }

    private void requireRequesterPending(LeaveRequestEntity request, UserPrincipal actor) {
        if (!request.getUserId().equals(actor.getId())) {
            throw new ApiException(ErrorCode.FORBIDDEN, "only the requester can modify attachments");
        }
        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new ApiException(ErrorCode.CONFLICT, "attachments can only be changed while request is PENDING");
        }
    }

    private LeaveRequestEntity requireRequest(Long requestId) {
        return leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Leave request not found: " + requestId));
    }

    private AttachmentEntity requireAttachment(Long requestId, Long attachmentId) {
        AttachmentEntity attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "Attachment not found: " + attachmentId));
        if (!attachment.getLeaveRequestId().equals(requestId)) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Attachment not found: " + attachmentId);
        }
        return attachment;
    }

    private List<AttachmentResponse> toResponses(List<AttachmentEntity> attachments) {
        Map<Long, String> names = uploaderNames(attachments);
        return attachments.stream().map(a -> toResponse(a, names)).toList();
    }

    private AttachmentResponse toResponse(AttachmentEntity a, Map<Long, String> uploaderNames) {
        return new AttachmentResponse(
                a.getId(),
                a.getLeaveRequestId(),
                a.getOriginalFilename(),
                a.getContentType(),
                a.getSizeBytes(),
                a.getUploadedBy(),
                uploaderNames.get(a.getUploadedBy()),
                a.getCreatedAt());
    }

    private Map<Long, String> uploaderNames(List<AttachmentEntity> attachments) {
        return userRepository.findAllById(attachments.stream()
                        .map(AttachmentEntity::getUploadedBy)
                        .distinct()
                        .toList()).stream()
                .collect(Collectors.toMap(UserEntity::getId, UserEntity::getFullName));
    }

    private Path resolveStoredPath(String storedKey) {
        Path root = properties.getStorageDir().toAbsolutePath().normalize();
        Path path = root.resolve(storedKey).normalize();
        if (!path.startsWith(root)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "invalid attachment path");
        }
        return path;
    }

    private void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw new ApiException(ErrorCode.NOT_FOUND, "Attachments are disabled");
        }
    }

    private static String sanitizeOriginalName(String originalFilename) {
        String name = originalFilename == null || originalFilename.isBlank() ? "attachment" : originalFilename;
        name = Path.of(name).getFileName().toString();
        return name.replaceAll("[\\r\\n\\t]", "_");
    }

    private static String extensionFor(String contentType, String originalName) {
        String lower = originalName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")) {
            return lower.substring(lower.lastIndexOf('.'));
        }
        return switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            default -> "";
        };
    }
}
