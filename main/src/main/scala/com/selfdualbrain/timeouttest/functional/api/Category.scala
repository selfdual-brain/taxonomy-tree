package com.selfdualbrain.timeouttest.functional.api

import com.selfdualbrain.timeouttest.common.Treepath

/**
  * Representa a node in a taxonomy tree.
  */
trait Category {

  /**
    * Name of this node. This name can never be changed.
    * Within one taxonomy tree the name may be reused, however it will always be unique
    * withing the colletion of direct childred od any node.
    */
  def name: String

  /**
    * Collection of child nodes.
    */
  def children: Iterable[Category]

  /**
    * Direct child node lookup.
    *
    * @param name child node name
    * @return child node if found, None otherwise
    */
  def findChild(name: String): Option[Category]

  /**
    * Descendant lookup.
    * For one-element path it works like child lookup.
    *
    * @param path tree path
    * @return descendant node if found, None otherwise
    */
  def findDescendant(path: Treepath): Option[Category]

  /**
    * Performs a subtree traversal starting from the this node and returns
    * the collection of all (recursively) subnodes below the specified node (including this node itself).
    */
  def listDescendants: Iterable[Category]

  /**
    * Returns true if the collecion of child nodes is empty.
    */
  def isLeaf: Boolean

}
