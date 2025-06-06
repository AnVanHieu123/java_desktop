package com.javaadv.Controller;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class MultipartBodyPublisher {

    private static final String BOUNDARY = "----WebKitFormBoundary" + System.currentTimeMillis();
    private final ByteArrayOutputStream outputStream;

    public MultipartBodyPublisher() {
        this.outputStream = new ByteArrayOutputStream();
    }

    // Thêm phần text vào form-data
    public MultipartBodyPublisher addPart(String key, String value) throws IOException {
        writeBoundary();
        outputStream.write(("--" + BOUNDARY + "\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Disposition: form-data; name=\"" + key + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
        return this;
    }

    // Thêm phần file vào form-data
    public MultipartBodyPublisher addPart(String key, File file) throws IOException {
        writeBoundary();
        outputStream.write(("--" + BOUNDARY + "\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Disposition: form-data; name=\"" + key + "\"; filename=\"" + file.getName() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        outputStream.write(("Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        outputStream.write("\r\n".getBytes(StandardCharsets.UTF_8));
        return this;
    }

    private void writeBoundary() throws IOException {
        if (outputStream.size() > 0) {
            outputStream.write(("--" + BOUNDARY + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
    }

    public HttpRequest.BodyPublisher build() throws IOException {
        outputStream.write(("--" + BOUNDARY + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return HttpRequest.BodyPublishers.ofByteArray(outputStream.toByteArray());
    }

    public String getContentType() {
        return "multipart/form-data; boundary=" + BOUNDARY;
    }
}