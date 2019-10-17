def next(i: Int): Int =
  java.math.BigInteger.valueOf(i).nextProbablePrime().intValue()

val primes = (1 to 1000).foldRight((1, Seq[BigDecimal]())){case (_,(acc, seq)) =>
    val nextAcc = next(acc)
    val factor = seq.lastOption.getOrElse(BigDecimal.valueOf(1)) * nextAcc
    nextAcc -> (seq :+ factor)
}._2


println(primes.mkString("\n"))

def primeCount(n: Long): Int = {
  val thees = BigDecimal.valueOf(n)
  primes.count(_ <= thees)
}


val check = List[(Long, Int)](1L -> 0, 2L -> 1, 3L -> 1, 500L -> 4, 5000L -> 5, 10000000000L -> 10)

check.foreach { case (l, i) =>
  println(s"\ncheck $l -> $i")
  assert(primeCount(l) == i, s"primeCount($l)=${primeCount(l)}  != $i")
}

val zero = List(1 -> 0)
val one = (2 to 5).map(_ -> 1)
val two = (6 to 14).map(_ -> 2)
val thr = List(30 -> 3)

val toCheck = zero ++ one ++ two ++ thr

toCheck.foreach { case (l, i) =>
  println(s"\ncheck $l -> $i")
  assert(primeCount(l) == i, s"primeCount($l)=${primeCount(l)}  != $i")
}