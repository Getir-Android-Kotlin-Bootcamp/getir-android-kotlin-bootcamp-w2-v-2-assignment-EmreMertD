package com.getir.patika.foodcouriers

import android.content.ContentValues.TAG
import android.graphics.drawable.BitmapDrawable
import android.location.Address
import android.location.Geocoder
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.getir.patika.foodcouriers.utils.SvgToBitmapConverter
import com.google.android.gms.common.api.ApiException

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import java.util.Locale

class MapsFragment : Fragment() {
    private lateinit var googleMap: GoogleMap
    private lateinit var viewModel: SharedMapsViewModel
    private lateinit var addressTextView: TextView
    private lateinit var yourLocationTw: TextView
    private lateinit var customMarkerIcon : BitmapDescriptor
    private lateinit var searchView : androidx.appcompat.widget.SearchView


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), "AIzaSyCaQfgz_dbGg5GpbVNCEHWy6G9ftWQLjpw")
        }

        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        customMarkerIcon = SvgToBitmapConverter.vectorToBitmap(R.drawable.radarpin, this.requireContext())
        viewModel = ViewModelProvider(requireActivity()).get(SharedMapsViewModel::class.java)
        addressTextView = view.findViewById(R.id.detail_adress_tw)
        yourLocationTw = view.findViewById(R.id.yourLocationTw)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        viewModel.selectedAddress.observe(viewLifecycleOwner, Observer { address ->
            addressTextView.text = address
        })

        searchView = view.findViewById(R.id.searchView)
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    searchInGoogleMap(query)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })

    }

    private val callback = OnMapReadyCallback { map ->
        googleMap = map

        val ankara = LatLng(39.925018, 32.836956)
        googleMap.addMarker(MarkerOptions().icon(customMarkerIcon).anchor(0.5f, 0.5f).position(ankara).title("Marker in Ankara"))
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ankara, 15f))

        // anıtkabir preview
        getAddressFromLocation(ankara) { address ->
            viewModel.setAddress(address)
            addressTextView.text = address
        }

        googleMap.setOnMapClickListener { latLng ->
            Log.d("MapsFragment", "Haritada tıklandı: $latLng")

            googleMap.clear()
            googleMap.addMarker(MarkerOptions().icon(customMarkerIcon).anchor(0.5f, 0.5f).position(latLng).title("Seçilen Konum"))
            getAddressFromLocation(latLng) { address ->
                viewModel.setAddress(address)
                addressTextView.text = address
            }

        }
    }

    private fun getAddressFromLocation(latLng: LatLng, callback: (String) -> Unit) {
        val geocoder = context?.let { Geocoder(it, Locale.getDefault()) }
        val addresses: List<Address> = geocoder?.getFromLocation(latLng.latitude, latLng.longitude, 1) as List<Address>

        if (addresses.isNotEmpty()) {
            val address: Address = addresses[0]
            val addressText = address.getAddressLine(0)
            callback(addressText)
        }
    }


    fun searchInGoogleMap(query: String) {
        val placesClient = Places.createClient(requireContext())

        // Türkiye'nin güneybatı ve kuzeydoğu köşeleri ile bir LatLngBounds nesnesi oluşturdum ve
        // bu latlong değerleri aralığına öncelik vermesini sağladım.
        val southwestTurkey = LatLng(36.0, 26.0)
        val northeastTurkey = LatLng(42.0, 45.0)
        val turkeyBounds = LatLngBounds(southwestTurkey, northeastTurkey)

        val request = FindAutocompletePredictionsRequest.builder()
            .setLocationBias(RectangularBounds.newInstance(turkeyBounds))
            .setTypeFilter(TypeFilter.ESTABLISHMENT)
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request).addOnSuccessListener { response ->
            val predictions = response.autocompletePredictions
            if (predictions.isNotEmpty()) {

                val placeId = predictions[0].placeId
                val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)

                val fetchPlaceRequest = FetchPlaceRequest.newInstance(placeId, placeFields)
                placesClient.fetchPlace(fetchPlaceRequest).addOnSuccessListener { fetchPlaceResponse ->
                    val place = fetchPlaceResponse.place
                    Log.i(TAG, "Place found: ${place.name}")

                    googleMap.clear()
                    place.latLng?.let {
                        val markerOptions = MarkerOptions().icon(customMarkerIcon).anchor(0.5f, 0.5f).position(it).title(place.name)
                        googleMap.addMarker(markerOptions)
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 15f))

                        addressTextView.text = place.address
                        yourLocationTw.text = place.name
                    }
                }.addOnFailureListener { exception ->
                    if (exception is ApiException) {
                        Log.e(TAG, "Place not found: ${exception.statusCode}")
                    }
                }
            }
        }.addOnFailureListener { exception ->
            if (exception is ApiException) {
                Log.e(TAG, "Place not found: ${exception.statusCode}")
            }
        }
    }


}


