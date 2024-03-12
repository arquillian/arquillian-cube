package org.arquillian.cube.docker.impl.client.reporter;

import io.restassured.RestAssured;
import io.restassured.builder.ResponseBuilder;
import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.filter.log.LogDetail;
import io.restassured.internal.RestAssuredResponseImpl;
import io.restassured.internal.print.RequestPrinter;
import io.restassured.internal.print.ResponsePrinter;
import io.restassured.response.Response;
import io.restassured.response.ResponseOptions;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.arquillian.cube.docker.restassured.RestAssuredConfiguration;
import org.arquillian.reporter.api.builder.Reporter;
import org.arquillian.reporter.api.event.SectionEvent;
import org.arquillian.reporter.api.model.entry.FileEntry;
import org.arquillian.reporter.config.ReporterConfiguration;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;

import static org.arquillian.cube.docker.impl.client.reporter.DockerEnvironmentReportKey.LOG_PATH;
import static org.arquillian.cube.docker.impl.client.reporter.DockerEnvironmentReportKey.REST_REQUEST_LOG;
import static org.arquillian.cube.docker.impl.client.reporter.DockerEnvironmentReportKey.REST_RESPONSE_LOG;

public class TakeRestAssuredContent {

    @Inject
    Event<SectionEvent> reportEvent;

    // Execute after customizer (org.arquillian.cube.docker.restassured.RestAssuredCustomizer)
    public void registerFilterForRecorder(@Observes(precedence = -10) RestAssuredConfiguration restAssuredConfiguration, ReporterConfiguration reporterConfiguration) {
        RestAssured.filters(new TakeRequest(reportEvent, reporterConfiguration), new TakeResponse(reportEvent, reporterConfiguration));
    }


    public static class TakeResponse implements Filter {

        private Event<SectionEvent> reportEvent;
        private ReporterConfiguration reporterConfiguration;

        public TakeResponse(Event<SectionEvent> reportEvent, ReporterConfiguration reporterConfiguration) {
            this.reportEvent = reportEvent;
            this.reporterConfiguration = reporterConfiguration;
        }

        @Override
        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
            final Response response = ctx.next(requestSpec, responseSpec);

            final ByteArrayOutputStream responseLog = new ByteArrayOutputStream();
            final PrintStream stream = new PrintStream(responseLog);

            ResponsePrinter.print((ResponseOptions)response, response, stream, LogDetail.ALL, true, new HashSet<String>());

            final File logFile = new File(createLogDirectory(new File(reporterConfiguration.getRootDirectory())), "restassuredResponse.log");
            writeContent(responseLog, logFile);

            final Path rootDir = Paths.get(reporterConfiguration.getRootDirectory());
            final Path relativePath = rootDir.relativize(logFile.toPath());

            Reporter.createReport(REST_RESPONSE_LOG)
                    .addKeyValueEntry(LOG_PATH, new FileEntry(relativePath))
                    .inSection(new DockerLogSection())
                    .fire(reportEvent);

            final byte[] responseBody = response.asByteArray();
            return cloneResponseIfNeeded(response, responseBody);
        }

        /*
        * If body expectations are defined we need to return a new Response otherwise the stream
        * has been closed due to the logging.
        */
        private Response cloneResponseIfNeeded(Response response, byte[] responseAsString) {
            if (responseAsString != null && response instanceof RestAssuredResponseImpl && !((RestAssuredResponseImpl) response).getHasExpectations()) {
                final Response build = new ResponseBuilder().clone(response).setBody(responseAsString).build();
                ((RestAssuredResponseImpl) build).setHasExpectations(true);
                return build;
            }
            return response;
        }
    }

    public static class TakeRequest implements Filter {

        private Event<SectionEvent> reportEvent;
        private ReporterConfiguration reporterConfiguration;

        public TakeRequest(Event<SectionEvent> reportEvent, ReporterConfiguration reporterConfiguration) {
            this.reportEvent = reportEvent;
            this.reporterConfiguration = reporterConfiguration;
        }

        @Override
        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
            String uri = requestSpec.getURI();
            uri = urlDecode(uri, Charset.forName(requestSpec.getConfig().getEncoderConfig().defaultQueryParameterCharset()), true);

            final ByteArrayOutputStream requestLog = new ByteArrayOutputStream();
            final PrintStream stream = new PrintStream(requestLog);

            RequestPrinter.print(requestSpec, requestSpec.getMethod(), uri, LogDetail.ALL, new HashSet<String>(), stream, true);

            final File logFile = new File(createLogDirectory(new File(reporterConfiguration.getRootDirectory())), "restassuredRequest.log");
            writeContent(requestLog, logFile);


            final Path rootDir = Paths.get(reporterConfiguration.getRootDirectory());
            final Path relativePath = rootDir.relativize(logFile.toPath());

            Reporter.createReport(REST_REQUEST_LOG)
                    .addKeyValueEntry(LOG_PATH, new FileEntry(relativePath))
                    .inSection(new DockerLogSection())
                    .fire(reportEvent);

            return ctx.next(requestSpec, responseSpec);
        }

        private static String urlDecode(final String content, final Charset charset, final boolean plusAsBlank) {
            if (content == null) {
                return null;
            }
            final ByteBuffer bb = ByteBuffer.allocate(content.length());
            final CharBuffer cb = CharBuffer.wrap(content);
            while (cb.hasRemaining()) {
                final char c = cb.get();
                if (c == '%' && cb.remaining() >= 2) {
                    final char uc = cb.get();
                    final char lc = cb.get();
                    final int u = Character.digit(uc, 16);
                    final int l = Character.digit(lc, 16);
                    if (u != -1 && l != -1) {
                        bb.put((byte) ((u << 4) + l));
                    } else {
                        bb.put((byte) '%');
                        bb.put((byte) uc);
                        bb.put((byte) lc);
                    }
                } else if (plusAsBlank && c == '+') {
                    bb.put((byte) ' ');
                } else {
                    bb.put((byte) c);
                }
            }
            bb.flip();
            return charset.decode(bb).toString();
        }
    }

    private static File createLogDirectory(File rootDirectory) {
        final Path reportsLogs = Paths.get("reports", "logs");
        final Path logsDir = rootDirectory.toPath().resolve(reportsLogs);
        if (Files.notExists(logsDir)) {
            try {

                Files.createDirectories(logsDir);
            } catch (IOException e) {
                throw new IllegalArgumentException(String.format("Could not created logs directory at %s", logsDir));
            }
        }
        return logsDir.toFile();
    }

    private static void writeContent(ByteArrayOutputStream requestLog, File logFile) {
        try {
            Files.write(logFile.toPath(), requestLog.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
