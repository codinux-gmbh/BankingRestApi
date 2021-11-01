package net.codinux.banking.rest.domain.model.tan


enum class TanMediumType {

  /**
   * All other TAN media, like AppTan.
   */
  Generic,

  /**
   * If I'm not wrong MobilePhone is only used for SmsTan.
   */
  MobilePhone,

  /**
   * Mostly used for chipTan.
   */
  TanGenerator

}