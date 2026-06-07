package com.app2.core.ui

sealed class ViewState<out T> {
    data object Loading : ViewState<Nothing>()
    data class Loaded<T>(val data: T) : ViewState<T>()
    data class Error(val message: String) : ViewState<Nothing>()
    data object Empty : ViewState<Nothing>()
}
