package com.selfdualbrain.timeouttest.imperative.api

import com.selfdualbrain.timeouttest.common.Treepath

/**
  * Represents a node in a taxonomy tree.
  */
trait Category {

  /**
    * Name of this node. This name can never be changed.
    * Within one taxonomy tree the name may be reused, however it will always be unique
    * withing the colletion of direct childred od any node.
    */
  def name: String

  /**
    * Sequence of node names in taxonomy tree, starting from root and ending in this node.
    * Considered an "address" of this node within the tree.
    *
    * @return
    */
  def path: Treepath

  /**
    * Collection of tags attached to this node.
    */
  def tags: Iterable[Tag]

  /**
    * Parent node. Returns None for root node.
    */
  def parent: Option[Category]

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
    * For empty path "this" node is returned.
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

  /**
    * Returns true if this is root node.
    * Only one root node can exist in given taxonomy instance.
    */
  def isRoot: Boolean

  /**
    * If this node has child with given name, updates its tags.
    * Otherwise - creates new child node.
    *
    * @param name name of new node
    * @param tagNames tag names to be attached to new node (or to be appended to existing node)
    * @return new node (or the node updated)
    */
  def addOrUpdateChild(name: String, tagNames: Iterable[String]): Category
}
