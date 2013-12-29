/**
 * User: Alexander Slesarenko
 * Date: 11/24/13
 */
package scalan.seq

import scalan.{ScalanSeq, Base}
import scala.language.{implicitConversions}

trait BaseSeq extends Base { self: ScalanSeq =>

  implicit def reifyObject[A: LElem](obj: ReifiableObject[A]): Rep[A] = !!!("should not be called")
  //def reifyObject1[A: Elem](obj: ReifiableObjectAux[A]): Rep[obj.ThisType] = !!!("should not be called")
  //def reifyObject1[A:Elem](obj: ReifiableObjectAux[A]): Rep[obj.ThisType]
  override def toRep[A](x: A)(implicit eA: Elem[A]) = x
}
