# Playwright 101 Assignment (Java)

Playwright (Java) + TestNG automation for the TestMu AI Selenium Playground assignment:
https://www.testmuai.com/selenium-playground/

## Project layout

```
pom.xml
testng.xml                     # 2 parallel <test> blocks = 2 browser/OS combinations
.env.example                    # copy to .env and fill in TestMu AI credentials
src/test/java/com/testmuai/
  base/BaseTest.java            # local-vs-cloud browser bootstrap (TestNG @BeforeMethod/@AfterMethod)
  tests/
    Scenario1SimpleFormDemoTest.java
    Scenario2DragDropSlidersTest.java
    Scenario3InputFormSubmitTest.java
```

## Scenario coverage

| Scenario | Spec |
|---|---|
| 1 | Simple Form Demo: enter a message, click "Get Checked Value", assert it is echoed under "Your Message:" |
| 2 | Drag & Drop Sliders: drag the "Default value 15" slider to 95, assert the range readout shows 95 |
| 3 | Input Form Submit: submit empty (native "required" validation fires), fill all fields incl. Country = "United States", submit, assert the success message |

At least 3 distinct locator strategies are used across the suite: accessible role + name
(`getByRole`), placeholder text (`getByPlaceholder`), CSS id selectors, and CSS attribute
selectors.

## Running locally (no TestMu AI credentials needed)

```powershell
mvn test
```

This launches local headless Chromium/Firefox browsers so you can verify the scenarios quickly
before spending TestMu AI automation minutes.

## Running on the TestMu AI cloud grid (parallel, 2 browser/OS combos)

1. Copy `.env.example` to `.env` and fill in your TestMu AI username/access key:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Run the suite:

   ```powershell
   mvn test
   ```

   When `LT_USERNAME`/`LT_ACCESS_KEY` are present, `BaseTest` connects each test to the TestMu AI
   CDP endpoint instead of a local browser, with `network`, `video`, `console`, `visual` and `w3c`
   capabilities enabled (network logs, video recording, screenshots and console logs, as required
   by the assignment). `testng.xml` runs the `Windows10-Chromium` and `macOSVentura-WebKit`
   `<test>` blocks in parallel (`parallel="tests" thread-count="2"`), so each scenario executes on
   both browser/OS combinations in the same run.

3. After the run, find the Test Session IDs/recordings on your TestMu AI Automation dashboard for
   the build named `Playwright 101 Assignment` (configurable via `LT_BUILD_NAME` in `.env`).

## Reports

- Surefire results: `target/surefire-reports/`
- Playwright traces (screenshots/snapshots/sources) per test: `traces/*.zip` — open with
  `mvn com.microsoft.playwright:playwright:1.49.0:show-trace traces/<name>.zip` or via
  https://trace.playwright.dev
