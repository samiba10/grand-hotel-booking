package com.grandhotel.fx.rate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FxRateRepository extends JpaRepository<FxRate, Long> {

  Optional<FxRate> findFirstByBaseCurrencyAndQuoteCurrencyOrderByAsOfDescIdDesc(
      String base, String quote);
}
