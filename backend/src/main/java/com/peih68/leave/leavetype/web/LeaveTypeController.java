package com.peih68.leave.leavetype.web;

import com.peih68.leave.common.web.ApiResponse;
import com.peih68.leave.leavetype.service.LeaveTypeService;
import com.peih68.leave.leavetype.web.dto.LeaveTypeRequest;
import com.peih68.leave.leavetype.web.dto.LeaveTypeResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/leave-types")
@RequiredArgsConstructor
public class LeaveTypeController {

    private final LeaveTypeService leaveTypeService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LeaveTypeResponse> create(@Valid @RequestBody LeaveTypeRequest req) {
        return ApiResponse.ok(leaveTypeService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<LeaveTypeResponse> update(
            @PathVariable Long id, @Valid @RequestBody LeaveTypeRequest req) {
        return ApiResponse.ok(leaveTypeService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @RequestParam(name = "hard", defaultValue = "false") boolean hard) {
        if (hard) {
            leaveTypeService.hardDelete(id);
        } else {
            leaveTypeService.softDelete(id);
        }
    }

    @GetMapping("/{id}")
    public ApiResponse<LeaveTypeResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(leaveTypeService.findById(id));
    }

    @GetMapping
    public ApiResponse<List<LeaveTypeResponse>> list(
            @RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly) {
        return ApiResponse.ok(leaveTypeService.list(activeOnly));
    }
}
