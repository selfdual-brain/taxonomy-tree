package com.selfdualbrain.timeouttest.functional.impl

import com.selfdualbrain.timeouttest.common.Treepath
import com.selfdualbrain.timeouttest.functional.api.Category

case class Node(name: String, childNodesMap: Map[String, Node], attachedTags: Set[String]) extends Category {

  override def isLeaf: Boolean = childNodesMap.isEmpty

  override def children: Iterable[Category] = childNodesMap.values

  override def findDescendant(path: Treepath): Option[Node] = {
    if (path.isEmpty)
      Some(this)
    else {
      this.findChild(path.firstSegment) flatMap {directChild =>
        path.length match {
          case 1 => Some(directChild)
          case _ => directChild.findDescendant(path.tail)
        }
      }
    }
  }

  override def findChild(name: String): Option[Node] = childNodesMap.get(name)

  override def listDescendants: Iterable[Node] = traverseSubtreeAndBuildIterableCollection {node => Iterable(node)}

  def findSubnodesByName(name: String): Iterable[Node] =
    traverseSubtreeAndBuildIterableCollection {
      node => if (node.name == name) Iterable(node) else Iterable.empty
    }

  def findSubnodesByTagName(tagName: String): Iterable[Node] =
    traverseSubtreeAndBuildIterableCollection {
      node => if (node.attachedTags.contains(tagName)) Iterable(node) else Iterable.empty
    }

  /**
    * Creates a copy of this node (i.e. a copy of the subtree rooted in this node) with a specified path added
    * and ensuring that on the end of this path all tag names provided will be attached.
    *
    * Caution: for path of length 1 this works as adding a direct child.
    */
  def withAddedNode(path: Treepath, tags: Seq[String]): Node = {
    if (path.isEmpty)
      Node(name, childNodesMap, attachedTags ++ tags)
    else {
      val childName = path.firstSegment
      val newChild = this.findChild(childName) match {
        case None => Node(childName, Map.empty, Set.empty)
        case Some(existingChild) => existingChild
      }
      Node(this.name, this.childNodesMap + (childName -> newChild.withAddedNode(path.tail, tags)), this.attachedTags)
    }
  }

  def withTagRemoved(path: Treepath, tag: String): Node = {
    if (path.isEmpty)
      Node(name, childNodesMap, attachedTags - tag)
    else {
      val childName = path.firstSegment
      childNodesMap.get(childName) match {
        case None => this
        case Some(directChild) => Node(name, childNodesMap + (childName -> directChild.withTagRemoved(path.tail, tag)), attachedTags)
      }
    }
  }

  /**
    * Creates a copy of this node (i.e. a copy of the subtree rooted in this node) with
    * a specified subtree deleted.
    * The subtree is specified by providing the path to its root node.
    * If the path leads no nowhere (there is no such node), this operation completes returning a copy of original tree.
    *
    * Caution: for path of length 1 this works as removing a direct child.
    * Caution: this operation makes no clear sense if the path is empty, so in such cace we blow with expection.
    */
  def withRemovedSubtree(pathToSubtreeRoot: Treepath): Node = {
    if (pathToSubtreeRoot.isEmpty)
      throw new RuntimeException("invoked removeSubtree() with empty path to subtree root - this must be a bug in client code")

    val childName = pathToSubtreeRoot.firstSegment
    pathToSubtreeRoot.length match {
      case 1 => Node(name, childNodesMap - childName, attachedTags)
      case _ =>
        childNodesMap.get(childName) match {
          case None => this
          case Some(child) => Node(name, childNodesMap + (childName -> child.withRemovedSubtree(pathToSubtreeRoot.tail)), attachedTags)
        }
    }
  }

  def withDeletedAllUsesOfTag(tagName: String): Node =
    Node(name, childNodesMap collect {case (childName, childNode) => childName -> childNode.withDeletedAllUsesOfTag(tagName)}, attachedTags - tagName)

  def listAllTagNamesUsedInSubtree: Set[String] = {
    val iterable = traverseSubtreeAndBuildIterableCollection {
      node => node.attachedTags
    }
    iterable.toSet
  }

  def traverseSubtreeAndBuildIterableCollection[T](f: Node => Iterable[T]): Iterable[T] = {
    if (childNodesMap.isEmpty)
      f(this)
    else {
      val collectionsForChildren = childNodesMap.values map (_.traverseSubtreeAndBuildIterableCollection(f))
      f(this) ++ collectionsForChildren.reduceLeft(_ ++ _)
    }
  }

  override def toString: String = s"Node(name=$name,tags=${attachedTags})"

}
