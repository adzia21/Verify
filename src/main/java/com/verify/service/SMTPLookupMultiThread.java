package com.verify.service;

import com.verify.model.VerimailResponse;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.Type;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class SMTPLookupMultiThread {
    private static final Logger logger = LogManager.getLogger(SMTPLookup.class);
    private static final String APIKey = "637779F77CF54541B6533B5FD324751A";

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

    @SneakyThrows
    public static boolean isAddressValid(String address) {
        // Find the separator for the domain name
        int pos = address.indexOf('@');

        // If the address does not contain an '@', it's not valid
        if (pos == -1) return false;

        // Isolate the domain/machine name and get a list of mail exchangers
        String domain = address.substring(++pos);
        Record[] mxArray;

        Lookup dnsLookup = new Lookup(domain, Type.MX);
        dnsLookup.run();
        mxArray = dnsLookup.getAnswers();


        // Just because we can send mail to the domain, doesn't mean that the
        // address is valid, but if we can't, it's a sure sign that it isn't
        if (mxArray.length == 0) return false;

        // Now, do the SMTP validation, try each mail exchanger until we get
        // a positive acceptance. It *MAY* be possible for one MX to allow
        // a message [store and forwarder for example] and another [like
        // the actual mail server] to reject it. This is why we REALLY ought
        // to take the preference into account.
        for (Record mx : mxArray) {
            int res;
            //
            Socket skt = new Socket(mx.getAdditionalName().toString(), 25);
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
            if (res != 250) {
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

    public String[] checkAll(String name, String domain) {

        String[] testData = generateEmailsAddress(name, domain);

        List<String> forApi = Arrays.asList(testData.clone());
        List<String> result = new ArrayList<>();

        Arrays.stream(testData).parallel().forEach(data -> {
            if (isAddressValid(data)) {
                result.add(data + " is working");
            } else
                result.add("! " + data + " is not working !");
        });

        int count = 0;

        for (String mail : result) {
            if (mail.charAt(0) == '!')
                count++;
        }

        if (count == result.size())
            return checkMostCommonFromAPI(forApi);
        else if (count == 0)
            return checkMostCommonFromAPI(forApi);
        else
            return result.toArray(new String[0]);
    }

    public String[] checkAll(String name, String surname, String domain) {

        String[] testData = generateEmailsAddress(name, surname, domain);
        List<String> forApi = Arrays.asList(testData.clone());

        List<String> result = new ArrayList<>();

        Arrays.stream(testData).parallel().forEach(data -> {
            if (isAddressValid(data)) {
                result.add(data + " is working");
            } else
                result.add("! " + data + " is not working!");
        });

        int count = 0;

        for (String mail : result) {
            if (mail.charAt(0) == '!')
                count++;
        }

        if (count == result.size())
            return checkMostCommonFromAPI(forApi);
        else if (count == 0)
            return checkMostCommonFromAPI(forApi);
        else
            return result.toArray(new String[0]);
    }

    private String[] generateEmailsAddress(String name, String domain) {
        List<String> result = new ArrayList<>();
        result.add(String.format("%s@%s", name, domain));
        result.add(String.format("prezes@%s", domain));
        result.add(String.format("kontakt@%s", domain));
        result.add(String.format("info@%s", domain));
        result.add(String.format("bok@%s", domain));
        result.add(String.format("biuro@%s", domain));

        return result.toArray(new String[0]);
    }

    private String[] generateEmailsAddress(String name, String surname, String domain) {
        List<String> result = new ArrayList<>();
        result.add(String.format("%s.%s@%s", name, surname, domain));
        result.add(String.format("%s%s@%s", name, surname, domain));
        result.add(String.format("%s_%s@%s", name, surname, domain));
        result.add(String.format("%s.%s@%s", surname, name, domain));
        result.add(String.format("%s.%s@%s", surname, name.charAt(0), domain));
        result.add(String.format("%s_%s@%s", surname, name.charAt(0), domain));
        result.add(String.format("%s_%s@%s", surname, name, domain));
        result.add(String.format("%s.%s@%s", name.charAt(0), surname, domain));
        result.add(String.format("%s%s@%s", name.charAt(0), surname, domain));
        result.add(String.format("%s%s@%s", surname, name.charAt(0), domain));
        result.add(String.format("%s.%s@%s", name, surname.charAt(0), domain));
        result.add(String.format("%s.%s@%s", name.charAt(0), surname.charAt(0), domain));
        result.add(String.format("%s%s@%s", name.charAt(0), surname.charAt(0), domain));
        result.add(String.format("%s@%s", name, domain));
        result.add(String.format("%s@%s", surname, domain));
        result.add(String.format("prezes@%s", domain));
        result.add(String.format("kontakt@%s", domain));
        result.add(String.format("info@%s", domain));
        result.add(String.format("bok@%s", domain));
        result.add(String.format("biuro@%s", domain));

        return result.toArray(new String[0]);
    }

    public static String checkFromApi(String address) {
        logger.info(new Date().toString() + "     " + "Pobieranie danych z API dla: " + address);
        RestTemplate restTemplate = new RestTemplate();

        VerimailResponse response = restTemplate.getForObject("https://api.verimail.io/v3/verify?email=" + address + "&key=" + APIKey, VerimailResponse.class);

        if (response.getResult().equals("catch_all"))
            return "Catch_all";
        else if (response.getResult().equals("deliverable"))
            return response.getEmail() + " deliverable";
        else
            return "!" + address + " nie działa";
    }

    private String[] checkMostCommonFromAPI(List<String> address) {
        logger.info(new Date().toString() + "     " + "Checking only the most common mails)");
        logger.info(new Date().toString() + "     " + "Getting data for: " + address);
        RestTemplate restTemplate = new RestTemplate();

        List<String> addressForAPI = new ArrayList<>();
        addressForAPI.add(address.get(0));
        if (address.size() > 10) {
            addressForAPI.add(address.get(8));
            addressForAPI.add(address.get(7));
        }
        VerimailResponse response1 = restTemplate.getForObject("https://api.verimail.io/v3/verify?email=" + addressForAPI.get(0) + "&key=" + APIKey, VerimailResponse.class);

        assert response1 != null;
        if (response1.getResult().equals("catch_all"))
            return new String[]{"Catch_all"};
        else if (response1.getResult().equals("deliverable"))
            return new String[]{response1.getEmail() + " deliverable"};

        else {
            VerimailResponse response2 = restTemplate.getForObject("https://api.verimail.io/v3/verify?email=" + addressForAPI.get(1) + "&key=" + APIKey, VerimailResponse.class);
            assert response2 != null;
            if (response2.getResult().equals("deliverable"))
                return new String[]{response2.getEmail() + " deliverable"};
            else {
                VerimailResponse response3 = restTemplate.getForObject("https://api.verimail.io/v3/verify?email=" + addressForAPI.get(2) + "&key=" + APIKey, VerimailResponse.class);
                assert response3 != null;
                if (response3.getResult().equals("deliverable"))
                    return new String[]{response3.getEmail() + " deliverable"};
            }
        }
        for (int i = 0; i < address.size(); i++) {
            address.set(i, "!" + address.get(i) + " nie działa!");
        }
        return address.toArray(new String[0]);
    }
}