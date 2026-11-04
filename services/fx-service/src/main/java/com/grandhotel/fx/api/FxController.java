package com.grandhotel.fx.api;

import com.grandhotel.fx.rate.FxRate;
import com.grandhotel.fx.rate.FxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rates are not secret, but the endpoint still requires an authenticated caller (zero trust).
 * Parameter constraints use Spring MVC built-in method validation (400 on violation).
 */
@RestController
@RequestMapping("/api/fx")
@Tag(name = "FX")
public class FxController {

  private final FxService fx;

  public FxController(FxService fx) {
    this.fx = fx;
  }

  @GetMapping("/rates/latest")
  @Operation(summary = "Latest rate: quote-currency micros per 1 unit of base currency")
  public FxRateResponse latest(
      @RequestParam @Pattern(regexp = "^[A-Z]{3}$") String base,
      @RequestParam @Pattern(regexp = "^[A-Z]{3}$") String quote) {
    return FxRateResponse.from(fx.latest(base, quote));
  }

  public record FxRateResponse(
      String base, String quote, long rateMicros, Instant asOf, String source) {

    static FxRateResponse from(FxRate r) {
      return new FxRateResponse(
          r.getBaseCurrency(), r.getQuoteCurrency(), r.getRateMicros(), r.getAsOf(), r.getSource());
    }
  }
}
