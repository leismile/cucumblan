/*
 *
 *
 *    Copyright (c) 2021.  Virtualan Contributors (https://virtualan.io)
 *
 *     Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *     in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software distributed under the License
 *     is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *     or implied. See the License for the specific language governing permissions and limitations under
 *     the License.
 *
 *
 *
 */


package io.virtualan.cucumblan.core;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.virtualan.cucumblan.props.ApplicationConfiguration;
import io.virtualan.cucumblan.props.util.ScenarioContext;
import io.virtualan.cucumblan.props.util.StepDefinitionHelper;
import io.virtualan.cucumblan.props.util.UIHelper;
import io.virtualan.cucumblan.ui.action.Action;
import io.virtualan.cucumblan.ui.core.PageElement;
import io.virtualan.cucumblan.ui.core.PagePropLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;


/**
 * The type Ui base step definition.
 *
 * @author Elan Thangamani
 */
public class UIBaseStepDefinition {

  private final static Logger LOGGER = Logger.getLogger(UIBaseStepDefinition.class.getName());
  private static PagePropLoader pagePropLoader;
  private static Map<String, Action> actionProcessorMap = new HashMap<>();

  static {
    loadActionProcessors();
  }

  private WebDriver driver = null;
  private Scenario scenario;

  /**
   * Load action processors.
   */
  public static void loadActionProcessors() {
    Reflections reflections = new Reflections("io.virtualan.cucumblan.ui.actionimpl",
        new SubTypesScanner(false));
    Set<Class<? extends Action>> buildInclasses = reflections.getSubTypesOf(Action.class);
    reflections = new Reflections(ApplicationConfiguration.getActionPackage(),
        new SubTypesScanner(false));
    Set<Class<? extends Action>> customclasses = reflections.getSubTypesOf(Action.class);
    if (customclasses != null) {
      buildInclasses.addAll(customclasses);
    }
    buildInclasses.stream().forEach(x -> {
      Action action = null;
      try {
        action = x.newInstance();
        actionProcessorMap.put(action.getType(), action);
      } catch (InstantiationException e) {
        LOGGER.warning("Unable to process this action (" + action.getType() + ") class: " + action);
      } catch (IllegalAccessException e) {
        LOGGER.warning("Unable to process this action (" + action.getType() + ") class: " + action);
      }
    });
  }

  @Before
  public void before(Scenario scenario) {
    this.scenario = scenario;
    //this.sequence = 1;
  }

  /**
   * Load driver and url.
   *
   * @param driverName the driver name
   * @param resource   the url
   */
  @Given("user wants to create (.*) on (.*)$")
  public void givenStatement(String driverName, String resource) {

  }

    /**
     * Load driver and url.
     *
     * @param driverName the driver name
     * @param resource   the url
     */
  @Given("Load Driver (.*) And URL on (.*)$")
  public void loadDriverAndURL(String driverName, String resource) {
    switch (driverName) {
      case "CHROME":
        System.setProperty("webdriver.chrome.driver", "conf/chromedriver.exe");
        ChromeOptions chromeOptions = new ChromeOptions();
        chromeOptions.addArguments("start-maximized");
        //chromeOptions.addArguments("--headless", "--window-size=1920,1200");
        driver = new ChromeDriver(chromeOptions);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        break;
      case "FIREFOX":
        driver = new FirefoxDriver();
        driver.manage().window().maximize();
        break;
      default:
        throw new IllegalArgumentException("Browser \"" + driverName + "\" isn't supported.");
    }
    String url = UIHelper.getUrl(resource);
    if (url == null) {
      scenario.log("Url missing for " + resource);
      System.exit(-1);
    } else {
      scenario.log("Url for " + resource + " : " + url);
    }
    driver.get(url);
  }

  @After
  public void embedScreenshotOnFail(Scenario s) {
    if (s.isFailed()) {
      try {
        final byte[] screenshot = ((TakesScreenshot) driver)
            .getScreenshotAs(OutputType.BYTES);
        s.attach(screenshot, "image/png", "Failed-Image :" + UUID.randomUUID().toString());
      } catch (ClassCastException cce) {
        LOGGER.warning(" Error Message : " + cce.getMessage());
      }
    }
  }

  /**
   * Load page.
   *
   * @param pageName the page name
   * @param resource the resource
   * @param dt       the dt
   * @throws Exception the exception
   */
  @Given("perform the (.*) page action on (.*)$")
  public void loadPage(String pageName, String resource, DataTable dt) throws Exception {
    List<Map<String, String>> data = dt.asMaps();
    Map<String, PageElement> pageMap = PagePropLoader.readPageElement(resource, pageName);
    if (pageMap != null && !pageMap.isEmpty()) {

      pageMap.forEach((k, v) -> {
        String elementValue = data.get(0).get(v.getName());
        if (elementValue != null && "DATA".equalsIgnoreCase(v.getType())|| "NAVIGATION".equalsIgnoreCase(v.getType()))  {
          try {
            actionProcessor(v.getName(), elementValue, v);
          } catch (InterruptedException e) {
            LOGGER.warning("Unable to process this page: " + pageName);
            Assertions.assertTrue(false,
                pageName + " Page for resource " + resource + " (" + v.getName() + " : "
                    + elementValue + ":" + v + "): " + e.getMessage());
          } catch (Exception e) {
            LOGGER.warning("Unable to process this page: " + pageName);
            Assertions.assertTrue(false,
                pageName + " Page for resource " + resource + " (" + v.getName() + " : "
                    + elementValue + ":" + v + "): " + e.getMessage());
          }
        } else {
          Assertions.assertTrue(false,
              pageName + " Page for resource " + resource + " (" + v.getName() + " : "
                  + elementValue + ":" + v + "): incorrect field name");
        }

      });
    } else {
      Assertions.assertTrue(false, pageName + " Page is not found for resource " + resource);
    }
  }

  /**
   * Verify.
   *
   * @param name  the name
   * @param value the value
   */
  @Then("verify (.*) has (.*) data in the page$")
  public void verify(String name, String value) {
    Assertions.assertEquals(value, StepDefinitionHelper.getActualValue(name));
  }

  /**
   * Verify.
   *
   * @param name  the name
   */
  @Then("verify (.*) contains data in the page$")
  public void verify(String name, Map<String, String> xpathWithValue) {
    for(Map.Entry<String, String> xpathMaps : xpathWithValue.entrySet()) {
      WebElement webelement = driver.findElement(By.xpath(xpathMaps.getKey()));
      Assertions.assertEquals(StepDefinitionHelper.getActualValue(xpathMaps.getValue()), webelement.getText() , xpathMaps.getKey() + " is not Matched.");
    }
  }

  /**
   * Action processor.
   *
   * @param key     the key
   * @param value   the value
   * @param element the element
   * @throws InterruptedException the interrupted exception
   */
  public void actionProcessor(String key, String value, PageElement element)
      throws InterruptedException {
    WebElement webelement = driver.findElement(By.xpath(element.getXPath()));
    Action action = actionProcessorMap.get(element.getAction());
    action.perform(key, webelement, value);
  }

  /**
   * Clean up.
   */
  @After
  public void cleanUp() {
    if (driver != null && ApplicationConfiguration.isProdMode()) {
      driver.close();
    } else {
      LOGGER.warning(" Driver not loaded/Closed : ");
    }
  }

}   