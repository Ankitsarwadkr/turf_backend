package com.example.turf_Backend.service;

import com.example.turf_Backend.dto.request.TurfRequest;
import com.example.turf_Backend.dto.request.TurfUpdateRequest;
import com.example.turf_Backend.dto.response.ImageActionResponse;
import com.example.turf_Backend.dto.response.OwnerTurfRowResponse;
import com.example.turf_Backend.dto.response.TurfResponse;
import com.example.turf_Backend.dto.response.TurfUpdateResponse;
import com.example.turf_Backend.entity.Turf;
import com.example.turf_Backend.entity.TurfImage;
import com.example.turf_Backend.entity.User;
import com.example.turf_Backend.exception.CustomException;
import com.example.turf_Backend.mapper.TurfMapper;
import com.example.turf_Backend.repository.TurfImageRepository;
import com.example.turf_Backend.repository.TurfRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TurfService {
    private final TurfRepository turfRepository;
    private final TurfMapper turfMapper;
    private final ImageStorageService imageStorageService;
    private final TurfImageRepository turfImageRepository;

    @Transactional
    public TurfResponse addTurf(TurfRequest request) {
        User owner=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if(request.getImages()==null || request.getImages().isEmpty())
        {
            throw  new CustomException("At least onw image is requires", HttpStatus.BAD_REQUEST);
        }
        Turf turf = Turf.builder()
                .name(request.getName())
                .address(request.getAddress())
                .mapUrl(request.getMapUrl())
                .locality(request.getLocality())
                .city(request.getCity())
                .description(request.getDescription())
                .amenities(request.getAmenities())
                .turfType(request.getTurfType())
                .available(request.getAvailable())
                .owner(owner)
                .build();
        List<TurfImage> savedImages=new ArrayList<>();
        for (MultipartFile file:request.getImages())
        {
            String path=imageStorageService.compressAndSaveImage(file, owner.getId());
            TurfImage image= TurfImage.builder()
                    .fileName(file.getOriginalFilename())
                    .filePath(path)
                    .turf(turf)
                    .build();
            savedImages.add(image);
        }
        turf.setImages(savedImages);

        Turf saved=turfRepository.save(turf);
        log.info("Turf '{}' added by owner '{}' ",turf.getName(),owner.getEmail());
        return turfMapper.toResponse(saved);
    }

    @Transactional
    public TurfUpdateResponse updateTurf(Long turfId, TurfUpdateRequest request) {
        User owner = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> new CustomException("Turf with Id " + turfId + " Not found", HttpStatus.NOT_FOUND));

        if (!turf.getOwner().getId().equals(owner.getId())) {
            throw new CustomException("Unauthorized to update this turf ", HttpStatus.FORBIDDEN);
        }
        turfMapper.updateTurfRequest(turf, request);
            Turf updated = turfRepository.save(turf);
            log.info("Turf '{}' updated by '{}'", updated.getName(), owner.getEmail());
            return turfMapper.toTurfUpdateResponse(updated,"Details Updated Successfully");
    }

    @Transactional
    public ImageActionResponse addImg(Long turfId, List<MultipartFile> images) {
        User owner = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Turf turf=turfRepository.findById(turfId)
                .orElseThrow(()->new CustomException("Turf with this Id : "+turfId+" Not found ",HttpStatus.NOT_FOUND));

        if (!turf.getOwner().getId().equals(owner.getId())) {
            throw new CustomException("Unauthorized to modify this turf", HttpStatus.FORBIDDEN);
        }
        if (images == null || images.isEmpty()) {
            throw new CustomException("No images provided for upload", HttpStatus.BAD_REQUEST);
        }
        List<TurfImage> newImages=new ArrayList<>();
        for (MultipartFile file:images)
        {
            String path=imageStorageService.compressAndSaveImage(file, owner.getId());
            TurfImage img=TurfImage.builder()
                    .fileName(file.getOriginalFilename())
                    .filePath(path)
                    .turf(turf)
                    .build();
            newImages.add(img);
        }
        turf.getImages().addAll(newImages);
        turfRepository.save(turf);

        log.info("Added {} images to Turf '{}' by owner '{}'", newImages.size(), turf.getName(), owner.getEmail());
        return ImageActionResponse.builder()
                .turfId(turf.getId())
                .message("Images added successfully")
                .changedCount(newImages.size())
                .totalImages(turf.getImages().size())
                .build();
    }

    @Transactional
    public ImageActionResponse deleteTurfImage(Long turfId, Long imageId) {
        User owner = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> new CustomException("Turf with ID " + turfId + " not found", HttpStatus.NOT_FOUND));

        if (!turf.getOwner().getId().equals(owner.getId())) {
            throw new CustomException("Unauthorized to delete image from this turf", HttpStatus.FORBIDDEN);
        }

        TurfImage image = turfImageRepository.findById(imageId)
                .orElseThrow(() -> new CustomException("Image with ID " + imageId + " not found", HttpStatus.NOT_FOUND));

        if (!image.getTurf().getId().equals(turfId)) {
            throw new CustomException("Image does not belong to this turf", HttpStatus.BAD_REQUEST);
        }

        // Delete physical image file from disk
        imageStorageService.deleteImage(image.getFilePath());

        // Remove from DB and Turf entity
        turf.getImages().remove(image);
        turfImageRepository.delete(image);

        log.info("Deleted image {} from Turf '{}' by owner '{}'", imageId, turf.getName(), owner.getEmail());
        return ImageActionResponse.builder()
                .turfId(turf.getId())
                .message("Image deleted successfully")
                .changedCount(1)
                .totalImages(turf.getImages().size())
                .build();
    }


    public void deleteTurf(Long turfId) {
        User owner = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> new CustomException("Turf with ID " + turfId + " not found", HttpStatus.NOT_FOUND));

        // Authorization check — only the owner who created it can delete
        if (!turf.getOwner().getId().equals(owner.getId())) {
            throw new CustomException("Unauthorized to delete this turf", HttpStatus.FORBIDDEN);
        }
        //delete the images physically first
        turf.getImages().forEach(image -> {
            imageStorageService.deleteImage(image.getFilePath());
        });
        turfRepository.delete(turf);

        log.info("Turf '{}' (ID: {}) deleted by owner '{}'", turf.getName(), turfId, owner.getEmail());
    }
    @Transactional
    public List<OwnerTurfRowResponse> getMyTurfs() {
        User owner=(User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<Turf> turfs=turfRepository.findByOwnerId(owner.getId());
        if (turfs.isEmpty())
        {
            throw  new CustomException("No turfs available for this owner",HttpStatus.NOT_FOUND);
        }
        return turfs.stream()
                .map(turfMapper::toOwnerRow)
                .toList();
    }
    @Transactional
    public TurfResponse getTurfById(Long turfId) {
        User owner = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Turf turf = turfRepository.findById(turfId)
                .orElseThrow(() -> new CustomException("Turf with ID " + turfId + " not found", HttpStatus.NOT_FOUND));

        if (!turf.getOwner().getId().equals(owner.getId())) {
            throw new CustomException("Unauthorized to view this turf", HttpStatus.FORBIDDEN);
        }

        return turfMapper.toResponse(turf);
    }
}
