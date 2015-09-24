package com.bfil.automapper

trait AutoMapping {
  def map[A, B](a: A)(implicit mapping: Mapping[A, B]): B = mapping.map(a)
  implicit class Mappable[A](a: A) {
    def mapTo[B](implicit mapping: Mapping[A, B]): B = AutoMapping.map(a)
  }
}

object AutoMapping extends AutoMapping {
  import scala.language.experimental.macros
  def generate[A, B]: Mapping[A, B] = macro Mapping.materializeMappingImpl[A, B]
  def generateDynamic[A, B](dynamicMapping: A => DynamicMapping): Mapping[A, B] = macro Mapping.materializeDynamicMappingImpl[A, B]
}