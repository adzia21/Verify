package com.verify.service;

import lombok.AllArgsConstructor;
import org.apache.commons.lang3.builder.ToStringExclude;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.naming.NameNotFoundException;

import static org.junit.jupiter.api.Assertions.*;

@AllArgsConstructor
@SpringBootTest
class SMTPLookupTest {

    @Test
    void shouldThrowExceptionWhenDNSNotFound() {
        // given
        String mailWithWrongDomain = "angelika.swiackaa@gmail.om";

        // when/then
        assertThrows(NameNotFoundException.class, () -> SMTPLookup.isAddressValid(mailWithWrongDomain));
    }

    @Test
    void isAddressValid() {
        // given
        String workingMail = "angelika.swiackaa@gmail.com";

        // when
        boolean addressValid = SMTPLookup.isAddressValid(workingMail);

        //then
        assertTrue(addressValid);
    }
}