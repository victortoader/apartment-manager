package com.example.demo.service;

import com.example.demo.model.*;
import com.example.demo.repository.ApartmentRepository;
import com.example.demo.repository.TicketRepository;
import com.example.demo.repository.UserRepository;
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
    private final TicketRepository ticketRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    public SeedDataService(UserRepository userRepository,
                           ApartmentRepository apartmentRepository,
                           TicketRepository ticketRepository,
                           PasswordEncoder passwordEncoder,
                           UserService userService) {
        this.userRepository = userRepository;
        this.apartmentRepository = apartmentRepository;
        this.ticketRepository = ticketRepository;
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
            tenant = userRepository.save(new User("tenant", passwordEncoder.encode("tenant"), Role.TENANT));
            tenant.setApartment(ap1);
            userRepository.save(tenant);
        }

        User tenant2 = userRepository.save(new User("tenant2", passwordEncoder.encode("tenant2"), Role.TENANT));
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
    }

    private byte[] createPlaceholderImage(String label) {
        int width = 200;
        int height = 150;
        byte[] pixels = new byte[width * height * 3];

        for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                int idx = (y * width + x) * 3;
                int region = (y * 3) / height;
                switch (region) {
                    case 0:
                        pixels[idx] = (byte) 0x42;
                        pixels[idx + 1] = (byte) 0x85;
                        pixels[idx + 2] = (byte) 0xF4;
                        break;
                    case 1:
                        pixels[idx] = (byte) 0x34;
                        pixels[idx + 1] = (byte) 0xA8;
                        pixels[idx + 2] = (byte) 0x53;
                        break;
                    default:
                        pixels[idx] = (byte) 0x88;
                        pixels[idx + 1] = (byte) 0x88;
                        pixels[idx + 2] = (byte) 0x88;
                }
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
