package com.example.turf_Backend.service;

import com.example.turf_Backend.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

@Service
@Slf4j
public class ImageStorageService {

    @Value("${file.upload-dir.turfs}")
    private String fileUploadDir;

    @Value("${file.upload-dir.owners}")
    private String ownerUploadDir;
    private static final long MAX_FILE_SIZE=5*1024*1024;
    private static final double COMPRESSION_QUALITY=0.65;
    private static final int MAX_WIDTH=1280;
    private static final int MAX_HEIGHT=720;


    public String saveOwnerDocument(MultipartFile file, Long ownerId) {

        log.info("Owner document upload started | ownerId={} | originalName={} | size={} bytes",
                ownerId, file.getOriginalFilename(), file.getSize());

        boolean isImage = isIsImage(file);

        String base = System.getProperty("user.dir")+"/uploads/"+ownerUploadDir+"/"+ownerId;

        File dir = new File(base);
        if (!dir.exists() && dir.mkdirs()) {
            log.info("Created owner upload directory: {}", base);
        }

        String original = Objects.requireNonNull(file.getOriginalFilename(), "Invalid filename");
        String clean = original.replaceAll("[^a-zA-Z0-9._-]", "_");
        String name = System.currentTimeMillis() + "_" + clean;

        File out = new File(dir, name); // FIXED PATH JOIN

        try {
            if (isImage) {
                log.info("Compressing owner image | ownerId={} | output={}", ownerId, out.getAbsolutePath());
                Thumbnails.of(file.getInputStream())
                        .size(1280, 720)
                        .outputQuality(0.65)
                        .toFile(out);
            } else {
                log.info("Saving owner document (non-image) | ownerId={} | output={}", ownerId, out.getAbsolutePath());
                file.transferTo(out);
            }
        } catch (IOException e) {
            log.error("Owner document upload FAILED | ownerId={} | file={}", ownerId, original, e);
            throw new CustomException("Upload failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String publicPath = "/uploads/"+ownerUploadDir+"/" + ownerId + "/" + name; // FIXED URL

        log.info("Owner document upload completed | ownerId={} | publicPath={}", ownerId, publicPath);

        return publicPath;
    }

    private boolean isIsImage(MultipartFile file) {

        if (file.isEmpty()) {
            log.warn("Empty owner document upload attempt");
            throw new CustomException("Document empty", HttpStatus.BAD_REQUEST);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("Owner document exceeds size limit | size={}", file.getSize());
            throw new CustomException("Document exceeds 5MB", HttpStatus.BAD_REQUEST);
        }

        String type = file.getContentType();
        boolean isImage = "image/jpeg".equals(type) || "image/png".equals(type);
        boolean isPdf   = "application/pdf".equals(type);

        if (!isImage && !isPdf) {
            log.warn("Invalid owner document type: {}", type);
            throw new CustomException("Only JPG, PNG, PDF allowed", HttpStatus.BAD_REQUEST);
        }

        return isImage;
    }


    public String compressAndSaveImage(MultipartFile file, Long ownerId) {

        if (file.isEmpty())
            throw new CustomException("File is empty", HttpStatus.BAD_REQUEST);

        if (file.getSize() > MAX_FILE_SIZE)
            throw new CustomException("File exceeds 5MB", HttpStatus.BAD_REQUEST);

        String type = file.getContentType();
        if (!"image/jpeg".equals(type) && !"image/png".equals(type))
            throw new CustomException("Only JPG and PNG allowed", HttpStatus.BAD_REQUEST);

        String clean = Objects.requireNonNull(file.getOriginalFilename())
                .replaceAll("[^a-zA-Z0-9._-]", "_");

        String name = System.currentTimeMillis() + "_" + clean;

        // PRIVATE DISK PATH
        Path diskPath = Paths.get(
                System.getProperty("user.dir"),
                "uploads",
                fileUploadDir,
                ownerId.toString(),
                name
        );


        try {
            Files.createDirectories(diskPath.getParent());

            try (InputStream in = file.getInputStream()) {
                Thumbnails.of(in)
                        .size(MAX_WIDTH, MAX_HEIGHT)
                        .outputQuality(COMPRESSION_QUALITY)
                        .toFile(diskPath.toFile());
            }

            // PUBLIC API PATH (DB value)
            return "/uploads/" + fileUploadDir + "/" + ownerId + "/" + name;

        } catch (IOException e) {
            throw new CustomException("Image save failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    public void deleteImage(String publicPath) {

        if (publicPath == null || publicPath.isBlank()) return;

        File diskFile = Paths.get(System.getProperty("user.dir"), publicPath).toFile();

        if (diskFile.exists() && diskFile.delete())
            log.info("Deleted {}", diskFile.getAbsolutePath());
        else
            log.warn("File not found: {}", diskFile.getAbsolutePath());
    }


}
