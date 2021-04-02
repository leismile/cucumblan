package io.virtualan.cucumblan.props.util;

import io.virtualan.cucumblan.props.ApplicationConfiguration;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Assert;

/**
 * The type Helper util.
 */
@Slf4j
public class HelperUtil {


  private static  String convertStreamToString(InputStream is) throws IOException {
    if (is != null) {
      StringBuilder sb = new StringBuilder();
      String line;

      try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        while ((line = reader.readLine()) != null) {
          sb.append(line);
        }
      } finally {
        is.close();
      }
      return sb.toString();
    } else {
      return null;
    }
  }

  /**
   * Read file as string string.
   *
   * @param fileBody the file body
   * @return the string
   */
  public static String readFileAsString(String fileBody) {
    String body = null;
    InputStream stream = Thread.currentThread().getContextClassLoader()
        .getResourceAsStream(fileBody);
    if (stream == null) {
      stream = ApplicationConfiguration.class.getClassLoader().getResourceAsStream(fileBody);
    }
    if (stream != null) {
      try {
        body = convertStreamToString(stream);
      } catch (IOException e) {
      }
    }
    return body;
  }


  /**
   * Assert xml equals.
   *
   * @param expectedXML the expected xml
   * @param actualXML   the actual xml
   * @throws Exception the exception
   */
  public static void assertXMLEquals(String expectedXML, String actualXML) throws Exception {
    XMLUnit.setIgnoreWhitespace(true);
    XMLUnit.setIgnoreAttributeOrder(true);
    DetailedDiff diff = new DetailedDiff(XMLUnit.compareXML(expectedXML, actualXML));
    List<?> allDifferences = diff.getAllDifferences();
    Assert.assertEquals("Differences found: " + diff.toString(), 0, allDifferences.size());
  }
}
