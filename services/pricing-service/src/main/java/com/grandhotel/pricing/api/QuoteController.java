package com.grandhotel.pricing.api;

import com.grandhotel.pricing.api.QuoteDtos.QuoteRequest;
import com.grandhotel.pricing.api.QuoteDtos.QuoteResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing")
@Tag(name = "Pricing")
public class QuoteController {

  private final QuoteService quotes;

  public QuoteController(QuoteService quotes) {
    this.quotes = quotes;
  }

  @PostMapping("/quotes")
  @PreAuthorize("hasAuthority('pricing:quote')")
  @Operation(summary = "Price a stay for one or more room types, per night, in the requested currency")
  public QuoteResponse quote(@Valid @RequestBody QuoteRequest req) {
    return quotes.quote(req);
  }
}
