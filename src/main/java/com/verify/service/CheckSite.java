package com.verify.service;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CheckSite {
    private static final Logger logger = LogManager.getLogger(SMTPLookup.class);

    private final List<String> visitedLinks = new ArrayList<>();
    private String baseName;

    public Set<String> checkSite(String siteName) {
        CheckSite readSite = new CheckSite();
        readSite.setBaseName(siteName);
        Set<String> emails = new HashSet<>();
        readSite.scrape(siteName);
        readSite.visitedLinks.parallelStream().forEach(e -> {
            try {
                URL url = new URL(e);
                BufferedReader inVisited = new BufferedReader(
                        new InputStreamReader(url.openStream()));
                String inputLineVisited;
                StringBuilder stringBuilderVisited = new StringBuilder();
                while ((inputLineVisited = inVisited.readLine()) != null) {
                    stringBuilderVisited.append(inputLineVisited);
                    stringBuilderVisited.append(System.lineSeparator());
                }
                inVisited.close();
                Pattern pVisited = Pattern.compile("\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,4}\\b",
                        Pattern.CASE_INSENSITIVE);
                Matcher matcherVisited = pVisited.matcher(stringBuilderVisited);
                String nip = findNip(stringBuilderVisited.toString());
                if (!nip.equals("")) {
                    emails.add("NIP: " + nip);
                }
                while (matcherVisited.find()) {
                    emails.add(matcherVisited.group());
                }
            } catch (IOException ex) {
                logger.error(ex.getMessage());
            }
        });

        Set<String> filteredEmails = emails.parallelStream().filter(p -> !p.contains(".jpg")).filter(p -> !p.contains(".png")).collect(Collectors.toSet());
        logger.info(filteredEmails);

        return filteredEmails;
    }

    public void scrape(String url) {
        try {
            Document document = Jsoup.connect(url).get();
            Elements foundUrls = document.select("a[href]");
            logger.info("Found " + foundUrls.size() + " links.");

            for (Element foundUrl : foundUrls) {

                String nextUrl = foundUrl.attr("href");

                if (!(visitedLinks.contains(nextUrl) || !isInSiteLink(nextUrl))) {
                    if (nextUrl.startsWith("/"))
                        visitedLinks.add(baseName + nextUrl);
                    else
                        visitedLinks.add(nextUrl);
                }
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }
    }

    boolean isInSiteLink(String url) {
        return (url.startsWith(baseName) || url.startsWith("/") || url.startsWith("./") || url.startsWith("../"));
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    private static String findNip(String site) {
        String nip = "";
        Pattern pVisited = Pattern.compile("(?<=(NIP:|NIP|nip|nip:): )(\\w+-\\w+-\\w+-\\w+)",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = pVisited.matcher(site);
        while (matcher.find()) {
            nip = matcher.group();
        }
        if (nip.length() < 7) {
            pVisited = Pattern.compile("(?<=(NIP:|NIP|nip|nip:): )(\\w+)",
                    Pattern.CASE_INSENSITIVE);
            matcher = pVisited.matcher(site);
            while (matcher.find()) {
                nip = matcher.group();
            }
        }
        if (nip.length() < 7) {
            pVisited = Pattern.compile("\\b([0-9]){3} ([0-9]){3} ([0-9]){2} ([0-9]){2}\\b",
                    Pattern.CASE_INSENSITIVE);
            matcher = pVisited.matcher(site);
            while (matcher.find()) {
                nip = matcher.group();
            }
        }

        return nip;
    }
}