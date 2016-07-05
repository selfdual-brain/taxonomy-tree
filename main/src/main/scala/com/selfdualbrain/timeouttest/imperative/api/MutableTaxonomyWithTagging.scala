package com.selfdualbrain.timeouttest.imperative.api

import java.util.Locale

import com.selfdualbrain.timeouttest.common.{NodeNotFound, StringWithLocale, Treepath, UnknownTag}

/**
  * API for mutable taxonomies.
  *
  * A taxonomy is a data structure containing 3 "parts":<ol>
  *   <li>a tree - composed of Category nodes</li>
  *   <li>a collection of registered tags, where each tag has attached a set of "tag translations"</li>
  *   <li>mapping between categories and tags - specifically to any category a (possibly empty) subset
  *   of registered tags is attached</li>
  * </ol>
  *
  * <p>
  * Each Category node has a name. This name does not need to be unique across the tree.
  * Node can be identified by a sequence of node names starting from root node.
  * Such a sequence we represent as Treepath.
  * We sometimes say "address of the node" when talking about its path.
  * <p>
  * Each node can have any number of "tags' attached. A tag is just a string,
  * however transaltions of this string to different languages can also be registered.
  * Therefore a tag is a pair: (primary name, collection of translations).
  * For a given tag, its collection of translations may be empty.
  * <p>
  * We use internationalization support built into JDK (java.util.Locale) for representing languages.
  * <p>
  * Example below explains how we think about a taxonomy tree:
  *
  * <pre>
  * *** TREE ***
  * categories
  *   shows
  *     theatre
  *     films
  *       chinese [tags: chinese, asian]
  *       comedy
  *       action
  *   music
  *     jazz
  *     pop
  *     rock
  *   restaurants [tags: food, restaurant]
  *     chinese [tags: asian]
  *     french
  *     italian
  *   books
  *     scientific
  *     novels
  *     encyclopedia
  *
  * *** TAGS TRANSLATIONS ***
  * chinese: (en_GB: Chinese), (fr_FR: Chinois), (it_IT: Cinese)
  * food: (pl_PL: jedzenie), (es_ES: comida), (en_US: food)
  * asian: (en_GB: Asian), (pt_PT: asiatico)
  * restaurant:
  * </pre>
  *
  * Please observe in the above example that:<ul>
  *   <li>node named "chinese" happenes to exist under two different paths of the tree: categories/shows/films/chinese and categories/restaurants/chinese</li>
  *   <li>not all tags have translations</li>
  *   <li>within tags translations list of languages is by no means uniform</li>
  * </ul>
  */
trait MutableTaxonomyWithTagging {

  /**
    * Returns root node of this taxonomy tree.
    * Can be None (if the taxonomy tree is empty).
    */
  def root: Option[Category]

  /**
    * Finds all nodes with specified name.
    *
    * @param name name of the nodes we search for
    * @return a collection of nodes (possibly empty)
    */
  def findNodesByName(name: String): Iterable[Category]

  /**
    * Finds a node with spefied path.
    * It follows from the nature of the tree that at most one such node can exist.
    *
    * @param path address of the node
    * @return node at this path (if present)
    */
  def findNodeByPath(path: Treepath): Option[Category]

  def findNodeByPath(pathSegments:String*): Option[Category] = this.findNodeByPath(Treepath(pathSegments.toList))

  /**
    * Finds all nodes tagged with specified tag name.
    * Caution: this method completely ignores language translations for tags.
    *
    * @param name tag name
    * @return a collection of nodes (possibly empty)
    */
  def findNodesByTagName(name: String): Iterable[Category]

  /**
    * Finds all nodes tagged with specified language-aware tag name.
    * Caution: this method completely ignores "primary" tag names.
    * Only tag translations matching the specified language are searched.
    *
    * @param name language-aware tag name
    * @return a collection of nodes (possibly empty)
    */
  def findNodesByTagTranslation(name: StringWithLocale): Iterable[Category]


  /**
    * Performs a subtree traversal starting from the specified node (which we call here a "subtree root")
    * Returns a collection of all (recursively) subnodes below the specified node - including the subtree root itself.
    * If a subtree root is not existing in the tree, the returned collection is just empty.
    * <p>
    * Caution: within resulting collection of nodes, each parent is required to be enlisted before its children nodes.
    * Apart from this, implementations are free to apply any order.
    * <p>
    * Implementation note: if for any reason you need to aplly a particular tree traversing semantics, it is always possible
    * to just fall back to using recursively Category.children method, when Taxonomy.root allows accessing the root node of the tree.
    *
    * @param path address of the subtree root
    * @return collection of des
    */
  def listDescendants(path: Treepath): Iterable[Category]

  def listDescendants(pathSegments:String*): Iterable[Category] = this.listDescendants(Treepath(pathSegments.toList))

  /**
    * Retrieves the collection of all tags REGISTERED in this taxonomy.
    */
  def allRegisteredTags: Iterable[Tag]

  /**
    * Retrieves the collection of all tags CURRENTLY ATTACHED to nodes of taxonomy tree.
    * Caution: allUsedTags is always a subset of allRegisteredTags.
    */
  def allUsedTags: Iterable[Tag]

