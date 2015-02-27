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

    @Test(dataProvider = "format")
    public void format(String format, Object arg, String expected) {
         assertEquals(String.format(format, arg), expected);
    }
    @DataProvider(name = "format")
    protected static final Object[][] formatTestData() {
        Object[][] testData = new Object[][] {
                                              { "%s", "string", "string" },
                                              { "%d", 3, "3" }, // int
                                              { "%1.1f", 2.3, "2.3" }, // double
                                              { "%f", 3D, "3.000000" }, // double
                                              { "%.0f", 3D, "3" }, // double
        };
        return testData;
    }
    
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
    
    @Test(dataProvider = "replaceAll")
    public void replaceAll(String str, String expected) {
         assertEquals(str.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5\\:\\.\\-\\_]", ""), expected);
    }
    @DataProvider(name = "replaceAll")
    protected static final Object[][] replaceAllTestData() {
        Object[][] testData = new Object[][] {
                                              { "hello-李华刚", "hello-李华刚" },
                                              { "w.or-l_d", "w.or-l_d" },
                                              { "comment:12345:reply.to", "comment:12345:reply.to" },
                                              { "hello, world!", "helloworld" },
                                              { "你好，皮皮！", "你好皮皮" },
        };
        return testData;
    }

}
