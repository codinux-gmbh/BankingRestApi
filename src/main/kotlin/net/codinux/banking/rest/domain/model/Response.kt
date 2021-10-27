package net.codinux.banking.rest.domain.model


class Response<T>(val data: T?) {

  constructor(error: String) : this(null) {
    this.error = error
  }


  var error: String? = null
    private set

  val successful: Boolean
    get() = data != null

}