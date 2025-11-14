package com.example.turf_Backend.controller;

import com.example.turf_Backend.dto.request.TurfRequest;
import com.example.turf_Backend.dto.request.TurfUpdateRequest;
import com.example.turf_Backend.dto.response.ImageActionResponse;
import com.example.turf_Backend.dto.response.TurfResponse;
import com.example.turf_Backend.dto.response.TurfUpdateResponse;
import com.example.turf_Backend.service.TurfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/owners/turfs")
@PreAuthorize("hasRole('OWNER')")
public class TurfController {
    private final TurfService turfService;

    @PostMapping("/addturf")
    public ResponseEntity<TurfResponse> addTurf(@ModelAttribute TurfRequest request) {
        return ResponseEntity.ok(turfService.addTurf(request));
    }

    @PutMapping("/update/{turfId}")
    public ResponseEntity<TurfUpdateResponse> updateTurf(@PathVariable Long turfId, @RequestBody TurfUpdateRequest request)
    {

        return ResponseEntity.ok(turfService.updateTurf(turfId,request));
    }
    @PostMapping("{turfId}/images")
    public ResponseEntity<ImageActionResponse> addImg(@PathVariable Long turfId, @RequestParam("images")List<MultipartFile> images)
    {
        return ResponseEntity.ok(turfService.addImg(turfId,images));
    }
    @DeleteMapping("/{turfId}/images/{imageId}")
    public ResponseEntity<ImageActionResponse> deleteImage(
            @PathVariable Long turfId,
            @PathVariable Long imageId)
    {
        return ResponseEntity.ok(turfService.deleteTurfImage(turfId, imageId));
    }

    @DeleteMapping("/delete/{turfId}")
    public ResponseEntity<Map<String, String>> deleteTurf(@PathVariable Long turfId)
    {
        turfService.deleteTurf(turfId);
        return ResponseEntity.ok(Map.of(
                "message", "Turf deleted successfully",
                "turfId", String.valueOf(turfId)
        ));
    }

    @GetMapping("/me")
    public ResponseEntity<List<TurfResponse>> getMyturfs()
    {
        return  ResponseEntity.ok(turfService.getMyturfs());
    }
    @GetMapping("/me/{turfId}")
    public ResponseEntity<TurfResponse> getTurfById(@PathVariable Long turfId) {
        return ResponseEntity.ok(turfService.getTurfById(turfId));
    }
}
