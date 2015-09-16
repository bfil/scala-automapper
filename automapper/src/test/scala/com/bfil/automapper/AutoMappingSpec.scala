package com.bfil.automapper

import java.util.Date

import org.specs2.mutable.Specification

case class Nested(date: Date)
case class Data(count: Int, nested: Option[Nested], optional: Option[String])
case class Test(field: String, data: Data)
case class AnotherTest(data: Data, field: String)

class AutoMappingSpec extends Specification with AutoMapping {

  val currentDate = new Date()

  val map = Map(
    "field" -> "whatever",
    "data" -> Map(
      "count" -> 10,
      "nested" -> Map("date" -> currentDate),
      "optional" -> "string"))

  val mapWithNoOptionals = Map(
    "field" -> "whatever",
    "data" -> Map(
      "count" -> 10))

  val test = Test("whatever", Data(10, Some(Nested(currentDate)), Some("string")))
  val testWithNoOptionals = Test("whatever", Data(10, None, None))

  val anotherTest = AnotherTest(Data(10, Some(Nested(currentDate)), Some("string")), "whatever")
  val anotherTestWithNoOptionals = AnotherTest(Data(10, None, None), "whatever")

  private def optional[T](t: => T): Option[T] = scala.util.Try(t).toOption

  "Map to Case Class" should {

    "be mapped correctly" in {

      map.as[Test] === test

    }

    "be mapped correctly with missing optionals" in {

      mapWithNoOptionals.as[Test] === testWithNoOptionals

    }
    
    "throw a NoSuchElementException if a non optional field is missing" in {
      
      val mapWithMissingField = map - "field"

      mapWithMissingField.as[Test] should throwA[NoSuchElementException]

    }

  }

  "Case Class to Map" should {

    "be mapped correctly" in {

      test.asMap === map

    }

    "be mapped correctly with missing optionals" in {

      testWithNoOptionals.asMap === mapWithNoOptionals

    }

  }

  "Case Class to Case Class" should {

    "be mapped correctly with" in {
      
      test.mapTo[AnotherTest] === anotherTest

    }

    "be mapped correctly missing optionals" in {

      testWithNoOptionals.mapTo[AnotherTest] === anotherTestWithNoOptionals

    }
    
    "have reasonable performance" in {
      
      val n = 1000000
      
      val manualStart = System.currentTimeMillis
      (1 to n) foreach { i =>
        AnotherTest(test.data, test.field)
      }
      val manualElapsed = System.currentTimeMillis - manualStart
      println(s"Manual mapping: ${manualElapsed}ms")
      
    
      val autoStart = System.currentTimeMillis
      (1 to n) foreach { i =>
        test.mapTo[AnotherTest]
      }
      val autoElapsed = System.currentTimeMillis - autoStart
      println(s"Auto mapping: ${autoElapsed}ms")
      
      autoElapsed should beGreaterThan(manualElapsed)

    }

  }

}