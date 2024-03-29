package com.getir.patika.foodcouriers.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.VectorDrawable
import androidx.annotation.DrawableRes
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import com.getir.patika.foodcouriers.MapsFragment
import com.getir.patika.foodcouriers.R
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

class SvgToBitmapConverter {

    companion object{
        fun vectorToBitmap(@DrawableRes id: Int, context: Context): BitmapDescriptor {
            val vectorDrawable = ContextCompat.getDrawable(context, id) as VectorDrawable
            val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            vectorDrawable.setBounds(0, 0, canvas.width, canvas.height)
            vectorDrawable.draw(canvas)
            return BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

}