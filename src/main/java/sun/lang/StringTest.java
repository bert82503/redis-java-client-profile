package sun.lang;

import static org.testng.Assert.*;

import java.util.Arrays;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test for {@link String}.
 * 
 * @author huagang.li 2014年12月13日 下午1:03:17
 */
public class StringTest {

    @Test(dataProvider = "split")
    public void split(String str, String regex, String expected) {
         assertEquals(Arrays.toString(str.split(regex)), expected);
    }
    @DataProvider(name = "split")
    protected static final Object[][] splitTestData() {
        Object[][] testData = new Object[][] {
                                              { new StringBuilder().append("hello").append('\n').append("world").toString(), "\n", "[hello, world]" },
        };
        return testData;
    }

}
