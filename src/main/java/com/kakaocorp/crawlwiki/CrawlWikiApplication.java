package com.kakaocorp.crawlwiki;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@SpringBootApplication
@Controller
public class CrawlWikiApplication {

    private static final String kowiki = "https://ko.wikipedia.org/w/index.php?search=%%22%s%%22";
    private static final String enwiki = "https://en.wikipedia.org/w/index.php?search=%%22%s%%22";

    public static void main(String[] args) {
        SpringApplication.run(CrawlWikiApplication.class, args);
    }

    @RequestMapping("/")
    @ResponseBody
    public String crawl() throws IOException {

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream("data/entry.txt"),"UTF-16"));
        FileWriter fw = new FileWriter("data/output.txt", false);
        BufferedWriter bw = new BufferedWriter(fw);

        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
            if(!StringUtils.isEmpty(line)) {
                String values[] = line.split("\t");
                if(values.length == 5) {
                    if(!values[2].contains("&#") && values[2].matches(".*[a-zA-Z]+.*") && StringUtils.isEmpty(values[3])) {
                        System.out.println("fetch : " + line);
                        Map<String, String> kowikiMap = getContents(String.format(kowiki, cleanText(values[2])));
                        Map<String, String> enwikiMap = getContents(String.format(enwiki, cleanText(values[2])));
    
                        for(String key : kowikiMap.keySet()) {
                            if(enwikiMap.containsKey(key)) {
                                if(!values[1].trim().equals(kowikiMap.get(key).trim())) {
                                    values[3] = kowikiMap.get(key);
                                    System.out.println(values[1] + " = " + values[3]);
                                }
                                break;
                            }
                        }
                    }

                    bw.write(String.join("\t", values));
                    bw.newLine();
                    bw.flush();
                } else {
                    System.out.print("invalid entry : " + line);
                }
            }
        }
        br.close();
        
        return "OK";
    }
    
    private String cleanText(String value) {
        return value.replaceAll("\\(", "").replaceAll("\\)", "").replaceAll(" ", "+");
    }

    private Map<String, String> getContents(String url) {
        Map<String, String> map = new HashMap<String, String>();
        Document doc;
        try {
            doc = Jsoup.connect(url).followRedirects(true).timeout(5000).get();
            if(doc.select(".mw-search-result-heading").size() > 0) {
                Elements links = doc.select(".mw-search-result-heading a");
                for (Element link : links) {
                    String detailUrl = link.attr("href");
                    Document detailDoc = Jsoup.connect(url.split("\\/w")[0]+ detailUrl).get();
                    String wikidataId = detailDoc.select("#t-wikibase a").attr("href");
                    map.put(wikidataId, detailDoc.title().split(" - ")[0]);
                }
            } else {
                String wikidataId = doc.select("#t-wikibase a").attr("href");
                if(!StringUtils.isEmpty(wikidataId)) {
                    map.put(wikidataId, doc.title().split(" - ")[0]);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return map;
    }
}
