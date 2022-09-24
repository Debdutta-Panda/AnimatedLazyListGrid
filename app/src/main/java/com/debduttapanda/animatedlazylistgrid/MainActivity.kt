package com.debduttapanda.animatedlazylistgrid

import android.content.ClipData
import android.content.ClipData.Item
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
import androidx.compose.material.*
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
                    Column(){
                        LazyRow(
                            modifier = Modifier.fillMaxWidth()
                        ){
                            item{
                                Checkbox(
                                    checked = vm.animated.value,
                                    onCheckedChange = {
                                        vm.animated.value = it
                                    }
                                )
                            }
                            item{
                                Checkbox(
                                    checked = vm.listType.value,
                                    onCheckedChange = {
                                        vm.listType.value = it
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color.Red,
                                        uncheckedColor = Color.Red
                                    )
                                )
                            }
                            item{
                                Button(onClick = {
                                    vm.addSome()
                                }) {
                                    Text("Some")
                                }
                            }
                            item{
                                Button(onClick = {
                                    vm.clearAll()
                                }) {
                                    Text("Clear")
                                }
                            }
                            item{
                                Button(onClick = {
                                    vm.shuffle()
                                }) {
                                    Text("Shuffle")
                                }
                            }
                        }
                        if(vm.listType.value){
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ){
                                items(
                                    items = vm.numbers,
                                    key = {
                                        it.id
                                    },
                                    enter = fadeIn(tween(700))+ expandVertically(tween(700)),
                                    exit = fadeOut(tween(700))+ shrinkVertically(tween(700)),
                                    exitDuration = 700
                                ){
                                    ItemUI(
                                        modifier = Modifier
                                            .animateItemPlacement(
                                                animationSpec = tween(2000)
                                            ),
                                        it,
                                        vm
                                    )
                                }
                            }
                        }
                        else{
                            LazyVerticalGrid(
                                cells = GridCells.Fixed(3)
                            ){
                                items(
                                    vm.numbers,
                                    enter = fadeIn(tween(700))+ expandVertically(tween(700)),
                                    exit = fadeOut(tween(700))+ shrinkVertically(tween(700)),
                                    exitDuration = 700
                                ){
                                    ItemUI(
                                        modifier = Modifier.animateItemPlacement(),
                                        it,
                                        vm
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ItemUI(
        modifier: Modifier = Modifier,
        item: ItemData,
        vm: MyViewModel
    ) {
        Row(
            modifier = modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .background(Color.LightGray)
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Text(item.value.toString())
            Text(
                "+",
                modifier = Modifier
                    .clickable {
                        vm.addAfter(item)
                    }
            )
            Text(
                "-",
                modifier = Modifier
                    .clickable {
                        vm.delete(item)
                    }
            )
        }
    }
}

data class ItemData(
    val value: Int,
    val id: String
)

class MyViewModel: ViewModel(){
    fun newId(it: Int): String{
        return System.currentTimeMillis().toString()+"_"+it.toString()
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