package com.selfdualbrain.timeouttest.imperative.impl

import java.io.{PrintWriter, File}
import java.util.Locale

import com.selfdualbrain.timeouttest.common.{Treepath, StringWithLocale}
import com.selfdualbrain.timeouttest.imperative.api.{Category, MutableTaxonomyWithTagging}
import scala.collection.mutable
import scala.io.Source

/**
  * This is taxonomy serializer.
  * Each taxonomy is serialized as 2 CSV files: one file contatins the tree, the other file contains tags registry.
  *
  * Lines in taxonomy tree file have the following format:
  * <pre>
  *   node-id | parent-node-id | comma-separated-list-of-tags
  * </pre>
  * ...where node ids are transient - we create them "on the fly" during the serialization process.
  * Thanhs to these ids we have easy way to enocode the parent node referencee.
  * For root node instead of parent id, the word "rood" is placed.
  * <p>
  * Lines in tags registry file have the following format:
  * <pre>
  *   tag-name | comma-separated-list-of-translations
  * </pre>
  *
  * ... where each translation is a:
  *
  * <pre>
  *   language-tag : translated-text
  * </pre>
  *
  * Caution: the implementation of serializer is very simple (no syntax checking).
  * Taxonomy tree parser is assuming that every parent node is enlisted BEFORE child node.
  */
class CsvSerializer {

  def writeToCsv(taxonomy: MutableTaxonomyWithTagging, taxonomyFile: File, tagsRegistryFile: File): Unit = {
    exportToFile(taxonomyFile) {writer => exportTree(taxonomy, writer)}
    exportToFile(tagsRegistryFile) {writer => exportTagsRegistry(taxonomy, writer)}
  }

  def parseFromCsv(taxonomyFile: File, tagsRegistryFile: File): MutableTaxonomyWithTagging = {
    val result = new MutableTaxonomyImpl
    parseLinesOfTaxonomyTreeFile(result, Source.fromFile(taxonomyFile).getLines())
    parseLinesOfTagsRegistryFile(result, Source.fromFile(tagsRegistryFile).getLines())
    return result
  }

  private def exportTree(taxonomy: MutableTaxonomyWithTagging, out: PrintWriter): Unit = {
    if (taxonomy.root.isDefined) {
      val node2id = new mutable.HashMap[Category, Int]
      var lastNodeId: Int = 0
      for (node <- taxonomy.root.get.listDescendants) {
        lastNodeId += 1
        node2id += (node -> lastNodeId)
        out.print(lastNodeId)
        out.print("|")
        out.print(if (node.isRoot) "root" else node2id(node.parent.get))
        out.print("|")
        out.print(node.name)
        out.print("|")
        out.print(node.tags.map(_.name).mkString(","))
        out.println()
      }
    }
  }

  private def exportTagsRegistry(taxonomy: MutableTaxonomyWithTagging, out: PrintWriter): Unit = {
    val translationLauout: StringWithLocale => String = t => t.locale.toLanguageTag + ":" + t.text
    for (tag <- taxonomy.allRegisteredTags) {
      out.print(tag.name)
      out.print("|")
      out.print(tag.translations.map(translationLauout).mkString(","))
      out.println()
    }
  }

  private def exportToFile(file: File)(f: PrintWriter => Unit): Unit = {
    val writer = new PrintWriter(file)
    try {
      f(writer)
    } finally {
      writer.close()
    }
  }

  private def parseLinesOfTaxonomyTreeFile(taxonomy: MutableTaxonomyWithTagging, fileLines: Iterator[String]): Unit = {
    val id2Node = new mutable.HashMap[Int, Category]
    for (line <- fileLines) {
      val tokens = line.split("\\|")
      val nodeId: Int = tokens(0).toInt
      val parentId: Option[Int] = if (tokens(1) == "root") None else Some(tokens(1).toInt)
      val nodeName: String = tokens(2)
      val tags: Set[String] = if (tokens.length == 3) Set.empty else tokens(3).split(",").toSet

      val createdNode = parentId match {
        case None => taxonomy.addNode(Treepath(nodeName), tags)
        case Some(id) => id2Node(id).addOrUpdateChild(nodeName, tags)
      }
      id2Node += (nodeId -> createdNode)
    }
  }

  private def parseLinesOfTagsRegistryFile(taxonomy: MutableTaxonomyWithTagging, fileLines: Iterator[String]): Unit = {
    for (line <- fileLines) {
      val tokens = line.split("\\|")
      val tagName: String = tokens(0)
      val translations = tokens(1).split(",")
      for (translation <- translations) {
        val parts = translation.split(":")
        val languageCode = parts(0)
        val text = parts(1)
        taxonomy.registerTag(tagName)
        taxonomy.addTagTranslation(tagName, StringWithLocale(text, Locale.forLanguageTag(languageCode)))
      }
    }

  }


}
