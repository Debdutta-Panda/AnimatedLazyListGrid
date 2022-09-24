package com.debduttapanda.animatedlazylistgrid

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay

data class VisibleItem<T>(
    val visible: Boolean,
    val data: T,
    val state: State,
){
    enum class State{
        ADDING,
        REMOVING,
        ADDED,
        REMOVED
    }
}

class VisibilityList<T>(core: SnapshotStateList<T>, initialAnimated: Boolean = false){
    private val visibilityList = mutableStateListOf<VisibleItem<T>>()
        .apply {
            val state = if(initialAnimated)
                VisibleItem.State.ADDING
            else
                VisibleItem.State.ADDED
            val visible = !initialAnimated
            addAll(
                core.map {
                    VisibleItem(
                        visible = visible,
                        data = it,
                        state = state
                    )
                }
            )
            _originalSize = core.size
        }
    private var _originalSize = 0
    val actualSize
        get() = visibilityList.size
    val size
        get() = _originalSize
    val list: SnapshotStateList<VisibleItem<T>>
        get() = visibilityList

    fun add(item: T){
        visibilityList.add(
            VisibleItem(
                visible = false,
                data = item,
                state = VisibleItem.State.ADDING
            )
        )
        ++_originalSize
    }

    fun addAll(items: Iterable<T>, initialAnimated: Boolean = false){
        val state = if(initialAnimated)
            VisibleItem.State.ADDING
        else
            VisibleItem.State.ADDED
        val visible = !initialAnimated
        visibilityList.addAll(items.map {
            VisibleItem(
                visible = visible,
                data = it,
                state = state
            )
        })
        _originalSize += items.count()
    }

    operator fun set(index: Int, item: T){
        val size = visibilityList.size
        if(index in 0 until size){
            val visibleItem = visibilityList[index]
            visibilityList[index] = visibleItem.copy(data = item)
        }
    }

    fun add(index: Int, item: T){
        visibilityList.add(index,VisibleItem(
            visible = false,
            data = item,
            state = VisibleItem.State.ADDING
        ))
        ++_originalSize
    }

    fun remove(item: T){
        val index = visibilityList.indexOfFirst {
            it.data == item
        }
        if(index > -1){
            visibilityList[index] = VisibleItem(
                visible = false,
                data = item,
                state = VisibleItem.State.REMOVING
            )
            --_originalSize
        }
    }

    fun makeVisible(item: VisibleItem<T>){
        val index = visibilityList.indexOf(item)
        if(index > -1){
            visibilityList[index] = item.copy(
                visible = true,
                data = item.data,
                state = VisibleItem.State.ADDED
            )
        }
    }

    fun makeInvisible(item: VisibleItem<T>){
        val index = visibilityList.indexOf(item)
        if(index > -1){
            visibilityList[index] = item.copy(
                visible = false,
                data = item.data,
                state = VisibleItem.State.REMOVED
            )
        }
    }

    fun dont_touch___delete(item: VisibleItem<T>) {
        val index = visibilityList.indexOfFirst {
            it.data==item.data
        }
        val success = visibilityList.removeAt(index)
    }

    fun clear(animated: Boolean = false){
        if(!animated){
            visibilityList.clear()
        }
        else{
            val list = visibilityList.map {
                VisibleItem(
                    visible = false,
                    data = it.data,
                    state = VisibleItem.State.REMOVING
                )
            }
            visibilityList.clear()
            visibilityList.addAll(list)
        }
        _originalSize = 0
    }

    fun indexOf(item: T):Int {
        return visibilityList.indexOfFirst {
            it.data==item
        }
    }

    fun shuffle() {
        visibilityList.shuffle()
    }
}

val <T>SnapshotStateList<T>.animated: VisibilityList<T>
    get(){
        return VisibilityList(this)
    }

@OptIn(ExperimentalFoundationApi::class)
inline fun <T> LazyListScope.items(
    items: VisibilityList<T>,
    noinline key: ((item: T) -> Any)? = null,
    enter: EnterTransition = EnterTransition.None,
    exit: ExitTransition = ExitTransition.None,
    exitDuration: Long = 0,
    animateItemPlacementSpec: FiniteAnimationSpec<IntOffset>? = spring(
        stiffness = Spring.StiffnessMediumLow,
        visibilityThreshold = IntOffset.VisibilityThreshold
    ),
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
){
    items(
        items.list,
        key =
        if(key==null)
            null
        else {item: VisibleItem<T>->
            key(item.data)
        }
    ){
        LaunchedEffect(key1 = it.visible){
            if(!it.visible&&it.state==VisibleItem.State.ADDING){
                items.makeVisible(it)
                return@LaunchedEffect
            }
            if(!it.visible&&it.state==VisibleItem.State.REMOVING){
                if(exitDuration>0){
                    items.makeInvisible(it)
                    delay(exitDuration)
                }
                items.dont_touch___delete(it)
            }
        }
        AnimatedVisibility(
            it.visible,
            enter = enter,
            exit = exit,
            modifier = Modifier.then(if(animateItemPlacementSpec!=null) Modifier.animateItemPlacement(animateItemPlacementSpec) else Modifier)
        ) {
            itemContent(it.data)
        }
    }
}

//use this if not using compose foundation
@ExperimentalFoundationApi
inline fun <T> LazyGridScope.items(
    items: VisibilityList<T>,
    noinline spans: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
    enter: EnterTransition = EnterTransition.None,
    exit: ExitTransition = ExitTransition.None,
    exitDuration: Long = 0,
    crossinline itemContent: @Composable LazyItemScope.(item: T) -> Unit
){
    items(
        items.list,
        spans = if(spans==null) null else {item: VisibleItem<T>->
            spans.invoke(this,item.data)
        }
    ){
        LaunchedEffect(key1 = it.visible){
            if(!it.visible&&it.state==VisibleItem.State.ADDING){
                items.makeVisible(it)
                return@LaunchedEffect
            }
            if(!it.visible&&it.state==VisibleItem.State.REMOVING){
                if(exitDuration>0){
                    items.makeInvisible(it)
                    delay(exitDuration)
                }
                items.dont_touch___delete(it)
            }
        }
        Box(){
            AnimatedVisibility(
                it.visible,
                enter = enter,
                exit = exit,
            ) {
                itemContent(it.data)
            }
        }
    }
}

/*
use this if you are using compose foundation
@ExperimentalFoundationApi
inline fun <T> LazyGridScope.items(
    items: VisibilityList<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline span: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyGridItemScope.(item: T) -> Unit,
    enter: EnterTransition = EnterTransition.None,
    exit: ExitTransition = ExitTransition.None,
    exitDuration: Long = 0,
){
    items(
        items = items.list,
        key =
        if(key==null)
            null
        else
            {item: VisibleItem<T>->
                key(item.data)
            },
        span =
        if(span==null)
            null
        else
            {item: VisibleItem<T>->
                span(this,item.data)
            },
        contentType = {item: VisibleItem<T>->
            contentType(item.data)
        },
    ){
        LaunchedEffect(key1 = it.visible){
            if(!it.visible&&it.state==VisibleItem.State.ADDING){
                items.makeVisible(it)
                return@LaunchedEffect
            }
            if(!it.visible&&it.state==VisibleItem.State.REMOVING){
                if(exitDuration>0){
                    items.makeInvisible(it)
                    delay(exitDuration)
                }
                items.dont_touch___delete(it)
            }
        }
        Box(){
            AnimatedVisibility(
                it.visible,
                enter = enter,
                exit = exit,
            ) {
                itemContent(it.data)
            }
        }
    }
}*/
