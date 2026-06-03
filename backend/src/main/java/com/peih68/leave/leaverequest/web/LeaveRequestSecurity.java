package com.peih68.leave.leaverequest.web;

import com.peih68.leave.leaverequest.domain.LeaveRequestEntity;
import com.peih68.leave.leaverequest.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * SpEL helpers for {@code @PreAuthorize} on leave-request endpoints, e.g.
 * {@code @leaveRequestSecurity.isManagerOf(#id, principal.id)}.
 */
@Component("leaveRequestSecurity")
@RequiredArgsConstructor
public class LeaveRequestSecurity {

    private final LeaveRequestRepository requestRepository;

    public boolean isRequester(Long requestId, Long userId) {
        return requestRepository.findById(requestId)
                .map(r -> r.getUserId().equals(userId))
                .orElse(false);
    }

    public boolean isManagerOf(Long requestId, Long userId) {
        return requestRepository.findById(requestId)
                .map(r -> userId.equals(r.getManagerId()))
                .orElse(false);
    }

    /** Either the requester or the assigned manager. */
    public boolean isParticipant(Long requestId, Long userId) {
        return requestRepository.findById(requestId)
                .map(r -> r.getUserId().equals(userId) || userId.equals(r.getManagerId()))
                .orElse(false);
    }
}
