package com.selfdualbrain.timeouttest.functional.impl

import java.util.Locale

import com.selfdualbrain.timeouttest.common.{StringWithLocale, Treepath, UnknownTag}
import com.selfdualbrain.timeouttest.functional.api.{Category, ImmutableTaxonomyWithTagging}

/**
  * This is Haskell-style (purely functional) implementation of taxonomy.
  *
  * For keeping this implementation simplistic we do not use indexing, so searches "find all nodes with a given name"
  * and "find all nodes with a given tag" are implemented by scanning the whole tree.
  */
case class ImmutableTaxonomyImpl (
  root: Node,
  translation2tag: Map[StringWithLocale,TagImpl],
  registeredTags: Map[String,TagImpl])
  extends ImmutableTaxonomyWithTagging {

  override def findNodesByName(name: String): Iterable[Node] = root.findSubnodesByName(name)

  override def findNodeByPath(path: Treepath): Option[Category] =
    if (path.isEmpty)
      None
    else if (path.firstSegment != root.name)
      None
    else if (path.length == 1)
      Some(root)
    else
      root.findDescendant(path.tail)

  override def findNodesByTagName(name: String): Iterable[Node] = root.findSubnodesByTagName(name)

  override def findNodesByTagTranslation(name: StringWithLocale): Iterable[Category] =
    translation2tag.get(name) match {
      case None => Set.empty
      case Some(tag) => this.findNodesByTagName(tag.name)
    }

  override def listDescendants(path: Treepath): Iterable[Category] =
    this.findNodeByPath(path) match {
      case None => Set.empty
      case Some(node) => node.listDescendants
    }

  override def allRegisteredTags: Iterable[TagImpl] = registeredTags.values

  override def allUsedTags: Iterable[TagImpl] = root.listAllTagNamesUsedInSubtree map registeredTags

  override def findTag(name: String): Option[TagImpl] = registeredTags.get(name)

  override def tag(tag: String, targetNode: Treepath): ImmutableTaxonomyImpl =
    this.addNode(targetNode, Seq(tag)) //we just re-use a corner case of adding a node: if the target node already existed, adding a node is just updating tags

  override def untag(tag: String, path: Treepath): ImmutableTaxonomyImpl = {
    if (path.isEmpty)
      throw new Exception("invoked untag() with empty target node path - this must be a bug in client code")

    ImmutableTaxonomyImpl(root.withTagRemoved(path.tail, tag), translation2tag, registeredTags)
  }

  override def addNode(path: Treepath, tags: Seq[String]): ImmutableTaxonomyImpl = {
    if (path.firstSegment != root.name)
      throw new RuntimeException(s"Cannot add node under $path because in this taxonomy root name is ${root.name}")

    val newTagNames = tags.filterNot(tagName => registeredTags.contains(tagName))
    val newTagsRegistryElements = newTagNames map (tagName => tagName -> TagImpl(tagName, Map.empty))
    ImmutableTaxonomyImpl(root.withAddedNode(path.tail, tags), translation2tag, registeredTags ++ newTagsRegistryElements)
  }

  override def removeSubtree(path: Treepath): ImmutableTaxonomyWithTagging = {
    if (path.isEmpty)
      throw new Exception("empty path passed to removeSubtree - this must be a bug in client code")

    if (path.firstSegment != root.name)
      this //first segment shows root name different than the actual root name which means we are removing non-existing subtree, so we are removing nothing
    else
      ImmutableTaxonomyImpl(root.withRemovedSubtree(path.tail), translation2tag, registeredTags)
  }

  override def registerTag(tagName: String): ImmutableTaxonomyImpl = {
    val newTag = TagImpl(tagName, Map.empty)
    ImmutableTaxonomyImpl(root, translation2tag, registeredTags + (tagName -> newTag))
  }

  override def unregisterTag(tagName: String): ImmutableTaxonomyImpl =
    ImmutableTaxonomyImpl(root.withDeletedAllUsesOfTag(tagName), translation2tag, registeredTags - tagName)

  override def addTagTranslation(tagName: String, translation: StringWithLocale): ImmutableTaxonomyImpl =
    this.findTag(tagName) match {
      case None => throw new UnknownTag(tagName)
      case Some(tag) =>
        ImmutableTaxonomyImpl(root, translation2tag + (translation -> tag), registeredTags + (tagName -> tag.withUpdatedTranslation(translation)))
    }

  override def removeTagTranslation(tagName: String, locale: Locale): ImmutableTaxonomyImpl =
    this.findTag(tagName) match {
      case None => throw new UnknownTag(tagName)
      case Some(tag) =>
        tag.findTranslationForLanguage(locale) match {
          case None => this //we are removing non-existing translation so doing nothing
          case Some(translatedText) =>
            val updatedTagDefinition = tag.withRemovedTranslation(locale)
            val updatedTranslationsMap = translation2tag - StringWithLocale(translatedText, locale)
            val updatedTagsRegistry = registeredTags + (tagName -> updatedTagDefinition)
            ImmutableTaxonomyImpl(root, updatedTranslationsMap, updatedTagsRegistry)
        }
    }

}

object ImmutableTaxonomyImpl {
  def apply(rootName: String): ImmutableTaxonomyImpl = {
    val root = Node(rootName, Map.empty, Set.empty)
    new ImmutableTaxonomyImpl(root, Map.empty, Map.empty)
  }
}
