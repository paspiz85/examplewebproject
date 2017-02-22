package it.myorg.examplewebproject.admin;

import static org.testng.Assert.assertTrue;

import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import it.myorg.common.test.selenium.WebDriverFactory;

/**
 * Created by papizzuti on 16/02/17.
 */
public class VersionTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionTest.class);

    @Test
    public void test() throws Exception {
        //System.setProperty(WebDriverFactory.PROP_DRIVER_NAME, "chromedriver");
        String baseUrl = "http://localhost:" + System.getProperty("test.tomcat.http.port");
        LOGGER.info("baseUrl = " + baseUrl);
        WebDriverFactory webDriverFactory = new WebDriverFactory();
        WebDriver webDriver = webDriverFactory.getWebDriver();
        webDriver.navigate().to(baseUrl + "/version.txt");
        assertTrue(webDriver.getPageSource().contains("nobranch"));
    }

}
