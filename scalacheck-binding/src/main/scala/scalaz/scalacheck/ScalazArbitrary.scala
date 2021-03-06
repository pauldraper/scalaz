package scalaz
package scalacheck

import java.math.BigInteger
import org.scalacheck.{Gen, Arbitrary}
import collection.mutable.ArraySeq
import reflect.ClassTag

/**
 * Instances of {@link scalacheck.Arbitrary} for many types in Scalaz.
 */
object ScalazArbitrary {
  import Scalaz._
  import Tags._
  import Arbitrary._
  import Gen._
  import ScalaCheckBinding._

  private def arb[A: Arbitrary]: Arbitrary[A] = implicitly[Arbitrary[A]]

  implicit def monoidCoproductArbitrary[M: Arbitrary, N: Arbitrary]: Arbitrary[M :+: N] =
    Functor[Arbitrary].map(arb[Vector[M \/ N]])(new :+:(_))

  /** @since 7.0.3 */
  implicit def theseArb[A: Arbitrary, B: Arbitrary]: Arbitrary[A \&/ B] =
    Arbitrary(Gen.oneOf(
      ^(arbitrary[A], arbitrary[B])(\&/.Both(_, _)),
      arbitrary[A].map(\&/.This(_)),
      arbitrary[B].map(\&/.That(_))
    ))

  implicit def EphemeralStreamArbitrary[A : Arbitrary] =
    Functor[Arbitrary].map(arb[Stream[A]])(EphemeralStream.fromStream[A](_))

  implicit def ImmutableArrayArbitrary[A : Arbitrary : ClassTag] =
    Functor[Arbitrary].map(arb[Array[A]])(ImmutableArray.fromArray[A](_))

  implicit def ValueArbitrary[A: Arbitrary]: Arbitrary[Value[A]] = Functor[Arbitrary].map(arb[A])(a => Value(a))
  implicit def NameArbitrary[A: Arbitrary]: Arbitrary[Name[A]] = Functor[Arbitrary].map(arb[A])(a => Name(a))
  implicit def NeedArbitrary[A: Arbitrary]: Arbitrary[Need[A]] = Functor[Arbitrary].map(arb[A])(a => Need(a))

  implicit val UnitArbitrary: Arbitrary[Unit] = Arbitrary(const(()))

  implicit val AlphaArbitrary: Arbitrary[Alpha] = Arbitrary(oneOf(Alpha.alphas))

  implicit val BooleanConjunctionArbitrary: Arbitrary[Boolean @@ Conjunction] = Functor[Arbitrary].map(arb[Boolean])(_.conjunction)

  implicit val arbBigInt: Arbitrary[BigInt] = Apply[Arbitrary].apply2[Int, Int, BigInt](arb[Int], arb[Int])(_ + _)

  implicit val arbBigInteger: Arbitrary[BigInteger] = Functor[Arbitrary].map(arb[BigInt])(_.bigInteger)

  implicit val BigIntegerMultiplicationArbitrary: Arbitrary[BigInteger @@ Multiplication] =
    Tag.subst[BigInteger, Arbitrary, Multiplication](arb[BigInteger])

  implicit val BigIntMultiplicationArbitrary: Arbitrary[BigInt @@ Multiplication] = Tag.subst(arb[BigInt])

  implicit val ByteMultiplicationArbitrary: Arbitrary[Byte @@ Multiplication] = Tag.subst(arb[Byte])

  implicit val CharMultiplicationArbitrary: Arbitrary[Char @@ Multiplication] = Tag.subst(arb[Char])

  implicit val ShortMultiplicationArbitrary: Arbitrary[Short @@ Multiplication] = Tag.subst(arb[Short])

  implicit val IntMultiplicationArbitrary: Arbitrary[Int @@ Multiplication] = Tag.subst(arb[Int])

  implicit val LongMultiplicationArbitrary: Arbitrary[Long @@ Multiplication] = Tag.subst(arb[Long])

  implicit val FloatMultiplicationArbitrary: Arbitrary[Float @@ Multiplication] = Tag.subst(arb[Float])

  implicit val DoubleMultiplicationArbitrary: Arbitrary[Double @@ Multiplication] = Tag.subst(arb[Double])

  implicit val DigitArbitrary: Arbitrary[Digit] = Arbitrary(oneOf(Digit.digits))

