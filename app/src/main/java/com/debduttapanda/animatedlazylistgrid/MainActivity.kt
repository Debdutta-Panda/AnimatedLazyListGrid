package com.debduttapanda.animatedlazylistgrid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.debduttapanda.animatedlazylistgrid.ui.theme.AnimatedLazyListGridTheme

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
                    Column() {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ){
                                    Text("Animate")
                                    Checkbox(
                                        checked = vm.animated.value,
                                        onCheckedChange = {
                                            vm.animated.value = it
                                        }
                                    )
                                }
                            }
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ){
                                    Text("List")
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

                            }
                            item {
                                Button(
                                    onClick = {
                                        vm.addSome()
                                    },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text("+")
                                }
                            }
                            item {
                                Button(onClick = {
                                    vm.clearAll()
                                }) {
                                    Text("-")
                                }
                            }
                            item {
                                Button(
                                    onClick = {
                                        vm.shuffle()
                                    },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text("⤭")
                                }
                            }
                        }
                        if (vm.listType.value) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    Text("size=${vm.numbers.size}")
                                }
                                item {
                                    Text("actualSize=${vm.numbers.actualSize}")
                                }
                                items(
                                    items = vm.numbers,
                                    key = {
                                        it.id
                                    },
                                    enter = fadeIn(tween(700)) + expandVertically(tween(700)),
                                    exit = fadeOut(tween(700)) + shrinkVertically(tween(700)),
                                    exitDuration = 700,
                                    animateItemPlacementSpec = tween(700)
                                ) {
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
                        } else {
                            LazyVerticalGrid(
                                cells = GridCells.Fixed(3)
                            ) {
                                items(
                                    vm.numbers,
                                    enter = fadeIn(tween(700)) + expandVertically(tween(700)),
                                    exit = fadeOut(tween(700)) + shrinkVertically(tween(700)),
                                    exitDuration = 700
                                ) {
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
        ) {
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