package com.sentinelap.web;

import com.sentinelap.dto.VendorDto;
import com.sentinelap.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @GetMapping
    public List<VendorDto> list() {
        return vendorService.listVendors();
    }

    @GetMapping("/{id}")
    public VendorDto get(@PathVariable UUID id) {
        return vendorService.getVendor(id);
    }
}
