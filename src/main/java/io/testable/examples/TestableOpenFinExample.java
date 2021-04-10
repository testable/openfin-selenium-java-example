package io.testable.examples;

import io.testable.selenium.TestableSelenium;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Simple example of opening the demo OpenFin app and taking a screenshot of the main window once it opens
 */
public class TestableOpenFinExample {

    // if the OUTPUT_DIR environment variable is defined then we are running on Testable
    private static boolean IS_TESTABLE = System.getenv("OUTPUT_DIR") != null;

    public static void main(String[] args) throws Exception {
        // Get the path to the config file, "binary", and chrome driver port, and the openfin remote debugger port
        String configUrl = getenv("CONFIG_URL", System.getProperty("user.dir") + "/app_sample.json");
        String binary = Paths.get("RunOpenFin.sh").toAbsolutePath().toString();
        String chromedriverPort = getenv("CHROMEDRIVER_PORT", "9515");

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--config=" + configUrl);
        options.setBinary(binary);

        // connect to chromedriver
        WebDriver driver = new RemoteWebDriver(new URL("http://localhost:" + chromedriverPort),
                options);

        try {
            // wait for OpenFin to become available
            waitForFinDesktop(driver);
            // switch to the main window
            switchWindowByTitle(driver, "Hello OpenFin");
            // take a screenshot
            Path screenshot = TestableSelenium.takeScreenshot(driver, "main.png");
            // if it is running locally copy the screenshot to the project root directory
            if (!IS_TESTABLE)
                Files.copy(screenshot, Paths.get("main.png"), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            exitOpenFin(driver);
        }
    }

    private static String getenv(String name, String defaultValue) {
        String answer = System.getenv(name);
        return answer != null && answer.length() > 0 ? name : defaultValue;
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
        return false;
    }

}
