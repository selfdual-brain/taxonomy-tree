package com.selfdualbrain.timeouttest.imperative.impl

import java.util.Locale

import com.selfdualbrain.timeouttest.common.{UnknownTag, Treepath, StringWithLocale, NodeNotFound}
import com.selfdualbrain.timeouttest.imperative.api.{MutableTaxonomyWithTagging, Tag, Category}

import scala.collection.mutable

/**
  * The java-style (=imperative) implementation of taxonomy.
  * It is based on mutable collections.
  * Tags and node names are indexed using hash maps, so search by node name and search by tag name operates in asymptotically O(1).
  * <p>
  * Overall, this implementation:<ul>
  *   <li>has excellent reads and writs performance</li>
  *   <li>is pretty economical with memory usage (mutable collections, some lazy initializations)</li>
  *   <li>is not thread safe</li>
  * </ul>
  */
class MutableTaxonomyImpl extends MutableTaxonomyWithTagging {
  private var rootHandle: Option[Node] = None
  private val tag2nodes = new mutable.HashMap[String, mutable.Set[Node]] with mutable.MultiMap[String, Node]
  private val name2nodes = new mutable.HashMap[String, mutable.Set[Node]] with mutable.MultiMap[String, Node]
  private val translation2tag = new mutable.HashMap[StringWithLocale, TagImpl]
  private[impl] val registeredTags = new mutable.HashMap[String, TagImpl]

  override def root: Option[Category] = rootHandle

  override def findNodesByName(name: String): Iterable[Node] =
    name2nodes.get(name) match {
      case None => Set.empty
      case Some(set) => set
    }

  override def findNodesByTagTranslation(name: StringWithLocale): Iterable[Node] =
    translation2tag.get(name) match {
      case None => Set.empty
      case Some(tag) => this.findNodesByTagName(tag.name)
    }

  override def addNode(path: Treepath, tags: Iterable[String]): Node = {
    if (rootHandle.isEmpty) {
      rootHandle = Some(new Node(name = path.firstSegment, parent = None, owner = this))
      this.addToNodeIndex(rootHandle.get)
    }

    if (path.firstSegment != rootHandle.get.name)
      throw new RuntimeException(s"Trying to add new root node to taxonomy: already existing root is ${rootHandle.get.name}, new root is ${path.firstSegment}")

    val newNode = rootHandle.get.addSubnode(path.tail, tags)
    return newNode
  }

  override def findNodesByTagName(name: String): Iterable[Node] =
    tag2nodes.get(name) match {
      case None => Set.empty
      case Some(set) => set
    }

  override def findNodeByPath(path: Treepath): Option[Node] = {
    if (rootHandle.isEmpty)
      None
    else if (path.isEmpty)
      None
    else if (path.firstSegment != rootHandle.get.name)
      None
    else if (path.length == 1)
      rootHandle
    else
      rootHandle.get.findDescendant(path.tail)
    }

  override def registerTag(tagName: String): Unit =
    if (! registeredTags.contains(tagName))
      registeredTags.put(tagName, new TagImpl(tagName))

  override def unregisterTag(tag: String): Unit = {
    if (registeredTags.contains(tag)) {
      tag2nodes.get(tag) match {
        case None =>
        case Some(nodeCollection) => for (node <- nodeCollection) node.removeTag(tag)
      }
      tag2nodes.remove(tag)
      registeredTags.remove(tag)
      translation2tag.retain((k,v) => v.name != tag)
    }
  }

  override def allRegisteredTags: Iterable[Tag] = registeredTags.values

  override def allUsedTags: Iterable[Tag] = tag2nodes.keys map registeredTags

  override def findTag(name: String): Option[TagImpl] = registeredTags.get(name)

  override def listDescendants(path: Treepath): Iterable[Node] =
    this.findNodeByPath(path) match {
      case None => Set.empty
      case Some(node) => node.listDescendants
    }

  override def removeSubtree(path: Treepath): Unit = {
    //finding subtree root
    val subtreeRoot = this.findNodeByPath(path)
    if (subtreeRoot.isDefined) {
      //detaching subtree root from parent
      if (subtreeRoot.get.isRoot)
        rootHandle = None
      else
        subtreeRoot.get.parent.get.detachChild(subtreeRoot.get.name)

      //updating all indexes
      for (node <- subtreeRoot.get.listDescendants) {
        name2nodes.removeBinding(node.name, node)
        for (tag <- node.tags)
          tag2nodes.removeBinding(tag.name, node)
      }
    }
  }

  override def tag(tag: String, path: Treepath): Unit =
    this.findNodeByPath(path) match {
      case None => throw new NodeNotFound(path)
      case Some(node) =>
        this.registerTag(tag)
        node.addTag(tag)
        tag2nodes.addBinding(tag, node)
    }

  override def untag(tag: String, path: Treepath): Unit =
    this.findNodeByPath(path) match {
      case None => throw new NodeNotFound(path)
      case Some(node) =>
        node.removeTag(tag)
        tag2nodes.removeBinding(tag, node)
    }

  override def addTagTranslation(tagName: String, translation: StringWithLocale): Unit =
    this.findTag(tagName) match {
      case None => throw new UnknownTag(tagName)
      case Some(tag) =>
        tag.addTranslation(translation.locale, translation.text)
        translation2tag += translation -> tag
    }

  override def removeTagTranslation(tagName: String, locale: Locale): Unit =
    this.findTag(tagName) match {
      case None => throw new UnknownTag(tagName)
      case Some(tag) =>
        tag.findTranslationForLanguage(locale) match {
          case None => //we are removing non-existing translation so doing nothing
          case Some(translatedText) =>
            tag.removeTranslation(locale)
            translation2tag.remove(StringWithLocale(translatedText, locale))
        }
    }

  private[impl] def addToNodeIndex(node: Node): Unit = {
    name2nodes.addBinding(node.name, node)
  }

  private[impl] def removeFromNodeIndex(node: Node): Unit = {
    name2nodes.removeBinding(node.name, node)
  }

  private[impl] def ensureTagsAreRegistered(tags: Iterable[String]): Unit = {
    for (tagName <- tags)
      if (! registeredTags.contains(tagName))
        registeredTags += tagName -> new TagImpl(tagName)
  }

}
