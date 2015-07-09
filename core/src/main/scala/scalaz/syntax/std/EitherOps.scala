package scalaz
package syntax
package std

final class EitherOps[A, B](val self: Either[A, B]) extends AnyVal {

  final def disjunction: A \/ B = \/ fromEither self

  final def leftOrElse[A2 >: A](f: B => A2): A2 = self.fold(identity, f)

  final def rightOrElse[B2 >: B](f: A => B2): B2 = self.fold(f, identity)

  final def validation: Validation[A, B] = Validation fromEither self
}

trait ToEitherOps {
  implicit def ToEitherOpsFromEither[A, B](e: Either[A, B]): EitherOps[A, B] = new EitherOps(e)
}

