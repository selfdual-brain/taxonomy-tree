package com.selfdualbrain.timeouttest

import java.io.File
import java.util.Locale

import com.selfdualbrain.timeouttest.common.{StringWithLocale, Treepath}
import com.selfdualbrain.timeouttest.imperative.impl.{CsvSerializer, MutableTaxonomyImpl}

object MutablePlayground {

  def main(args: Array[String]): Unit = {
    val taxonomy = new MutableTaxonomyImpl
    taxonomy.addNode("A", "B")
    taxonomy.addNode("A", "C")
    taxonomy.addNode("A", "A", "A")
    taxonomy.addNode("A", "A", "D", "E")

    taxonomy.tag("alfa", Treepath("A"))
    println(taxonomy.findNodesByTagName("alfa").toList.map(_.name).toList.sorted)

    taxonomy.tag("alfa", Treepath("A", "A", "A"))
    println(taxonomy.findNodesByTagName("alfa").toList.map(_.name).toList.sorted)

    taxonomy.tag("alfa", Treepath("A", "A", "D"))
    println(taxonomy.findNodesByTagName("alfa").toList.map(_.name).toList.sorted)

    taxonomy.tag("beta", Treepath("A", "A", "D"))
    println(taxonomy.findNodesByTagName("alfa").toList.map(_.name).toList.sorted)

    taxonomy.addTagTranslation("alfa", StringWithLocale("alfa_uk", Locale.UK))
    taxonomy.addTagTranslation("alfa", StringWithLocale("alfa_chinese", Locale.SIMPLIFIED_CHINESE))
    taxonomy.addTagTranslation("beta", StringWithLocale("beta_chinese", Locale.SIMPLIFIED_CHINESE))

    val serializer = new CsvSerializer
    val baseDir = new File("/home/wojtek/tmp/timeout")
    val treeFile = new File(baseDir, "taxonomy-tree.txt")
    val tagsFile = new File(baseDir, "tags-registry.txt")
    serializer.writeToCsv(taxonomy, treeFile, tagsFile)

    val parsedTaxonomy = serializer.parseFromCsv(treeFile, tagsFile)
    println("after parsing", parsedTaxonomy.root.get.listDescendants)

    //    println("finding nodes by name (A):", taxonomy.findNodesByName("A"))
//    println("finding all descendants of root:", taxonomy.listDescendants("A").map(_.name))
//    println(taxonomy.findNodesByTagName("alfa").map(_.name).toList.sorted)
  }

}
