package com.selfdualbrain.timeouttest.functional.impl

import java.util.Locale

import com.selfdualbrain.timeouttest.common.StringWithLocale
import com.selfdualbrain.timeouttest.functional.api.Tag

case class TagImpl(name: String, translationsMap: Map[Locale, String]) extends Tag {

  override def findTranslationForLanguage(language: Locale): Option[String] = translationsMap.get(language)

  override def languages: Iterable[Locale] = translationsMap.keys

  override def translations: Iterable[StringWithLocale] = translationsMap collect {case (locale,text) => StringWithLocale(text, locale)}

  def withUpdatedTranslation(translation: StringWithLocale): TagImpl = TagImpl(name, translationsMap + (translation.locale -> translation.text))

  def withRemovedTranslation(locale: Locale): TagImpl = TagImpl(name, translationsMap - locale)

}
