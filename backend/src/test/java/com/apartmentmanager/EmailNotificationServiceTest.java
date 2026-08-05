package com.apartmentmanager.service;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.BillPayment;
import com.apartmentmanager.model.Role;
import com.apartmentmanager.model.User;
import com.apartmentmanager.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EmailNotificationServiceTest {

    private JavaMailSender mailSender;
    private ObjectProvider<JavaMailSender> mailSenderProvider;
    private UserRepository userRepository;
    private EmailNotificationService service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        mailSenderProvider = mock(ObjectProvider.class);
        userRepository = mock(UserRepository.class);
        service = new EmailNotificationService(mailSenderProvider, userRepository);
    }

    @Test
    void disabled_doesNothing() {
        ReflectionTestUtils.setField(service, "enabled", false);

        service.notifyDocumentUploaded(new Apartment(), new BillPayment());

        verifyNoInteractions(mailSender);
        verifyNoInteractions(mailSenderProvider);
        verifyNoInteractions(userRepository);
    }

    @Test
    void enabled_withoutMailSender_skipsSending() {
        ReflectionTestUtils.setField(service, "enabled", true);

        service.notifyDocumentUploaded(new Apartment(), new BillPayment());

        verifyNoInteractions(mailSender);
        verifyNoInteractions(userRepository);
    }

    @Test
    void enabled_sendsToOneEmailPerTenantWithEmail() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "from", "owner@example.com");
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);

        Apartment apt = new Apartment("Sunny Studio", "Desc", "Loc", 1000.0, 2, 50.0);
        apt.setId(1L);

        User withEmail = new User("tenant", "x", Role.TENANT, "tenant@example.com");
        User blankEmail = new User("tenant2", "x", Role.TENANT, "");

        when(userRepository.findByApartment_IdAndRole(1L, Role.TENANT))
                .thenReturn(List.of(withEmail, blankEmail));

        BillPayment bill = new BillPayment("bill.pdf", "stored.pdf", "application/pdf", "Electricity", "bill", apt, null);
        service.notifyDocumentUploaded(apt, bill);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(2000).times(1)).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("tenant@example.com", message.getTo()[0]);
        assertEquals("owner@example.com", message.getFrom());
        assertTrue(message.getSubject().contains("Sunny Studio"));
        assertTrue(message.getText().contains("bill.pdf"));
        assertTrue(message.getText().contains("Electricity"));
        assertTrue(message.getText().contains("tenant"));
    }

    @Test
    void enabled_noTenants_doesNothing() {
        ReflectionTestUtils.setField(service, "enabled", true);
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(userRepository.findByApartment_IdAndRole(anyLong(), eq(Role.TENANT))).thenReturn(List.of());

        service.notifyDocumentUploaded(new Apartment(), new BillPayment());

        verifyNoInteractions(mailSender);
    }
}
