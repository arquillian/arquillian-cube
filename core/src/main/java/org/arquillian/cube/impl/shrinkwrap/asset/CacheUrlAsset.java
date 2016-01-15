package org.arquillian.cube.impl.shrinkwrap.asset;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

import org.jboss.shrinkwrap.api.asset.UrlAsset;

/**
 * Class that extends from UrlAsset which basically stores the URL content in temp directory.
 * Every time that same URL is requested to download, first of all it checks if it has been already downloaded previously
 * and in this case, and the among of time since it was downloaded has not expired, then the cache content is used.
 */
public class CacheUrlAsset extends UrlAsset {

    static String TEMP_LOCATION = System.getProperty("java.io.tmpdir");
    private long expirationTime = 1;
    private TimeUnit timeUnit = TimeUnit.HOURS;

    /**
     * Create a new resource with a <code>URL</code> source with default time to live.
     *
     * @param url A valid URL
     * @throws IllegalArgumentException <Code>URL</code> can not be null
     */
    public CacheUrlAsset(URL url) {
        super(url);
    }

    public CacheUrlAsset(URL url, long expirationTime, TimeUnit timeUnit) {
        super(url);
        this.expirationTime = expirationTime;
        this.timeUnit = timeUnit;
    }

    @Override
    public InputStream openStream() {
        final URL source = this.getSource();
        String fileName = getFileName(source);

        File file = new File(TEMP_LOCATION, fileName);
        try {
            if (!file.exists() || !file.isFile() || isExpired(file)) {
                InputStream is = super.openStream();
                Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return new BufferedInputStream(new FileInputStream(file), 8192);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isExpired(File file) throws IOException {
        final BasicFileAttributes fileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
        final long to = fileAttributes.lastModifiedTime().to(TimeUnit.MILLISECONDS);

        final long currentTime = System.currentTimeMillis();
        final long expirationTime = TimeUnit.MILLISECONDS.convert(this.expirationTime, this.timeUnit);
        if (to + expirationTime < currentTime ) {
            return true;
        }

        return false;
    }

    private String getFileName(URL source) {
        final String path = source.getPath();
        String fileName;
        if (path != null) {
            fileName = path.substring(path.lastIndexOf('/') + 1, path.length());
        } else {
            throw new IllegalArgumentException(String.format("URL %s does not contain a valid filename to download.", source.toString()));
        }
        return fileName;
    }
}
