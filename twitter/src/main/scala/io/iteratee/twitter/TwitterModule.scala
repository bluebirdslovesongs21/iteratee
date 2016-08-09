package io.iteratee.twitter

import cats.{ Eval, MonadError }
import com.twitter.util.{ Future, FuturePool }
import io.catbird.util.Rerunnable
import io.iteratee.{ EnumerateeModule, EnumeratorErrorModule, IterateeErrorModule, Module }
import io.iteratee.files.FileModule
import scala.Predef.implicitly

trait TwitterModule extends Module[Rerunnable]
  with EnumerateeModule[Rerunnable]
  with EnumeratorErrorModule[Rerunnable, Throwable] with IterateeErrorModule[Rerunnable, Throwable]
  with FileModule[Rerunnable] {
  final type M[f[_]] = MonadError[f, Throwable]

  protected def toFuture[A](a: => A): Future[A]

  final protected val F: MonadError[Rerunnable, Throwable] = implicitly

  /**
   * Since Rerunnable is already lazy, the Eval here is strict
   */
  final override protected def captureEffect[A](a: => A): Eval[Rerunnable[A]] =
    Eval.now(new Rerunnable[A] {
      final def run: Future[A] = toFuture(a)
    })
}

trait DefaultFuturePoolTwitterModule extends TwitterModule {
  private[this] val futurePool: FuturePool = FuturePool.unboundedPool

  protected final def toFuture[A](a: => A): Future[A] = futurePool(a)
}
