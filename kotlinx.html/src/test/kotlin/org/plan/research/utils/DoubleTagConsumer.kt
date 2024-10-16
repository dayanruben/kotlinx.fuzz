package org.plan.research.utils

import kotlinx.html.Entities
import kotlinx.html.Tag
import kotlinx.html.TagConsumer
import kotlinx.html.Unsafe
import kotlinx.html.org.w3c.dom.events.Event

class DoubleTagConsumer<T, U>(val consumer1: TagConsumer<T>, val consumer2: TagConsumer<U>) :
    TagConsumer<Pair<T, U>> {
    override fun finalize(): Pair<T, U> {
        return consumer1.finalize() to consumer2.finalize()
    }

    override fun onTagAttributeChange(tag: Tag, attribute: String, value: String?) {
        consumer1.onTagAttributeChange(tag, attribute, value)
        consumer2.onTagAttributeChange(tag, attribute, value)
    }

    override fun onTagComment(content: CharSequence) {
        consumer1.onTagComment(content)
        consumer2.onTagComment(content)
    }

    override fun onTagContent(content: CharSequence) {
        consumer1.onTagContent(content)
        consumer2.onTagContent(content)
    }

    override fun onTagContentEntity(entity: Entities) {
        consumer1.onTagContentEntity(entity)
        consumer2.onTagContentEntity(entity)
    }

    override fun onTagContentUnsafe(block: Unsafe.() -> Unit) {
        consumer1.onTagContentUnsafe(block)
        consumer2.onTagContentUnsafe(block)
    }

    override fun onTagEnd(tag: Tag) {
        consumer1.onTagEnd(tag)
        consumer2.onTagEnd(tag)
    }

    override fun onTagEvent(tag: Tag, event: String, value: (Event) -> Unit) {
        consumer1.onTagEvent(tag, event, value)
        consumer2.onTagEvent(tag, event, value)
    }

    override fun onTagStart(tag: Tag) {
        consumer1.onTagStart(tag)
        consumer2.onTagStart(tag)
    }
}