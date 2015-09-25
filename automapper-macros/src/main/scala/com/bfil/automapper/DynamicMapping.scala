package com.bfil.automapper

import scala.language.dynamics

class DynamicMapping(args: Map[String, Any]) extends Dynamic {
  def selectDynamic[T](name: String): T = args.get(name) match {
    case Some(value) => value.asInstanceOf[T]
    case None        => throw new NoSuchElementException(s"$name has not been specified in the dynamic mapping")
  }
}

object DynamicMapping extends Dynamic {
  def applyDynamic(name: String)(args: Any*): DynamicMapping = new DynamicMapping(Map.empty)
  def applyDynamicNamed(name: String)(args: (String, Any)*): DynamicMapping = name match {
    case "apply" => new DynamicMapping(args.toMap)
    case _       => throw new NoSuchMethodException(s"Please use DynamicMapping(...)")
  }
  def empty[A]: A => DynamicMapping = a => DynamicMapping()
}