  /**
    * Checks if a given tag name is registered within this taxonomy tags..
    * If it is registered - returns Tag instance (this gives access to all translations for this tag).
    *
    * @param name tag name
    * @return Tag instance if tag is found, None otherwise
    */
  def findTag(name: String): Option[Tag]

  /**
    * Adds new node to the tree.
    * <p>
    * Because we operate on paths, this method will add all missing nodes along the path if needed
    * (what we really want is to be sure that after this method completes, the specified path is existing in the taxonomy tree).
    * <p>
    * If such a node already exists, this method just ensures that all provided tags are attached to this node,
    * so it works as "add" on the set of tags of this node.
    * <p>
    * This method is idempotent.
    * <p>
    * Caution: taxonomy tree enforces that only one root node exists. Attempt to add a second root will result in exception.
    * Example:
    * <pre>
    *   taxonomy.add(Treepath("a", "b"))
    *   taxonomy.add(Treepath("c", "d")) // <------------ this line will throw exception because we are adding new root "c"
    * </pre>
    *
    * @param path path that we want to be added (or updated)
    * @param tags tags that should be added to the node on the end of the path (= the most deep node on this path)
    * @return new node just created
    */
  def addNode(path: Treepath, tags: Iterable[String] = List.empty): Category

  def addNode(pathSegments:String*): Category = this.addNode(Treepath(pathSegments.toList))

  /**
    * Removes the node identified by the specified path from the taxonomy tree (and so the whole subtree below this node of course).
    * Corner cases:<ul>
    *   <li>node was not found (= path leads to nowhere) - silently exits (performs nothing)</li>
    *   <li>node is a leaf - just removes this leaf</li>
    *   <li>node is root - removes the whole taxonomy tree</li>
    * </ul>
    *
    * This method is idempotent.
    *
    * @param path path to the subtree root
    */
  def removeSubtree(path: Treepath): Unit

  def removeSubtree(pathSegments:String*): Unit = this.removeSubtree(Treepath(pathSegments.toList))

  /**
    * Adds a tag to a set of tags associated with specified taxonomy tree node.
    * If such a tag is already attached to this node - nothing happens.
    * This tag do not need to previously registered in this taxonomy - the registration is automatic.
    * This method throws exception if specified node was not found.
    *
    * This method is idempotent.
    *
    * @param tag tag name to be added
    * @param targetNode node where a tag is to be attached
    * @throws NodeNotFound if specified node is not found in taxonomy tree
    */
  @throws[NodeNotFound]
  def tag(tag: String, targetNode: Treepath)

  /**
    * Removes a tag from a set of tags associated with specified taxonomy tree node.
    * Corner cases:<ul>
    *   <li>node not found - throws exeption</li>
    *   <li>tag not registered - silently exits (performs nothing)</li>
    *   <li>tag not attached to this node - silently exits (performs nothing)</li>
    * </ul>
    * This method will never unregister a tag from a given taxonomy. It is just to remove this tag from one node.
    *
    * @param tag tag name to be added
    * @param targetNode node where a tag is to be removed
    * @throws NodeNotFound if specified node is not found in taxonomy tree
    */
  @throws[NodeNotFound]
  def untag(tag: String, targetNode: Treepath)

  /**
    * Registers new tag into the collection of tags known in this taxonomy.
    * This is in any way changing mapping between tree nodes and tags.
    * Also the existing tags translations are left intact.
    * If such a tag is already registered - nothing happens.
    *
    * This method is idempotent.
    *
    * @param tag tag name to be registered
    */
  def registerTag(tag: String)

  /**
    * Removes a tag from the collection of tags known in this taxonomy (together with all its translations).
    * All uses of this tag across the taxonomy tree are removed too.
    * If such a tag is not registered - nothing happens.
    *
    * This method is idempotent.
    *
    * @param tag
    */
  def unregisterTag(tag: String)

  /**
    * Adds a tranlation to a set of translations associated to a specified tag.
    * This tag must be previously registered - for unknown tag an exception is thrown.
    * Caution: at most one translation may be registered for a givel language (= locale).
    * So for a given tag, translations form a map: Locale ----> String.
    * This method will override a translation, if a translation for the same language was already registered.
    *
    * This method is idempotent.
    *
    * @param tag target tag
    * @param translation translation to be added (this is just a string paired with a locale)
    * @throws UnknownTag if such tag is not registered in the taxonomy
    */
  @throws[UnknownTag]
  def addTagTranslation(tag: String, translation: StringWithLocale)

  /**
    * Removes a translation from a set of translations associated to a specified tag.
    * Mapping between taxonomy tree nodes and tags is left intact.
    *
    * This tag must be previously registered - for unknown tag an exception is thrown.
    * This method is idempotent.
    *
    * @param tag target tag
    * @param locale translation to be removed (identified by the language)
    * @throws UnknownTag if the tag is not registered
    */
  @throws[UnknownTag]
  def removeTagTranslation(tag: String, locale: Locale)


}
