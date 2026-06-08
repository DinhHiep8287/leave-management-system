package com.peih68.leave.department.web;

import com.peih68.leave.auth.domain.UserPrincipal;
import com.peih68.leave.common.web.ApiResponse;
import com.peih68.leave.department.service.DepartmentService;
import com.peih68.leave.department.web.dto.DepartmentRequest;
import com.peih68.leave.department.web.dto.DepartmentResponse;
import com.peih68.leave.department.web.dto.MyDepartmentResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DepartmentResponse> create(@Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.ok(departmentService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DepartmentResponse> update(
            @PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.ok(departmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        departmentService.softDelete(id);
    }

    @GetMapping("/mine")
    public ApiResponse<MyDepartmentResponse> mine(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(departmentService.myDepartment(principal.getId()));
    }

    @GetMapping("/{id}")
    public ApiResponse<DepartmentResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(departmentService.findById(id));
    }

    @GetMapping
    public ApiResponse<Page<DepartmentResponse>> list(
            @RequestParam(required = false) String q,
            @RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly,
            @PageableDefault(size = 20, sort = "code") Pageable pageable) {
        Page<DepartmentResponse> page = departmentService.list(q, activeOnly, pageable);
        return ApiResponse.ok(page, Map.of(
                "page", page.getNumber(),
                "size", page.getSize(),
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages()));
    }
}
