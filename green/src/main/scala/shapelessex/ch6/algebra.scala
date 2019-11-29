package shapelessex.ch6

object algebra {
  trait Migration[A, B] {
    def apply(a: A): B
  }
  object Migration {
    implicit class MigrationOps[A](a: A) {
      def migrateTo[B](implicit migration: Migration[A, B]): B =
        migration.apply(a)
    }
  }
}
