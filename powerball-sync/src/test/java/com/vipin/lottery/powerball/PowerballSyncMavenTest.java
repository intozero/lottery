package com.vipin.lottery.powerball;

import org.junit.Test;

/** Makes the standalone regression suite part of Maven's test/install lifecycle. */
public class PowerballSyncMavenTest {
    @Test
    public void regressionSuite() throws Exception {
        PowerballSyncTest.main(new String[0]);
    }
}
