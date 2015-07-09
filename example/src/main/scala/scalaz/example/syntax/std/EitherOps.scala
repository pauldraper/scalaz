package scalaz.example.syntax.std

import scalaz.Scalaz.ToEitherOpsFromEither

object EitherOps {

  {
    val e: Either[Int, String] = Right("1")
    assert(e.leftOrElse(_.toInt) == 1)
    assert(e.rightOrElse(Function.const("")) == "1")
  }

  {
    val e: Either[Int, String] = Left(1)
    assert(e.leftOrElse(Function.const(0)) == 1)
    assert(e.rightOrElse(_.toString) == "1")
  }

  {
    val e: Either[Int, String] = Left(1)
    assert(e.rightOrElse(Function.const({} : Any)) == {})
  }

}