  import NonEmptyList._
  implicit def NonEmptyListArbitrary[A: Arbitrary]: Arbitrary[NonEmptyList[A]] = Apply[Arbitrary].apply2[A, IList[A], NonEmptyList[A]](arb[A], ilistArbitrary)(nel(_, _))


  /** @since 7.0.3 */
  implicit def OneAndArbitrary[F[_], A](implicit A: Arbitrary[A], FA: Arbitrary[F[A]]
                                        ): Arbitrary[OneAnd[F, A]] =
    Apply[Arbitrary].apply2(arb[A], arb[F[A]])(OneAnd.apply)

  /** @since 7.0.3 */
  implicit def OneOrArbitrary[F[_], A](implicit A: Arbitrary[A], FA: Arbitrary[F[A]]): Arbitrary[OneOr[F, A]] =
    Functor[Arbitrary].map(arb[F[A] \/ A])(OneOr(_))

  /** @since 7.1.0 */
  implicit def Arbitrary_==>>[A, B](implicit o: Order[A], A: Arbitrary[A], B: Arbitrary[B]): Arbitrary[A ==>> B] =
    Functor[Arbitrary].map(arb[List[(A, B)]])(as => ==>>.fromList(as))

  implicit def Arbitrary_ISet[A](implicit o: Order[A], A: Arbitrary[A]): Arbitrary[ISet[A]] =
    Functor[Arbitrary].map(arb[List[A]])(as => ISet.fromList(as))

  implicit def Arbitrary_Maybe[A: Arbitrary]: Arbitrary[Maybe[A]] =
    Functor[Arbitrary].map(arb[Option[A]])(Maybe.fromOption)

  import scalaz.Ordering._
  implicit val OrderingArbitrary: Arbitrary[Ordering] = Arbitrary(oneOf(LT, EQ, GT))

  implicit def TreeArbitrary[A: Arbitrary]: Arbitrary[Tree[A]] = Arbitrary {
  import scalaz.Tree._
    def tree(n: Int): Gen[Tree[A]] = n match {
      case 0 => arbitrary[A] map (leaf(_))
      case _ => {
        val nextSize = n.abs / 2
        Apply[Gen].apply2(arbitrary[A], resize(nextSize, containerOf[Stream, Tree[A]](Arbitrary(tree(nextSize)).arbitrary)))(node(_, _))
      }
    }
    Gen.sized(tree _)
  }

  implicit def IterableArbitrary[A: Arbitrary]: Arbitrary[Iterable[A]] =
      Apply[Arbitrary].apply2[A, List[A], Iterable[A]](arb[A], arb[List[A]])((a, list) => a :: list)

  // could not use type alias `TreeLoc.Parents` and `TreeLoc.TreeForest`.
  // https://github.com/scalaz/scalaz/pull/527#discussion_r6315123
  implicit def TreeLocArbitrary[A: Arbitrary]: Arbitrary[TreeLoc[A]] =
    Apply[Arbitrary].apply4(
      arb[Tree[A]],
      arb[Stream[Tree[A]]],
      arb[Stream[Tree[A]]],
      arb[Stream[(Stream[Tree[A]], A, Stream[Tree[A]])]]
    )(TreeLoc.loc)

  implicit def DisjunctionArbitrary[A: Arbitrary, B: Arbitrary]: Arbitrary[A \/ B] =
    Functor[Arbitrary].map(arb[Either[A, B]]) {
      case Left(a) => -\/(a)
      case Right(b) => \/-(b)
    }

  implicit def ValidationArbitrary[A: Arbitrary, B: Arbitrary]: Arbitrary[Validation[A, B]] =
    Functor[Arbitrary].map(arb[A \/ B])(_.validation)

//  implicit def ZipStreamArbitrary[A](implicit a: Arbitrary[A]): Arbitrary[ZipStream[A]] = arb[Stream[A]] ∘ ((s: Stream[A]) => s.ʐ)

  implicit def Tuple1Arbitrary[A: Arbitrary]: Arbitrary[Tuple1[A]] = Functor[Arbitrary].map(arb[A])(Tuple1(_))

  implicit def Function0Arbitrary[A: Arbitrary]: Arbitrary[() => A] = Functor[Arbitrary].map(arb[A])(() => _)

  implicit def FirstOptionArbitrary[A: Arbitrary]: Arbitrary[Option[A] @@ First] = Functor[Arbitrary].map(arb[Option[A]])(_.first)

