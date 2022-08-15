package io.testable.examples;

import io.testable.selenium.TestableSelenium;
import io.testable.selenium.TestableTest;
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
    private static boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");
    private static int DEFAULT_DEBUGGING_PORT = 12565;

    public static void main(String[] args) throws Exception {
        // Get the path to the config file, "binary", and chrome driver port, and the openfin remote debugger port
        String configUrl = getenv("CONFIG_URL", System.getProperty("user.dir") + "/app_sample.json");
        String binary = Paths.get(IS_WINDOWS ? "RunOpenFin.bat" : "RunOpenFin.sh").toAbsolutePath().toString();
	String port = getenv("CHROME_PORT", DEFAULT_DEBUGGING_PORT);
        Runtime.getRuntime.exec(binary + " " + configUrl + " --remote-debugging-port=" + port);
	Thread.sleep(5000);

        ChromeOptions options = new ChromeOptions();
	options.addExperimentalOption("debuggerAddress", "localhost:" + port);

        // connect to chromedriver
        WebDriver driver = new RemoteWebDriver(new URL("http://localhost:" + getenv("CHROMEDRIVER_PORT", "9515")),
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

            test.finish();
        } finally {
            exitOpenFin(driver);
        }
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
        return false;
    }

}
