package com.bfil.automapper

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

trait AutoMappable[A, B] {
  def mapTo(a: A): B
}

object AutoMappable {

  implicit def materializeAutoMappable[A, B]: AutoMappable[A, B] = macro materializeAutoMappableImpl[A, B]

  def materializeAutoMappableImpl[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context): c.Expr[AutoMappable[A, B]] = {
    import c.universe._

    val sourceType = weakTypeOf[A]
    val destType = weakTypeOf[B]
    val destCompanion = destType.typeSymbol.companion

    def getFirstTypeParam(tpe: Type) = {
      val TypeRef(_, _, tps) = tpe
      tps.head
    }

    def isOptionSymbol(typeSymbol: Symbol) = typeSymbol == typeOf[Option[_]].typeSymbol
    def isCaseClassSymbol(typeSymbol: Symbol) = typeSymbol.isClass && typeSymbol.asClass.isCaseClass

    def getFields(tpe: Type): List[FieldInfo] =
      tpe.decls.collectFirst {
        case m: MethodSymbol if m.isPrimaryConstructor => m
      }.get.paramLists.head.map(FieldInfo(_))

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

    def extractParams(sourceType: Type, destType: Type, acc: List[FieldInfo]): List[Tree] = {

      val sourceFields = getFields(sourceType)
      val destFields = getFields(destType)

      destFields.map { destField =>

        val sourceFieldOption = sourceFields.find(_.key == destField.key)

        if (sourceFieldOption.isDefined) {

          val sourceField = sourceFieldOption.get

          val fieldSelector = (acc ++ List(sourceField)).foldLeft(Ident(TermName("a")): Tree) {
            case (tree, field) => Select(tree, field.termName)
          }

          val value =
            if (sourceField.tpe != destField.tpe && (sourceField.isCaseClass || sourceField.isOptionalCaseClass)) {

              if (sourceField.isOptionalCaseClass) {
                val params = extractParams(getFirstTypeParam(sourceField.tpe), getFirstTypeParam(destField.tpe), List.empty)
                val value = q"${destField.companion}(..$params)"

                val lambda = Apply(Select(fieldSelector, TermName("map")),
                  List(Function(List(ValDef(Modifiers(Flag.PARAM), TermName("a"), TypeTree(), EmptyTree)), value)))

                q"$lambda"
              } else {
                val params = extractParams(sourceField.tpe, destField.tpe, acc :+ sourceField)
                q"${destField.companion}(..$params)"
              }

            } else fieldSelector

          q"${destField.termName} = $value"
        } else {
          if (destField.isOptional) AssignOrNamedArg(Ident(destField.termName), q"None")
          else throw new NoSuchElementException(s"${destField.key} is not a member of ${sourceType}")
        }
      }
    }

    val params = extractParams(sourceType, destType, List.empty)

    def generateCode() =
      q"""
        new AutoMappable[$sourceType, $destType] {
          def mapTo(a: $sourceType): $destType = $destCompanion(..$params)
        }
      """

    // println(showCode(generateCode()))

    c.Expr[AutoMappable[A, B]](generateCode())
  }
}