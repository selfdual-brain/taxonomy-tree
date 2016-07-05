package com.selfdualbrain.timeouttest.functional

import java.util.Locale

import com.selfdualbrain.timeouttest.BaseSpec
import com.selfdualbrain.timeouttest.common.{StringWithLocale, Treepath}
import com.selfdualbrain.timeouttest.functional.api.ImmutableTaxonomyWithTagging
import com.selfdualbrain.timeouttest.functional.impl._

/**
  * Reference tree Alfa is:
  *
  * <pre>
  *     A
  *    /|\
  *   / | \
  *  B  C  A
  *       / \
  *      A   D
  *          |
  *          E
  * </pre>
  *
  *
  * Reference tree beta is:
  * <pre>
  *   a
  *   |
  *   b
  *   |
  *   c
  *   |
  *   d
  *   |
  *   e
  *   |
  *   f
  * </pre>
  */
class ImmutableTaxonomySpec extends BaseSpec {

  trait AlfaTaxonomySetup {
    var taxonomy: ImmutableTaxonomyWithTagging = ImmutableTaxonomyImpl("A")
    taxonomy = taxonomy.addNode("A", "B")
    taxonomy = taxonomy.addNode("A", "C")
    taxonomy = taxonomy.addNode("A", "A", "A")
    taxonomy = taxonomy.addNode("A", "A", "D", "E")
    taxonomy = taxonomy.tag("alfa", Treepath("A"))
    taxonomy = taxonomy.tag("alfa", Treepath("A", "A", "A"))
    taxonomy = taxonomy.tag("alfa", Treepath("A", "A", "D"))
    taxonomy = taxonomy.tag("beta", Treepath("A", "A", "D"))
  }

  trait BetaTaxonomySetup {
    var taxonomy: ImmutableTaxonomyWithTagging = ImmutableTaxonomyImpl("A")
    taxonomy = taxonomy.addNode("a", "b", "c", "d", "e", "f")
  }

  it must "correctly find nodes by name (alfa tree)" in new AlfaTaxonomySetup {
    taxonomy.root.name mustEqual "A"
    taxonomy.findNodesByName("A").size mustEqual 3
    taxonomy.findNodesByName("B").size mustEqual 1
    taxonomy.findNodesByName("E").size mustEqual 1
    taxonomy.findNodesByName("").size mustEqual 0
    taxonomy.findNodesByName("X").size mustEqual 0
  }

  it must "correctly find nodes by path (alfa tree)" in new AlfaTaxonomySetup {
    taxonomy.findNodeByPath(Treepath.empty).isDefined mustEqual false
    taxonomy.findNodeByPath("A").isDefined mustEqual true
    taxonomy.findNodeByPath("A", "C").isDefined mustEqual true
    taxonomy.findNodeByPath("A", "A", "D", "E").isDefined mustEqual true
    taxonomy.findNodeByPath("A", "A", "D", "E", "X").isDefined mustEqual false
    taxonomy.findNodeByPath("X").isDefined mustEqual false
  }

  it must "correctly find descendants (alfa tree)" in new AlfaTaxonomySetup {
    taxonomy.listDescendants(Treepath.empty).isEmpty mustEqual true
    taxonomy.listDescendants("X").isEmpty mustEqual true
    taxonomy.listDescendants("A").map(_.name).toList.sorted mustEqual List("A", "A", "A", "B", "C", "D", "E")
    taxonomy.listDescendants("A", "B").map(_.name).toList.sorted mustEqual List("B")
    taxonomy.listDescendants("A", "B", "C").map(_.name).toList mustEqual List.empty
    taxonomy.listDescendants("A", "A").map(_.name).toList.sorted mustEqual List("A", "A", "D", "E")
  }

  it must "auto-register tags" in new AlfaTaxonomySetup {
    taxonomy.allRegisteredTags.map(_.name).toList.sorted mustEqual List("alfa", "beta")
    taxonomy.allUsedTags.map(_.name).toList.sorted mustEqual List("alfa", "beta")
  }

