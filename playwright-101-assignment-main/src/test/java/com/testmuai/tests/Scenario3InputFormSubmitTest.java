package com.testmuai.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import com.testmuai.base.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class Scenario3InputFormSubmitTest extends BaseTest {

    @Test(description = "Shows a required-field validation error, then submits successfully once filled")
    public void inputFormValidationThenSuccessfulSubmit() {
        // Step 1: open the Selenium Playground and go to "Input Form Submit"
        page.navigate(BASE_URL + "/selenium-playground/");
        waitForPageReady();
        clickRobust(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Input Form Submit")));
        assertThat(page).hasURL(Pattern.compile("input-form-demo"));
        waitForPageReady();

        // Locator #1: CSS id selector
        Locator nameInput = page.locator("#name");
        // Locator #2: accessible role + name
        Locator submitButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Submit"));

        // Step 2: click Submit without filling in any information
        clickRobust(submitButton);

        // Step 3: all fields are HTML5 "required", so the browser blocks submission natively.
        // We assert that native validation fired (field is invalid with a non-empty message),
        // since the exact wording of "Please fill in the fields" is rendered by the browser itself
        // and is not part of the page's DOM text.
        boolean isNameValid = (boolean) nameInput.evaluate("el => el.checkValidity()");
        String validationMessage = (String) nameInput.evaluate("el => el.validationMessage");
        Assert.assertFalse(isNameValid, "Name field should be invalid when submitted empty");
        Assert.assertTrue(validationMessage != null && !validationMessage.isEmpty(),
                "Browser should show a required-field validation message");
        System.out.println("Required-field validation message shown by the browser: \"" + validationMessage + "\"");

        // Step 4: fill in Name, Email, and other fields
        nameInput.fill("John Doe");
        page.locator("#inputEmail4").fill("john.doe@example.com");
        page.locator("#inputPassword4").fill("P@ssw0rd123");
        page.locator("#company").fill("TestMu AI");
        page.locator("#websitename").fill("https://www.testmuai.com");

        // Step 5: select "United States" from the Country drop-down using its visible text
        // Locator #3: CSS attribute selector
        page.locator("select[name=\"country\"]").selectOption(new SelectOption().setLabel("United States"));

        page.locator("#inputCity").fill("New York");
        page.locator("#inputAddress1").fill("123 Main Street");
        page.locator("#inputAddress2").fill("Apt 4B");
        page.locator("#inputState").fill("New York");
        page.locator("#inputZip").fill("10001");

        // Step 6: submit the completed form
        // (clicking Submit twice on this page is not idempotent, so a single click plus a
        // generous auto-retrying assertion is more reliable than retrying the click itself.)
        clickRobust(submitButton);
        scrollRevealNudge();

        // Step 7: validate the success message
        Locator successMessage = page.locator("p.success-msg");
        assertThat(successMessage).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(15000));
        assertThat(successMessage).hasText("Thanks for contacting us, we will get back to you shortly.",
                new LocatorAssertions.HasTextOptions().setTimeout(15000));
    }
}
