package com.example.demo.Controller;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.example.demo.Service.AmazonService;
import com.example.demo.Service.MetadataService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@AllArgsConstructor
public class DashboardController {

    private final MetadataService metadataService;
    private final AmazonService amazonService;


    @GetMapping("/dashboard")
    public String dashboard(Model model) {

        var files = metadataService.list();
        model.addAttribute("files", files);
        return "dashboard";
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(
            @RequestPart("file") MultipartFile file) throws IOException {
        metadataService.upload(file);
        return ResponseEntity.ok("File uploaded!");
    }

    @GetMapping("/downloadZip/{id}")
    @ResponseBody
    public ResponseEntity<?> downloadZip(@PathVariable int id, HttpServletResponse response) throws
            IOException {

        S3Object s3Object = metadataService.getFileById(id);
        metadataService.downloadZipFile(response, s3Object);
        return ResponseEntity.ok("success");
    }

    @GetMapping("/download/{id}")
    @ResponseBody
    public HttpEntity<byte[]> download(@PathVariable int id) throws
            IOException {
        S3Object s3Object = metadataService.getFileById(id);
        // display the file in browser
        String contentType = s3Object.getObjectMetadata().getUserMetaDataOf("content-type");
//        String contentType = s3Object.getObjectMetadata().getContentType();
        var bytes = s3Object.getObjectContent().readAllBytes();

        HttpHeaders header = new HttpHeaders();
        header.setContentType(MediaType.valueOf(contentType));
        header.setContentLength(bytes.length);

        return new HttpEntity<byte[]>(bytes, header);
    }

    @GetMapping("/objects/db")
    @ResponseBody
    public ResponseEntity<?> getObjectsFromDB() {
        try {
            List<S3Object> s3ObjectList = metadataService.getAllFiles();
            return ResponseEntity.ok(s3ObjectList.stream().map(S3Object::getKey).collect(Collectors.toList()));
        } catch (AmazonServiceException e) {
            return ResponseEntity.ok(e.getErrorMessage());
        }
    }

    @PostMapping("/new")
    @ResponseBody
    public ResponseEntity<?> createBucket(@RequestParam String bucketName) {
        try {
            return ResponseEntity.ok(amazonService.createBucket(bucketName));
        } catch (AmazonServiceException e) {
            return ResponseEntity.ok(e.getErrorMessage());
        }
    }

    @GetMapping("/buckets/s3")
    @ResponseBody
    public ResponseEntity<?> getBucketsFromS3() {
        return ResponseEntity.ok(amazonService.listBuckets());
    }

    @GetMapping("/objects/{bucketName}")
    @ResponseBody
    public ResponseEntity<?> getObjectsFromS3(@PathVariable String bucketName) {
        ListObjectsV2Result objectsV2Result = amazonService.listObjects(bucketName);
        return ResponseEntity.ok(objectsV2Result.getObjectSummaries().stream().map(S3ObjectSummary::getKey).collect(Collectors.toList()));
    }

    @DeleteMapping("/bucket/delete")
    @ResponseBody
    public ResponseEntity<?> deleteBucket(@RequestParam String bucketName) {
        try {
            amazonService.removeUnversionedObjects(bucketName);
            return ResponseEntity.ok("Successful delete");
        } catch (AmazonServiceException e) {
            return ResponseEntity.ok(e.getErrorMessage());
        }
    }

    @DeleteMapping("/object/delete")
    @ResponseBody
    public ResponseEntity<?> deleteObject(@RequestParam String bucketName, @RequestParam String objectName) {
        try {
            amazonService.deleteObject(bucketName, objectName);
            metadataService.deleteAllFiles(objectName);
            return ResponseEntity.ok("Successful delete object");
        } catch (AmazonServiceException e) {
            return ResponseEntity.ok(e.getErrorMessage());
        }
    }
}