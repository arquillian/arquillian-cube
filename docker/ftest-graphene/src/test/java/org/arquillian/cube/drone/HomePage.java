package org.arquillian.cube.drone;

import org.jboss.arquillian.graphene.page.Location;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

@Location("/")
public class HomePage {

    @FindBy(tagName = "h1")
    private WebElement welcomeMessageElement;

    public void assertOnWelcomePage() {
        assertThat(this.welcomeMessageElement.getText().trim())
            .isEqualTo("Hello world!");
    }
}
