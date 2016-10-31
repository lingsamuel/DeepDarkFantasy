package com.thoughtworks.DDF

import com.thoughtworks.DDF.Arrow.BEvalArr
import com.thoughtworks.DDF.Language.{BEvalLang, Lang, LangTerm, LangTermLang}
import scalaz.Leibniz._

trait ImpW[T] {
  ext =>

  type Weight

  val w: Weight

  val exp: LangTerm[Weight => T]

  implicit val bel = BEvalLang

  implicit val ltl = LangTermLang

  implicit val wi = ltl.domInfo(ltl.reprInfo(exp(ltl)))

  implicit val ti = ltl.rngInfo(ltl.reprInfo(exp(ltl)))

  implicit val wl: LossInfo[Weight] = wi(bel)

  implicit val tl = ti(bel)

  trait Forward {
    val res: BEval[T]
    def update(rate: Double, tloss: Loss[T]): ImpW[T]
  }
  def forward = new Forward {

    val fres = new BEvalArr {}.aeval(exp(bel)).forward(ext.wl.convert(w))

    override val res: BEval[T] = fres.eb

    def update(rate: Double, tloss: Loss[T]): ImpW[T] = {
      val newW = wl.updatel(w)(rate)(fres.backward(tloss))
      new ImpW[T] {
        override type Weight = ext.Weight

        override val w: ext.Weight = newW

        override val exp: LangTerm[ext.Weight => T] = ext.exp
      }
    }
  }
}

object ImpW {
  def apply[T](expT: LangTerm[T]): ImpW[T] =
    new ImpW[T] {
      override type Weight = scala.Unit

      override val w: scala.Unit = ()

      override val exp: LangTerm[scala.Unit => T] = new LangTerm[scala.Unit => T] {
        override def apply[Info[_], Repr[_]](implicit lang: Lang[Info, Repr]): Repr[scala.Unit => T] =
          lang.K_(expT(lang))(lang.unitInfo)
      }
    }

  type Aux[X, XL] = ImpW[X] { type Weight = XL }
}