package com.example.turf_Backend.controller;


import com.example.turf_Backend.dto.request.AdminRejectRequest;
import com.example.turf_Backend.dto.response.OwnerResponse;
import com.example.turf_Backend.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final AdminService adminService;

    @GetMapping("/owners/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<OwnerResponse>> getPendingOwners()
    {
        return ResponseEntity.ok(adminService.getPendingOwners());
    }
    @PutMapping("/owners/{ownerId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> approveOwner(@PathVariable Long ownerId)
    {
        return  ResponseEntity.ok(adminService.approveOwner(ownerId));
    }
    @PutMapping("/owners/{ownerId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> rejectOwner(@PathVariable Long ownerId, @RequestBody AdminRejectRequest reason)
    {
        return ResponseEntity.ok(adminService.rejectOwner(ownerId,reason));
    }

}
