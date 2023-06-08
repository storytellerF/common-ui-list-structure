package com.example.ui_list_annotation_common

class Holder(
    val bindingName: String,
    val bindingFullName: String,
    val viewHolderName: String,
    val viewHolderFullName: String
)

class Entry<T>(
    val itemHolderName: String,
    val itemHolderFullName: String,
    val viewHolders: MutableMap<String, Holder>,
    val origin: T
)

class Event<T>(
    val receiver: String,
    val receiverFullName: String,
    val functionName: String,
    val parameterList: String,
    val key: String,
    val origin: T
) {
    override fun toString(): String {
        return "Event(receiver='$receiver', functionName='$functionName', parameterCount=$parameterList)"
    }
}