  it must "find nodes by tags" in new AlfaTaxonomySetup {
    taxonomy.findNodesByTagName("alfa").toList.map(_.name).sorted mustEqual List("A", "A", "D")
    taxonomy.findNodesByTagName("beta").toList.map(_.name).sorted mustEqual List("D")
    taxonomy.findNodesByTagName("gamma").toList.map(_.name).sorted mustEqual List()
  }

  it must "correctly remove subtree (leaf node case)" in new AlfaTaxonomySetup {
    taxonomy = taxonomy.removeSubtree("A", "A", "A")
    taxonomy.listDescendants("A").map(_.name).toList.sorted mustEqual List("A", "A", "B", "C", "D", "E")
    taxonomy.allRegisteredTags.map(_.name).toList.sorted mustEqual List("alfa", "beta")
    taxonomy.allUsedTags.map(_.name).toList.sorted mustEqual List("alfa", "beta")
  }

  it must "correctly remove subtree (middle node case)" in new AlfaTaxonomySetup {
    taxonomy = taxonomy.removeSubtree("A", "A")
    taxonomy.listDescendants("A").map(_.name).toList.sorted mustEqual List("A", "B", "C")
    taxonomy.allRegisteredTags.map(_.name).toList.sorted mustEqual List("alfa", "beta")
    taxonomy.allUsedTags.map(_.name).toList.sorted mustEqual List("alfa")
  }

  it must "correctly remove subtree (missing node case)" in new AlfaTaxonomySetup {
    taxonomy = taxonomy.removeSubtree("A", "A", "X", "Y")
    taxonomy.listDescendants("A").map(_.name).toList.sorted mustEqual List("A", "A", "A", "B", "C", "D", "E")
    taxonomy.allRegisteredTags.map(_.name).toList.sorted mustEqual List("alfa", "beta")
    taxonomy.allUsedTags.map(_.name).toList.sorted mustEqual List("alfa", "beta")
  }

  it must "correctly untag nodes" in new AlfaTaxonomySetup {
    taxonomy = taxonomy.untag("alfa", Treepath("A"))
    taxonomy = taxonomy.untag("alfa", Treepath("A", "A", "D"))
    taxonomy.findNodesByTagName("alfa").map(_.name).toList mustEqual List("A")
  }

  it must "clear uses on tag unregistering" in new AlfaTaxonomySetup {
    taxonomy = taxonomy.unregisterTag("alfa")
    taxonomy.allRegisteredTags.map(_.name).toList.sorted mustEqual List("beta")
    taxonomy.allUsedTags.map(_.name).toList.sorted mustEqual List("beta")
    taxonomy.findNodesByTagName("alfa").size mustEqual 0
  }

  it must "register translations and find nodes using them" in new AlfaTaxonomySetup {
    taxonomy = taxonomy.addTagTranslation("alfa", StringWithLocale("alfa_uk", Locale.UK))
    taxonomy = taxonomy.addTagTranslation("alfa", StringWithLocale("alfa_chinese", Locale.SIMPLIFIED_CHINESE))
    taxonomy = taxonomy.addTagTranslation("beta", StringWithLocale("beta_chinese", Locale.SIMPLIFIED_CHINESE))
    taxonomy.findNodesByTagTranslation(StringWithLocale("alfa_uk", Locale.UK)).toList.map(_.name).sorted mustEqual List("A", "A", "D")
    taxonomy.findNodesByTagTranslation(StringWithLocale("alfa_uk", Locale.ITALIAN)).toList.map(_.name).sorted mustEqual List.empty
  }

  it must "correctly de-register translations" in new AlfaTaxonomySetup {
    taxonomy = taxonomy.addTagTranslation("alfa", StringWithLocale("alfa_uk", Locale.UK))
    taxonomy = taxonomy.removeTagTranslation("alfa", Locale.UK)
    taxonomy.findNodesByTagTranslation(StringWithLocale("alfa_uk", Locale.UK)).toList.map(_.name).sorted mustEqual List.empty
  }

}
