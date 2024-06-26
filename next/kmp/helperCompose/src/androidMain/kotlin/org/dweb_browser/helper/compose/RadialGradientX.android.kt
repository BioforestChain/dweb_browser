package org.dweb_browser.helper.compose

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Build
import android.util.AttributeSet
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


@Composable
actual fun RadialGradientX(
  modifier: Modifier,
  startX: Float,
  startY: Float,
  startRadius: Float,
  endX: Float,
  endY: Float,
  endRadius: Float,
  colors: Array<Color>,
  stops: Array<Float>?,
) {
  val context = LocalContext.current
  val d = LocalDensity.current.density
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val canvasView = remember(context) { SkiaRadialGradientView(context) }
    // Creating a gradient using makeTwoPointConicalGradient
    remember(canvasView, d, startX, startY, startRadius, endX, endY, endRadius, colors, stops) {
      @OptIn(ExperimentalUnsignedTypes::class) RadialGradient(
        // start circle
        startX * d, startY * d, startRadius * d,
        // end circle
        endY * d, endY * d, endRadius * d,

        colors.map { it.value }.toULongArray().toLongArray(),// colors
        stops?.toFloatArray(),// stops
        Shader.TileMode.CLAMP,//tileMode
      ).also { canvasView.gradient = it }
    }

    AndroidView(
      factory = {
        canvasView
      },
      modifier = modifier,
      update = {
//    it.invalidate()
      },
    )
  } else {
    val canvasView = remember(context) {
      GLSurfaceView(context).apply {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
//        holder.setFormat(PixelFormat.TRANSLUCENT)
      }
    }
    BoxWithConstraints(modifier) {
      val H = maxHeight.value * d;
      remember(d, H, startX, startY, startRadius, endX, endY, endRadius, colors, stops) {
        GlRadialGradientView(
          startX = startX * d,
          startY = H - (startY * d),
          startRadius = startRadius * d,
          endX = endX * d,
          endY = H - (endY * d),
          endRadius = endRadius * d,
          colors = colors,
          stops = stops
        ).also {
          canvasView.setRenderer(it)
          canvasView.holder.setFormat(PixelFormat.RGBA_8888);
          canvasView.holder.setFormat(PixelFormat.TRANSLUCENT);
//          canvasView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY;
        }
      }
      AndroidView(
        factory = {
          canvasView
        },
        modifier = Modifier.fillMaxSize(),
        update = {
//    it.invalidate()
        },
      )
    }
  }

}

private class SkiaRadialGradientView @JvmOverloads constructor(
  context: Context, attrs: AttributeSet? = null,
) : View(context, attrs) {
  private val paint = Paint().apply {
    // Use the Paint.ANTI_ALIAS_FLAG for better rendering
    isAntiAlias = true
  }
  var gradient: RadialGradient? = null

  @RequiresApi(Build.VERSION_CODES.S)
  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)

    when (val shader = gradient) {
      null -> {
      }

      else -> {
        val width = width.toFloat()
        val height = height.toFloat()

        paint.shader = shader
        // Draw a circle using the paint with gradient shader
        canvas.drawRect(0f, 0f, width, height, paint)
      }
    }
  }
}

