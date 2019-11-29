package com.iserba.fp

object model {
  case class Model(id:       Option[Long])
  case class Event(ts:       Long, model: Model)
  case class Request(entity: Option[Event])
  case class Response(body:  Option[Event])
}
