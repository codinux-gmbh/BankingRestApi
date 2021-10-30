package net.codinux.banking.rest.domain.model

import net.codinux.banking.rest.domain.model.tan.TanRequired


class Response<T>(
  val data: T?,
  val error: String?,
  val tanRequired: TanRequired?
) {

  constructor(data: T) : this(data, null, null)

  constructor(error: String) : this(null, error, null)

  constructor(tanRequired: TanRequired) : this(null, null, tanRequired)


  val successful: Boolean
    get() = data != null

}