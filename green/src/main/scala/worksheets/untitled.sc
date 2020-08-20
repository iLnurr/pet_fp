import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
//def fib5( n : Int) : Int = {
//  def fib_tail( n: Int, a:Int, b:Int): Int = n match {
//    case 0 => a
//    case _ => fib_tail( n-1, b, (a+b)%1000000 )
//  }
//  fib_tail( n%1500000, 0, 1)
//}

//object Main {
//  def fib5(n: Long,exp: Int=18): Long = {
//    val k           = BigInt(10).pow(exp).toLong
//    val periodicity = BigInt(150).pow(exp - 1).toLong
//    def fib_tail(n: Long, a: Long, b: Long): Long = n match {
//      case 0 => a
//      case _ => fib_tail(n - 1, b, (a + b) % k)
//    }
//    fib_tail(n % periodicity, 0, 1)
//  }
//  def main(args: Array[String]): Unit = {
//    val arr    = readLine().split(" ")
//    val (n, m) = (arr.head.toLong, arr.tail.head.toLong)
//    val result = fib5(n) % m
//    println(result)
//  }
//}


def fib5(n: Long,exp: Int=18): Long = {
  val k           = BigInt(10).pow(exp).toLong
  val periodicity = BigInt(150).pow(exp - 1).toLong
  def fib_tail(n: Long, a: Long, b: Long): Long = n match {
    case 0 => a
    case _ => fib_tail(n - 1, b, (a + b) % k)
  }
  fib_tail(n % periodicity, 0, 1)
}


//fib5(BigInt(10).pow(18).toLong) % BigInt(10).pow(5).toLong

def periodPisano(m: Long): ListBuffer[Long] = {
  @ tailrec def recur(periodBuffer: ListBuffer[Long], a: Long, b: Long, pa: Long, pb: Long): ListBuffer[Long] = (pa,pb) match {
    case (0,1) => periodBuffer
    case _ =>
      recur(periodBuffer += pb, b, a + b, pb, (a + b) % m)
  }

  if (m == 1) ListBuffer(0) else recur(ListBuffer(0,1), 1, 1, 1,1).init
}

5 % 5

periodPisano(1)
periodPisano(2)
periodPisano(3)
periodPisano(4)
periodPisano(5)
periodPisano(6)
periodPisano(16)
periodPisano(67)

Map(1 -> 1, 2 -> 3, 3 -> 8, 4 -> 6, 5 -> 20, 6 -> 24,16 -> 24, 67 -> 136).foreach{case (k,v) =>
  assert(periodPisano(k).size == v, s"p($k) != $v, but ${periodPisano(k)}")
}