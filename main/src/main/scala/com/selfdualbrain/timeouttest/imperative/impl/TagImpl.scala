package com.selfdualbrain.timeouttest.imperative.impl

import java.util.Locale

import com.selfdualbrain.timeouttest.common.StringWithLocale
import com.selfdualbrain.timeouttest.imperative.api.Tag

import scala.collection.mutable

private[impl] class TagImpl(val name: String) extends Tag {
  private val translationsMap = new mutable.HashMap[Locale, String]

  override def languages: Iterable[Locale] = translationsMap.keys

  override def translations: Iterable[StringWithLocale] = translationsMap collect {case (locale,text) => StringWithLocale(text, locale)}

  override def findTranslationForLanguage(language: Locale): Option[String] = translationsMap.get(language)

  def addTranslation(language: Locale, text: String): Unit = {
    translationsMap += (language -> text)
  }

  def removeTranslation(language: Locale): Unit = {
    translationsMap -= language
  }

}
