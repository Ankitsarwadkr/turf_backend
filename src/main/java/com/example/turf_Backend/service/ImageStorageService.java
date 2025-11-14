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
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
public class ImageStorageService {

    @Value("${file.upload-dir.turfs}")
    private String fileUploadDir;
    private static final long MAX_FILE_SIZE=5*1024*1024;
    private static final double COMPRESSION_QUALITY=0.65;
    private static final int MAX_WIDTH=1280;
    private static final int MAX_HEIGHT=720;

    public String compressAndSaveImage(MultipartFile file,Long ownerId)
    {
        if (file.isEmpty())
        {
            throw new CustomException("File is empty", HttpStatus.BAD_REQUEST);
        }
        if (file.getSize()>MAX_FILE_SIZE)
        {
            throw new CustomException("File size exceeds 5MB limit ",HttpStatus.BAD_REQUEST);
        }
        String contentType=file.getContentType();
        if (contentType==null || !(contentType.equals("image/jpeg") || contentType.equals("image/png")))
        {
            throw new CustomException("Only JPEG and PNG files are allowed ",HttpStatus.BAD_REQUEST);
        }
        String folderPath= Paths.get(fileUploadDir,String.valueOf(ownerId)).toString();

        File dir=new File(folderPath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                throw new CustomException("Could not create upload directory: " + folderPath, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }

        String sanitizedName = file.getOriginalFilename().replaceAll("\\s+", "_");
        String fileName = System.currentTimeMillis() + "_" + sanitizedName;
        String filePath=Paths.get(folderPath,fileName).toString();
        try(InputStream inputStream=file.getInputStream())
        {
            Thumbnails.of(inputStream)
                    .size(MAX_WIDTH,MAX_HEIGHT)
                    .outputQuality(COMPRESSION_QUALITY)
                    .toFile(filePath);

            log.info("Compressed and saved image :{} (ownerId={})",filePath,ownerId);
            return filePath;
        }catch (IOException e)
        {
            log.error("Failed to save compressed image :{}",e.getMessage());
            throw new CustomException("Image Compression failed : "+e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    public void  deleteImage(String filePath)
    {
        try
        {
            File file=new File(filePath);
            if (file.exists()&&file.delete())
            {
                log.info("Deletd Image file : {} ",filePath);
            }
            else
            {
                log.warn("Image file not found or already deleted: {}", filePath);
            }
        }catch (Exception e)
        {
            log.error("Failed to delete image : {}",e.getMessage());
        }
    }


}
