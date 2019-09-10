package catsex.ch10

import impl._

object main extends App {
  createUser("Noel", "noel@underscore.io").println()
  // res14: cats.data.Validated[wrapper.Errors,User] = Valid(User(Noel,noel@underscore.io))
  createUser("", "dave@underscore@io").println()
  // res15: cats.data.Validated[wrapper.Errors,User] = Invalid(NonEmptyList(Must be longer than 3 characters, Must contain a single @ character))

  kleisliimpl.createUser("Noel", "noel@underscore.io").println()
  kleisliimpl.createUser("", "dave@underscore@io").println()

}
