package io.testable.examples;

import io.testable.selenium.TestableSelenium;
import io.testable.selenium.TestableTest;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Simple example of opening the demo OpenFin app and taking a screenshot of the main window once it opens
 */
public class TestableOpenFinExample {

    // if the OUTPUT_DIR environment variable is defined then we are running on Testable
    private static boolean IS_TESTABLE = System.getenv("OUTPUT_DIR") != null;
    private static boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");

    public static void main(String[] args) throws Exception {
        // Get the path to the config file, "binary", and chrome driver port, and the openfin remote debugger port
        String configUrl = getenv("CONFIG_URL", System.getProperty("user.dir") + "/app_sample.json");
        String chromePort = getenv("CHROME_PORT", "12565");
        String chromedriverPort = getenv("CHROMEDRIVER_PORT", "9515");
        String debuggerAddress = "localhost:" + chromePort;

        System.out.println("Launching OpenFin");
        launchOpenFin(configUrl, chromePort);

        System.out.println("Launching chromedriver with debuggerAddress=" + debuggerAddress);

        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.PERFORMANCE, Level.ALL);

        ChromeOptions options = new ChromeOptions();
        options.setExperimentalOption("debuggerAddress", debuggerAddress);
        options.setCapability("goog:loggingPerfs", logPrefs);

        // connect to chromedriver
        WebDriver driver = new RemoteWebDriver(new URL("http://localhost:" + chromedriverPort),
                options);

        try {
            TestableTest test = TestableSelenium.startTest("OpenFin Demo Test");

            test.startStep("Wait for the OpenFin runtime to launch");
            waitForFinDesktop(driver);
            test.finishSuccessfulStep();

            test.startStep("Switch to the 'Hello OpenFin' window by title");
            switchWindowByTitle(driver, "Hello OpenFin");
            test.finishSuccessfulStep();

            test.startStep("Take a screenshot");
            Path screenshot = TestableSelenium.takeScreenshot(driver, "main.png");
            test.finishSuccessfulStep();
            // if it is running locally copy the screenshot to the project root directory
            if (!IS_TESTABLE)
                Files.copy(screenshot, Paths.get("main.png"), StandardCopyOption.REPLACE_EXISTING);

            LogEntries logEntries = driver.manage().logs().get(LogType.PERFORMANCE);
            for (LogEntry entry : logEntries) {
                System.out.println(entry.toString());
            }
            test.finish();
        } finally {
            exitOpenFin(driver);
        }
    }

    private static Process launchOpenFin(String configUrl, String chromePort) throws IOException {
        ProcessBuilder builder = new ProcessBuilder("openfin", "-l", "-c", configUrl).inheritIO();
        builder.environment().put("runtimeArgs", "--remote-debugging-port=" + chromePort);
        return builder.start();
    }

    private static String getenv(String name, String defaultValue) {
        String answer = System.getenv(name);
        return answer != null && answer.length() > 0 ? answer : defaultValue;
    }

    private static void exitOpenFin(WebDriver driver) {
        ((JavascriptExecutor) driver).executeScript("fin.desktop.System.exit();");
        driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
    }

    private static Boolean checkFinGetVersion(WebDriver driver) {
        return (Boolean)((JavascriptExecutor) driver).executeAsyncScript(
                "var done = arguments[arguments.length - 1];" +
                "if (fin && fin.desktop && fin.desktop.System && fin.desktop.System.getVersion) {\n" +
                "  done(true);\n" +
                "} else {\n" +
                "  done(false);\n" +
                "}");
    }

    private static void waitForFinDesktop(WebDriver driver) {
        if (!checkFinGetVersion(driver)) {
            driver.manage().timeouts().implicitlyWait(1, TimeUnit.SECONDS);
            waitForFinDesktop(driver);
        }
    }

    private static boolean switchWindowByTitle(WebDriver driver, String title) {
        String currentWindow = driver.getWindowHandle();
        Set<String> availableWindows = driver.getWindowHandles();
        if (!availableWindows.isEmpty()) {
            for (String windowId : availableWindows) {
                if (driver.switchTo().window(windowId).getTitle().equals(title)) {
                    return true;
                } else {
                    driver.switchTo().window(currentWindow);
                }
            }
        }
        try {
            Integer.getInteger("TESTABLE_GLOBAL_CLIENT_INDEX")
        } catch(Exception e) {

        }
        return false;
    }

}