  implicit def LastOptionArbitrary[A: Arbitrary]: Arbitrary[Option[A] @@ Last] = Functor[Arbitrary].map(arb[Option[A]])(_.last)

  implicit def MinOptionArbitrary[A: Arbitrary]: Arbitrary[MinOption[A]] = Tag.subst(arb[Option[A]])

  implicit def MaxOptionArbitrary[A: Arbitrary]: Arbitrary[MaxOption[A]] = Tag.subst(arb[Option[A]])

  implicit def FirstMaybeArbitrary[A: Arbitrary]: Arbitrary[Maybe[A] @@ First] = Functor[Arbitrary].map(arb[Maybe[A]])(_.first)

  implicit def LastMaybeArbitrary[A: Arbitrary]: Arbitrary[Maybe[A] @@ Last] = Functor[Arbitrary].map(arb[Maybe[A]])(_.last)

  implicit def MinMaybeArbitrary[A: Arbitrary]: Arbitrary[MinMaybe[A]] = Tag.subst(arb[Maybe[A]])

  implicit def MaxMaybeArbitrary[A: Arbitrary]: Arbitrary[MaxMaybe[A]] = Tag.subst(arb[Maybe[A]])

  implicit def EitherLeftProjectionArbitrary[A: Arbitrary, B: Arbitrary]: Arbitrary[Either.LeftProjection[A, B]] = Functor[Arbitrary].map(arb[Either[A, B]])(_.left)

  implicit def EitherRightProjectionArbitrary[A: Arbitrary, B: Arbitrary]: Arbitrary[Either.RightProjection[A, B]] = Functor[Arbitrary].map(arb[Either[A, B]])(_.right)

  implicit def EitherFirstLeftProjectionArbitrary[A: Arbitrary, B: Arbitrary]: Arbitrary[Either.LeftProjection[A, B] @@ First] = Functor[Arbitrary].map(arb[Either[A, B]])(x => Tag(x.left))

  implicit def EitherFirstRightProjectionArbitrary[A: Arbitrary, B: Arbitrary]: Arbitrary[Either.RightProjection[A, B] @@ First] = Functor[Arbitrary].map(arb[Either[A, B]])(x => Tag(x.right))

  implicit def EitherLastLeftProjectionArbitrary[A: Arbitrary, B: Arbitrary]: Arbitrary[Either.LeftProjection[A, B] @@ Last] = Functor[Arbitrary].map(arb[Either[A, B]])(x => Tag(x.left))

  implicit def EitherLastRightProjectionArbitrary[A: Arbitrary, B: Arbitrary]: Arbitrary[Either.RightProjection[A, B] @@ Last] = Functor[Arbitrary].map(arb[Either[A, B]])(x => Tag(x.right))

  implicit def ArraySeqArbitrary[A: Arbitrary]: Arbitrary[ArraySeq[A]] = Functor[Arbitrary].map(arb[List[A]])(x => ArraySeq(x: _*))

  import FingerTree._

  implicit def FingerArbitrary[V, A](implicit a: Arbitrary[A], measure: Reducer[A, V]): Arbitrary[Finger[V, A]] = Arbitrary(oneOf(
    arbitrary[A].map(one(_): Finger[V, A]),
    ^(arbitrary[A], arbitrary[A])(two(_, _): Finger[V, A]),
    ^^(arbitrary[A], arbitrary[A], arbitrary[A])(three(_, _, _): Finger[V, A]),
    ^^^(arbitrary[A], arbitrary[A], arbitrary[A], arbitrary[A])(four(_, _, _, _): Finger[V, A])
  ))

  implicit def NodeArbitrary[V, A](implicit a: Arbitrary[A], measure: Reducer[A, V]): Arbitrary[Node[V, A]] = Arbitrary(oneOf(
    ^(arbitrary[A], arbitrary[A])(node2[V, A](_, _)),
    ^^(arbitrary[A], arbitrary[A], arbitrary[A])(node3[V, A](_, _, _))
  ))

  implicit def FingerTreeArbitrary[V, A](implicit a: Arbitrary[A], measure: Reducer[A, V]): Arbitrary[FingerTree[V, A]] = Arbitrary {
    def fingerTree[A](n: Int)(implicit a1: Arbitrary[A], measure1: Reducer[A, V]): Gen[FingerTree[V, A]] = n match {
      case 0 => empty[V, A]
      case 1 => arbitrary[A].map(single[V, A](_))
      case n => {
        val nextSize = n.abs / 2
        ^^(FingerArbitrary[V, A].arbitrary,
          fingerTree[Node[V, A]](nextSize)(NodeArbitrary[V, A], implicitly),
          FingerArbitrary[V, A].arbitrary
        )(deep[V, A](_, _, _))
      }
    }
    Gen.sized(fingerTree[A] _)
  }

