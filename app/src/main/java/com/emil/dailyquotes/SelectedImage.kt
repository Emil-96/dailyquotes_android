package com.emil.dailyquotes

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SelectedImage: ViewModel(){

    private val _image: MutableLiveData<Uri> = MutableLiveData(Uri.EMPTY)
    val image: LiveData<Uri> = _image

    fun setImage(uri: Uri){
        _image.postValue(uri)
    }
}