package it.myorg.common.test.selenium;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.util.StringUtils;

import net.lightbody.bmp.proxy.ProxyServer;

/**
 * Noddy POC wrapper around the chrome WebDriver. This is just to get a simple test running.
 *
 * @author paspiz85
 */
public class WebDriverFactory {
    public static final int DEFAULT_SECONDS_TO_WAIT = 20;
    public static final String PROP_DRIVER_NAME = "webdriver.name";
    public static final String PHANTOMJS_DRIVER = "phantomjs";
    private static final String CHROME_DRIVER = "chromedriver";
    private static final String DEFAULT_DRIVER_NAME = PHANTOMJS_DRIVER;
    private static final String LOCALE_SETTING_HEADER = "Accept-Language";
    private static final String US_LOCALE = "en-US";

    private ProxyServer proxy;
    private WebDriver driver;
    private WebDriverWait webDriverWait;

    public WebDriver getWebDriver() {
        return getWebDriver(false, null);
    }

    public WebDriver getWebDriver(boolean createNew) {
        return getWebDriver(createNew, null);
    }

    public WebDriver getWebDriver(boolean createNew, String userAgent) {

        if (driver == null || createNew) {
            if (driver != null && !isDriverQuit()) {
                driver.close();
                driver.quit();
                driver = null;
            }
            String driverName = getDriverNameToUse();
            switch (driverName) {
            case PHANTOMJS_DRIVER:
                driver = getPhantomJSDriver(userAgent);
                break;
            case CHROME_DRIVER:
                driver = getChromeDriver(userAgent);
                break;
            default:
                throw new RuntimeException("Unrecognised driver name, driver=" + driverName);
            }
            webDriverWait = new WebDriverWait(driver, DEFAULT_SECONDS_TO_WAIT);
        }
        return driver;
    }

    private boolean isDriverQuit() {
        return driver.toString().contains("(null)");
    }

    public String getAgent() {
        return String.valueOf(((JavascriptExecutor) driver).executeScript("return navigator.userAgent"));
    }

    public void destroySelenium() {
        //  we may have quit the driver as part of FacebookHook. So, just check if the session is still available
        //  before calling the quit.
        RemoteWebDriver remoteWebDriver = (RemoteWebDriver) driver;

        if (remoteWebDriver.getSessionId() != null) {
            driver.quit();
        }
    }

    public WebDriverWait getWebDriverWait() {
        return webDriverWait;
    }

    private WebDriver getChromeDriver(String userAgent) {
        String driverPath = getDriverPath(CHROME_DRIVER);
        System.setProperty("webdriver.chrome.driver", driverPath);

        //  Set the below chrome option to remove 'unsupported command-line flag –ignore-certificate-errors.'
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--test-type");
        if (StringUtils.hasText(userAgent)) {
            options.addArguments("--user-agent=" + userAgent);
        }
        options.addArguments(US_LOCALE);

        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(ChromeOptions.CAPABILITY, options);
        setupProxy(capabilities);

        return new ChromeDriver(capabilities);
    }

    private WebDriver getPhantomJSDriver(String userAgent) {
        List<String> cliArgs = new ArrayList<>();
        cliArgs.add("--ignore-ssl-errors=true");

        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, getDriverPath(PHANTOMJS_DRIVER));

        capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_CLI_ARGS, cliArgs);
        capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_CUSTOMHEADERS_PREFIX + LOCALE_SETTING_HEADER, US_LOCALE);
        if (StringUtils.hasText(userAgent)) {
            capabilities.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "userAgent", userAgent);
        }
        setupProxy(capabilities);
        return new PhantomJSDriver(capabilities);
    }

    private void setupProxy(DesiredCapabilities capabilities) {
        String proxyOnOrOff = System.getProperty("proxy");

        if (proxy != null && (StringUtils.isEmpty(proxyOnOrOff) || "on".equals(proxyOnOrOff))) {
            capabilities.setCapability(CapabilityType.PROXY, proxy.seleniumProxy());
        }
    }

    public void setProxy(ProxyServer proxy) {
        this.proxy = proxy;
    }

    private String getOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        String os = null;

        if (osName.contains("mac")) {
            os = "mac";
        } else if (osName.contains("win")) {
            os = "win";
        } else if (osName.contains("linux")) {
            os = "linux";
        }

        if (os == null) {
            throw new IllegalStateException("Unrecognised OS '" + osName + "'");
        }

        return os;
    }

    private String getDriverPath(String driver) {
        String os = getOS();
        String path = "/" + driver + "/" + os + "/" + driver;
        if ("win".equals(os)) {
            path += ".exe";
        }
        URL resource = getClass().getResource(path);
        if (resource == null) {
            throw new IllegalStateException("Driver '" + driver + "' at '" + path + "' not found");
        }
        return resource.getFile();
    }

    private String getDriverNameToUse() {
        return System.getProperty(PROP_DRIVER_NAME, DEFAULT_DRIVER_NAME);
    }
}
