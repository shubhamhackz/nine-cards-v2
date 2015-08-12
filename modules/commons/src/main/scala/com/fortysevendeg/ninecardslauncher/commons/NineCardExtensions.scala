package com.fortysevendeg.ninecardslauncher.commons

import rapture.core._
import rapture.core.scalazInterop.ResultT

import scala.language.implicitConversions
import scala.reflect.ClassTag
import scalaz._
import scalaz.concurrent.Task

object NineCardExtensions {

  object CatchAll {

    def apply[E <: Exception] = new CatchingAll[E]()

    class CatchingAll[E <: Exception]() {
      def apply[A](blk: => A)(implicit classTag: ClassTag[E], cv: Throwable => E): Result[A, E] =
        \/.fromTryCatchNonFatal(blk) match {
          case \/-(x) => Result.answer[A, E](x)
          case -\/(e) => Errata(Seq((implicitly[ClassTag[E]], (e.getMessage, cv(e)))))
        }
    }

  }

  implicit class ResultTExtensions[A, B <: Exception : ClassTag](r : ResultT[Task, A, B]) {

    def resolve[E <: Exception : ClassTag](implicit cv: Exception => E) = {
      val task: Task[Result[A, B]] = r.run
      val innerResult: Task[Result[A, E]] = task.map {
        case e@Errata(_) =>
          val exs = e.exceptions map (ie => (implicitly[ClassTag[E]], (ie.getMessage, cv(ie))))
          Errata[A, E](exs)
        case Unforeseen(e) => Unforeseen[A, E](e)
        case Answer(s) => Answer[A, E](s)
      }
      ResultT(innerResult)
    }

  }


}