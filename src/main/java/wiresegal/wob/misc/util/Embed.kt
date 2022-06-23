package wiresegal.wob.misc.util

import com.fasterxml.jackson.databind.node.ObjectNode
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.core.entity.message.embed.EmbedBuilderDelegateImpl

fun EmbedBuilder.toJsonNode(): ObjectNode = (delegate as EmbedBuilderDelegateImpl).toJsonNode()

fun String.spoilerTag(shouldTag: Boolean) = if (shouldTag) "||${this}||" else this
