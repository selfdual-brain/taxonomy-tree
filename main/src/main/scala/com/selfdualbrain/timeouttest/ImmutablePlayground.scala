package com.selfdualbrain.timeouttest

import com.selfdualbrain.timeouttest.common.Treepath
import com.selfdualbrain.timeouttest.functional.api.ImmutableTaxonomyWithTagging
import com.selfdualbrain.timeouttest.functional.impl.ImmutableTaxonomyImpl

object ImmutablePlayground {

  def main(args: Array[String]): Unit = {
    var taxonomy: ImmutableTaxonomyWithTagging = ImmutableTaxonomyImpl("A")
    taxonomy = taxonomy.addNode("A", "B")
    taxonomy = taxonomy.addNode("A", "C")
    taxonomy = taxonomy.addNode("A", "A", "A")
    taxonomy = taxonomy.addNode("A", "A", "D", "E")
//    println("finding all descendants of root:", taxonomy.listDescendants("A").map(_.name))

    taxonomy = taxonomy.tag("alfa", Treepath("A"))
//    println(taxonomy.findNodesByTagName("alfa").toList.map(_.name).toList.sorted)

    taxonomy = taxonomy.tag("alfa", Treepath("A", "A", "A"))
//    println(taxonomy.findNodesByTagName("alfa").toList.map(_.name).toList.sorted)

    taxonomy = taxonomy.tag("alfa", Treepath("A", "A", "D"))
//    println(taxonomy.findNodesByTagName("alfa").toList.map(_.name).toList.sorted)

    taxonomy = taxonomy.tag("beta", Treepath("A", "A", "D"))
//    println(taxonomy.findNodesByTagName("alfa").toList.map(_.name).toList.sorted)

//    println("finding nodes by name (A):", taxonomy.findNodesByName("A"))
//    println("finding all descendants of root:", taxonomy.listDescendants("A").map(_.name))
//    println(taxonomy.findNodesByTagName("alfa").map(_.name).toList.sorted)
//
//    println("find node by path", taxonomy.findNodeByPath(Treepath.empty))

    println("initial tree", taxonomy.listDescendants("A"))
//    taxonomy = taxonomy.removeSubtree("A", "C")
//    println("tree after removing A->C", taxonomy.listDescendants("A"))
    taxonomy = taxonomy.removeSubtree("A", "A", "A")
    println("tree after removing A->A->A", taxonomy.listDescendants("A"))
  }


}
