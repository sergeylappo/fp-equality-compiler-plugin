package io.github.sergeylappo.plugin

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

@OptIn(ExperimentalCompilerApi::class)
class FloatEqualityCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        // Register our IR generation extension for float equality transformation
        IrGenerationExtension.registerExtension(FloatEqualityIrGenerationExtension())
    }
}