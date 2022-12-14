package com.example.demo.Repository;

import com.example.demo.Model.FileMeta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FileMetaRepository extends JpaRepository<FileMeta, Integer> {
    FileMeta findFileMetaByFileName(String fileName);
}