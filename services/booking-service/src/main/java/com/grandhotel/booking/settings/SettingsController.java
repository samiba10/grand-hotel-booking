package com.grandhotel.booking.settings;

import com.grandhotel.booking.settings.SettingsDtos.SettingsResponse;
import com.grandhotel.booking.settings.SettingsDtos.UpdateSettingsRequest;
import com.grandhotel.common.security.AuthenticatedUser;
import com.grandhotel.common.security.servlet.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
@Tag(name = "System settings")
public class SettingsController {

  private final SettingsService settings;

  public SettingsController(SettingsService settings) {
    this.settings = settings;
  }

  @GetMapping
  @Operation(summary = "Offered currencies, default currency and region -> currency (any signed-in user)")
  public SettingsResponse get() {
    return settings.get();
  }

  @PutMapping
  @PreAuthorize("hasAuthority('settings:write')")
  @Operation(summary = "Update system settings (ADMIN). Audited with before -> after")
  public SettingsResponse update(
      @Valid @RequestBody UpdateSettingsRequest req, @CurrentUser AuthenticatedUser user) {
    return settings.update(req, user);
  }
}
