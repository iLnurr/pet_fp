package com.iserba.fp

trait Converter[I, O] {
  def convert: I => O
}
