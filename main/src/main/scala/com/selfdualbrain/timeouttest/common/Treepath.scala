package com.selfdualbrain.timeouttest.common

/**
  * Representation of path in a taxonomy tree.
  * Generally speaking this is a list of strings (each element is just a node name), with ordering root->leaf.
  * We use a light encapsulation (value class) to just keep things elegant and have the opportunity
  * to replace the representation in the future.
  */
case class Treepath(segments: List[String]) extends AnyVal {
  def length: Int = segments.length
  def firstSegment: String = segments.head
  def lastSegment: String = segments.last
  def tail = Treepath(segments.tail)
  def isEmpty: Boolean = segments.isEmpty
  def nonEmpty: Boolean = segments.nonEmpty
}

object Treepath {
  def apply(segments:String*): Treepath = new Treepath(segments.toList)
  val empty = Treepath(List.empty)
}
