package org.arquillian.cube.docker.impl.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.arquillian.cube.docker.impl.model.LatestRepository;

public class GitHubUtil {

    private static final String DOCKER_MACHINE_LATEST_URL = "https://api.github.com/repos/docker/machine/releases/latest";
    private static Logger log = Logger.getLogger(GitHubUtil.class.getName());

    public static String getDockerMachineLatestVersion() {
        try {
            URL url = new URL(DOCKER_MACHINE_LATEST_URL);
            LatestRepository latestRepository = consumeHttp(url);
            return latestRepository.getTagName();
        } catch (MalformedURLException e) {
            log.log(Level.WARNING, e.getMessage());
        }

        return "";
    }

    public static LatestRepository consumeHttp(URL url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            ObjectMapper mapper = new ObjectMapper();
            LatestRepository latestRepository = mapper.readValue(connection.getInputStream(), LatestRepository.class);
            return latestRepository;
        } catch (IOException e) {
            log.log(Level.WARNING, e.getMessage());
        }

        return null;
    }
}
