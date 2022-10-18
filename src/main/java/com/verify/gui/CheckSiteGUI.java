package com.verify.gui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.verify.service.CheckSite;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static com.verify.service.SMTPLookupMultiThread.checkFromApi;
import static com.verify.service.SMTPLookupMultiThread.isAddressValid;

@Route("check-site")
public class CheckSiteGUI extends VerticalLayout {

    public CheckSiteGUI() {
        CheckSite checkSite = new CheckSite();
        FormLayout checker = new FormLayout();
        checker.setMaxWidth("500px");

        TextField siteField = new TextField();
        siteField.setLabel("Site");
        siteField.setRequiredIndicatorVisible(true);

        Button check = new Button("Check! ");

        HorizontalLayout actions = new HorizontalLayout();
        actions.add(check);

        checker.add(siteField, actions);
        MultiSelectListBox<String> listBox = new MultiSelectListBox<>();
        MultiSelectListBox<String> mailChecked = new MultiSelectListBox<>();
        Button checkMails = new Button("Check mails! ");
        Anchor krsLink = new Anchor();

        check.addClickListener(event -> {
            AtomicInteger wrongMail = new AtomicInteger();
            String siteName = siteField.getValue();
            Set<String> result;
            if (!(siteName.startsWith("http://") || siteName.startsWith("https://"))) {
                siteName = "https://" + siteName;
            }

            result = checkSite.checkSite(siteName);
            result.parallelStream().forEach(data -> {
                if (data.startsWith("NIP:")) {
                    krsLink.setText(data);
                    krsLink.setHref("https://aktualnyodpis.pl/company?utf8=%E2%9C%93&search=" + data.substring(5).replace("-", "").replace(" ", "") +" &commit=Szukaj");
                    krsLink.setTarget("_blank");
                }
            });

            List<String> resultMails = new ArrayList<>();
            List<String> resultSelected = new ArrayList<>();

            checkMails.addClickListener(event2 -> {
                result.parallelStream().forEach(data -> {
                    if (checkFromApi(data).charAt(0) == '!') {
                        resultMails.add(data + " is working");
                        resultSelected.add(data + " is working");
                    } else {
                        resultMails.add("!" + data + " is not working!");
                        wrongMail.getAndIncrement();
                    }
                });
                if (wrongMail.getAndIncrement() == result.size()) {
                    resultMails.clear();
                    result.parallelStream().forEach(data -> {
                        if (!(checkFromApi(data).charAt(0) == '!')) {
                            resultMails.add(data + " is working");
                            resultSelected.add(data + " is working");
                        } else {
                            resultMails.add("!" + data + " is not working!");
                            wrongMail.getAndIncrement();
                        }
                    });
                }

                mailChecked.setItems(resultMails);
                mailChecked.select(resultSelected);
                listBox.deselectAll();
                add(mailChecked);
            });

            listBox.setItems(result);
            listBox.select(result);
            add(krsLink);
            add(checkMails);

            add(listBox);
        });

        add(checker);

    }
}