  implicit def IndSeqArbibrary[A: Arbitrary]: Arbitrary[IndSeq[A]] = Functor[Arbitrary].map(arb[List[A]])(IndSeq.fromSeq)

  import java.util.concurrent.Callable

  implicit def CallableArbitrary[A: Arbitrary]: Arbitrary[Callable[A]] = Functor[Arbitrary].map(arb[A])((x: A) => Applicative[Callable].point(x))

  import scalaz.concurrent.Future
  implicit def FutureArbitrary[A: Arbitrary]: Arbitrary[Future[A]] =
    Arbitrary(arbitrary[A] map ((x: A) => Future.now(x)))

  import scalaz.concurrent.Task
  implicit def TaskArbitrary[A: Arbitrary]: Arbitrary[Task[A]] =
    Arbitrary(arbitrary[A] map ((x: A) => Task.now(x)))

  import Zipper._
  implicit def ZipperArbitrary[A: Arbitrary]: Arbitrary[Zipper[A]] =
    Apply[Arbitrary].apply3[Stream[A], A, Stream[A], Zipper[A]](arb[Stream[A]], arb[A], arb[Stream[A]])(zipper[A](_, _, _))

  implicit def KleisliArbitrary[M[_], A, B](implicit a: Arbitrary[A => M[B]]): Arbitrary[Kleisli[M, A, B]] =
    Functor[Arbitrary].map(a)(Kleisli[M, A, B](_))

  implicit def CoproductArbitrary[F[_], G[_], A](implicit a: Arbitrary[F[A] \/ G[A]]): Arbitrary[Coproduct[F, G, A]] =
    Functor[Arbitrary].map(a)(Coproduct(_))

  implicit def writerTArb[F[_], W, A](implicit A: Arbitrary[F[(W, A)]]): Arbitrary[WriterT[F, W, A]] =
    Functor[Arbitrary].map(A)(WriterT[F, W, A](_))

  implicit def unwriterTArb[F[+_], U, A](implicit A: Arbitrary[F[(U, A)]]): Arbitrary[UnwriterT[F, U, A]] =
    Functor[Arbitrary].map(A)(UnwriterT[F, U, A](_))

  implicit def optionTArb[F[+_], A](implicit A: Arbitrary[F[Option[A]]]): Arbitrary[OptionT[F, A]] =
    Functor[Arbitrary].map(A)(OptionT[F, A](_))

  implicit def maybeTArb[F[+_], A](implicit A: Arbitrary[F[Maybe[A]]]): Arbitrary[MaybeT[F, A]] =
    Functor[Arbitrary].map(A)(MaybeT[F, A](_))

  implicit def lazyOptionArb[F[_], A](implicit A: Arbitrary[Option[A]]): Arbitrary[LazyOption[A]] =
    Functor[Arbitrary].map(A)(LazyOption.fromOption[A](_))

  implicit def lazyOptionTArb[F[+_], A](implicit A: Arbitrary[F[LazyOption[A]]]): Arbitrary[LazyOptionT[F, A]] =
    Functor[Arbitrary].map(A)(LazyOptionT[F, A](_))

  implicit def lazyEitherArb[F[_], A: Arbitrary, B: Arbitrary]: Arbitrary[LazyEither[A, B]] =
    Functor[Arbitrary].map(arb[Either[A, B]]) {
      case Left(a)  => LazyEither.lazyLeft(a)
      case Right(b) => LazyEither.lazyRight(b)
    }

  implicit def lazyEitherTArb[F[+_], A, B](implicit A: Arbitrary[F[LazyEither[A, B]]]): Arbitrary[LazyEitherT[F, A, B]] =
    Functor[Arbitrary].map(A)(LazyEitherT[F, A, B](_))

  // backwards compatibility
  def stateTArb[F[+_], S, A](implicit A: Arbitrary[S => F[(S, A)]]): Arbitrary[StateT[F, S, A]] =
    indexedStateTArb[F, S, S, A](A)

