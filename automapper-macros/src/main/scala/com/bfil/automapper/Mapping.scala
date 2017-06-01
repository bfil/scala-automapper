package com.bfil.automapper

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

trait Mapping[A, B] {
  def map(a: A): B
}

object Mapping {

  implicit def materializeMapping[A, B]: Mapping[A, B] = macro materializeMappingImpl[A, B]

  def materializeMappingImpl[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context): c.Expr[Mapping[A, B]] = generateMapping[A, B](c)(Seq.empty)

  def materializeDynamicMappingImpl[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context)(name: c.Expr[String])(args: c.Expr[(String, Any)]*): c.Expr[B] = {
    import c.universe._

    val c.Expr(Literal(Constant(methodName))) = name

    val dynamicParams = args.map {
      _.tree.collect {
        case arg @ Apply(TypeApply(Select(Select(Ident(scala), tuple2), TermName("apply")), List(TypeTree(), TypeTree())), List(Literal(Constant(key: String)), impl)) => arg
      }
    }.flatten

    val mapping = methodName match {
      case "dynamicallyTo" =>
        generateMapping[A, B](c)(dynamicParams)
      case methodName =>
        c.error(name.tree.pos, s"not found value $methodName in com.bfil.automapper.PartialMapping")
        generateMapping[A, B](c)(dynamicParams)
    }

    val source = c.macroApplication.collect {
      case arg @ Apply(TypeApply(Select(Select(_, _), TermName("automap")), List(TypeTree())), List(source)) => source
    }.headOption

    if(!source.isDefined) c.error(c.enclosingPosition, "Unable to resolve source reference to be used for auto mapping")

    def generateCode() = q"""${mapping}.map(${source.get}): ${weakTypeOf[B]}"""

    c.Expr[B](generateCode())
  }

