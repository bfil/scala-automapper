package io.bfil

import scala.language.dynamics
import scala.language.experimental.macros

package object automapper {
  def automap[A](a: A): PartialMapping[A] = new PartialMapping(a)
  class PartialMapping[A](a: A) extends Dynamic {
    def to[B](implicit mapping: Mapping[A, B]) = mapping.map(a)
    def applyDynamicNamed[B](name: String)(args: (String, Any)*): B =
      macro Mapping.materializeDynamicMappingImpl[A, B]
  }
  def generateMapping[A, B]: Mapping[A, B] = macro Mapping.materializeMappingImpl[A, B]
}
