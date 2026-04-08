package fr.geming400.localisationhelper.ui.activities

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.core.content.res.ResourcesCompat
import com.utsman.osmandcompose.CameraProperty
import com.utsman.osmandcompose.CameraState
import com.utsman.osmandcompose.Marker
import com.utsman.osmandcompose.OpenStreetMap
import com.utsman.osmandcompose.rememberMarkerState
import fr.geming400.localisationhelper.LogTags
import fr.geming400.localisationhelper.R
import fr.geming400.localisationhelper.ui.theme.LocalisationHelperTheme
import org.osmdroid.util.GeoPoint

/**
 * OpenStreetMap activity
 *
 * Bundle extras:
 * - latitude ([Double])
 * - longitude ([Double])
 * - zoom ([Double]) *(optional)* (defaults to `5`)
 * - markers ([Array]<[GeoPoint]>) *(optional)* (defaults to `emptyArray()`)
 */
class OSMActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val bundle = intent.extras!!

        val latitude = bundle.getDouble("latitude")
        val longitude = bundle.getDouble("longitude")
        val zoom = bundle.getDouble("zoom", 5.0)

        var markersPoints: Array<GeoPoint>
        if (bundle.containsKey("markers")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                markersPoints = bundle.getParcelableArray("markers", GeoPoint::class.java)!!
            } else {
                @Suppress("UNCHECKED_CAST", "DEPRECATION")
                markersPoints = bundle.getParcelableArray("markers") as Array<GeoPoint>
            }
        } else {
            markersPoints = emptyArray()
        }

        Log.i(LogTags.OSM_ACTIVITY, "Found geopoints $markersPoints")


        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        enableEdgeToEdge()
        setContent {
            LocalisationHelperTheme {
                Map(
                    latitude = latitude,
                    longitude = longitude,
                    zoom = zoom,
                    markers = markersPoints
                )
            }
        }
    }
}

@Composable
fun Map(
    latitude: Double,
    longitude: Double,
    zoom: Double = 5.0,
    markers: Array<GeoPoint> = emptyArray()
) {
    val context = LocalContext.current

    var cameraState by remember {
        mutableStateOf(
            CameraState(
                CameraProperty(
                    geoPoint = GeoPoint(latitude, longitude),
                    zoom = zoom
                )
            )
        )
    }

    val resources = LocalResources.current

    var markerIcon by remember {
        mutableStateOf(ResourcesCompat.getDrawable(resources, R.drawable.ic_location_icon, null))
    }

    // This blocks the camera from zooming in again
    // https://github.com/utsmannn/osm-android-compose/issues/4
    LaunchedEffect(cameraState.zoom) {
        val zoom = cameraState.zoom
        val geoPoint = cameraState.geoPoint
        cameraState = CameraState(
            CameraProperty(
                geoPoint = geoPoint,
                zoom = zoom
            )
        )
    }


    OpenStreetMap(
        modifier = Modifier.fillMaxSize(),
        cameraState = cameraState,
        onFirstLoadListener = {
            Toast.makeText(
                context,
                R.string.back_button_exit,
                Toast.LENGTH_LONG
            ).show()
        }
    ) {
        markers.forEach { markerGeoPoint ->
            Marker(
                state = rememberMarkerState(
                    geoPoint = markerGeoPoint
                ),
                icon = markerIcon
            ) {
                // TODO: add text telling that's the phone's position
                // with th given coordinates
            }
        }
    }
}