  implicit def indexedStateTArb[F[_], S1, S2, A](implicit A: Arbitrary[S1 => F[(S2, A)]]): Arbitrary[IndexedStateT[F, S1, S2, A]] =
    Functor[Arbitrary].map(A)(IndexedStateT[F, S1, S2, A](_))

  implicit def eitherTArb[F[+_], A, B](implicit A: Arbitrary[F[A \/ B]]): Arbitrary[EitherT[F, A, B]] =
      Functor[Arbitrary].map(A)(EitherT[F, A, B](_))

  implicit def constArbitrary[A: Arbitrary, B]: Arbitrary[Const[A, B]] =
    Functor[Arbitrary].map(arb[A])(Const(_))

  implicit def dlistArbitrary[A](implicit A: Arbitrary[List[A]]) = Functor[Arbitrary].map(A)(as => DList(as : _*))

  implicit def ilistArbitrary[A](implicit A: Arbitrary[List[A]]) = Functor[Arbitrary].map(A)(IList.fromList)

  implicit def dequeueArbitrary[A](implicit A: Arbitrary[List[A]]) = Functor[Arbitrary].map(A)(Dequeue.apply)

  implicit def lazyTuple2Arbitrary[A: Arbitrary, B: Arbitrary]: Arbitrary[LazyTuple2[A, B]] =
    Applicative[Arbitrary].apply2(arb[A], arb[B])(LazyTuple2(_, _))

  implicit def lazyTuple3Arbitrary[A: Arbitrary, B: Arbitrary, C: Arbitrary]: Arbitrary[LazyTuple3[A, B, C]] =
    Applicative[Arbitrary].apply3(arb[A], arb[B], arb[C])(LazyTuple3(_, _, _))

  implicit def lazyTuple4Arbitrary[A: Arbitrary, B: Arbitrary, C: Arbitrary, D: Arbitrary]: Arbitrary[LazyTuple4[A, B, C, D]] =
    Applicative[Arbitrary].apply4(arb[A], arb[B], arb[C], arb[D])(LazyTuple4(_, _, _, _))

  implicit def heapArbitrary[A](implicit O: Order[A], A: Arbitrary[List[A]]) = {
    Functor[Arbitrary].map(A)(as => Heap.fromData(as))
  }

  // backwards compatibility
  def storeTArb[F[+_], A, B](implicit A: Arbitrary[(F[A => B], A)]): Arbitrary[StoreT[F, A, B]] = indexedStoreTArb[F, A, A, B](A)

  implicit def indexedStoreTArb[F[+_], I, A, B](implicit A: Arbitrary[(F[A => B], I)]): Arbitrary[IndexedStoreT[F, I, A, B]] = Functor[Arbitrary].map(A)(IndexedStoreT[F, I, A, B](_))

  implicit def listTArb[F[+_], A](implicit FA: Arbitrary[F[List[A]]], F: Applicative[F]): Arbitrary[ListT[F, A]] = Functor[Arbitrary].map(FA)(ListT.fromList(_))

  implicit def streamTArb[F[+_], A](implicit FA: Arbitrary[F[Stream[A]]], F: Applicative[F]): Arbitrary[StreamT[F, A]] = Functor[Arbitrary].map(FA)(StreamT.fromStream(_))

  // workaround bug in Scalacheck 1.8-SNAPSHOT.
  private def arbDouble: Arbitrary[Double] = Arbitrary { Gen.oneOf(posNum[Double], negNum[Double])}

  implicit def CaseInsensitiveArbitrary[A](implicit A0: Arbitrary[A], A1: FoldCase[A]): Arbitrary[CaseInsensitive[A]] =
    Functor[Arbitrary].map(A0)(CaseInsensitive(_))

  implicit def dievArbitrary[A](implicit A: Arbitrary[List[A]], E: Enum[A]): Arbitrary[Diev[A]] = Functor[Arbitrary].map(A)(_.grouped(2).foldLeft(Diev.empty[A]){(working, possiblePair) =>
    possiblePair match {
      case first :: second :: Nil => working + ((first, second))
      case value :: Nil => working
      case _ => sys.error("Unexpected amount of items in paired list.")
    }
  })

  implicit def iterateeInputArbitrary[A: Arbitrary]: Arbitrary[scalaz.iteratee.Input[A]] = {
    import scalaz.iteratee.Input._
    Arbitrary(Gen.oneOf(
      Gen.const(emptyInput[A]),
      Gen.const(eofInput[A]),
      arbitrary[A].map(e => elInput(e))
    ))
  }

}
