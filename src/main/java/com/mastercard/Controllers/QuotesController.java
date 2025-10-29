package com.mastercard.Controllers;

import com.mastercard.Utils.MastercardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mastercard")
public class QuotesController
{
    private final MastercardService mastercardService;

    public QuotesController(MastercardService mastercardService) {
        this.mastercardService = mastercardService;
    }

    @PostMapping("/quotes")
    public ResponseEntity<String> createQuote() {
        try {
            String response = mastercardService.sendQuoteRequest();
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            // Log exception appropriately
            return ResponseEntity.status(500).body("Error calling Mastercard Quotes API: " + ex.getMessage());
        }
    }
}
