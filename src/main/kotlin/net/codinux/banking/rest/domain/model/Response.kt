package net.codinux.banking.rest.domain.model

import net.codinux.banking.rest.domain.model.tan.TanRequired


class Response<T>(
  val type: ResponseType,
  val data: T?,
  val error: String?,
  val errorType: ErrorType?,
  val tanRequired: TanRequired?
) {

  constructor(data: T) : this(ResponseType.Success, data, null, null, null)

  constructor(error: String, errorType: ErrorType? = null) : this(ResponseType.Error, null, error, errorType, null)

  constructor(tanRequired: TanRequired) : this(ResponseType.TanRequired, null, null, null, tanRequired)

}