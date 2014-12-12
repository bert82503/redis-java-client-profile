/*
 * Copyright 2014 FraudMetrix.cn All right reserved. This software is the
 * confidential and proprietary information of FraudMetrix.cn ("Confidential
 * Information"). You shall not disclose such Confidential Information and shall
 * use it only in accordance with the terms of the license agreement you entered
 * into with FraudMetrix.cn.
 */
package sun.util.concurrent;

import static org.testng.Assert.assertEquals;

import java.util.concurrent.TimeUnit;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Test for {@link TimeUnit}.
 * 
 * @author huagang.li 2014年12月12日 下午3:34:50
 */
public class TimeUnitTest {

    @Test(dataProvider = "toMillis")
    public void toMillis(TimeUnit timeUnit, long time, long timeMillis) {
        assertEquals(timeUnit.toMillis(time), timeMillis);
    }

    @DataProvider(name = "toMillis")
    protected static final Object[][] toMillisTestData() {
        Object[][] testData = new Object[][] {
                // setTimeBetweenEvictionRunsMillis
                { TimeUnit.SECONDS, 30L, 30000 }, // 30s（半分钟）
                { TimeUnit.SECONDS, 5L, 5000 }, // 5s - [T]
                // setMinEvictableIdleTimeMillis
                { TimeUnit.MINUTES, 1L, 60000 }, // 1min
                { TimeUnit.MINUTES, 5L, 300000 }, // 5min - [T]
        };
        return testData;
    }

}
