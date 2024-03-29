package com.getir.patika.foodcouriers

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedMapsViewModel : ViewModel() {
    private val _selectedAddress = MutableLiveData<String>()
    val selectedAddress: LiveData<String> = _selectedAddress

    fun setAddress(address: String) {
        _selectedAddress.value = address
    }
}
