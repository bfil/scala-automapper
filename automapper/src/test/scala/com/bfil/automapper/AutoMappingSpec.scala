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
      "nested" -> Map("date" -> currentDate)))

  val test = Test("whatever", Data(10, Some(Nested(currentDate)), None))
  val anotherTest = AnotherTest(Data(10, Some(Nested(currentDate)), None), "whatever")

  "Case Class to Map" should {

    "be mapped correctly" in {

      map.as[Test] === test

    }

  }

  "Map to Case Class" should {

    "be mapped correctly" in {

      test.asMap === map

    }

  }

  "Case Class to Case Class" should {

    "be mapped correctly" in {

      test.mapTo[AnotherTest] === anotherTest

    }

  }

}