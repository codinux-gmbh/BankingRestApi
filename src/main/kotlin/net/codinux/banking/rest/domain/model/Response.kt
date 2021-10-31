package net.codinux.banking.rest.domain.model

import net.codinux.banking.rest.domain.model.tan.TanRequired


class Response<T> private constructor(
  val type: ResponseType,
  val data: T?,
  val error: String?,
  val tanRequired: TanRequired?
) {

  constructor(data: T) : this(ResponseType.Success, data, null, null)

  constructor(error: String) : this(ResponseType.Error, null, error, null)

  constructor(tanRequired: TanRequired) : this(ResponseType.TanRequired, null, null, tanRequired)


  val successful: Boolean
    get() = type == ResponseType.Success

}