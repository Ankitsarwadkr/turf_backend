package com.example.turf_Backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.turf_Backend.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryService {
    private final Cloudinary cloudinary;

    public String uploadPublicImage(MultipartFile file,String folder){
        try{
            Map<?,?>res=cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder",folder,
                    "resource_type","image",
                    "quality","auto",
                    "fetch_format","auto"
            ));
            return res.get("secure_url").toString();
        }catch (Exception e){
            throw new RuntimeException("Image upload failed",e);
        }
    }

    public String uploadPrivateDocument(MultipartFile file,String folder){
        try{
            Map<?,?> res=cloudinary.uploader().upload(
                    file.getBytes(),ObjectUtils.asMap(
                            "folder",folder,
                            "resource_type","raw",
                            "type","private"
                    )
            );
            return res.get("public_id").toString();
        }catch (Exception e){
            throw new RuntimeException("Document Upload failed",e);
        }
    }
    public void delete(String publicId){
        try{
            cloudinary.uploader().destroy(publicId,ObjectUtils.emptyMap());
        }catch (Exception ignored){}
    }
}
