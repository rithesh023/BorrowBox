package com.borrowbox.service;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ImageStorageService {
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    private final Path root;
    private final String cloudName;
    private final String cloudApiKey;
    private final String cloudApiSecret;
    private final boolean cloudinaryRequired;
    private final RestClient restClient;

    public ImageStorageService(@Value("${app.upload.directory:uploads}") String directory,
                               @Value("${app.upload.cloudinary.cloud-name:}") String cloudName,
                               @Value("${app.upload.cloudinary.api-key:}") String cloudApiKey,
                               @Value("${app.upload.cloudinary.api-secret:}") String cloudApiSecret,
                               @Value("${app.upload.cloudinary.required:false}") boolean cloudinaryRequired) {
        this.root = Paths.get(directory).toAbsolutePath().normalize();
        this.cloudName = cloudName.trim();
        this.cloudApiKey = cloudApiKey.trim();
        this.cloudApiSecret = cloudApiSecret.trim();
        this.cloudinaryRequired = cloudinaryRequired;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose an image file first");
        if (file.getSize() > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image must be 5 MB or smaller");

        byte[] bytes;
        try { bytes = file.getBytes(); }
        catch (IOException exception) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the uploaded image"); }
        String extension = extensionFor(bytes);
        if (extension == null) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Use a valid JPG, PNG, WEBP, or GIF image");

        String filename = UUID.randomUUID() + "." + extension;
        if (cloudinaryConfigured()) return storeInCloudinary(bytes, filename);
        if (cloudinaryRequired) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Image storage is not configured. Add the Cloudinary environment variables to the backend.");
        }
        try {
            Files.createDirectories(root);
            Files.write(root.resolve(filename), bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save the image on the server");
        }
        return "/api/uploads/" + filename;
    }

    private boolean cloudinaryConfigured() {
        boolean anyCredential = !cloudName.isBlank() || !cloudApiKey.isBlank() || !cloudApiSecret.isBlank();
        boolean allCredentials = !cloudName.isBlank() && !cloudApiKey.isBlank() && !cloudApiSecret.isBlank();
        if (anyCredential && !allCredentials) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Cloudinary image storage needs a cloud name, API key, and API secret.");
        }
        if (allCredentials && !cloudName.matches("[A-Za-z0-9_-]+")) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Cloudinary cloud name is invalid.");
        }
        return allCredentials;
    }

    private String storeInCloudinary(byte[] bytes, String filename) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new ByteArrayResource(bytes) {
            @Override public String getFilename() { return filename; }
        });
        String credentials = Base64.getEncoder().encodeToString(
                (cloudApiKey + ":" + cloudApiSecret).getBytes(StandardCharsets.UTF_8));
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("https://api.cloudinary.com/v1_1/{cloudName}/image/upload", cloudName)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .header("Authorization", "Basic " + credentials)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            Object secureUrl = response == null ? null : response.get("secure_url");
            if (!(secureUrl instanceof String url) || !url.startsWith("https://")) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Image storage did not return a secure image URL.");
            }
            URI.create(url);
            return url;
        } catch (RestClientResponseException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Cloudinary could not store the image (HTTP " + exception.getStatusCode().value() + ").");
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not connect to Cloudinary to store the image.");
        }
    }

    public Resource load(String filename) {
        if (filename == null || !filename.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\.(jpg|png|webp|gif)"))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        Path file = root.resolve(filename).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
            return resource;
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        }
    }

    public String contentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }

    private String extensionFor(byte[] bytes) {
        if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) return "jpg";
        if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47
                && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) return "png";
        if (bytes.length >= 12 && ascii(bytes, 0, "RIFF") && ascii(bytes, 8, "WEBP")) return "webp";
        if (bytes.length >= 6 && (ascii(bytes, 0, "GIF87a") || ascii(bytes, 0, "GIF89a"))) return "gif";
        return null;
    }

    private boolean ascii(byte[] bytes, int offset, String value) {
        if (bytes.length < offset + value.length()) return false;
        for (int index = 0; index < value.length(); index++) if (bytes[offset + index] != (byte) value.charAt(index)) return false;
        return true;
    }
}
