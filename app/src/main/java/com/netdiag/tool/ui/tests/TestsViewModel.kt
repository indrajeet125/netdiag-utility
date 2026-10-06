package com.netdiag.tool.ui.tests

import androidx.lifecycle.ViewModel
import com.netdiag.tool.data.repository.TestHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TestsViewModel @Inject constructor(
    val historyRepository: TestHistoryRepository
) : ViewModel()
