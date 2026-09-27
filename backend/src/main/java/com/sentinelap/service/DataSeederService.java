package com.sentinelap.service;

import com.sentinelap.domain.*;
import com.sentinelap.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Random;

/**
 * Generates a realistic-looking synthetic AP dataset on first startup so the
 * product is fully demoable with zero external accounts. A handful of
 * deliberately-injected fraud patterns (a recent bank-account change followed
 * by an invoice, a duplicate invoice, and a velocity spike) guarantee the
 * dashboard has real alerts to show immediately.
 *
 * This is skipped automatically once data already exists, and can be disabled
 * entirely via sentinelap.seed-demo-data=false (e.g. once QuickBooks sync is
 * the real data source).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeederService implements CommandLineRunner {

    private final VendorRepository vendorRepository;
    private final InvoiceRepository invoiceRepository;
    private final BankAccountChangeEventRepository bankChangeRepository;
    private final AppUserRepository appUserRepository;
    private final InvoiceService invoiceService;
    private final PasswordEncoder passwordEncoder;

    @Value("${sentinelap.seed-demo-data:true}")
    private boolean seedEnabled;

    private static final String[] VENDOR_NAMES = {
            "Northgate Office Supplies", "Cascade Logistics LLC", "Meridian IT Services",
            "BrightPath Facilities Group", "Union Steel Fabricators", "Harborline Freight Co",
            "Silverline Consulting Partners", "Redwood Janitorial Services", "Alta Vista Marketing",
            "Pioneer Equipment Rentals", "Crestview Legal Services", "Blue Ridge Packaging",
            "Summit Networks Inc", "Fieldstone Landscaping", "Vantage Point Security",
            "Copperline Electrical Contractors", "Westbrook Print & Design", "Ironwood Manufacturing",
            "Clearwater HVAC Solutions", "Granite Peak Software"
    };

    private final Random random = new Random(42);

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Demo data seeding disabled (sentinelap.seed-demo-data=false)");
            return;
        }
        if (vendorRepository.count() > 0) {
            log.info("Data already present, skipping demo seed");
            return;
        }

        seedDemoUser();
        List<Vendor> vendors = seedVendors();
        seedInvoicesWithFraudPatterns(vendors);
        log.info("Seeded {} vendors and demo invoice/alert data", vendors.size());
    }

    private void seedDemoUser() {
        AppUser user = AppUser.builder()
                .email("demo@sentinelap.com")
                .passwordHash(passwordEncoder.encode("Demo1234!"))
                .fullName("Demo Analyst")
                .organization("SentinelAP Demo Org")
                .role("ANALYST")
                .build();
        appUserRepository.save(user);
        log.info("Seeded demo login -> email: demo@sentinelap.com / password: Demo1234!");
    }

    private List<Vendor> seedVendors() {
        java.util.ArrayList<Vendor> vendors = new java.util.ArrayList<>();
        for (String name : VENDOR_NAMES) {
            Vendor vendor = Vendor.builder()
                    .name(name)
                    .taxId(String.format("%02d-%07d", random.nextInt(90) + 10, random.nextInt(9_999_999)))
                    .email(name.toLowerCase().replaceAll("[^a-z]", "") + "@vendor-example.com")
                    .phone(String.format("(%03d) 555-%04d", random.nextInt(900) + 100, random.nextInt(10000)))
                    .bankAccountLast4(String.format("%04d", random.nextInt(10000)))
                    .bankRoutingNumber(String.format("%09d", random.nextInt(1_000_000_000)))
                    .bankAccountUpdatedAt(OffsetDateTime.now().minusDays(200 + random.nextInt(500)))
                    .isVerified(true)
                    .source(Vendor.DataSource.SYNTHETIC)
                    .createdAt(OffsetDateTime.now().minusDays(300 + random.nextInt(400)))
                    .build();
            vendors.add(vendorRepository.save(vendor));
        }
        return vendors;
    }

    private void seedInvoicesWithFraudPatterns(List<Vendor> vendors) {
        // Normal invoice history for every vendor.
        for (Vendor vendor : vendors) {
            int invoiceCount = 3 + random.nextInt(5);
            for (int i = 0; i < invoiceCount; i++) {
                createInvoice(vendor, randomAmount(500, 4000), LocalDate.now().minusDays(random.nextInt(180)));
            }
        }

        // --- Injected fraud pattern #1: bank-account change immediately before a large invoice ---
        Vendor bankFraudVendor = vendors.get(2); // Meridian IT Services
        bankChangeRepository.save(BankAccountChangeEvent.builder()
                .vendor(bankFraudVendor)
                .oldAccountLast4(bankFraudVendor.getBankAccountLast4())
                .oldRoutingNumber(bankFraudVendor.getBankRoutingNumber())
                .newAccountLast4("9981")
                .newRoutingNumber("071000288")
                .changedVia("EMAIL_REQUEST")
                .detectedAt(OffsetDateTime.now().minusDays(4))
                .build());
        bankFraudVendor.setBankAccountLast4("9981");
        bankFraudVendor.setBankAccountUpdatedAt(OffsetDateTime.now().minusDays(4));
        vendorRepository.save(bankFraudVendor);
        createInvoice(bankFraudVendor, new BigDecimal("18450.00"), LocalDate.now().minusDays(1));

        // --- Injected fraud pattern #2: duplicate invoice ---
        Vendor dupVendor = vendors.get(5); // Harborline Freight Co
        Invoice original = createInvoice(dupVendor, new BigDecimal("6200.00"), LocalDate.now().minusDays(10));
        Invoice duplicate = Invoice.builder()
                .vendor(dupVendor)
                .invoiceNumber(original.getInvoiceNumber())
                .amount(original.getAmount())
                .currency("USD")
                .issuedDate(LocalDate.now().minusDays(2))
                .dueDate(LocalDate.now().plusDays(28))
                .status(Invoice.InvoiceStatus.PENDING)
                .source(Vendor.DataSource.SYNTHETIC)
                .build();
        invoiceService.ingestAndEvaluate(duplicate);

        // --- Injected fraud pattern #3: velocity anomaly (invoice-count spike) ---
        Vendor velocityVendor = vendors.get(9); // Pioneer Equipment Rentals
        for (int i = 0; i < 9; i++) {
            createInvoice(velocityVendor, randomAmount(800, 2200), LocalDate.now().minusDays(i));
        }
    }

    private Invoice createInvoice(Vendor vendor, BigDecimal amount, LocalDate issuedDate) {
        Invoice invoice = Invoice.builder()
                .vendor(vendor)
                .invoiceNumber("INV-" + vendor.getId().toString().substring(0, 6).toUpperCase() + "-" + random.nextInt(99999))
                .amount(amount)
                .currency("USD")
                .issuedDate(issuedDate)
                .dueDate(issuedDate.plusDays(30))
                .status(Invoice.InvoiceStatus.PENDING)
                .source(Vendor.DataSource.SYNTHETIC)
                .build();
        invoiceService.ingestAndEvaluate(invoice);
        return invoice;
    }

    private BigDecimal randomAmount(int min, int max) {
        return BigDecimal.valueOf(min + random.nextInt(max - min)).setScale(2);
    }
}
