package com.bfil.automapper

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

trait Mappable[T] {
  def toMap(t: T): Map[String, Any]
  def fromMap(map: Map[String, Any]): T
}

object Mappable {

  implicit def materializeMappable[T]: Mappable[T] = macro materializeMappableImpl[T]

  def materializeMappableImpl[T: c.WeakTypeTag](c: Context): c.Expr[Mappable[T]] = {
    import c.universe._

    def getFirstTypeParam(tpe: Type) = {
      val TypeRef(_, _, tps) = tpe
      tps.head
    }

    def isOptionSymbol(typeSymbol: Symbol) = typeSymbol == typeOf[Option[_]].typeSymbol
    def isCaseClassSymbol(typeSymbol: Symbol) = typeSymbol.isClass && typeSymbol.asClass.isCaseClass

    case class FieldInfo(field: Symbol) {
      lazy val term = field.asTerm
      lazy val termName = term.name
      lazy val key = termName.decodedName.toString
      lazy val tpe = term.typeSignature
      lazy val typeSymbol = tpe.typeSymbol
      lazy val isCaseClass = isCaseClassSymbol(typeSymbol)
      lazy val isOptional = isOptionSymbol(typeSymbol)
      lazy val isOptionalCaseClass = isOptional && isCaseClassSymbol(getFirstTypeParam(tpe).typeSymbol)
      lazy val companion = if (isOptional) getFirstTypeParam(tpe).typeSymbol.companion else typeSymbol.companion
    }

    val mapOfStringToAny = List(AppliedTypeTree(Ident(TypeName("Map")), List(Ident(TypeName("String")), Ident(TypeName("Any")))))

    def extractMapParams(tpe: Type, acc: List[FieldInfo]): List[(Tree, Tree)] = {

      val fields = tpe.decls.collectFirst {
        case m: MethodSymbol if m.isPrimaryConstructor => m
      }.get.paramLists.head

      fields.map(FieldInfo(_)).map { field =>

        if (field.isCaseClass || field.isOptionalCaseClass) {

          if (field.isOptionalCaseClass) {
            val (toMapParams, fromMapParams) = extractMapParams(getFirstTypeParam(field.tpe), acc :+ field).unzip
            (q"${field.key} -> flattenMap(Map(..$toMapParams))", q"optional(Option(${field.companion}(..$fromMapParams))).flatten.asInstanceOf[${field.tpe}]")
          } else {
            val (toMapParams, fromMapParams) = extractMapParams(field.tpe, acc :+ field).unzip
            (q"${field.key} -> flattenMap(Map(..$toMapParams))", q"${field.companion}(..$fromMapParams).asInstanceOf[${field.tpe}]")
          }

        } else {

          val value = (acc ++ List(field)).foldLeft(Ident(TermName("t")): Tree) {
            case (tree, field) =>
              if (field.isOptionalCaseClass) Select(Select(tree, field.termName), TermName("get"))
              else Select(tree, field.termName)
          }

          val valueExtractor = acc.foldLeft(Ident(TermName("map")): Tree) {
            case (tree, field) =>
              TypeApply(Select(Apply(
                tree, List(Literal(Constant(field.key)))),
                TermName("asInstanceOf")), mapOfStringToAny)
          }

          if (field.isOptional) {
            val instance: Tree =
              TypeApply(Select(Apply(
                Ident(TermName("Option")),
                List(Apply(valueExtractor, List(Literal(Constant(field.key)))))),
                TermName("asInstanceOf")), List(q"${field.tpe}"))
            (q"${field.key} -> optional($value).flatten", q"optional($instance).flatten")
          } else {
            val instance: Tree =
              TypeApply(Select(Apply(
                valueExtractor, List(Literal(Constant(field.key)))),
                TermName("asInstanceOf")), List(q"${field.tpe}"))
            (q"${field.key} -> optional($value)", q"$instance")
          }

        }
      }
    }

    val tpe = weakTypeOf[T]
    val companion = tpe.typeSymbol.companion

    val (toMapParams, fromMapParams) = extractMapParams(tpe, List.empty).unzip

    def generateCode() =
      q"""
        new Mappable[$tpe] {
          def toMap(t: $tpe): Map[String, Any] = flattenMap(Map(..$toMapParams))
          def fromMap(map: Map[String, Any]): $tpe = $companion(..$fromMapParams)
          private def flattenMap(map: Map[String, Any]) = map.collect {
            case (key, Some(value)) => key -> value
            case (key, map: Map[_, _]) if map.size > 0 => key -> map
          }
          private def optional[T](t: => T): Option[T] = scala.util.Try(t).toOption
        }
      """
    
    // println(showCode(generateCode()))

    c.Expr[Mappable[T]](generateCode())
  }
}