private class GlRadialGradientView(
  private val startX: Float,
  private val startY: Float,
  private val startRadius: Float,
  private val endX: Float,
  private val endY: Float,
  private val endRadius: Float,
  private val colors: Array<Color>,
  private val stops: Array<Float>?,
) : GLSurfaceView.Renderer {

  private var gradientProgram: Int = 0
  private val vertexShaderCode = """
        attribute vec4 vPosition;
        void main() {
            gl_Position = vPosition;
        }
    """

  private val fragmentShaderCode = """
        precision mediump float;
        uniform vec2 start;
        uniform vec2 end;
        uniform float startRadius;
        uniform float endRadius;
        uniform vec4 colors[${colors.size}];
        uniform float stops[${colors.size}];

        float calc_t(vec2 c0, float r0, vec2 c1, float r1, vec2 p) {
          float w = 1.0;
          float result = 0.0;
          vec2 ab = c1 - c0;
          float dr = r1 - r0;
          float delta = 1.0 / length(ab);
          while (w >= 0.0) {
            vec2 cw = w * ab + c0;
            float rw = w * dr + r0;
            if (length(p - cw) <= rw) {
              result = w;
              break;
            }
            w -= delta;
          }
          return 1.0 - result;
        }

        void main() {
          float d1 = distance(gl_FragCoord.xy, start) - startRadius;
          float d2 = endRadius - distance(gl_FragCoord.xy, end);

          if (d1 <= 0.0) {
            gl_FragColor = colors[0];
          } else if (d2 <= 0.0) {
            gl_FragColor = colors[${colors.size - 1}];
          } else {
            vec2 c0 = end;
            vec2 c1 = start;
            float r0 = endRadius;
            float r1 = startRadius;
            vec2 p = gl_FragCoord.xy;
            float t = calc_t(c0, r0, c1, r1, p);
            if (t <= stops[0]) {
              gl_FragColor = colors[0];
            } else if (t >= stops[${colors.size - 1}]) {
              gl_FragColor = colors[${colors.size - 1}];
            } else {
              vec4 color = vec4(0.0);
              for (int i = 1; i < ${colors.size}; ++i) {
                if (t <= stops[i]) {
                  float localT = (stops[i] - t) / (stops[i] - stops[i-1]);
                  color = mix(colors[i], colors[i-1], localT);
                  break;
                }
              }

              gl_FragColor = color;
            }
          }
        }
    """

  private val vertexCoords = floatArrayOf(
    -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f
  )

  private val vertexBuffer =
    ByteBuffer.allocateDirect(vertexCoords.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
      .apply {
        put(vertexCoords)
        position(0)
      }

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

    val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
    val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)
    gradientProgram = GLES20.glCreateProgram().also {
      GLES20.glAttachShader(it, vertexShader)
      GLES20.glAttachShader(it, fragmentShader)
      GLES20.glLinkProgram(it)
    }
  }

  override fun onDrawFrame(gl: GL10?) {
    GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f)
//    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

    GLES20.glUseProgram(gradientProgram)

    val positionHandle = GLES20.glGetAttribLocation(gradientProgram, "vPosition")
    GLES20.glEnableVertexAttribArray(positionHandle)
    GLES20.glVertexAttribPointer(
      positionHandle, 2, GLES20.GL_FLOAT, false, 2 * 4, vertexBuffer
    )

    val startHandle = GLES20.glGetUniformLocation(gradientProgram, "start")
    GLES20.glUniform2f(startHandle, startX, startY)

    val endHandle = GLES20.glGetUniformLocation(gradientProgram, "end")
    GLES20.glUniform2f(endHandle, endX, endY)

    val startRadiusHandle = GLES20.glGetUniformLocation(gradientProgram, "startRadius")
    GLES20.glUniform1f(startRadiusHandle, startRadius)

    val endRadiusHandle = GLES20.glGetUniformLocation(gradientProgram, "endRadius")
    GLES20.glUniform1f(endRadiusHandle, endRadius)

    val colorsHandle = GLES20.glGetUniformLocation(gradientProgram, "colors")
    val stopHandle = GLES20.glGetUniformLocation(gradientProgram, "stops")

    val colorArray = FloatArray(colors.size * 4)
    colors.forEachIndexed { i, color ->
      val startPos = i * 4
      colorArray[startPos] = color.red
      colorArray[startPos + 1] = color.green
      colorArray[startPos + 2] = color.blue
      colorArray[startPos + 3] = color.alpha
    }

    GLES20.glUniform4fv(colorsHandle, colors.size, colorArray, 0)

    val stopArray = stops?.let {
      FloatArray(it.size).also { stopArray ->
        stops.forEachIndexed { i, fl ->
          stopArray[i] = fl
        }
      }
    } ?: run {
      // If stops are not provided, generate based on colors size
      val maxIndex = colors.size - 1f
      FloatArray(colors.size) { it / maxIndex }
    }
    GLES20.glUniform1fv(stopHandle, stopArray.size, stopArray, 0)


    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, vertexCoords.size / 2)
    GLES20.glDisableVertexAttribArray(positionHandle)
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    GLES20.glViewport(0, 0, width, height)
  }

  private fun loadShader(type: Int, shaderCode: String): Int {
    return GLES20.glCreateShader(type).also { shader ->
      GLES20.glShaderSource(shader, shaderCode)
      GLES20.glCompileShader(shader)
    }
  }
}