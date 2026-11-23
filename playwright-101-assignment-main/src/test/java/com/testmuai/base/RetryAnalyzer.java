package com.testmuai.base;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * The scenarios drive a live third-party site (ads/trackers/chat widgets can add timing noise),
 * so each test gets up to 2 extra attempts before being reported as a genuine failure.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final int MAX_RETRY_COUNT = 2;
    private int retryCount = 0;

    @Override
    public boolean retry(ITestResult result) {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            return true;
        }
        return false;
    }
}
