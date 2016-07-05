package com.selfdualbrain.timeouttest.imperative.impl

import com.selfdualbrain.timeouttest.common.Treepath
import com.selfdualbrain.timeouttest.imperative.api.{Tag, Category}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Implementation of SimpleTaxonomy tree nodes.
  * Implemenation note: each node keeps a reference to its taxonnomy ('owner' field).
  */
private[impl] class Node(val name: String, val parent: Option[Node], owner: MutableTaxonomyImpl) extends Category {
  private var childNodesMap: Option[mutable.Map[String, Node]] = None //we avoid having this map allocated for leaves of the tree (=memory optimization)
  private var attachedTags: Option[mutable.Set[String]] = None //we avoid having this set allocated when there is no tags attached (=memory optimization)

  override def children: Iterable[Node] =
    childNodesMap match {
      case None => Set.empty
      case Some(map) => map.values
    }

  override def isLeaf: Boolean = children.isEmpty

  override def isRoot: Boolean = parent.isEmpty

  override def path: Treepath = Treepath(this.reversePath.reverse) //we carefully avoid using slow "append" on lists, preferring head-tail operations only

  override def tags: Iterable[Tag] =
    attachedTags match {
      case None => Set.empty[Tag]
      case Some(set) => set map owner.registeredTags
    }

  override def findChild(name: String): Option[Node] = childNodesMap flatMap (_.get(name))

  override def findDescendant(path: Treepath): Option[Node] = {
    if (path.isEmpty)
      Some(this)
    else
      this.findChild(path.firstSegment) match {
        case None => None
        case Some(child) => child.findDescendant(path.tail)
      }
  }

  def collectDescentants(buffer: ArrayBuffer[Node]): Unit = {
    //less beautiful than "pure" functional solution but with much better performance
    buffer += this
    for (node <- this.children)
      node.collectDescentants(buffer)
  }

  override def listDescendants: Iterable[Node] = {
    val buf = new ArrayBuffer[Node]
    this.collectDescentants(buf)
    return buf
  }

  def addTags(tags: Iterable[String]): Unit = {
    owner.ensureTagsAreRegistered(tags)
    for (tagName <- tags)
      this.addTag(tagName)
  }

  def addTag(tag: String): Unit = {
    if (attachedTags.isEmpty)
      attachedTags = Some(new mutable.HashSet[String])
    attachedTags.get += tag
  }

  def removeTag(tag: String): Unit = {
    if (attachedTags.isDefined)
      attachedTags.get -= tag
  }

  def addSubnode(path: Treepath, addedTagNames: Iterable[String]): Node = {
    path.length match {
      case 0 =>
        this.addTags(addedTagNames)
        this
      case _ =>
        if (childNodesMap.isEmpty)
          childNodesMap = Some(new mutable.HashMap[String, Node])
        val directChild = childNodesMap.get.get(path.firstSegment) match {
          case None =>
            val newNode = new Node(path.firstSegment, Some(this), owner)
            owner.addToNodeIndex(newNode)
            childNodesMap.get += (path.firstSegment -> newNode)
            newNode
          case Some(node) => node
        }
        directChild.addSubnode(path.tail, addedTagNames)
    }
  }

  def addOrUpdateChild(name: String, tagNames: Iterable[String]): Node = this.addSubnode(Treepath(name), tagNames)

  def detachChild(name: String): Unit =
    childNodesMap match {
      case None => //do nothing
      case Some(map) => map -= name
    }

  def reversePath: List[String] =
    parent match {
      case None => List(name)
      case Some(node) => node.name :: node.reversePath
    }

  override def toString: String = s"Node(name=$name,parent=${parent.map(_.name)})"
}
