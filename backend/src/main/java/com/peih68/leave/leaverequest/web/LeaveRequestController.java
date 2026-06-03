package com.peih68.leave.leaverequest.web;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.common.web.ApiResponse;
import com.peih68.leave.leaverequest.domain.LeaveStatus;
import com.peih68.leave.leaverequest.service.LeaveRequestService;
import com.peih68.leave.leaverequest.web.dto.ApprovalActionResponse;
import com.peih68.leave.leaverequest.web.dto.ApprovalDecisionRequest;
import com.peih68.leave.leaverequest.web.dto.LeaveRequestCreateRequest;
import com.peih68.leave.leaverequest.web.dto.LeaveRequestResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    /** Submit a leave request for the authenticated user. */
    @PostMapping("/leave-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LeaveRequestResponse> submit(
            @Valid @RequestBody LeaveRequestCreateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(leaveRequestService.submit(req, principal));
    }

    /** Detail — requester, the assigned manager, or HR/ADMIN. */
    @GetMapping("/leave-requests/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR') or @leaveRequestSecurity.isParticipant(#id, principal.id)")
    public ApiResponse<LeaveRequestResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(leaveRequestService.findById(id));
    }

    /** A user's own requests; HR/ADMIN can read anyone's. */
    @GetMapping("/users/{id}/leave-requests")
    @PreAuthorize("hasAnyRole('ADMIN','HR') or #id == principal.id")
    public ApiResponse<List<LeaveRequestResponse>> byUser(
            @PathVariable Long id,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) LeaveStatus status) {
        return ApiResponse.ok(leaveRequestService.listByUser(id, year, status));
    }

    /** Approver inbox: MANAGER sees their team; HR/ADMIN see all. Paged. */
    @GetMapping("/leave-requests")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ApiResponse<Page<LeaveRequestResponse>> inbox(
            @RequestParam(required = false) LeaveStatus status,
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<LeaveRequestResponse> page = leaveRequestService.listForApprover(principal, status, pageable);
        return ApiResponse.ok(page, Map.of(
                "page", page.getNumber(),
                "size", page.getSize(),
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages()));
    }

    /** Approve a pending request — the assigned manager or HR/ADMIN. */
    @PostMapping("/leave-requests/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','HR') or @leaveRequestSecurity.isManagerOf(#id, principal.id)")
    public ApiResponse<LeaveRequestResponse> approve(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ApprovalDecisionRequest req,
            @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(leaveRequestService.approve(id, req, actor));
    }

    /** Reject a pending request — the assigned manager or HR/ADMIN. */
    @PostMapping("/leave-requests/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','HR') or @leaveRequestSecurity.isManagerOf(#id, principal.id)")
    public ApiResponse<LeaveRequestResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ApprovalDecisionRequest req,
            @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(leaveRequestService.reject(id, req, actor));
    }

    /** Cancel a request — requester (PENDING only) or manager/HR/ADMIN. */
    @PostMapping("/leave-requests/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','HR') or @leaveRequestSecurity.isManagerOf(#id, principal.id)"
            + " or @leaveRequestSecurity.isRequester(#id, principal.id)")
    public ApiResponse<LeaveRequestResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ApprovalDecisionRequest req,
            @AuthenticationPrincipal UserPrincipal actor) {
        return ApiResponse.ok(leaveRequestService.cancel(id, req, actor));
    }

    /** Workflow history — requester, the assigned manager, or HR/ADMIN. */
    @GetMapping("/leave-requests/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN','HR') or @leaveRequestSecurity.isParticipant(#id, principal.id)")
    public ApiResponse<List<ApprovalActionResponse>> history(@PathVariable Long id) {
        return ApiResponse.ok(leaveRequestService.history(id));
    }
}
