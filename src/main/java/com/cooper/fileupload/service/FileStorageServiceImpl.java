/**
 * Copyright 2019 China Mobile Communications Group Co.,Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cooper.fileupload.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.cooper.config.FileStorageConfigure;
import com.cooper.exception.FileStorageException;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;

    @Autowired
    public FileStorageServiceImpl(FileStorageConfigure fileStorageConfigure) throws FileStorageException {
        this.fileStorageLocation = Paths.get(fileStorageConfigure.getUpLoadDir()).toAbsolutePath().normalize();
        try {
            boolean isExist = Files.exists(this.fileStorageLocation, LinkOption.NOFOLLOW_LINKS);
            if (!isExist) {
                Files.createDirectories(this.fileStorageLocation);
            }
        } catch (Exception ex) {
            throw new FileStorageException("Could not create the directory where the uploaded files will be saved", ex);
        }
    }

    @Override
    public String store(MultipartFile file) throws FileStorageException {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (file.isEmpty()) {
                throw new FileStorageException("Failed to store empty file " + filename);
            }
            if (filename.contains("..")) {
                // This is a security check
                throw new FileStorageException(
                        "Cannot store file with relative path outside current directory " + filename);
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, this.fileStorageLocation.resolve(filename),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new FileStorageException("Failed to store file " + filename, e);
        }

        return filename;
    }

    @Override
    public Stream<Path> loadAll() throws FileStorageException {
        try {
            return Files.walk(this.fileStorageLocation, 1)
                    .filter(path -> !path.equals(this.fileStorageLocation))
                    .map(this.fileStorageLocation::relativize);
        } catch (IOException e) {
            throw new FileStorageException("Failed to read stored files", e);
        }
    }

    @Override
    public Path load(String filename) throws FileStorageException {
        return this.fileStorageLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) throws FileStorageException {
        try {
            Path file = load(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new FileStorageException("Could not read file: " + filename);

            }
        } catch (MalformedURLException e) {
            throw new FileStorageException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void deleteAll() throws FileStorageException {
        FileSystemUtils.deleteRecursively(this.fileStorageLocation.toFile());
    }

    @Override
    public void storeBigFile(HttpServletRequest request, HttpServletResponse response,
                               String guid, Integer chunk, Integer chunks, MultipartFile file) throws FileStorageException {
        try {
            boolean isMultipart = ServletFileUpload.isMultipartContent(request);
            if (isMultipart) {
                if (chunk == null) chunk = 0;
                // 临时目录用来存放所有分片文件
                Path tempFileDir = Paths.get(this.fileStorageLocation.toAbsolutePath().toString(), guid);
                boolean isExist = Files.exists(tempFileDir, LinkOption.NOFOLLOW_LINKS);
                if (!isExist) {
                    Files.createDirectories(tempFileDir);
                }
                // 分片处理时，前台会多次调用上传接口，每次都会上传文件的一部分到后台
                try (InputStream inputStream = file.getInputStream()) {
                    Files.copy(inputStream, tempFileDir.resolve(guid + "_" + chunk + ".part"), StandardCopyOption.REPLACE_EXISTING);
                }
            }

        } catch (Exception e) {
            throw new FileStorageException("Could not store file ", e);
        }
    }

    @Override
    public void mergeFile(String guid, String fileName) throws FileStorageException {
        // 得到 destTempFile 就是最终的文件
        String path = fileStorageLocation.toAbsolutePath().toString();
        File parentFileDir = new File(path + File.separator + guid);
        try {
            if (parentFileDir.isDirectory()) {
                File destTempFile = new File(path , fileName);
                if (!destTempFile.exists()) {
                    //先得到文件的上级目录，并创建上级目录，在创建文件
                    destTempFile.getParentFile().mkdir();
                    try {
                        destTempFile.createNewFile();
                    } catch (IOException e) {
                        throw new FileStorageException("Could not merge file ", e);
                    }
                }
                for (int i = 0; i < parentFileDir.listFiles().length; i++) {
                    File partFile = new File(parentFileDir, guid + "_" + i + ".part");
                    FileOutputStream destTempfos = new FileOutputStream(destTempFile, true);
                    //遍历"所有分片文件"到"最终文件"中
                    FileUtils.copyFile(partFile, destTempfos);
                    destTempfos.close();
                }

            } else {
                throw new FileStorageException("Could not found directory ");
            }
        } catch (Exception e) {
            throw new FileStorageException("Could not merge file ", e);
        } finally {
            // 删除临时目录中的分片文件
            try {
                FileUtils.deleteDirectory(parentFileDir);
            } catch (IOException e) {
                throw new FileStorageException("Delete temp directory error ", e);
            }
        }
    }
}
