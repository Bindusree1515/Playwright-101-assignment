package com.testmuai.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.testmuai.base.BaseTest;
import org.testng.annotations.Test;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class Scenario1SimpleFormDemoTest extends BaseTest {

    @Test(description = "Entered message is echoed back after clicking \"Get Checked Value\"")
    public void simpleFormDemoEchoesEnteredMessage() {
        // Step 1: open the Selenium Playground
        page.navigate(BASE_URL + "/selenium-playground/");
        waitForPageReady();

        // Step 2: click "Simple Form Demo" -- Locator #1: accessible role + name
        clickRobust(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Simple Form Demo").setExact(true)));

        // Step 3: validate the URL contains "simple-form-demo"
        assertThat(page).hasURL(Pattern.compile("simple-form-demo"));
        waitForPageReady();

        // Step 4: variable holding the string value to be used in the test
        String message = "Welcome to TestMu AI";

        // Step 5: enter the value in the "Enter Message" text box -- Locator #2: placeholder text
        Locator messageInput = page.getByPlaceholder("Please enter your Message");
        messageInput.fill(message);

        // Step 6: click "Get Checked Value" -- Locator #3: CSS id selector
        Locator checkedValueButton = page.locator("button#showInput");
        Locator yourMessage = page.locator("p#message");
        retryUntil(() -> clickRobust(checkedValueButton), () -> message.equals(yourMessage.textContent()), 3);

        // Step 7: validate the same text is displayed under "Your Message:"
        assertThat(yourMessage).hasText(message, new LocatorAssertions.HasTextOptions().setTimeout(10000));
    }
}
