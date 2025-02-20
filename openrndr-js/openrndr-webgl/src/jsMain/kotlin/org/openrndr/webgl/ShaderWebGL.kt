package org.openrndr.webgl

import org.khronos.webgl.*
import org.khronos.webgl.WebGLRenderingContext
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.*
import org.openrndr.math.*
import org.khronos.webgl.WebGLRenderingContext as GL


fun GL.checkErrors(msg: String = "") {
    val e = getError()
    if (e != WebGLRenderingContext.NO_ERROR) {
        val m = when (e) {
            WebGLRenderingContext.INVALID_ENUM -> "Invalid enum"
            WebGLRenderingContext.INVALID_VALUE -> "Invalid value"
            WebGLRenderingContext.INVALID_OPERATION -> "Invalid operation"
            WebGLRenderingContext.INVALID_FRAMEBUFFER_OPERATION -> "Invalid framebuffer operation"
            WebGLRenderingContext.OUT_OF_MEMORY -> "Out of memory"
            WebGLRenderingContext.CONTEXT_LOST_WEBGL -> "Context lst webgl"
            else -> "unknown error"
        }
        error("$m: $msg")
    }
}

data class ActiveUniform(val name: String, val size: Int, val type: Int)

class ShaderWebGL(
    val context: GL,
    val program: WebGLProgram,
    val activeUniforms: Map<String, ActiveUniform>,
    override val session: Session?
) : Shader {
    companion object {
        fun create(
            context: GL,
            vertexShader: VertexShaderWebGL,
            fragmentShader: FragmentShaderWebGL,
            @Suppress("UNUSED_PARAMETER") name: String,
            session: Session?
        ): ShaderWebGL {
            val program = context.createProgram() ?: error("failed to create program")
            context.attachShader(program, vertexShader.shaderObject)
            context.attachShader(program, fragmentShader.shaderObject)
            context.linkProgram(program)

            val activeUniformCount = context.getProgramParameter(program, GL.ACTIVE_UNIFORMS) as Int
            val activeUniforms = (0 until activeUniformCount).mapNotNull {
                val activeUniform = context.getActiveUniform(program, it)

                if (activeUniform != null) {
                    ActiveUniform(activeUniform.name, activeUniform.size, activeUniform.type)
                } else {
                    null
                }
            }

            return ShaderWebGL(context, program, activeUniforms.associateBy { it.name }, session)
        }
    }

    var userShader = false


    fun attributeIndex(name: String): Int {
        val index = context.getAttribLocation(program, name)
        if (index == -1) {
            //console.warn("missing attribute $name")
        }
        return index
    }

    override val types: Set<ShaderType> = setOf(ShaderType.FRAGMENT, ShaderType.VERTEX)

    override fun begin() {
        context.useProgram(program)
    }

    override fun end() {
        context.useProgram(null as WebGLProgram?)
    }

    fun uniformIndex(uniform: String): WebGLUniformLocation? {
        val index = context.getUniformLocation(program, uniform)
        if (index == null) {
            //console.warn("missing uniform $uniform")
        }
        return index
    }

    override fun hasUniform(name: String): Boolean {
        return uniformIndex(name) != null
    }

    override fun createBlock(blockName: String): UniformBlock? {
        error("uniform blocks are not supported by WebGL")
    }

    override fun blockLayout(blockName: String): UniformBlockLayout? {
        error("uniform blocks are not supported by WebGL")
    }

    override fun block(blockName: String, block: UniformBlock) {
        error("uniform blocks are not supported by WebGL")
    }

    override fun uniform(name: String, value: Matrix33) {
        val index = uniformIndex(name)
        if (index != null) {
            context.uniformMatrix3fv(index, false, value.toFloat32Array())
            context.checkErrors("$name $value")
        }
    }

    override fun uniform(name: String, value: Matrix44) {
        context.checkErrors("older error persists")
        val index = uniformIndex(name)
        if (index != null) {
            context.uniformMatrix4fv(index, false, value.toFloat32Array())
            context.checkErrors("$name $value")
        }
    }

    override fun uniform(name: String, value: ColorRGBa) {
        val index = uniformIndex(name)
        if (index != null) {
            context.uniform4fv(index, value.toFloat32Array())
            context.checkErrors("$name $value")
        }
    }

    override fun uniform(name: String, value: Vector4) {
        val index = uniformIndex(name)
        if (index != null) {
            context.uniform4fv(index, value.toFloat32Array())
            context.checkErrors("$name $value")
        }
    }

    override fun uniform(name: String, value: Vector3) {
        val index = uniformIndex(name)
        if (index != null) {
            context.uniform3fv(index, value.toFloat32Array())
            context.checkErrors("$name $value")
        }
    }

    override fun uniform(name: String, value: Vector2) {
        val index = uniformIndex(name)
        if (index != null) {
            context.uniform2fv(index, value.toFloat32Array())
            context.checkErrors("$name $value")
        }
    }

    override fun uniform(name: String, value: IntVector4) {
        TODO("Not yet implemented")
    }

    override fun uniform(name: String, value: IntVector3) {
        TODO("Not yet implemented")
    }

    override fun uniform(name: String, value: IntVector2) {
        TODO("Not yet implemented")
    }

    override fun uniform(name: String, x: Float, y: Float, z: Float, w: Float) {
        val index = uniformIndex(name)
        if (index != null) {
            context.uniform4f(index, x, y, z, w)
            context.checkErrors("$name $x $y $z $w")
        }
    }

    override fun uniform(name: String, x: Float, y: Float, z: Float) {
        val index = uniformIndex(name)
        if (index != null) {
            context.uniform3f(index, x, y, z)
            context.checkErrors("$name $x $y $z")
        }
    }

    override fun uniform(name: String, x: Float, y: Float) {
        val index = uniformIndex(name)
        if (index != null) {
            context.uniform2f(index, x, y)
            context.checkErrors("$name $x $y")
        }
    }

    override fun uniform(name: String, value: Float) {
        val index = uniformIndex(name)
        if (index != null) {
            context.uniform1f(index, value.toFloat())
            context.checkErrors("$name $value")
        }
    }

    override fun uniform(name: String, value: Double) {
        val index = uniformIndex(name)

        val activeUniform = activeUniforms[name]
        if (index != null) {
            require(activeUniform != null) {
                "no active uniform for $name"
            }
        }
        if (index != null && activeUniform != null) {
            when (activeUniform.type) {
                GL.INT -> context.uniform1i(index, value.toInt())
                GL.FLOAT -> context.uniform1f(index, value.toFloat())
                else -> error("unsupported type ${activeUniform.type}")
            }

            context.checkErrors("$name $value (float)")
        }
    }

    override fun uniform(name: String, value: Int) {
        val index = uniformIndex(name)
        if (index != null) {
            context.uniform1i(index, value)
            context.checkErrors("$name $value (int)")
        }
    }

    override fun uniform(name: String, value: Boolean) {
        val index = uniformIndex(name)
        if (index != null) {
            context.uniform1i(index, if (value) 1 else 0)
            context.checkErrors("$name $value")
        }
    }

    override fun uniform(name: String, value: Array<Matrix44>) {
        val floatValues = Float32Array(value.size * 4 * 4)
        var offset = 0
        for (j in value.indices) {
            val mf = value[j].toFloat32Array()
            for (i in 0 until 16) {
                floatValues[offset] = mf[i]
                offset++
            }
        }
        val index = uniformIndex(name)
        if (index != null) {
            context.uniformMatrix4fv(index, false, floatValues)
            context.checkErrors("$name $value")
        }

    }

    override fun buffer(name: String, vertexBuffer: VertexBuffer) {
        TODO("Not yet implemented")
    }

    override fun buffer(name: String, shaderStorageBuffer: ShaderStorageBuffer) {
        TODO("Not yet implemented")
    }

    override fun buffer(name: String, counterBuffer: AtomicCounterBuffer) {
        TODO("Not yet implemented")
    }

    override fun uniform(name: String, value: Array<Vector4>) {
        val floatValues = Float32Array(value.size * 4)
        for (i in value.indices) {
            floatValues[i * 4] = value[i].x.toFloat()
            floatValues[i * 4 + 1] = value[i].y.toFloat()
            floatValues[i * 4 + 2] = value[i].z.toFloat()
            floatValues[i * 4 + 3] = value[i].w.toFloat()
        }
        val index = uniformIndex(name)
        if (index != null) {
            context.uniform4fv(index, floatValues)
            context.checkErrors("$name $value")
        }
    }

    override fun uniform(name: String, value: Array<Vector3>) {
        val floatValues = Float32Array(value.size * 3)
        for (i in value.indices) {
            floatValues[i * 3] = value[i].x.toFloat()
            floatValues[i * 3 + 1] = value[i].y.toFloat()
            floatValues[i * 3 + 2] = value[i].z.toFloat()
        }
        val index = uniformIndex(name)
        if (index != null) {
            context.uniform3fv(index, floatValues)
            context.checkErrors("$name $value")
        }
    }

    override fun uniform(name: String, value: Array<Vector2>) {
        val floatValues = Float32Array(value.size * 2)
        for (i in value.indices) {
            floatValues[i * 3] = value[i].x.toFloat()
            floatValues[i * 3 + 1] = value[i].y.toFloat()
        }

        val index = uniformIndex(name)
        if (index != null) {
            context.uniform3fv(index, floatValues)
            context.checkErrors("$name $value")
        }
    }

    override fun uniform(name: String, value: Array<IntVector4>) {
        TODO("Not yet implemented")
    }

    override fun uniform(name: String, value: Array<IntVector3>) {
        TODO("Not yet implemented")
    }

    override fun uniform(name: String, value: Array<IntVector2>) {
        TODO("Not yet implemented")
    }

    override fun uniform(name: String, value: Array<ColorRGBa>) {
        TODO("Not yet implemented")
    }

    override fun uniform(name: String, value: Array<Double>) {
        val floatValues = Float32Array(value.size)
        for (i in value.indices) {
            floatValues[i * 3] = value[i].toFloat()

        }
        val index = uniformIndex(name)
        if (index != null) {
            context.uniform1fv(index, floatValues)
            context.checkErrors("$name $value")
        }
    }

    override fun uniform(name: String, value: FloatArray) {
        val floatValues = Float32Array(value.size)
        for (i in value.indices) {
            floatValues[i * 3] = value[i].toFloat()

        }
        val index = uniformIndex(name)
        if (index != null) {
            context.uniform1fv(index, floatValues)
            context.checkErrors("$name $value")
        }
    }

    override fun uniform(name: String, value: IntArray) {
        TODO("Not yet implemented")
    }

    override fun destroy() {
        TODO("Not yet implemented")
    }

    override fun image(name: String, image: Int, imageBinding: ImageBinding) {
        error("image bindings are not supported by WebGL")
    }

    override fun image(name: String, image: Int, imageBinding: Array<out ImageBinding>) {
        error("image bindings are not supported by WebGL")
    }
}