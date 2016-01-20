package org.arquillian.cube.docker.impl.util;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import org.junit.Test;

public class GitHubUtilTest {

	@Test
	public void test_download_docker_machine() throws IOException {
		HttpURLConnection connection = mock(HttpURLConnection.class);
		URL url = mockUrl(connection);
		String content = "{\"id\":\"1\", \"name\":\"v0.5.5\", \"tag_name\":\"v0.5.5\"}";
		when(connection.getInputStream()).thenReturn(new ByteArrayInputStream(content.getBytes()));
		assertThat(GitHubUtil.consumeHttp(url).getTagName(), is("v0.5.5"));
	}

	private URL mockUrl(final URLConnection con) throws IOException {
		final URLStreamHandler urlHandler = new URLStreamHandler() {
			@Override protected URLConnection openConnection(URL u) throws IOException {
				return con;
			}
		};

		return new URL("http", "some-host", 0, "", urlHandler);
	}

}
