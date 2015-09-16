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

    val tpe = weakTypeOf[T]
    val companion = tpe.typeSymbol.companion

    def firstTypeParam(tpe: Type) = {
      val TypeRef(_, _, tps) = tpe
      tps.head
    }
    def isOptional(typeSymbol: Symbol) = typeSymbol == typeOf[Option[_]].typeSymbol
    def isCaseClass(typeSymbol: Symbol) = typeSymbol.isClass && typeSymbol.asClass.isCaseClass
    def isOptionalCaseClass(tpe: Type) = if (isOptional(tpe.typeSymbol)) isCaseClass(firstTypeParam(tpe).typeSymbol) else false

    def extractMapParams(tpe: Type, acc: List[(String, Boolean)]): List[(c.Tree, c.Tree)] = {

      val fields = tpe.decls.collectFirst {
        case m: MethodSymbol if m.isPrimaryConstructor => m
      }.get.paramLists.head

      fields.map { field =>
        val name = field.asTerm.name
        val key = name.decodedName.toString
        val fieldType = field.asTerm.typeSignature
        val fieldTypeSymbol = fieldType.typeSymbol

        val fieldIsCaseClass = isCaseClass(fieldTypeSymbol)
        val fieldIsOptional = isOptional(fieldTypeSymbol)
        val fieldIsOptionalCaseClass = isOptionalCaseClass(fieldType)

        if (fieldIsCaseClass || fieldIsOptionalCaseClass) {

          if (fieldIsOptionalCaseClass) {
            val companion = firstTypeParam(fieldType).typeSymbol.companion

            val (toMapParams, fromMapParams) = extractMapParams(firstTypeParam(fieldType), acc :+ (key, true)).unzip
            (q"$key -> Map(..$toMapParams).filter { case (k, v) => v != null }", q"Option($companion(..$fromMapParams)).asInstanceOf[$fieldType]")
          } else {
            val companion = fieldTypeSymbol.companion
            val (toMapParams, fromMapParams) = extractMapParams(fieldType, acc :+ (key, false)).unzip
            (q"$key -> Map(..$toMapParams).filter { case (k, v) => v != null }", q"$companion(..$fromMapParams).asInstanceOf[$fieldType]")
          }
        } else {

          val value = (acc ++ List((key, false))).foldLeft(Ident(TermName("t")): c.Tree) {
            case (tree, (term, opt)) =>
              if (opt) Select(Select(tree, TermName(term)), TermName("get"))
              else Select(tree, TermName(term))
          }

          val mapOfStringToAny = List(AppliedTypeTree(Ident(TypeName("Map")),
            List(Ident(TypeName("String")),
              Ident(TypeName("Any")))))

          val valueExtractor = acc.foldLeft(Ident(TermName("map")): c.Tree) {
            case (tree, (term, opt)) =>
              TypeApply(Select(Apply(
                tree, List(Literal(Constant(term)))),
                TermName("asInstanceOf")), mapOfStringToAny)
          }

          if (fieldIsOptional) {
            val instance: c.Tree =
              TypeApply(Select(Apply(
                Ident(TermName("Option")),
                List(Apply(valueExtractor, List(Literal(Constant(key)))))),
                TermName("asInstanceOf")), List(q"$fieldType"))
            (q"$key -> $value.getOrElse(null)", q"scala.util.Try($instance).toOption.flatten")
          } else {
            val instance: c.Tree =
              TypeApply(Select(Apply(
                valueExtractor, List(Literal(Constant(key)))),
                TermName("asInstanceOf")), List(q"$fieldType"))
            (q"$key -> $value", q"$instance")
          }

        }
      }
    }

    val (toMapParams, fromMapParams) = extractMapParams(tpe, List.empty).unzip

    def generateCode() =
      q"""
      new Mappable[$tpe] {
        def toMap(t: $tpe): Map[String, Any] = Map(..$toMapParams).filter { case (k, v) => v != null }
        def fromMap(map: Map[String, Any]): $tpe = $companion(..$fromMapParams)
      }
    """

    // println(showCode(generateCode()))

    c.Expr[Mappable[T]] {
      generateCode()
    }
  }
}