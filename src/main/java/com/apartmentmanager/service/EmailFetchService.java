package com.apartmentmanager.service;

import com.apartmentmanager.model.Apartment;
import com.apartmentmanager.model.User;
import com.apartmentmanager.repository.ApartmentRepository;
import com.apartmentmanager.repository.UserRepository;
import jakarta.mail.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Properties;

@Service
public class EmailFetchService {

    private static final Logger log = LoggerFactory.getLogger(EmailFetchService.class);

    private final BillPaymentService billPaymentService;
    private final ApartmentRepository apartmentRepository;
    private final UserRepository userRepository;

    @Value("${app.email.fetch.enabled:false}")
    private boolean enabled;

    @Value("${app.email.fetch.address:}")
    private String emailAddress;

    @Value("${app.email.fetch.password:}")
    private String appPassword;

    @Value("${app.email.fetch.imap-host:imap.gmail.com}")
    private String imapHost;

    public EmailFetchService(BillPaymentService billPaymentService,
                             ApartmentRepository apartmentRepository,
                             UserRepository userRepository) {
        this.billPaymentService = billPaymentService;
        this.apartmentRepository = apartmentRepository;
        this.userRepository = userRepository;
    }

    @Scheduled(fixedDelayString = "${app.email.fetch.interval-ms:300000}", initialDelay = 30000)
    public void fetchEmails() {
        if (!enabled || emailAddress.isBlank() || appPassword.isBlank()) {
            return;
        }

        List<Apartment> apartments = apartmentRepository.findAll();
        if (apartments.isEmpty()) {
            log.warn("No apartments found, cannot save email bills");
            return;
        }
        Apartment firstApartment = apartments.get(0);

        User owner = userRepository.findByUsername("owner").orElse(null);
        if (owner == null) {
            log.warn("No owner user found, cannot save email bills");
            return;
        }

        log.info("Fetching emails from {}...", emailAddress);

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", imapHost);
        props.put("mail.imaps.port", "993");
        props.put("mail.imaps.ssl.enable", "true");

        Session session = Session.getInstance(props);

        try {
            Store store = session.getStore("imaps");
            store.connect(imapHost, emailAddress, appPassword);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            Message[] messages = inbox.search(new jakarta.mail.search.FlagTerm(new Flags(Flags.Flag.SEEN), false));
            log.info("Found {} unread emails", messages.length);

            for (Message message : messages) {
                try {
                    processMessage(message, firstApartment, owner);
                    message.setFlag(Flags.Flag.SEEN, true);
                } catch (Exception e) {
                    log.warn("Failed to process email '{}': {}", message.getSubject(), e.getMessage());
                }
            }

            inbox.close(true);
            store.close();
        } catch (Exception e) {
            log.error("Email fetch failed: {}", e.getMessage());
        }
    }

    private void processMessage(Message message, Apartment apartment, User owner) throws Exception {
        String subject = message.getSubject();
        if (subject == null || subject.isBlank()) {
            log.debug("Skipping email with no subject");
            return;
        }

        if (message.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) ||
                    (bodyPart.getFileName() != null && !bodyPart.getFileName().isBlank())) {

                    String fileName = bodyPart.getFileName();
                    String contentType = bodyPart.getContentType();
                    byte[] data = bodyPart.getInputStream().readAllBytes();

                    billPaymentService.uploadFromBytes(apartment, owner, data, fileName, contentType, "Other Payments", "bill");
                    log.info("Saved attachment '{}' for apartment '{}' from email '{}'", fileName, apartment.getTitle(), subject);
                }
            }
        }
    }
}
