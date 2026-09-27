package com.sentinelap.web;

import com.sentinelap.dto.InvoiceDto;
import com.sentinelap.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    public List<InvoiceDto> list() {
        return invoiceService.listInvoices();
    }
}
