package com.crawlcore.thread;

import com.crawl.util.HttpHelper;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created with IntelliJ IDEA.
 * User: Zephery
 * Time: 2018/1/11 23:18
 * Description:
 */
public class CommonCrawl implements Runnable {
    //logger
    private static final Logger logger = LoggerFactory.getLogger(CommonCrawl.class);
    private String url;

    public CommonCrawl(String url) {
        this.url = url;
    }

    @Override
    public void run() {
        String content = HttpHelper.getInstance().get(url);
        System.out.println(Jsoup.parse(content).title());
    }
}