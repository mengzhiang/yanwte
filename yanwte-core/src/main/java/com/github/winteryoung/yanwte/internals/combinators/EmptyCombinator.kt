package com.github.winteryoung.yanwte.internals.combinators

import com.github.winteryoung.yanwte.Combinator
import com.github.winteryoung.yanwte.ExtensionPointInput
import com.github.winteryoung.yanwte.ExtensionPointOutput
import com.github.winteryoung.yanwte.internals.YanwteExtension

/**
 * In case anyone want to write an empty provide, this node can help.
 * It does nothing but returns null on any input.
 *
 * @author Winter Young
 * @since 2016/1/19
 */
internal class EmptyCombinator(
        extensionPointName: String
) : ExtensionAwareCombinator(extensionPointName, "empty") {
    override val extension: YanwteExtension?
        get() = null

    override fun invokeImpl(input: ExtensionPointInput): ExtensionPointOutput {
        return ExtensionPointOutput.empty
    }
}