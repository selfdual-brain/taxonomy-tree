package com.selfdualbrain.timeouttest.functional.api

import java.util.Locale

import com.selfdualbrain.timeouttest.common.StringWithLocale

/**
  * Represents a tag with a collection of its translations.
  * Conceptually a tag is just a string value.
  * Translations are pairs (string, locale).
  * We represent languages as java.util.Locale instances (this is part of JDK i18n supprt).
  *
  * Within a tag, we have a map: language ----> string.
  * In other words for any locale only one translation can be registered.
  */
trait Tag {

  /**
    * Name of this tag.
    */
  def name: String

  /**
    * Collection of languages for which translations of this tag are defined.
    */
  def languages: Iterable[Locale]

  /**
    * Collection of all translations registered for this tag.
    */
  def translations: Iterable[StringWithLocale]

  /**
    * Returns translation of this tag to a given language (if available).
    */
  def findTranslationForLanguage(language: Locale): Option[String]
}
