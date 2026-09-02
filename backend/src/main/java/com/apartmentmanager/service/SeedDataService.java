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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
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
        ap2.setStatus(ApartmentStatus.AVAILABLE_IMMEDIATELY);
        apartmentRepository.save(ap2);

        Apartment ap3 = apartmentRepository.save(
                new Apartment("City Loft", "Modern loft in a renovated factory building", "789 Industrial Way, Berlin", 1500.0, 2, 65.0));
        ap3.setStatus(ApartmentStatus.AVAILABLE_FROM);
        ap3.setAvailableFrom(java.time.LocalDate.now().plusDays(14));
        apartmentRepository.save(ap3);

        String defaultPassword = requireDefaultPassword();

        User tenant = userRepository.findByUsername("tenant").orElse(null);
        if (tenant != null) {
            tenant.setApartment(ap1);
            userRepository.save(tenant);
        } else {
            tenant = userRepository.save(new User("tenant", passwordEncoder.encode(defaultPassword), Role.TENANT, "tenant@example.com"));
            tenant.setApartment(ap1);
            userRepository.save(tenant);
        }

        User tenant2 = userRepository.save(new User("tenant2", passwordEncoder.encode(defaultPassword), Role.TENANT, "tenant2@example.com"));
        tenant2.setApartment(null);
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
                    byte[] placeholder = createGradientImage(0x4285F4, 0x34A853);
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

        int[][] ap1Gradients = {
            {0x667eea, 0x764ba2}, {0xf093fb, 0xf5576c}, {0x4facfe, 0x00f2fe},
            {0x43e97b, 0x38f9d7}, {0xfa709a, 0xfee140}, {0xa18cd1, 0xfbc2eb},
            {0xfbc2eb, 0xa6c1ee}, {0xfda085, 0xf6d365}, {0xf5576c, 0xff6a88},
            {0x667eea, 0x764ba2}
        };
        for (int i = 0; i < 10; i++) {
            String fileName = UUID.randomUUID() + ".jpg";
            Path filePath = uploadPath.resolve(fileName);
            try {
                byte[] img = createGradientImage(ap1Gradients[i][0], ap1Gradients[i][1]);
                Files.write(filePath, img);
            } catch (IOException e) { continue; }
            ap1.getPhotoPaths().add(fileName);
        }
        apartmentService.save(ap1);

        int[][] ap2Gradients = {
            {0x11998e, 0x38ef7d}, {0x0cebeb, 0x20e3b2}, {0xfc5c7d, 0x6a82fb},
            {0xee9ca7, 0xffdde1}, {0x2193b0, 0x6dd5ed}, {0x834d9b, 0xd04ed6},
            {0xc94b4b, 0x4b134f}, {0x2b5876, 0x4e4376}, {0x00b09b, 0x96c93d},
            {0xfc4a1a, 0xf7b733}
        };
        for (int i = 0; i < 10; i++) {
            String fileName = UUID.randomUUID() + ".jpg";
            Path filePath = uploadPath.resolve(fileName);
            try {
                byte[] img = createGradientImage(ap2Gradients[i][0], ap2Gradients[i][1]);
                Files.write(filePath, img);
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

        int[][] ap3Gradients = {
            {0x8e2de2, 0x4a00e0}, {0xf953c6, 0xb91d73}, {0x1a2a6c, 0xb21f1f},
            {0x614385, 0x516395}, {0x02aab0, 0x00cdac}, {0xda22ff, 0x9733ee},
            {0xf12711, 0xf5af19}, {0x16a085, 0xf4d03f}, {0xc31432, 0x240b36},
            {0x7f00ff, 0xe100ff}
        };
        for (int i = 0; i < 10; i++) {
            String fileName = UUID.randomUUID() + ".jpg";
            Path filePath = uploadPath.resolve(fileName);
            try {
                byte[] img = createGradientImage(ap3Gradients[i][0], ap3Gradients[i][1]);
                Files.write(filePath, img);
            } catch (IOException e) { continue; }
            ap3.getPhotoPaths().add(fileName);
        }
        ap3.setPresentation("Stunning industrial loft in the heart of Berlin-Friedrichshain, converted from a historic factory building. The apartment features exposed brick walls, polished concrete floors, floor-to-ceiling windows, and 4-meter ceilings creating an incredible sense of space.\n\nThe open-plan living area includes a state-of-the-art kitchen with island, a dining area for 8, and a spacious lounge. The master bedroom features a walk-in closet and en-suite bathroom with rainfall shower. A second bedroom/office provides flexibility.\n\nBuilding amenities include a rooftop terrace with panoramic views, fitness room, bicycle storage, and secure underground parking (available for rent).\n\nWalking distance to Boxhagener Platz, East Side Gallery, and Warschauer Straße. S-Bahn Warschauer Straße (2 min), U5 Frankfurter Tor (5 min).\n\nAvailable from 14 days. Ideal for professionals or couples.");
        apartmentService.save(ap3);

        contactRepository.save(new Contact("Building Management", "loft@city-management.de", ap3));

        seedOcrKeywords();
    }

    private static String requireDefaultPassword() {
        return com.apartmentmanager.util.DefaultPassword.require();
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

    private byte[] createGradientImage(int color1, int color2) {
        int width = 640;
        int height = 480;
        int r1 = (color1 >> 16) & 0xFF, g1 = (color1 >> 8) & 0xFF, b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF, g2 = (color2 >> 8) & 0xFF, b2 = color2 & 0xFF;
        byte[] pixels = new byte[width * height * 3];

        for (int y = 0; y < height; y++) {
            double t = (double) y / (height - 1);
            int r = (int) (r1 + (r2 - r1) * t);
            int g = (int) (g1 + (g2 - g1) * t);
            int b = (int) (b1 + (b2 - b1) * t);
            for (int x = 0; x < width; x++) {
                int idx = (y * width + x) * 3;
                pixels[idx] = (byte) r;
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
