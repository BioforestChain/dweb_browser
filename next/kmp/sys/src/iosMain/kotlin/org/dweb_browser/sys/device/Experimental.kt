package org.dweb_browser.sys.device

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.CONSTRUCTOR
import kotlin.annotation.AnnotationTarget.FIELD
import kotlin.annotation.AnnotationTarget.FUNCTION
import kotlin.annotation.AnnotationTarget.LOCAL_VARIABLE
import kotlin.annotation.AnnotationTarget.PROPERTY
import kotlin.annotation.AnnotationTarget.PROPERTY_GETTER
import kotlin.annotation.AnnotationTarget.PROPERTY_SETTER
import kotlin.annotation.AnnotationTarget.TYPEALIAS
import kotlin.annotation.AnnotationTarget.VALUE_PARAMETER

/**
 * This signifies an API which is either new, not well tested, or may change in form or behavior in future versions
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(
  CLASS,
  PROPERTY,
  FIELD,
  LOCAL_VARIABLE,
  VALUE_PARAMETER,
  CONSTRUCTOR,
  FUNCTION,
  PROPERTY_GETTER,
  PROPERTY_SETTER,
  TYPEALIAS
)
public annotation class ExperimentalSettingsApi

/**
 * This signifies an implementation class which is either new, not well tested, or may change in form or behavior in
 * future versions
 */
@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Target(
  CLASS,
  PROPERTY,
  FIELD,
  LOCAL_VARIABLE,
  VALUE_PARAMETER,
  CONSTRUCTOR,
  FUNCTION,
  PROPERTY_GETTER,
  PROPERTY_SETTER,
  TYPEALIAS
)
public annotation class ExperimentalSettingsImplementation