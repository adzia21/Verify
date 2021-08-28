package com.verify.service;

import com.verify.model.EmailValidationResponse;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.*;
import java.net.Socket;
import java.util.*;

//used https://www.rgagnon.com/javadetails/java-0452.html implementation
@Service
public class SMTPLookup {
    private static final Logger logger = LogManager.getLogger(SMTPLookup.class);

    private static int hear(BufferedReader in) throws IOException {
        String line;
        int res = 0;

        while ((line = in.readLine()) != null) {
            String pfx = line.substring(0, 3);
            try {
                res = Integer.parseInt(pfx);
            } catch (Exception ex) {
                res = -1;
            }
            if (line.charAt(3) != '-') break;
        }

        return res;
    }

    private static void say(BufferedWriter wr, String text)
            throws IOException {
        wr.write(text + "\r\n");
        wr.flush();
    }

    private static Set<String> getMX(String hostName)
            throws NamingException {
        // Perform a DNS lookup for MX records in the domain
        Hashtable<String, String> env = new Hashtable<>();
        env.put("java.naming.factory.initial",
                "com.sun.jndi.dns.DnsContextFactory");
        DirContext ictx = new InitialDirContext(env);
        Attributes attrs = ictx.getAttributes
                (hostName, new String[]{"MX"});
        Attribute attr = attrs.get("MX");

        // if we don't have an MX record, try the machine itself
        if ((attr == null) || (attr.size() == 0)) {
            attrs = ictx.getAttributes(hostName, new String[]{"A"});
            attr = attrs.get("A");
            if (attr == null)
                throw new NamingException
                        ("No match for name '" + hostName + "'");
        }

        // Huzzah! we have machines to try. Return them as an array list
        // NOTE: We SHOULD take the preference into account to be absolutely
        //   correct. This is left as an exercise for anyone who cares.
        Set<String> results = new HashSet<>();
        NamingEnumeration<?> en = attr.getAll();

        while (en.hasMore()) {
            String mailhost;
            String x = (String) en.next();
            String[] f = x.split(" ");
            //  THE fix *************
            if (f.length == 1)
                mailhost = f[0];
            else if (f[1].endsWith("."))
                mailhost = f[1].substring(0, (f[1].length() - 1));
            else
                mailhost = f[1];
            //  THE fix *************
            results.add(mailhost);
        }
        return results;
    }

    @SneakyThrows
    public static boolean isAddressValid(String address) {
        // Find the separator for the domain name
        int pos = address.indexOf('@');

        // If the address does not contain an '@', it's not valid
        if (pos == -1) return false;

        // Isolate the domain/machine name and get a list of mail exchangers
        String domain = address.substring(++pos);
        Set<String> mxSet = getMX(domain);

        // Just because we can send mail to the domain, doesn't mean that the
        // address is valid, but if we can't, it's a sure sign that it isn't
        if (mxSet.size() == 0) return false;

        // Now, do the SMTP validation, try each mail exchanger until we get
        // a positive acceptance. It *MAY* be possible for one MX to allow
        // a message [store and forwarder for example] and another [like
        // the actual mail server] to reject it. This is why we REALLY ought
        // to take the preference into account.
        for (String mx : mxSet) {
                int res;
                //
                Socket skt = new Socket(mx, 25);
                BufferedReader rdr = new BufferedReader
                        (new InputStreamReader(skt.getInputStream()));
                BufferedWriter wtr = new BufferedWriter
                        (new OutputStreamWriter(skt.getOutputStream()));

                res = hear(rdr);
                if (res != 220) {
                    logger.info("Address " + address + " is not valid!");
                    return false;
                }
                say(wtr, "EHLO rgagnon.com");

                res = hear(rdr);
                if (res != 250){
                    logger.info("Address " + address + " is not valid!");
                    return false;
                }

                // validate the sender address
                say(wtr, "MAIL FROM: <mat@yahoo.com>");
                res = hear(rdr);
                if (res != 250) {
                    logger.info("Address " + address + " is not valid!");
                    return false;
                }

                say(wtr, "RCPT TO: <" + address + ">");
                res = hear(rdr);

                // be polite
                say(wtr, "RESET");
                hear(rdr);
                say(wtr, "QUIT");
                hear(rdr);
                if (res != 250) {
                    logger.info("Address " + address + " is not valid!");
                    return false;
                }

                rdr.close();
                wtr.close();
                skt.close();
                return true;
        }

        return false;
    }

    public String[] checkAll(String name, String surname, String domain) {

        String[] testData = generateEmailsAddress(name, surname, domain);
        List<String> forApi = Arrays.asList(testData.clone());

        List<String> result = new ArrayList<>();
        int count = 0;

        for (String data : testData) {
            if (isAddressValid(data)) {
                result.add(data + " working");
            } else {
                result.add("! " + data + " not working !");
                count++;
            }
        }

        if (count == result.size()) {
            return checkFromAPI(forApi);
        } else if (count == 0) {
            return checkFromAPI(forApi);
        } else
            return result.toArray(new String[0]);
    }

    private String[] generateEmailsAddress(String name, String surname, String domain) {
        List<String> result = new ArrayList<>();
        result.add(String.format("%s.%s@%s", name, surname, domain));
        result.add(String.format("%s_%s@%s", name, surname, domain));
        result.add(String.format("%s.%s@%s", surname, name, domain));
        result.add(String.format("%s_%s@%s", surname, name, domain));
        result.add(String.format("%s.%s@%s", name.charAt(0), surname, domain));
        result.add(String.format("%s.%s@%s", name, surname.charAt(0), domain));
        result.add(String.format("%s.%s@%s", name.charAt(0), surname.charAt(0), domain));
        result.add(String.format("%s@%s", name, domain));
        result.add(String.format("%s@%s", surname, domain));
        result.add(String.format("prezes@%s", domain));
        result.add(String.format("kontakt@%s", domain));
        result.add(String.format("info@%s", domain));
        result.add(String.format("bok@%s", domain));
        result.add(String.format("biuro@%s", domain));

        return result.toArray(new String[0]);
    }

    private String[] checkFromAPI(List<String> address) {
        logger.info(new Date().toString() + "     " + "Getting data from API for: " + address);
        System.out.println("GETTING DATA FROM API");
        RestTemplate restTemplate = new RestTemplate();
        EmailValidationResponse response = restTemplate.getForObject("https://emailvalidation.abstractapi.com/v1/?api_key=a8a6ff502bd24bdd936dc4c9af4a30d8&email=" + address.get(0), EmailValidationResponse.class);

        if (response == null) {
            throw new IllegalArgumentException();
        }

        logger.info("DELIVERABILITY: " + response.getDeliverability());
        logger.info("CATCH-ALL: " + response.getDeliverability());
        logger.info("address: " + "https://emailvalidation.abstractapi.com/v1/?api_key=a8a6ff502bd24bdd936dc4c9af4a30d8&email=" + address.get(0));

        if (response.getDeliverability().equals("DELIVERABLE") && !response.getIsCatchallEmail().getText().equals("TRUE"))
            return new String[]{address.get(0) + " DELIVERABLE"};
        else if (response.getIsCatchallEmail().getText().equals("TRUE"))
            return new String[]{"CATCH-ALL"};
        else {
            for (int i = 0; i < address.size(); i++) {
                address.set(i, "! " + address.get(i) + " nie działa!");
            }
        }

        return address.toArray(new String[0]);
    }
} 