package net.codinux.banking.rest.domain.util

import net.codinux.banking.rest.domain.model.tan.TanMethod
import net.codinux.banking.rest.domain.model.tan.TanMethodType


class TanMethodSelector {

  fun selectNonVisual(supportedTanMethods: List<TanMethod>): TanMethod? {
    return supportedTanMethods.firstOrNull { it.type == TanMethodType.AppTan }
      ?: supportedTanMethods.firstOrNull { it.type == TanMethodType.SmsTan }
      ?: supportedTanMethods.firstOrNull { it.displayName.contains("manuell", true) }
      ?: supportedTanMethods.firstOrNull()
  }

}