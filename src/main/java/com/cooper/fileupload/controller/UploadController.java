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

package com.cooper.fileupload.controller;

import java.nio.file.Path;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.cooper.exception.FileStorageException;
import com.cooper.fileupload.service.FileStorageService;
import com.cooper.result.Result;

@CrossOrigin
@Controller
@RequestMapping("/api/upload")
public class UploadController {

    @Autowired
    private FileStorageService fileStorageService;


    @GetMapping("/files")
    @ResponseBody
    public ResponseEntity<Stream<Path>> searchFiles() throws FileStorageException {
        Stream<Path> files = fileStorageService.loadAll();
        return ResponseEntity.ok().body(files);
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename)  throws FileStorageException {
        Resource file = fileStorageService.loadAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    @PostMapping("/files/upload")
    public ResponseEntity handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes){

        fileStorageService.store(file);
        return ResponseEntity.ok(Result.ok().build());
    }

    @PostMapping("/files/part")
    @ResponseBody
    public ResponseEntity handleBigFile(HttpServletRequest request, HttpServletResponse response,
                                  String guid, Integer chunk, Integer chunks, MultipartFile file) throws FileStorageException {
        fileStorageService.storeBigFile(request, response, guid, chunk, chunks, file);
        return ResponseEntity.ok(Result.ok().build());
    }

    @PostMapping("/files/merge")
    @ResponseBody
    public ResponseEntity mergeBigFile(String guid, String fileName) throws FileStorageException {
        fileStorageService.mergeFile(guid, fileName);
        return ResponseEntity.ok(Result.ok().build());
    }

    public static void main(String[] args) {
        System.out.println(491*100/491);
        System.out.println((double) 1*100/491);
    }
}
