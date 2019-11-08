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

import java.nio.file.Path;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.cooper.exception.FileStorageException;

public interface FileStorageService {
    /**
     * 小文件上传
     * @param file
     * @return
     * @throws FileStorageException
     */
    String store(MultipartFile file) throws FileStorageException;

    /**
     * 文件列表
     * @return
     * @throws FileStorageException
     */
    Stream<Path> loadAll() throws FileStorageException;

    /**
     * 文件下载
     * @param filename
     * @return
     * @throws FileStorageException
     */
    Path load(String filename) throws FileStorageException;

    /**
     * 文件下载
     * @param filename
     * @return
     * @throws FileStorageException
     */
    Resource loadAsResource(String filename) throws FileStorageException;

    /**
     * 删除所有文件
     * @throws FileStorageException
     */
    void deleteAll() throws FileStorageException;

    /**
     * 删除文件
     * @param guid
     * @throws FileStorageException
     */
    void deleteChunk(String guid) throws Exception;

    /**
     * 大文件分片处理
     * @param request
     * @param response
     * @param guid
     * @param chunk   当前分片
     * @param file
     * @param chunks  总分片
     * @return
     */
    void storeBigFile(HttpServletRequest request, HttpServletResponse response,
                      String guid, Integer chunk, Integer chunks, MultipartFile file) throws FileStorageException;

    /**
     * 大文件分片文件合并
     * @param guid
     * @param fileName
     * @return
     */
    void mergeFile(String guid, String fileName) throws FileStorageException;
}
