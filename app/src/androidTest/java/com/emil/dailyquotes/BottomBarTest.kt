package com.emil.dailyquotes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test

class BottomBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun getNavItems(): List<NavigationDestination> {
        return listOf(
            NavigationDestination(
                navItem = BottomNavigationItem.HomeScreenItem,
                content = {}
            ),
            NavigationDestination(
                navItem = BottomNavigationItem.ProfileScreenItem,
                content = {}
            )
        )
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun bottomBarInitialTest() {
        composeTestRule.setContent {
            val scope = rememberCoroutineScope()
            val pagerState = rememberPagerState(pageCount = { 2 })

            BottomBar(
                pagerState = pagerState,
                setPosition = {
                    scope.launch {
                        pagerState.animateScrollToPage(it)
                    }
                },
                navigationItems = getNavItems(),
            )
        }

        composeTestRule
            .onNodeWithText(BottomNavigationItem.HomeScreenItem.label)
            .assertIsSelected()

        composeTestRule
            .onNodeWithText(BottomNavigationItem.ProfileScreenItem.label)
            .assertIsNotSelected()
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun bottomBarProfileTest(){
        composeTestRule.setContent {
            val scope = rememberCoroutineScope()
            val pagerState = rememberPagerState(
                initialPage = 1,
                pageCount = { 2 }
            )

            BottomBar(
                pagerState = pagerState,
                setPosition = {
                    scope.launch {
                        pagerState.animateScrollToPage(it)
                    }
                },
                navigationItems = getNavItems(),
            )
        }

        composeTestRule
            .onNodeWithText(BottomNavigationItem.ProfileScreenItem.label)
            .assertIsSelected()

        composeTestRule
            .onNodeWithText(BottomNavigationItem.HomeScreenItem.label)
            .assertIsNotSelected()
    }
}