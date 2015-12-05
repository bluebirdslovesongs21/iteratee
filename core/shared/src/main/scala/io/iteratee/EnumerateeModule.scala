package io.iteratee

import algebra.Order
import cats.Monad

trait EnumerateeModule[F[_]] {
  /**
   * Applies a function to each input element and feeds the resulting outputs to the inner iteratee.
   */
  final def map[O, I](f: O => I)(implicit F: Monad[F]): Enumeratee[F, O, I] =
    Enumeratee.map[F, O, I](f)

  final def flatMap[O, I](f: O => Enumerator[F, I])(implicit F: Monad[F]): Enumeratee[F, O, I] =
    Enumeratee.flatMap[F, O, I](f)

  final def collect[O, I](pf: PartialFunction[O, I])(implicit F: Monad[F]): Enumeratee[F, O, I] =
    Enumeratee.collect[F, O, I](pf)

  final def filter[E](p: E => Boolean)(implicit F: Monad[F]): Enumeratee[F, E, E] =
    Enumeratee.filter[F, E](p)

  final def sequenceI[O, I](iteratee: Iteratee[F, O, I])(implicit
    F: Monad[F]
  ): Enumeratee[F, O, I] =
    Enumeratee.sequenceI[F, O, I](iteratee)

  /**
   * Uniqueness filter. Assumes that the input enumerator is already sorted.
   */
  final def uniq[E: Order](implicit F: Monad[F]): Enumeratee[F, E, E] = Enumeratee.uniq[F, E]
    
  /**
   * Zip with the count of elements that have been encountered.
   */
  final def zipWithIndex[E](implicit F: Monad[F]): Enumeratee[F, E, (E, Long)] =
    Enumeratee.zipWithIndex[F, E]

  final def grouped[E](n: Int)(implicit F: Monad[F]): Enumeratee[F, E, Vector[E]] =
    Enumeratee.grouped[F, E](n)

  final def splitOn[E](p: E => Boolean)(implicit F: Monad[F]): Enumeratee[F, E, Vector[E]] =
    Enumeratee.splitOn[F, E](p)

  final def cross[E1, E2](e2: Enumerator[F, E2])(implicit
    F: Monad[F]
  ): Enumeratee[F, E1, (E1, E2)] =
    Enumeratee.cross[F, E1, E2](e2)
}