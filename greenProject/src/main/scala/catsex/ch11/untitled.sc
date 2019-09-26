// To execute Scala code, please define an object named Solution that extends App

def solution(list: List[Int], num: Int): Boolean = {
  def exists(check: Int, l: List[Int]) =
    l.foldLeft(false)((acc,el) => acc || (el + check) == num)
  (for {
    tuple <- list.zipWithIndex
  } yield {
    val (toCheck, index) = tuple

    val (left, right) = list.splitAt(index)
    exists(toCheck, left++ right)
  }).contains(true)
}

def solution2(list: List[Int], num: Int): Boolean = {
  @scala.annotation.tailrec
  def helper(toCheck: Int, source: List[Int], l: List[Int], acc: Boolean = false): Boolean = l match {
    case Nil =>
      source match {
        case Nil =>
          acc
        case ::(head, tl) =>
          helper(head, tl, tl, acc)
      }
    case ::(head, tl) =>
      helper(toCheck, source, tl, acc || (head + toCheck) == num)
  }

  list match {
    case Nil =>
      false
    case ::(head, tl) =>
      helper(head, tl, tl)
  }
}


assert(solution(List(1, 1, 3, 4, 5, 8), 8)) // 3 + 5 = 8 => true
assert(!solution(List(1, 1, 5, 8), 8)) // false
assert(!solution(List(1, 5, 10, 15), 7)) // false
assert(solution(List(4, 4, 5, 8), 8)) // 4 + 4 = 8 => true
assert(!solution(List.empty, 5)) // false
assert(!solution(List(5), 5)) //false
assert(!solution(List(2, 2, 2, 2), 8)) // false

println("Success!")