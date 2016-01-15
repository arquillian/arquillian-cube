package org.arquillian.cube.impl.shrinkwrap.asset;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class CacheUrlAssetTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldCacheFirstTime() throws IOException {
        final File newFolder = temporaryFolder.newFolder();
        CacheUrlAsset cacheUrlAsset = new CacheUrlAsset(new URL("http://arquillian.org/images/arquillian_crown_icon_glossy_256.png"));
        CacheUrlAsset.TEMP_LOCATION = newFolder.getAbsolutePath();
        cacheUrlAsset.openStream();

        assertThat(new File(newFolder, "arquillian_crown_icon_glossy_256.png").exists(), is(true));
    }

    @Test
    public void shouldGetCachedResult() throws IOException {
        final File newFolder = temporaryFolder.newFolder();
        // Notice that arq.txt does not exists
        CacheUrlAsset cacheUrlAsset = new CacheUrlAsset(new URL("http://arquillian.org/images/arq.txt"));
        CacheUrlAsset.TEMP_LOCATION = newFolder.getAbsolutePath();
        final Path path = Paths.get(newFolder.getAbsolutePath(), "arq.txt");
        Files.write(path, "Hello".getBytes("UTF-8"));
        InputStream is = cacheUrlAsset.openStream();
        String content = slurp(is);

        assertThat(content, is("Hello"));
    }

    @Test
    public void shouldDownloadFileIfExpired() throws IOException, InterruptedException {
        final File newFolder = temporaryFolder.newFolder();
        final Path path = Paths.get(newFolder.getAbsolutePath(), "arquillian_crown_icon_glossy_256.png");
        Files.write(path, "invalidchunk".getBytes("UTF-8"));
        Thread.sleep(3000);
        CacheUrlAsset cacheUrlAsset = new CacheUrlAsset(new URL("http://arquillian.org/images/arquillian_crown_icon_glossy_256.png"), 2, TimeUnit.SECONDS);
        CacheUrlAsset.TEMP_LOCATION = newFolder.getAbsolutePath();
        cacheUrlAsset.openStream();
        File newFile = new File(newFolder, "arquillian_crown_icon_glossy_256.png");
        assertThat(newFile.length(), is(Matchers.greaterThan(500L)));

    }

    private static String slurp(final InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        int length;
        while ((length = in.read(buffer)) != -1) {
            out.write(buffer, 0, length);
        }
        out.flush();
        return new String(out.toByteArray());
    }

}
