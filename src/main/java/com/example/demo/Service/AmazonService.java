package com.example.demo.Service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
@Slf4j
public class AmazonService {

    private final AmazonS3 amazonS3;

    public AmazonService(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
    }

    public PutObjectResult upload(
            String path,
            String fileName,
            Optional<Map<String, String>> optionalMetaData,
            InputStream inputStream) throws IOException {

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(inputStream.available());

        optionalMetaData.ifPresent(map -> {
            if (!map.isEmpty()) {
                map.forEach(objectMetadata::addUserMetadata);
            }
        });
        log.debug("Path: " + path + ", FileName:" + fileName);
        return amazonS3.putObject(path, fileName, inputStream, objectMetadata);
    }

    public S3Object getS3Object(String path, String fileName) {
        return amazonS3.getObject(path, fileName);
    }

    public ListObjectsV2Result listObjects(String bucketName) {
        List<Bucket> bucketList = amazonS3.listBuckets();
        if (bucketList.stream().anyMatch(bucket -> Objects.equals(bucket.getName(), bucketName))) {
            return amazonS3.listObjectsV2(bucketName);
        } else {
            throw new AmazonServiceException("Bucket does not exist");
        }
    }

    public List<Bucket> listBuckets() {
        return amazonS3.listBuckets();
    }

    public Bucket createBucket(String bucket_name) {
        if (amazonS3.doesBucketExistV2(bucket_name)) {
            throw new AmazonServiceException("Bucket already exists");
        } else {
            try {
                return amazonS3.createBucket(bucket_name);
            } catch (AmazonS3Exception e) {
                throw new AmazonServiceException("Could not create bucket with this name");
            }
        }
    }

    public void removeUnversionedObjects(String bucketName) {
        ListObjectsV2Result objects = listObjects(bucketName);
        List<S3ObjectSummary> objectSummaryList = objects.getObjectSummaries();

        if (objectSummaryList.isEmpty()) {
            amazonS3.deleteBucket(bucketName);
        } else {
            for (S3ObjectSummary o : objectSummaryList) {
                amazonS3.deleteObject(bucketName, o.getKey());
                amazonS3.deleteBucket(bucketName);
            }
        }
    }

    public void deleteObject(String bucketName, String objectName) {
        S3ObjectSummary objectSummary = new S3ObjectSummary();
        objectSummary.setKey(objectName);
        amazonS3.deleteObject(bucketName, objectSummary.getKey());
    }
}