package com.verify.gui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.listbox.MultiSelectListBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.verify.service.SMTPLookupMultiThread;

import java.util.ArrayList;
import java.util.List;

@Route("mail")
@JsModule("./copytoclipboard.js")
public class MultiThreadMailGUI extends VerticalLayout  {
    SMTPLookupMultiThread smtpmxLookup = new SMTPLookupMultiThread();
    public MultiThreadMailGUI() {
        FormLayout checker = new FormLayout();
        checker.setMaxWidth("500px");

        TextField nameField = new TextField();
        nameField.setLabel("Name");
        nameField.setRequiredIndicatorVisible(true);

        TextField surnameField = new TextField();
        surnameField.setLabel("Surname");

        TextField domainField = new TextField();
        domainField.setLabel("Domain");
        domainField.setRequiredIndicatorVisible(true);

        Button search = new Button("Search");
        Button copy = new Button("Copy");

        HorizontalLayout actions = new HorizontalLayout();
        actions.add(search);

        checker.add(nameField, surnameField, domainField, actions);
        MultiSelectListBox<String> listBox = new MultiSelectListBox<>();

        search.addClickListener(event -> {
            String name = nameField.getValue();
            String surname = surnameField.getValue();
            String domain = domainField.getValue();
            String[] result;
            if (!surname.equals("")){
                result = smtpmxLookup.checkAll(name, surname, domain);
            }
            else
                result = smtpmxLookup.checkAll(name, domain);
            List<String> checked = new ArrayList<>();

            for (String data: result) {
                if (data.charAt(0) != '!')
                    checked.add(data);
            }

            listBox.setItems(result);
            listBox.select(checked);

            TextArea textArea = new TextArea();
            textArea.setValue(checked.toString());

            copy.addClickListener(
                    e -> UI.getCurrent().getPage().executeJs("window.copyToClipboard($0)", textArea.getValue())
            );

            add(listBox);
            add(copy);
        });

        add(checker);
    }
}
