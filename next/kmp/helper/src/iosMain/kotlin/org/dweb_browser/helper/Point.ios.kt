@file:OptIn(ExperimentalForeignApi::class)

package org.dweb_browser.helper

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake

fun CValue<CGPoint>.toPoint() = useContents { Point(x.toFloat(), y.toFloat()) }

fun Point.toIosPoint() = CGPointMake(x.toDouble(), y.toDouble())