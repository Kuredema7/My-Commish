package com.example.mycommish.feature.prize.presentation.screen.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycommish.core.presentation.navigation.Route.Home.Prize.PrizeEdit.PRIZE_ID
import com.example.mycommish.core.util.DecimalFormatter
import com.example.mycommish.feature.prize.domain.model.Prize
import com.example.mycommish.feature.prize.domain.usecase.PrizeUseCases
import com.example.mycommish.feature.prize.domain.util.PrizeValidationResult
import com.example.mycommish.feature.prize.presentation.screen.PrizeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrizeEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val prizeUseCases: PrizeUseCases
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrizeUiState())
    val prizeUiState = _uiState.asStateFlow()

    private val prizeId: Long = checkNotNull(savedStateHandle[PRIZE_ID])
    private val decimalFormatter = DecimalFormatter()

    init {
        viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(
                    prize = prizeUseCases.getPrize(prizeId),
                    isEntryValid = true
                )
            }
        }
    }

    fun updateUiState(prize: Prize) {
        val result = prizeUseCases.prizeEntryValidatorUseCase(prize)
        decimalFormatter.cleanup(prize.value)

        when (result) {
            is PrizeValidationResult.Success -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        prize = result.prize.copy(
                            value = decimalFormatter.cleanup(result.prize.value)
                        ),
                        isEntryValid = true,
                        validatorHasError = false
                    )
                }
            }

            is PrizeValidationResult.Error -> {
                _uiState.update { currentState ->
                    currentState.copy(
                        prize = prize.copy(
                            value = decimalFormatter.cleanup(prize.value)
                        ),
                        isEntryValid = false,
                        validatorHasError = true,
                        errorMessage = result.errorMessage
                    )
                }
            }
        }
    }

    fun updatePrize() {
        viewModelScope.launch(Dispatchers.IO) {
            prizeUseCases.editPrize(prizeUiState.value.prize)
        }
    }
}