  private def generateMapping[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context)(dynamicParams: Seq[c.universe.Apply]): c.Expr[Mapping[A, B]] = {
    import c.universe._

    import scala.util.control.ControlThrowable
    class AutomapperException(val pos: Position, val msg: String) extends Throwable(msg) with ControlThrowable

    val sourceType = weakTypeOf[A]
    val targetType = weakTypeOf[B]

    if(targetType =:= typeOf[Nothing]) {
      c.error(c.enclosingPosition, "Unable to infer target type for auto mapping")
    }

    val targetCompanion = targetType.typeSymbol.companion

    def getFirstTypeParam(tpe: Type) = { val TypeRef(_, _, tps) = tpe; tps.head }
    def getSecondTypeParam(tpe: Type) = { val TypeRef(_, _, tps) = tpe; tps.tail.head }

    def isOptionSymbol(typeSymbol: Symbol) = typeSymbol == typeOf[Option[_]].typeSymbol
    def isCaseClassSymbol(typeSymbol: Symbol) = typeSymbol.isClass && typeSymbol.asClass.isCaseClass
    def isIterableType(tpe: Type): Boolean = tpe.baseClasses.contains(typeOf[Iterable[_]].typeSymbol) && !isMapType(tpe)
    def isMapType(tpe: Type): Boolean = tpe.baseClasses.contains(typeOf[Map[_, _]].typeSymbol)

    def getFields(tpe: Type): List[FieldInfo] =
      tpe.decls.collectFirst {
        case m: MethodSymbol if m.isPrimaryConstructor => m
      }.map(_.paramLists.head.map(FieldInfo(_))).getOrElse(List.empty)

    case class FieldInfo(field: Symbol) {
      lazy val term = field.asTerm
      lazy val termName = term.name
      lazy val key = termName.decodedName.toString
      lazy val tpe = term.typeSignature
      lazy val typeSymbol = tpe.typeSymbol
      lazy val isCaseClass = isCaseClassSymbol(typeSymbol)
      lazy val isOptional = isOptionSymbol(typeSymbol)
      lazy val isOptionalCaseClass = isOptional && isCaseClassSymbol(getFirstTypeParam(tpe).typeSymbol)
      lazy val isIterable = isIterableType(tpe)
      lazy val isIterableCaseClass = isIterable && isCaseClassSymbol(getFirstTypeParam(tpe).typeSymbol)
      lazy val isMap = isMapType(tpe)
      lazy val companion = typeSymbol.companion
      lazy val firstTypeParamCompanion =
        if (isOptional || isIterable) getFirstTypeParam(tpe).typeSymbol.companion
        else throw new NoSuchElementException(s"$key is of type $tpe and does not have a type parameter")
      lazy val secondTypeParamCompanion =
        if (isMap) getSecondTypeParam(tpe).typeSymbol.companion
        else throw new NoSuchElementException(s"$key is of type $tpe and does not have a second type parameter")
    }

    def extractParams(sourceType: Type, targetType: Type, parentFields: List[FieldInfo], isRoot: Boolean = true): List[Tree] = {

      val sourceFields = getFields(sourceType)
      val targetFields = getFields(targetType)

      targetFields.map { targetField =>

        val sourceFieldOption = sourceFields.find(_.key == targetField.key)

        val targetFieldLiteral = Literal(Constant(targetField.key))
        val dynamicField = dynamicParams.find { term =>
          term.children(1).equalsStructure(targetFieldLiteral)
        }

        if (dynamicField.isDefined && isRoot) {
          AssignOrNamedArg(Ident(targetField.termName), dynamicField.get.children(2))
        } else if (sourceFieldOption.isDefined) {

          val sourceField = sourceFieldOption.get

          val fieldSelector = (parentFields ++ List(sourceField)).foldLeft(Ident(TermName("a")): Tree) {
            case (tree, field) => Select(tree, field.termName)
          }

          val sourceAndTargetHaveDifferentTypes = sourceField.tpe != targetField.tpe ||
            (sourceField.isMap && getSecondTypeParam(sourceField.tpe) != getSecondTypeParam(targetField.tpe))

          val value = {
            if (sourceAndTargetHaveDifferentTypes &&
              (targetField.isCaseClass || targetField.isOptionalCaseClass || targetField.isIterableCaseClass || targetField.isMap)) {

              if (targetField.isOptionalCaseClass || targetField.isIterableCaseClass) {
                val params = extractParams(getFirstTypeParam(sourceField.tpe), getFirstTypeParam(targetField.tpe), List.empty, false)
                val value = q"${targetField.firstTypeParamCompanion}(..$params)"

                val lambda = Apply(Select(fieldSelector, TermName("map")),
                  List(Function(List(ValDef(Modifiers(Flag.PARAM), TermName("a"), TypeTree(), EmptyTree)), value)))

                q"$lambda"
              } else if (targetField.isMap) {
                val params = extractParams(getSecondTypeParam(sourceField.tpe), getSecondTypeParam(targetField.tpe), List.empty, false)
                val value = q"${targetField.secondTypeParamCompanion}(..$params)"

                val lambda = Apply(Select(fieldSelector, TermName("mapValues")),
                  List(Function(List(ValDef(Modifiers(Flag.PARAM), TermName("a"), TypeTree(), EmptyTree)), value)))

                q"$lambda"
              } else {
                val params = extractParams(sourceField.tpe, targetField.tpe, parentFields :+ sourceField, false)
                q"${targetField.companion}(..$params)"
              }

            } else fieldSelector
          }

          q"${targetField.termName} = $value"
        } else {
          def namedAssign(value: Tree) = AssignOrNamedArg(Ident(targetField.termName), value)

          if (targetField.isOptional) namedAssign(q"None")
          else if (targetField.isIterable) namedAssign(q"${targetField.companion}.empty")
          else if (targetField.isMap) namedAssign(q"Map.empty")
          else EmptyTree
        }
      }.filter(p => !p.isEmpty)
    }

    val params = extractParams(sourceType, targetType, List.empty)

    def generateCode() =
      q"""
        new Mapping[$sourceType, ${targetType}] {
          def map(a: $sourceType): ${targetType} = {
            $targetCompanion(..$params)
          }
        }
      """

    c.Expr[Mapping[A, B]](generateCode())
  }
}
