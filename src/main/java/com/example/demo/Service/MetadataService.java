package com.example.demo.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import com.example.demo.Model.FileMeta;
import com.example.demo.Repository.FileMetaRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
public class MetadataService {

    private final AmazonService amazonService;

    private final FileMetaRepository fileMetaRepository;

    @Value("${aws.s3.bucket.name}")
    private String bucketName;

    public MetadataService(AmazonService amazonService, FileMetaRepository fileMetaRepository) {
        this.amazonService = amazonService;
        this.fileMetaRepository = fileMetaRepository;
    }

    public void upload(MultipartFile file) throws IOException {

        if (file.isEmpty())
            throw new IllegalStateException("Cannot upload empty file");

        Map<String, String> metadata = new HashMap<>();
        metadata.put("Content-Type", file.getContentType());
        metadata.put("Content-Length", String.valueOf(file.getSize()));

        String path = String.format("%s/%s", bucketName, UUID.randomUUID());
        String fileName = String.format("%s", file.getOriginalFilename());

        // Uploading file to s3
        PutObjectResult putObjectResult = amazonService.upload(
                path, fileName, Optional.of(metadata), file.getInputStream());

        // Saving metadata to db
        fileMetaRepository.save(new FileMeta(fileName, path, putObjectResult.getMetadata().getVersionId()));

    }

    public S3Object getFileById(int id) {
        FileMeta fileMeta = fileMetaRepository.findById(id).orElseThrow(EntityNotFoundException::new);
        return amazonService.getS3Object(fileMeta.getFilePath(), fileMeta.getFileName());
    }

    public List<S3Object> getAllFiles() {
        List<FileMeta> fileMetaList = fileMetaRepository.findAll();
        if (fileMetaList.isEmpty()) {
            throw new AmazonServiceException("Empty table");
        }
        return fileMetaList.stream()
                .map(fileMeta -> amazonService.getS3Object(fileMeta.getFilePath(), fileMeta.getFileName()))
                .collect(Collectors.toList());
    }

    public void deleteAllFiles(String objectName) throws AmazonServiceException {
        // get fileName
        String s1 = objectName.substring(objectName.indexOf("/") + 1).trim();
        FileMeta fileMeta = fileMetaRepository.findFileMetaByFileName(s1);
        if(Objects.isNull(fileMeta)) {
            throw new AmazonServiceException("File does not exist in DB");
        }
        fileMetaRepository.delete(fileMeta);
    }

    private static void createZipEntry(ZipOutputStream zipOutputStream, S3Object fileName) throws IOException {
        ZipEntry zipEntry = new ZipEntry(fileName.getKey());
        zipEntry.setTime(System.currentTimeMillis());
        zipOutputStream.putNextEntry(zipEntry);
        zipOutputStream.write(fileName.getObjectContent().readAllBytes());
        zipOutputStream.closeEntry();
    }

    public void downloadZipFile(HttpServletResponse response, S3Object s3Object) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream())) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            String headerValue = String.format("attachment; filename=" + s3Object.getKey() + "");
            response.setHeader("Content-Disposition", headerValue);
            createZipEntry(zipOutputStream, s3Object);
            zipOutputStream.finish();
        }
    }

    public List<FileMeta> list() {
        return new ArrayList<>(fileMetaRepository.findAll());
    }
}