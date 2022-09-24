package com.debduttapanda.animatedlazylistgrid

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
data class ItemData(
    val value: Int,
    val id: String
)
class MyViewModel: ViewModel(){
    fun newId(it: Int): String{
        return System.currentTimeMillis().toString()+"_"+it.toString()
    }

    val list = mutableStateListOf<Int>().apply { addAll((0..10).toList()) }
    init {
        viewModelScope.launch {
            while (true){
                delay(5000)
                list.shuffle()
            }
        }
    }
    val listType = mutableStateOf(true)
    val animated = mutableStateOf(false)
    var i = 0
    val numbers = mutableStateListOf<ItemData>().animated
    fun delete(item: ItemData) {
        numbers.remove(item)
    }

    fun addSome() {
        val s = i + 1
        val e = i + 5
        i = e
        numbers.addAll((s..e).toList().map {
            ItemData(
                value = it,
                id = newId(it)
            )
        },animated.value)
    }

    fun clearAll() {
        numbers.clear(animated.value)
        i = 0
    }

    fun addAfter(item: ItemData) {
        val index = numbers.indexOf(item)
        numbers
            .add(
                index+1,
                ItemData(
                    value = ++i,
                    id = newId(i)
                )
            )
    }

    fun shuffle() {
        numbers.shuffle()
    }
}