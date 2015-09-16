package com.bfil.automapper

trait AutoMapping {
  private def fromMap[T: Mappable](map: Map[String, Any]) = implicitly[Mappable[T]].fromMap(map)
  private def toMap[T: Mappable](t: T) = implicitly[Mappable[T]].toMap(t)

  implicit class MappableMap(map: Map[String, Any]) {
    def as[T: Mappable]: T = fromMap(map)
  }

  implicit class MappableInstance[T: Mappable](t: T) {
    def asMap: Map[String, Any] = toMap(t)
    def mapTo[T2: Mappable] = t.asMap.as[T2]
    
    def autoMapTo[T2](implicit mappable: AutoMappable[T, T2]): T2 = AutoMapping.mapTo(t)
  }
  
}

object AutoMapping {
  private def mapTo[A, B](a: A)(implicit mappable: AutoMappable[A, B]): B = mappable.mapTo(a)
}