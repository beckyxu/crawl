package com.crawlcore.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA.
 * User: Zephery
 * Time: 2018/1/11 23:21
 * Description:
 */
public class CommonThread {
    //logger
    private static final Logger logger = LoggerFactory.getLogger(CommonThread.class);
    private static final BlockingQueue<Runnable> queuelength = new ArrayBlockingQueue<>(2000);
    private static final Integer CORE = Runtime.getRuntime().availableProcessors();
    private static final ExecutorService es = new ThreadPoolExecutor(CORE, CORE,
            0L, TimeUnit.MILLISECONDS,
            queuelength);

    public static void submiturl(String url) {
        CommonCrawl crawl = new CommonCrawl(url);
        es.submit(crawl);
    }

    public static void main(String[] args) {
        submiturl("https://www.cnblogs.com");
        es.shutdown();
    }

}