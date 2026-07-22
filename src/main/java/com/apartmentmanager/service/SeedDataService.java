package com.apartmentmanager.service;

import com.apartmentmanager.model.*;
import com.apartmentmanager.repository.ApartmentRepository;
import com.apartmentmanager.repository.ContactRepository;
import com.apartmentmanager.repository.NoteRepository;
import com.apartmentmanager.repository.OcrKeywordsRepository;
import com.apartmentmanager.repository.TicketRepository;
import com.apartmentmanager.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class SeedDataService {

    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final ApartmentService apartmentService;
    private final TicketRepository ticketRepository;
    private final ContactRepository contactRepository;
    private final NoteRepository noteRepository;
    private final OcrKeywordsRepository ocrKeywordsRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    public SeedDataService(UserRepository userRepository,
                           ApartmentRepository apartmentRepository,
                           ApartmentService apartmentService,
                           TicketRepository ticketRepository,
                           ContactRepository contactRepository,
                           NoteRepository noteRepository,
                           OcrKeywordsRepository ocrKeywordsRepository,
                           PasswordEncoder passwordEncoder,
                           UserService userService) {
        this.userRepository = userRepository;
        this.apartmentRepository = apartmentRepository;
        this.apartmentService = apartmentService;
        this.ticketRepository = ticketRepository;
        this.contactRepository = contactRepository;
        this.noteRepository = noteRepository;
        this.ocrKeywordsRepository = ocrKeywordsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    @Transactional
    public void seed() {
        if (!seedEnabled) return;
        if (apartmentRepository.count() > 0) return;

        String uploadDir = System.getProperty("app.upload.dir", "uploads");
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();

        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }

        Apartment ap1 = apartmentRepository.save(
                new Apartment("Sunny Studio", "Bright and cozy studio apartment in the city center", "123 Main St, Berlin", 850.0, 1, 35.0));
        Apartment ap2 = apartmentRepository.save(
                new Apartment("Garden Flat", "Spacious flat with a private garden", "456 Park Ave, Berlin", 1200.0, 3, 75.0));

        User tenant = userRepository.findByUsername("tenant").orElse(null);
        if (tenant != null) {
            tenant.setApartment(ap1);
            userRepository.save(tenant);
        } else {
            tenant = userRepository.save(new User("tenant", passwordEncoder.encode(System.getenv().getOrDefault("DEFAULT_PASSWORD", "admin")), Role.TENANT, "tenant@example.com"));
            tenant.setApartment(ap1);
            userRepository.save(tenant);
        }

        User tenant2 = userRepository.save(new User("tenant2", passwordEncoder.encode(System.getenv().getOrDefault("DEFAULT_PASSWORD", "admin")), Role.TENANT, "tenant2@example.com"));
        tenant2.setApartment(ap2);
        userRepository.save(tenant2);

        User owner = userRepository.findByUsername("owner").orElseThrow();

        Ticket t1 = new Ticket("Leaky faucet", "Water is dripping from the kitchen faucet constantly", ap1, tenant);
        t1.setStatus(TicketStatus.NEW);
        ticketRepository.save(t1);

        Ticket t2 = new Ticket("Broken heater", "The heating system stopped working last night", ap1, tenant);
        t2.setStatus(TicketStatus.IN_PROGRESS);
        ticketRepository.save(t2);

        Ticket t3 = new Ticket("Garden fence damaged", "The wooden fence on the east side fell during the storm", ap2, tenant2);
        t3.setStatus(TicketStatus.NEW);
        ticketRepository.save(t3);

        Ticket t4 = new Ticket("Paint peeling in bedroom", "The paint on the bedroom wall is peeling off", ap2, tenant2);
        t4.setStatus(TicketStatus.DONE);
        ticketRepository.save(t4);

        Ticket t5 = new Ticket("Storage room access", "Please grant access to the building storage room", ap2, owner);
        t5.setStatus(TicketStatus.NEW);
        ticketRepository.save(t5);

        for (Ticket ticket : new Ticket[]{t1, t2, t3, t4}) {
            for (int i = 0; i < 2; i++) {
                String fileName = UUID.randomUUID() + ".jpg";
                Path filePath = uploadPath.resolve(fileName);
                try {
                    byte[] placeholder = createPlaceholderImage(ticket.getTitle() + " photo " + (i + 1));
                    Files.write(filePath, placeholder);
                } catch (IOException e) {
                    continue;
                }
                ticket.getPhotoPaths().add(fileName);
            }
            ticketRepository.save(ticket);
        }

        contactRepository.save(new Contact("Building Administration", "admin@berlin-housing.de", ap1));
        contactRepository.save(new Contact("Emergency Plumber", "+49 30 12345678", ap1));
        contactRepository.save(new Contact("Electrician", "electric@berlin-housing.de", ap1));

        contactRepository.save(new Contact("Building Administration", "admin@berlin-housing.de", ap2));
        contactRepository.save(new Contact("Garden Maintenance", "+49 30 87654321", ap2));
        contactRepository.save(new Contact("Locksmith", "locks@berlin-housing.de", ap2));

        int[][] ap1Colors = {
            {0x42, 0x85, 0xF4}, {0x34, 0xA8, 0x53}, {0xFB, 0xBC, 0x05},
            {0xEA, 0x43, 0x35}, {0x9C, 0x27, 0xB0}, {0x00, 0xBC, 0xD4},
            {0xFF, 0x98, 0x00}, {0x79, 0x55, 0x48}, {0x60, 0x7D, 0x8B},
            {0xE9, 0x1E, 0x63}
        };
        for (int i = 0; i < 10; i++) {
            String fileName = UUID.randomUUID() + ".jpg";
            Path filePath = uploadPath.resolve(fileName);
            try {
                byte[] placeholder = createSolidImage(ap1Colors[i][0], ap1Colors[i][1], ap1Colors[i][2], "Apt 1 - Photo " + (i + 1));
                Files.write(filePath, placeholder);
            } catch (IOException e) { continue; }
            ap1.getPhotoPaths().add(fileName);
        }
        apartmentService.save(ap1);

        int[][] ap2Colors = {
            {0x3F, 0x51, 0xB5}, {0x00, 0x96, 0x88}, {0x8B, 0xC3, 0x4A},
            {0xFF, 0x57, 0x22}, {0x67, 0x3A, 0xB7}, {0x03, 0xA9, 0xF4},
            {0xF4, 0x43, 0x36}, {0x00, 0xE6, 0x76}, {0xFF, 0xEB, 0x3B},
            {0x21, 0x21, 0x21}
        };
        for (int i = 0; i < 10; i++) {
            String fileName = UUID.randomUUID() + ".jpg";
            Path filePath = uploadPath.resolve(fileName);
            try {
                byte[] placeholder = createSolidImage(ap2Colors[i][0], ap2Colors[i][1], ap2Colors[i][2], "Apt 2 - Photo " + (i + 1));
                Files.write(filePath, placeholder);
            } catch (IOException e) { continue; }
            ap2.getPhotoPaths().add(fileName);
        }
        apartmentService.save(ap2);

        noteRepository.save(new Note("Rent increased to 850 EUR as of January 2025. Previous tenant had a cat — check for scratches.", ap1));
        noteRepository.save(new Note("Appliance warranty expires March 2026. Keep receipts for any repairs.", ap1));
        noteRepository.save(new Note("Garden maintenance included in rent. Tenant responsible for watering plants.", ap2));
        noteRepository.save(new Note("Key safe code changed on 15.06.2025. New code shared with tenant2.", ap2));

        ap1.setPresentation("Bright and spacious 2-bedroom apartment in the heart of Berlin-Mitte, just steps away from Unter den Linden. The apartment features high ceilings, original parquet flooring, and large windows flooding every room with natural light.\n\nThe open-plan kitchen is fully equipped with modern appliances including dishwasher and washing machine. The bathroom has been recently renovated with a walk-in shower.\n\nPublic transport: U5 Museumsinsel (3 min walk), S-Bahn Hackescher Markt (8 min walk). Grocery stores, restaurants, and parks are all within walking distance.\n\nAvailable immediately. Long-term lease preferred. Deposit: 2 months' rent.");
        ap1 = apartmentService.save(ap1);

        ap2.setPresentation("Charming garden apartment on the ground floor of a quiet residential building in Berlin-Charlottenburg. This 3-room apartment offers a private garden terrace, perfect for families or anyone who loves outdoor space.\n\nThe apartment has been tastefully renovated while preserving its original character. Features include a modern open kitchen, spacious living room with garden access, two bright bedrooms, and a separate dining area.\n\nThe building has a shared courtyard with children's play area and bike storage. Street parking is available with a resident permit.\n\nNearest transit: U3 Wilmersdorfer Straße (5 min walk), bus 109 direct to Kurfürstendamm. Close to Savignyplatz, Charlottenburg Palace, and KaDeWe.\n\nPets are welcome upon discussion.");
        ap2 = apartmentService.save(ap2);

        seedOcrKeywords();
    }

    private void seedOcrKeywords() {
        if (ocrKeywordsRepository.count() > 0) return;

        ocrKeywordsRepository.save(new OcrKeywords("ro",
            "total de achitat|suma de plată|total de plată|factură|sumă|valoare|plătit|contravaloare|ultima zi de plată|perioada facturată|factura seria",
            "factură|factura|factura seria|perioada facturată|ultima zi de plată|emitere|scadență|plată|cont|client|furnizor|servicii|utilități|număr|adresă|adresa|numar|data",
            "extras de cont|ordin de plată|transfer|virament|chitanță|bon fiscal|op|mandat de plată|confirmare plată|debit|credit",
            "RON"));

        ocrKeywordsRepository.save(new OcrKeywords("de",
            "total|totalbetrag|betrag|gesamt|rechnungsbetrag|zahlbetrag|fälliger betrag|offener betrag|brutto|netto|jährlich zahlbar|monatlich zahlbar|totalbetrag|gesamtbetrag",
            "rechnung|faktura|betrag fällig|zahlungsziel|lieferant|kunde|dienstleistung|verbrauch|gebühr|mahnung|kosten|preis|steuer|mwst|netto|brutto|datum|konto|vertrag|firma|adresse|referenz",
            "zahlungsbeleg|überweisung|lastschrift|kontoauszug|belastung|gutschrift|quittung|beleg|mandat|bestätigung| debit| credit",
            "CHF"));

        ocrKeywordsRepository.save(new OcrKeywords("en",
            "total|total amount|amount|sum due|payment amount|balance due|invoice total|total due|grand total|amount due|payable|outstanding balance|total payable|charge|fee",
            "invoice|bill|statement|due date|amount due|supplier|provider|utility|consumption|meter|account number|customer|billing|address|date|reference|charge|fee|service|tariff|rate",
            "debit note|bank statement|transfer|wire|receipt|paid|confirmation|mandate|payment slip|proof| credit| minus",
            "EUR"));
    }

    private byte[] createPlaceholderImage(String label) {
        return createSolidImage(0x42, 0x85, 0xF4, label);
    }

    private byte[] createSolidImage(int r, int g, int b, String label) {
        int width = 640;
        int height = 480;
        byte[] pixels = new byte[width * height * 3];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = (y * width + x) * 3;
                pixels[idx]     = (byte) r;
                pixels[idx + 1] = (byte) g;
                pixels[idx + 2] = (byte) b;
            }
        }

        return createMinimalPng(width, height, pixels);
    }

    private byte[] createMinimalPng(int width, int height, byte[] rgbPixels) {
        int stride = width * 3;
        byte[] rawData = new byte[(stride + 1) * height];
        for (int y = 0; y < height; y++) {
            rawData[y * (stride + 1)] = 0;
            System.arraycopy(rgbPixels, y * stride, rawData, y * (stride + 1) + 1, stride);
        }

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try {
            baos.write(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

            byte[] ihdr = new byte[13];
            ihdr[0] = (byte) ((width >> 24) & 0xFF);
            ihdr[1] = (byte) ((width >> 16) & 0xFF);
            ihdr[2] = (byte) ((width >> 8) & 0xFF);
            ihdr[3] = (byte) (width & 0xFF);
            ihdr[4] = (byte) ((height >> 24) & 0xFF);
            ihdr[5] = (byte) ((height >> 16) & 0xFF);
            ihdr[6] = (byte) ((height >> 8) & 0xFF);
            ihdr[7] = (byte) (height & 0xFF);
            ihdr[8] = 8;
            ihdr[9] = 2;
            ihdr[10] = 0;
            ihdr[11] = 0;
            ihdr[12] = 0;
            writeChunk(baos, "IHDR", ihdr);

            java.util.zip.Deflater deflater = new java.util.zip.Deflater();
            deflater.setInput(rawData);
            deflater.finish();
            java.io.ByteArrayOutputStream compressed = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                compressed.write(buffer, 0, count);
            }
            deflater.end();
            byte[] compressedData = compressed.toByteArray();
            writeChunk(baos, "IDAT", compressedData);

            writeChunk(baos, "IEND", new byte[0]);
        } catch (IOException e) {
            return new byte[0];
        }
        return baos.toByteArray();
    }

    private void writeChunk(java.io.OutputStream os, String type, byte[] data) throws IOException {
        byte[] len = new byte[4];
        len[0] = (byte) ((data.length >> 24) & 0xFF);
        len[1] = (byte) ((data.length >> 16) & 0xFF);
        len[2] = (byte) ((data.length >> 8) & 0xFF);
        len[3] = (byte) (data.length & 0xFF);
        os.write(len);

        byte[] typeBytes = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        os.write(typeBytes);
        os.write(data);

        java.util.zip.CRC32 crc = new java.util.zip.CRC32();
        crc.update(typeBytes);
        crc.update(data);
        long crcValue = crc.getValue();
        byte[] crcBytes = new byte[4];
        crcBytes[0] = (byte) ((crcValue >> 24) & 0xFF);
        crcBytes[1] = (byte) ((crcValue >> 16) & 0xFF);
        crcBytes[2] = (byte) ((crcValue >> 8) & 0xFF);
        crcBytes[3] = (byte) (crcValue & 0xFF);
        os.write(crcBytes);
    }
}
