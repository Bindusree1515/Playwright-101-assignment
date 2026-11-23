package com.testmuai.base;

import com.google.gson.Gson;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.options.LoadState;
import io.github.cdimascio.dotenv.Dotenv;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Base class shared by every scenario test.
 *
 * <p>When {@code LT_USERNAME}/{@code LT_ACCESS_KEY} are present (via environment variables or a
 * local {@code .env} file), the browser connects to the TestMu AI (formerly LambdaTest) cloud
 * grid with network logs, video, console logs and visual capture enabled, as required by the
 * assignment. Otherwise it falls back to a local Playwright-managed browser for quick
 * verification.</p>
 */
public class BaseTest {

    protected static final String BASE_URL = "https://www.testmuai.com";

    private static final Dotenv DOTENV = Dotenv.configure().ignoreIfMissing().load();

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

    @BeforeMethod
    @Parameters({"browserName", "browserVersion", "platform", "testName"})
    public void setUp(@Optional("pw-chromium") String browserName,
                       @Optional("latest") String browserVersion,
                       @Optional("Windows 10") String platform,
                       @Optional("Playwright 101 Assignment") String testName) {
        playwright = Playwright.create();

        String username = env("LT_USERNAME");
        String accessKey = env("LT_ACCESS_KEY");
        boolean isCloudRun = isNotBlank(username) && isNotBlank(accessKey);

        browser = isCloudRun
                ? connectToTestMuAI(browserName, browserVersion, platform, testName, username, accessKey)
                : launchLocalBrowser(browserName);

        context = browser.newContext();
        context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));
        page = context.newPage();
    }

    @AfterMethod
    public void tearDown(ITestResult result) {
        if (context != null) {
            String traceName = "traces/" + result.getMethod().getMethodName() + "-" + result.getTestContext().getName() + ".zip";
            context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get(traceName)));
            context.close();
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    private Browser launchLocalBrowser(String browserName) {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(true);
        return switch (browserName) {
            case "pw-firefox" -> playwright.firefox().launch(options);
            case "pw-webkit" -> playwright.webkit().launch(options);
            default -> playwright.chromium().launch(options);
        };
    }

    private Browser connectToTestMuAI(String browserName, String browserVersion, String platform,
                                       String testName, String username, String accessKey) {
        Map<String, Object> ltOptions = new LinkedHashMap<>();
        ltOptions.put("platform", platform);
        ltOptions.put("build", envOrDefault("LT_BUILD_NAME", "Playwright 101 Assignment"));
        ltOptions.put("name", testName);
        ltOptions.put("user", username);
        ltOptions.put("accessKey", accessKey);
        // Required by the assignment: network logs, video recording, screenshots and console logs.
        ltOptions.put("network", true);
        ltOptions.put("video", true);
        ltOptions.put("console", true);
        ltOptions.put("visual", true);
        ltOptions.put("w3c", true);

        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("browserName", browserName);
        capabilities.put("browserVersion", browserVersion);
        capabilities.put("LT:Options", ltOptions);

        String capsJson = new Gson().toJson(capabilities);
        String wsEndpoint = "wss://cdp.lambdatest.com/playwright?capabilities="
                + URLEncoder.encode(capsJson, StandardCharsets.UTF_8);

        // TestMu AI's CDP tunnel is always reached through the chromium connect API;
        // the actual remote browser is selected by the "browserName" capability above.
        // The CDP tunnel occasionally drops the handshake (ECONNRESET); retry a couple of
        // times before surfacing the failure, since a single network blip shouldn't burn
        // an entire RetryAnalyzer attempt.
        PlaywrightException lastError = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return playwright.chromium().connect(wsEndpoint);
            } catch (PlaywrightException e) {
                lastError = e;
            }
        }
        throw lastError;
    }

    private static String env(String key) {
        String value = System.getenv(key);
        if (!isNotBlank(value)) {
            value = DOTENV.get(key);
        }
        return value;
    }

    private static String envOrDefault(String key, String fallback) {
        String value = env(key);
        return isNotBlank(value) ? value : fallback;
    }

    private static boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * On a brand-new browser context the page's own JS (event handlers, widgets) can still be
     * initializing right after navigation; interacting immediately sometimes gets silently
     * dropped. Waiting for the load event, plus a short best-effort network-idle wait, lets those
     * scripts finish attaching before the test starts clicking/filling.
     */
    protected void waitForPageReady() {
        page.waitForLoadState(LoadState.LOAD);
        try {
            page.waitForLoadState(LoadState.NETWORKIDLE, new Page.WaitForLoadStateOptions().setTimeout(8000));
        } catch (PlaywrightException ignored) {
            // This site's chat-widget/analytics scripts keep polling the network indefinitely;
            // a short best-effort wait is enough, so a timeout here is not a real failure.
        }
        scrollRevealNudge();
    }

    /**
     * On the remote TestMu AI Firefox grid, scroll-reveal widgets (elements that start with a
     * "hidden" class and get un-hidden by a scroll/IntersectionObserver listener) sometimes never
     * fire because the page never actually scrolls. A tiny scroll nudge triggers those listeners
     * consistently without affecting Chromium, where the reveal already fires on its own. Call
     * this after any action (navigation, form submit) that can reveal new "hidden" content.
     */
    protected void scrollRevealNudge() {
        page.mouse().wheel(0, 300);
        page.mouse().wheel(0, -300);
    }

    /**
     * Repeats {@code action} (e.g. a click) until {@code condition} is satisfied, up to
     * {@code maxAttempts} times. The live third-party site occasionally ignores the first
     * interaction (ads/chat-widget scripts competing for the event loop), so this gives the page
     * a few extra chances to react before the test's own assertion reports a real failure.
     */
    protected void retryUntil(Runnable action, BooleanSupplier condition, int maxAttempts) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            action.run();
            page.waitForTimeout(500);
            if (condition.getAsBoolean()) {
                return;
            }
        }
    }

    /**
     * On the remote TestMu AI Firefox grid, Playwright's built-in scroll-into-view/viewport check
     * can keep reporting "element is outside of the viewport" for links near the page header, even
     * after an explicit native scroll (and even a "force" click still performs that same viewport
     * check). Falling back to dispatching a native DOM click fully bypasses Playwright's
     * viewport/actionability checks, which is safe here since the target is always a plain
     * {@code <a href>} navigation link.
     */
    protected void clickRobust(Locator locator) {
        locator.evaluate("el => el.scrollIntoView({block: 'center'})");
        page.waitForTimeout(300);
        try {
            locator.click(new Locator.ClickOptions().setTimeout(10000));
        } catch (PlaywrightException e) {
            locator.evaluate("el => el.click()");
        }
    }
}
