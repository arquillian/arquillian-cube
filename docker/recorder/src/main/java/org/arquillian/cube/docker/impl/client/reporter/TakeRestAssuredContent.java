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
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import org.arquillian.cube.docker.restassured.RestAssuredConfiguration;
import org.arquillian.recorder.reporter.event.PropertyReportEvent;
import org.arquillian.recorder.reporter.model.entry.GroupEntry;
import org.arquillian.recorder.reporter.model.entry.TextEntry;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

public class TakeRestAssuredContent {

    @Inject
    Event<PropertyReportEvent> propertyReportEvent;

    // Execute after customizer (org.arquillian.cube.docker.restassured.RestAssuredCustomizer)
    public void registerFilterForRecorder(@Observes(precedence = -10) RestAssuredConfiguration restAssuredConfiguration) {
        RestAssured.filters(new TakeRequest(propertyReportEvent), new TakeResponse(propertyReportEvent));
    }


    public static class TakeResponse implements Filter {

        private Event<PropertyReportEvent> propertyReportEvent;

        public TakeResponse(Event<PropertyReportEvent> propertyReportEvent) {
            this.propertyReportEvent = propertyReportEvent;
        }

        @Override
        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
            final Response response = ctx.next(requestSpec, responseSpec);

            final ByteArrayOutputStream responseLog = new ByteArrayOutputStream();
            final PrintStream stream = new PrintStream(responseLog);

            ResponsePrinter.print(response, response, stream, LogDetail.ALL, true);

            final GroupEntry request = new GroupEntry("Response Log");
            request.getPropertyEntries().add(new TextEntry(
                    new String(responseLog.toByteArray())
            ));
            propertyReportEvent.fire(new PropertyReportEvent(request));

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

        private Event<PropertyReportEvent> propertyReportEvent;

        public TakeRequest(Event<PropertyReportEvent> propertyReportEvent) {
            this.propertyReportEvent = propertyReportEvent;
        }

        @Override
        public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec, FilterContext ctx) {
            String uri = requestSpec.getURI();
            uri = urlDecode(uri, Charset.forName(requestSpec.getConfig().getEncoderConfig().defaultQueryParameterCharset()), true);

            final ByteArrayOutputStream requestLog = new ByteArrayOutputStream();
            final PrintStream stream = new PrintStream(requestLog);

            RequestPrinter.print(requestSpec, requestSpec.getMethod(), uri, LogDetail.ALL, stream, true);

            final GroupEntry response = new GroupEntry("Request Log");
            response.getPropertyEntries().add(new TextEntry(
                    new String(requestLog.toByteArray())
            ));

            propertyReportEvent.fire(new PropertyReportEvent(response));

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

}
