package org.arquillian.cube.restassured;

import io.restassured.RestAssured;
import org.hamcrest.CoreMatchers;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.net.MalformedURLException;

import static org.hamcrest.CoreMatchers.equalTo;

@RunWith(Arquillian.class)
public class PingPongTest {

    @Test
    public void should_receive_ok_message() throws MalformedURLException, InterruptedException {
        RestAssured.get().then().assertThat().body("status", equalTo("OK"));
    }

}
