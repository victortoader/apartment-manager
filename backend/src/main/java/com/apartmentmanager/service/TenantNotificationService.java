package com.apartmentmanager.service;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.BillPayment;
import com.apartmentmanager.model.Role;
import com.apartmentmanager.model.User;
import com.apartmentmanager.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class TenantNotificationService {

    private static final Logger log = LoggerFactory.getLogger(TenantNotificationService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final UserRepository userRepository;
    private final ExecutorService notifyExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "notify-worker");
        t.setDaemon(true);
        return t;
    });

    @Value("${app.email.notify.enabled:false}")
    private boolean enabled;

    @Value("${app.email.notify.from:${spring.mail.username:}}")
    private String from;

    public TenantNotificationService(ObjectProvider<JavaMailSender> mailSenderProvider, UserRepository userRepository) {
        this.mailSenderProvider = mailSenderProvider;
        this.userRepository = userRepository;
    }

    public void notifyDocumentUploaded(Apartment apartment, BillPayment bill) {
        if (!enabled) {
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Email notifications are enabled but no JavaMailSender bean is configured; skipping notification");
            return;
        }

        List<User> tenants = userRepository.findByApartment_IdAndRole(apartment.getId(), Role.TENANT);
        for (User tenant : tenants) {
            if (tenant.getEmail() == null || tenant.getEmail().isBlank()) {
                continue;
            }
            CompletableFuture.runAsync(() -> send(mailSender, tenant, apartment, bill), notifyExecutor);
        }
    }

    private void send(JavaMailSender mailSender, User tenant, Apartment apartment, BillPayment bill) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (from != null && !from.isBlank()) {
                message.setFrom(from);
            }
            message.setTo(tenant.getEmail());
            message.setSubject("New document uploaded to your apartment \u2014 " + apartment.getTitle());
            message.setText(buildBody(tenant, apartment, bill));
            mailSender.send(message);
            log.info("Notification email sent to {} for apartment '{}'", tenant.getEmail(), apartment.getTitle());
        } catch (Exception e) {
            log.warn("Failed to send notification email to {}: {}", tenant.getEmail(), e.getMessage());
        }
    }

    private String buildBody(User tenant, Apartment apartment, BillPayment bill) {
        String uploader = bill.getUploadedBy() != null ? bill.getUploadedBy().getUsername() : "unknown";
        String date = bill.getUploadDate() != null ? DATE_FORMAT.format(bill.getUploadDate()) : "unknown";
        return String.join("\n",
                "Hello " + tenant.getUsername() + ",",
                "",
                "A new document has been uploaded to your apartment \"" + apartment.getTitle() + "\".",
                "",
                "  File:      " + bill.getOriginalFileName(),
                "  Bill type: " + bill.getBillType(),
                "  Document:  " + bill.getDocumentType(),
                "  Uploaded:  " + date + " by " + uploader,
                "",
                "You can view it in the apartment portal.",
                "",
                "-- Apartment Manager");
    }
}
