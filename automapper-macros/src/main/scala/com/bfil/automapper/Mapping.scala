package com.bfil.automapper

import scala.language.dynamics
import scala.language.experimental.macros
import scala.reflect.macros.blackbox.Context

trait Mapping[A, B] {
  def map(a: A): B
}

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

object Mapping {

  implicit def materializeMapping[A, B]: Mapping[A, B] = macro materializeMappingImpl[A, B]

  def materializeMappingImpl[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context): c.Expr[Mapping[A, B]] = {
    import c.universe._
    materializeDynamicMappingImpl[A, B](c)(c.Expr(reify(DynamicMapping.empty).tree))
  }

  def materializeDynamicMappingImpl[A: c.WeakTypeTag, B: c.WeakTypeTag](c: Context)(dynamicMapping: c.Expr[A => DynamicMapping]): c.Expr[Mapping[A, B]] = {
    import c.universe._

    val sourceType = weakTypeOf[A]
    val targetType = weakTypeOf[B]
    val targetCompanion = targetType.typeSymbol.companion
    
    val dynamicParams = dynamicMapping.tree.collect {
      case arg @ Apply(TypeApply(Select(Select(Ident(scala), tuple2), TermName("apply")), List(TypeTree(), TypeTree())), List(Literal(Constant(key: String)), impl)) => arg
    }

    def getFirstTypeParam(tpe: Type) = { val TypeRef(_, _, tps) = tpe; tps.head }

    def isOptionSymbol(typeSymbol: Symbol) = typeSymbol == typeOf[Option[_]].typeSymbol
    def isCaseClassSymbol(typeSymbol: Symbol) = typeSymbol.isClass && typeSymbol.asClass.isCaseClass
    def isIterableType(tpe: Type): Boolean = tpe.baseClasses.contains(typeOf[Iterable[_]].typeSymbol) && !isMapType(tpe)
    def isMapType(tpe: Type): Boolean = tpe.baseClasses.contains(typeOf[Map[_, _]].typeSymbol)

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
      lazy val isIterable = isIterableType(tpe)
      lazy val isIterableCaseClass = isIterable && isCaseClassSymbol(getFirstTypeParam(tpe).typeSymbol)
      lazy val isMap = isMapType(tpe)
      lazy val companion = typeSymbol.companion
      lazy val typeParamCompanion =
        if (isOptional || isIterable) getFirstTypeParam(tpe).typeSymbol.companion
        else throw new NoSuchElementException(s"$key is of type $tpe and does not have a type parameter")
    }

    def extractParams(sourceType: Type, targetType: Type, parentFields: List[FieldInfo]): List[Tree] = {

      val sourceFields = getFields(sourceType)
      val targetFields = getFields(targetType)

      targetFields.map { targetField =>

        val sourceFieldOption = sourceFields.find(_.key == targetField.key)

        if (sourceFieldOption.isDefined) {

          val sourceField = sourceFieldOption.get

          val fieldSelector = (parentFields ++ List(sourceField)).foldLeft(Ident(TermName("a")): Tree) {
            case (tree, field) => Select(tree, field.termName)
          }

          val value = {
            if (sourceField.tpe != targetField.tpe && (sourceField.isCaseClass || sourceField.isOptionalCaseClass || sourceField.isIterableCaseClass)) {

              if (sourceField.isOptionalCaseClass || sourceField.isIterableCaseClass) {
                val params = extractParams(getFirstTypeParam(sourceField.tpe), getFirstTypeParam(targetField.tpe), List.empty)
                val value = q"${targetField.typeParamCompanion}(..$params)"

                val lambda = Apply(Select(fieldSelector, TermName("map")),
                  List(Function(List(ValDef(Modifiers(Flag.PARAM), TermName("a"), TypeTree(), EmptyTree)), value)))

                q"$lambda"
              } else {
                val params = extractParams(sourceField.tpe, targetField.tpe, parentFields :+ sourceField)
                q"${targetField.companion}(..$params)"
              }

            } else fieldSelector
          }

          q"${targetField.termName} = $value"
        } else {

          val targetFieldLiteral = Literal(Constant(targetField.key))
          val dynamicField = dynamicParams.find(term => term.children(1).equalsStructure(targetFieldLiteral))

          if (dynamicField.isDefined) AssignOrNamedArg(Ident(targetField.termName), q"dynamicMapping.${targetField.termName}")
          else if (targetField.isOptional) AssignOrNamedArg(Ident(targetField.termName), q"None")
          else if (targetField.isIterable) AssignOrNamedArg(Ident(targetField.termName), q"${targetField.companion}.empty")
          else throw new NoSuchElementException(s"${targetField.key} is not a member of ${sourceType}")
        }
      }
    }

    val params = extractParams(sourceType, targetType, List.empty)
    
    def generateMappingCode() = 
       if (dynamicParams.length > 0) q"val dynamicMapping = $dynamicMapping(a); $targetCompanion(..$params)"
      else q"$targetCompanion(..$params)"

    def generateCode() =
        q"""
          import com.bfil.automapper.Mapping
          new Mapping[$sourceType, $targetType] {
            def map(a: $sourceType): $targetType = {
              ${generateMappingCode()}
            }
          }
        """

    c.Expr[Mapping[A, B]](generateCode())
  }
}