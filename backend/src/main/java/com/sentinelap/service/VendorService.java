package com.sentinelap.service;

import com.sentinelap.domain.Vendor;
import com.sentinelap.dto.VendorDto;
import com.sentinelap.repo.FraudAlertRepository;
import com.sentinelap.repo.InvoiceRepository;
import com.sentinelap.repo.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
    private final InvoiceRepository invoiceRepository;
    private final FraudAlertRepository fraudAlertRepository;

    public List<VendorDto> listVendors() {
        return vendorRepository.findAll().stream().map(this::toDto).toList();
    }

    public VendorDto getVendor(UUID id) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Vendor not found: " + id));
        return toDto(vendor);
    }

    private VendorDto toDto(Vendor v) {
        long openAlerts = fraudAlertRepository.findByVendorIdOrderByCreatedAtDesc(v.getId()).stream()
                .filter(a -> a.getStatus().name().equals("OPEN"))
                .count();
        long invoiceCount = invoiceRepository.countByVendorId(v.getId());
        return new VendorDto(
                v.getId(), v.getName(), v.getTaxId(), v.getEmail(), v.getPhone(),
                v.getBankAccountLast4(), v.getBankRoutingNumber(), v.getBankAccountUpdatedAt(),
                v.getRiskScore(), v.getIsVerified(), v.getSource().name(), openAlerts, invoiceCount
        );
    }
}
