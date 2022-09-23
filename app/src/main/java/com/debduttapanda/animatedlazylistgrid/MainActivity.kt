package com.debduttapanda.animatedlazylistgrid

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.debduttapanda.animatedlazylistgrid.ui.theme.AnimatedLazyListGridTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AnimatedLazyListGridTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    val vm: MyViewModel by viewModels()
                    /*LazyRow{
                        items(
                            items = vm.numbers,
                            key = {
                                it
                            },
                            enter = fadeIn(tween(700))+ expandVertically(tween(700)),
                            exit = fadeOut(tween(700))+ shrinkVertically(tween(700)),
                            exitDuration = 700
                        ){
                            Text(
                                it.toString(),
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(100.dp)
                                    .background(Color.LightGray)
                                    .padding(12.dp)
                                    .clickable {
                                        vm.delete(it)
                                    }
                            )
                        }
                    }*/


                    LazyVerticalGrid(
                        cells = GridCells.Fixed(3)
                    ){
                        /*items(
                            vm.numbers
                        ){
                            Text(
                                it.toString(),
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(100.dp)
                                    .background(Color.LightGray)
                                    .padding(12.dp)
                                    .clickable {
                                        vm.delete(it)
                                    }
                            )
                        }*/
                        items(
                            vm.numbers,
                            enter = fadeIn(tween(700)),
                            exit = fadeOut(tween(700)),
                            exitDuration = 700
                        ){
                            Text(
                                it.toString(),
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .size(100.dp)
                                    .background(Color.LightGray)
                                    .padding(12.dp)
                                    .clickable {
                                        vm.delete(it)
                                    }
                            )
                        }
                    }
                    Text(vm.numbers.size.toString())
                }
            }
        }
    }
}

class MyViewModel: ViewModel(){
    var i = 0
    val numbers = mutableStateListOf<Int>().animated
    fun delete(it: Int) {
        numbers.remove(it)
    }

    init {
        viewModelScope.launch {
            while (true){
                delay(2000)
                numbers.add(++i)
            }
        }
    }
}

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

class VisibilityList<T>(core: SnapshotStateList<T>){
    private val visibilityList = mutableStateListOf<VisibleItem<T>>()
        .apply {
            addAll(
                core.map {
                    VisibleItem(
                        visible = true,
                        data = it,
                        state = VisibleItem.State.ADDED
                    )
                }
            )
        }
    val size
    get() = visibilityList.size
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
    }

    fun add(index: Int, item: T){
        visibilityList.add(index,VisibleItem(
            visible = false,
            data = item,
            state = VisibleItem.State.ADDING
        ))
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

    fun delete(item: VisibleItem<T>) {
        val index = visibilityList.indexOfFirst {
            it.data==item.data
        }
        val success = visibilityList.removeAt(index)
        Log.d("fldkfdf","$success")
    }
}

val <T>SnapshotStateList<T>.animated: VisibilityList<T>
get(){
    return VisibilityList(this)
}

inline fun <T> LazyListScope.items(
    items: VisibilityList<T>,
    noinline key: ((item: T) -> Any)? = null,
    enter: EnterTransition = EnterTransition.None,
    exit: ExitTransition = ExitTransition.None,
    exitDuration: Long = 0,
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
                items.delete(it)
            }
        }
        AnimatedVisibility(
            it.visible,
            enter = enter,
            exit = exit
        ) {
            itemContent(it.data)
        }
    }
}

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
                items.delete(it)
            }
        }
        Box(){
            AnimatedVisibility(
                it.visible,
                enter = enter,
                exit = exit
            ) {
                itemContent(it.data)
            }
        }
    }
}