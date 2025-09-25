package cz.muriel.core.storage.service;

import io.awspring.cloud.s3.S3Resource;
import io.awspring.cloud.s3.S3Template;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.UUID;

/**
 * Služba pro správu souborů v S3-kompatibilním úložišti. Poskytuje metody pro
 * nahrávání, stahování a získávání URL souborů.
 */
@Service
public class StorageService {

    private final S3Template s3Template;
    private final String bucketName;

    public StorageService(S3Template s3Template,
            @Value("${storage.s3.bucket-name}") String bucketName) {
        this.s3Template = s3Template;
        this.bucketName = bucketName;
    }

    /**
     * Nahraje soubor do S3 úložiště.
     *
     * @param file Soubor k nahrání.
     * @return Unikátní klíč (název souboru) v S3.
     * @throws IOException Pokud dojde k chybě při čtení souboru.
     */
    public String uploadFile(MultipartFile file) throws IOException {
        String key = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
        s3Template.upload(bucketName, key, file.getInputStream());
        return key;
    }

    /**
     * Získá veřejně dostupnou URL k souboru v S3.
     *
     * @param key Klíč souboru v S3.
     * @return URL adresa k souboru.
     */
    public URL getFileUrl(String key) {
        S3Resource resource = s3Template.download(bucketName, key);
        try {
            return resource.getURL();
        } catch (IOException e) {
            // V praxi by zde mělo být robustnější ošetření chyb
            throw new RuntimeException("Failed to get file URL for key: " + key, e);
        }
    }

    /**
     * Stáhne soubor jako S3Resource.
     *
     * @param key Klíč souboru v S3.
     * @return S3Resource, který umožňuje přístup k obsahu souboru.
     */
    public S3Resource downloadFile(String key) {
        return s3Template.download(bucketName, key);
    }

    /**
     * Smaže soubor z S3 úložiště.
     *
     * @param key Klíč souboru v S3.
     */
    public void deleteFile(String key) {
        s3Template.deleteObject(bucketName, key);
    }
}
