package com.bfil.automapper

import java.util.Date

import org.specs2.mutable.Specification

class AutoMappingSpec extends Specification with AutoMapping with TestData {
  
  "mapTo" should {

    "map a case class to another case class as expected" in {
      
      test.mapTo[AnotherTest] === AnotherTest(anotherData, "whatever")

    }
    
    "map a case class with missing optionals to another case class as expected" in {

      testWithNoOptionals.mapTo[AnotherTest] === AnotherTest(anotherEmptyData, "whatever")

    }
    
    "map a case class to another case class with a subset of fields" in {
      
      test.mapTo[SubsetOfTest] === SubsetOfTest(data)

    }
    
    "map a case class to another case class by setting None for fields not present in the first class" in {
      
      test.mapTo[CanBeMappedWithNoneValues] === CanBeMappedWithNoneValues(data, None)
      
    }
    
//    "not compile if mapping cannot be generated" in {
//      
//      test.mapTo[CannotBeMapped]
//      
//    }
    
    "map a case class to another case class as expected using the singleton object" in {
      
      implicit val mapping = AutoMapping.generate[Test, AnotherTest]
      
      AutoMapping.map(test) === AnotherTest(anotherData, "whatever")

    }
    
    "map a case class to another case class as expected using the singleton object and be able to disambiguate mappings" in {
      
      implicit val mapping = AutoMapping.generate[Test, AnotherTest]
      implicit val mapping2 = AutoMapping.generate[Test, SubsetOfTest]
      
      AutoMapping.map[Test, AnotherTest](test) === AnotherTest(anotherData, "whatever")

    }

  }

}

trait TestData {
  
  case class Inner(what: String)
  case class Nested(date: Option[Date], inner: Option[Inner])
  case class Data(count: Int, nested: Option[Nested], optional: Option[String])
  case class Test(field: String, data: Data)
  
  case class AnotherNested(date: Option[Date], inner: Option[Inner])
  case class AnotherData(count: Int, nested: Option[AnotherNested], optional: Option[String])
  case class AnotherTest(data: AnotherData, field: String)
  
  case class SubsetOfTest(data: Data)
  case class CannotBeMapped(data: Data, unexpectedField: Exception)
  case class CanBeMappedWithNoneValues(data: Data, unexpectedField: Option[Exception])
  
  val currentDate = Some(new Date())

  val data = Data(10, Some(Nested(currentDate, Some(Inner("what")))), Some("string"))
  val anotherData = AnotherData(10, Some(AnotherNested(currentDate, Some(Inner("what")))), Some("string"))
  val emptyData = Data(10, None, None)
  val anotherEmptyData = AnotherData(10, None, None)
      
  val test = Test("whatever", data)
  val testWithNoOptionals = Test("whatever", emptyData)
  
}