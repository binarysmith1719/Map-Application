package com.codezilla.mapbox3

import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.extension.style.expressions.dsl.generated.interpolate
import com.mapbox.maps.extension.style.expressions.dsl.generated.literal
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.eq
import com.mapbox.maps.extension.style.expressions.generated.Expression.Companion.get
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.*
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.MapProjection
import com.mapbox.maps.plugin.animation.CameraAnimatorOptions.Companion.cameraAnimatorOptions
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.AnnotationPlugin
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.viewannotation.ViewAnnotationManager
import com.mapbox.maps.viewannotation.viewAnnotationOptions


//import com.mapbox.maps.mapBox3.databinding.ActivityFillExtrusionBinding
class MainActivity : AppCompatActivity() {
    var mapView: MapView? = null
    var mapboxMap:MapboxMap?=null
    var annotationApi:AnnotationPlugin?=null
    lateinit var annotaionConfig : AnnotationConfig
    val layerID = "map_annotation";
    var pointAnnotationManager : PointAnnotationManager? = null
    private lateinit var viewAnnotationManager: ViewAnnotationManager
    var pointAnnotation:PointAnnotation?=null
//    val pluginId:String="plugin_id"

    private val EARTHQUAKE_SOURCE_URL = "https://www.mapbox.com/mapbox-gl-js/assets/earthquakes.geojson"
    private val EARTHQUAKE_SOURCE_ID = "earthquakes"
    private val HEATMAP_LAYER_ID = "earthquakes-heat"
    private val HEATMAP_LAYER_SOURCE = "earthquakes"
    private val CIRCLE_LAYER_ID = "earthquakes-circle"
    private val GEOJSON_SOURCE_ID = "line"
    val JSON_SOURCE_ID="linenew"

    private  val LATITUDE = -122.486052
    private  val LONGITUDE = 37.830348
    private  val ZOOM = 14.0
    private var Switch= 0
    private val originLocation = Location("test").apply {
        longitude = -122.4192
        latitude = 37.7627
        bearing = 10f
    }
    private val destination:Point = Point.fromLngLat(-122.4106, 37.7676)
    public var queue:PointQueue?=null
    public var lqueue:LineQueue?=null
    public var squeue:SrcQueue?=null
    var value:Int=2
    var qsize:Int=1000
    var lineEnabled=true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val actionBar: ActionBar?
        actionBar = supportActionBar
        val colorDrawable = ColorDrawable(Color.parseColor("#6CA0DC"))
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(colorDrawable)
        }

        mapView=findViewById<View>(R.id.mapView) as MapView
        var floatact =findViewById<View>(R.id.fab_style_toggle)
        queue =PointQueue(qsize)
        lqueue = LineQueue(qsize-1)
        squeue= SrcQueue(qsize-1)

        mapboxMap = mapView?.getMapboxMap();
//        var gesturesPlugin : GesturesPlugin? = mapPluginProviderDelegate?.gestures
//        gesturesPlugin?.addOnMapClickListener(this)
        val shpf = getSharedPreferences(MAIN_KEY, MODE_PRIVATE)
        value=shpf.getInt("style",2)

        mapView?.getMapboxMap()?.loadStyleUri(if(value==1)
        {Style.DARK} else if(value==2) { Style.MAPBOX_STREETS } else if(value==3)
        {Style.SATELLITE_STREETS}else{Style.TRAFFIC_DAY}
            ,object :Style.OnStyleLoaded{
            override fun onStyleLoaded(style: Style) {
                initiateCamera()
                annotationApi = mapView?.annotations
                annotaionConfig = AnnotationConfig(layerId = layerID)
                pointAnnotationManager = annotationApi?.createPointAnnotationManager(annotaionConfig)!!
                managingOnPointAnnotation()
                try{
                    mapView!!.gestures.pitchEnabled =false
                }catch (e : Exception){
                    e.printStackTrace()
                }
            }
        })
//        setup3DBuildings()

        mapView!!.gestures.addOnMapClickListener { point ->
            if(Switch==1) //1 Means a thread is in process
            {true}
            Switch=1

            if(queue!!.getSize()==queue!!.list.size)
            {
                if(lqueue!!.getSize()==lqueue!!.list.size)
                {Toast.makeText(this, "LQUEUE FULL!! (Delete Some)", Toast.LENGTH_SHORT).show()}

                Toast.makeText(this, "QUEUE FULL!! (Delete Some)", Toast.LENGTH_SHORT).show()
                Switch=0
                true
            }
            else{
            addNewPoint(point)
                if(lineEnabled==true)
                {addNewline(point)}
            }
            true
        }
    }


    //ON LAYER BUTTON CLICK
    public fun onActionbtnclick2(view:View)
    {

        if(Switch==1) return
        Switch=1
        mapView?.getMapboxMap()?.getStyle()
            {
                var sl:String=lqueue!!.dequeue()
                var ss:String=squeue!!.dequeue()
                it.removeStyleLayer(sl)
                it.removeStyleSource(ss)
                queue!!.dequeue()
            }
        Switch=0
    }

    private fun setup3DBuildings() {
        mapboxMap!!.loadStyle(
                style (styleUri = if(value==1)
                {Style.DARK} else if(value==2) { Style.MAPBOX_STREETS } else if(value==3)
                {Style.SATELLITE_STREETS}else{Style.TRAFFIC_DAY}){
                    // Specify a unique string as the layer ID (LAYER_ID)
                    // and assign the source ID added above.
                    +fillExtrusionLayer("3d-buildings", "composite") {
                        fillExtrusionHeight(150.0)
                        fillExtrusionBase(1.5)
                        fillExtrusionOpacity(1.0)
                        filter(eq(get("extrude"), literal("true")))
                        minZoom(5.0)
                        fillExtrusionColor(Color.parseColor("#ff03dac5"))
                        sourceLayer("building")
                    }
                }
            )
    }
    var strnum=0
    var clr=arrayOf("70f","ff4","f18","999","00f","0f0","f00","70f","82c","0af","7fd","bc4","f50","c04","818","333","222")
    var s:Int=0
    var x=0
    fun addNewline(point1:Point) {

//        Log.d("tag1", "addNewline entered1 ")
//        Toast.makeText(this,"${queue!!.getSize()}",Toast.LENGTH_SHORT).show()

        if(queue!!.getSize()==0 || queue!!.isEmpty() == true)
        {   moveCamera(point1)
            queue?.enqueue(point1)
            return}
        if(x==15||x==14)x=0
        var clrx:String?="#"+clr[x]
        var strl="l+$strnum"
        var strs="s+$strnum"
        strnum++
        Log.d("tag1", "addNewline entered2 ")
//        Toast.makeText(this,"layer",Toast.LENGTH_SHORT).show()
        var point2:Point= queue!!.getPoint(queue!!.tail)

        var lyr:String=strl
        var sor:String=strs
        mapView?.getMapboxMap()?.getStyle {
            // Create a list to store our line coordinates.
            val routeCoordinates = ArrayList<Point>()

            lqueue!!.enqueue(strl)
            squeue!!.enqueue(strs)
            queue!!.enqueue(point1)
            routeCoordinates.add(point1)
            routeCoordinates.add(Point.fromLngLat(point2.longitude()-0.0001,point2.latitude()-0.0001))

            sor=squeue!!.getPoint(squeue!!.tail)
            lyr=lqueue!!.getPoint(lqueue!!.tail)

            val lineString = LineString.fromLngLats(routeCoordinates)
            val feature = Feature.fromGeometry(lineString)
            val geoJsonSource = GeoJsonSource(GeoJsonSource.Builder(sor))

            geoJsonSource.feature(feature)
            it.addSource(geoJsonSource)
        }
//        var clr2=clr[s]
        mapView?.getMapboxMap()?.getStyle {
//                    style(styleUri = Style.MAPBOX_STREETS) {
            it.addLayer(   lineLayer(lyr, sor) {
                lineCap(LineCap.ROUND)
                lineJoin(LineJoin.ROUND)
                lineOpacity(0.7)
                lineWidth(8.0)
                lineColor(clrx!!)

            }
            )
            x++
            Toast.makeText(this, "Qsize ${queue!!.getSize()}", Toast.LENGTH_SHORT).show()
            Switch=0;
        }

        moveCamera(point1)
    }
    private fun addRuntimeLayers(style: Style) {
        style.addSource(createEarthquakeSource())
        style.addLayerAbove(createHeatmapLayer(), "waterway-label")
        style.addLayerBelow(createCircleLayer(), HEATMAP_LAYER_ID)
    }

    //ADDS THE ANNOTATION ON USER'S CLICK , AT THE CLICKED LOCATION

    var pntAnnoList:Array<PointAnnotation>? = null
    var viewPntAnnoList =ArrayList<View>()
    var pointList=ArrayList<Point>()
//    var tagList
    fun addNewPoint(point:Point)
    {
        pointList.add(point)
        currentPos=pointList.size-1;

        if(currentPos!=0) {
            var view = viewPntAnnoList.get(currentPos-1)
            view.visibility=View.INVISIBLE
        }
        var bitmap = convertDrawableToBitmap(AppCompatResources.getDrawable(this@MainActivity, R.drawable.marker))
        var pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(bitmap!!)
        pointAnnotation = pointAnnotationManager?.create(pointAnnotationOptions)

        pntAnnoList!![currentPos]= pointAnnotation!!

        viewAnnotationManager = mapView!!.viewAnnotationManager
        var viewAnnotation:View = viewAnnotationManager.addViewAnnotation(
            resId = R.layout.view_annot_layout,
            options = viewAnnotationOptions {
                geometry(point)
                associatedFeatureId(pointAnnotation?.featureIdentifier)
                anchor(ViewAnnotationAnchor.BOTTOM)
                offsetY((pointAnnotation?.iconImageBitmap?.height!!).toInt())
            }
        )
        viewPntAnnoList.add(viewAnnotation)
        var txt1: TextView? = viewAnnotation.findViewById<View>(R.id.annotation1) as TextView
        var txt2: TextView? = viewAnnotation.findViewById<View>(R.id.annotation2) as TextView
        var button:Button? = viewAnnotation.findViewById<Button>(R.id.button) as Button
        txt1?.setText("long ::${String.format("%.4f", point.longitude())}")
        txt2?.setText("lat :: ${String.format("%.4f", point.latitude())}")
//        button?.setTag(55,"currentPos")
        button?.setOnClickListener{view->

            //HANDLE THE ANNOTATION TRAVERSAL AND ANNOTATION DELETION
            Toast.makeText(this,"flag ${currentPos}",Toast.LENGTH_SHORT).show()
//
            val pointAnnotationx = pntAnnoList?.get(currentPos)
            if (pointAnnotationx != null) {
                pointAnnotationManager!!.delete(pointAnnotationx)
            }
            var viewPntAnnoListx =ArrayList<View>()
            var pointListx=ArrayList<Point>()
            var size =pointList.size
            if(currentPos!=size-1) {

                for(i in 0..currentPos-1)
                {
                    viewPntAnnoListx.add(viewPntAnnoList.get(i))
                    pointListx.add(pointList.get(i))
                }
                for (i in currentPos..(size - 2)) {
                    viewPntAnnoListx.add(viewPntAnnoList.get(i+1))
                    pointListx.add(pointList.get(i+1))
                }
                viewPntAnnoList=viewPntAnnoListx
                pointList=pointListx
                currentPos--
            }
            else if(currentPos==0 && size==1)
            {
                viewPntAnnoList=viewPntAnnoListx
                pointList=pointListx
                currentPos=-1
            }
            else if(currentPos==0&& size>1)
            {
                for (i in currentPos..(size - 2)) {
                    viewPntAnnoListx.add(viewPntAnnoList.get(i+1))
                    pointListx.add(pointList.get(i+1))
                }
                viewPntAnnoList=viewPntAnnoListx
                pointList=pointListx
                currentPos==0
            }
            else
            {
                for(i in 0..currentPos-1)
                {
                    viewPntAnnoListx.add(viewPntAnnoList.get(i))
                    pointListx.add(pointList.get(i))
                }
                viewPntAnnoList=viewPntAnnoListx
                pointList=pointListx
                currentPos--
            }
            if(currentPos!=-1){
            viewPntAnnoListx.get(currentPos).visibility=View.VISIBLE
            moveCamera(pointListx.get(currentPos))}
        }
    }

    //TRAVERSING THE ANNOTATIONS
    var currentPos:Int = -1
    fun onLeftClick(view :View)
    { if(currentPos==-1 || currentPos==0)
        { Toast.makeText(this,"No Previous Available !!",Toast.LENGTH_SHORT).show()
            return }
        var view0 = viewPntAnnoList.get(currentPos)
        view0.visibility=View.INVISIBLE
         currentPos--
         var view = viewPntAnnoList.get(currentPos)
         var point= pointList.get(currentPos)
         moveCamera(point)
         view.visibility=View.VISIBLE
    }
    fun onRightClick(view:View)
    {
        if(currentPos==-1 || currentPos==pointList.size-1)
        { Toast.makeText(this," No Next Available !!",Toast.LENGTH_SHORT).show()
            return }
        var view0 = viewPntAnnoList.get(currentPos)
        view0.visibility=View.INVISIBLE
        currentPos++
        var view = viewPntAnnoList.get(currentPos)
        var point= pointList.get(currentPos)
        moveCamera(point)
        view.visibility=View.VISIBLE
    }


    //ADDING ANNOTATION ON START
    fun managingOnPointAnnotation()
    {
        // Log.d("bug", "managing on point annotation")
        var bitmap = convertDrawableToBitmap(AppCompatResources.getDrawable(this@MainActivity, R.drawable.marker))

        var jsonA = JsonObject();
        jsonA.addProperty("anon",126)
        val pointAnnotationOptions: PointAnnotationOptions = PointAnnotationOptions()
            .withPoint(Point.fromLngLat(77.1888,28.6550))
            .withData(jsonA.get("anon"))
            .withIconImage(bitmap!!)

        pointAnnotation=pointAnnotationManager?.create(pointAnnotationOptions)
        pntAnnoList = Array(1000,{init: Int -> pointAnnotation!!})
        //CLICK LISTENER ON THE ANNOTATION
        pointAnnotationManager?.addClickListener(OnPointAnnotationClickListener {annotation:PointAnnotation ->
            onMarkerClick(annotation)
            Toast.makeText(this, "ANNOTATION CLICKED !!", Toast.LENGTH_SHORT).show()
            true
        })
    }

    private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
        if (sourceDrawable == null) {
            return null
        }
        return if (sourceDrawable is BitmapDrawable) {
            sourceDrawable.bitmap
        } else {
// copying drawable object to not manipulate on the same reference
            val constantState = sourceDrawable.constantState ?: return null
            val drawable = constantState.newDrawable().mutate()
            val bitmap: Bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    private fun initiateCamera(){
        mapView!!.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(originLocation.longitude,originLocation.latitude))
                .zoom(12.0)
                .build()
        )
    }
    private fun moveCamera(point:Point){
        mapView!!.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(point)
                .zoom(12.0)
                .build()
        )
    }
    fun cameraFor3Dbuildings(point:Point)
    {
        mapView!!.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(point)
                .zoom(12.0)
                .pitch(45.0)
                .build()
        )

    }
    fun onMarkerClick(annotation: PointAnnotation)
    {
        val jsonElement: JsonElement? = annotation.getData()

        AlertDialog.Builder(this)
            .setTitle("Marker Click")
            .setMessage("Here is the value --> "+jsonElement.toString())
            .setPositiveButton(
                "OK"
            ) { dialog, whichButton ->
                dialog.dismiss()
            }
            .setNegativeButton(
                "Cancel"
            ) { dialog, which -> dialog.dismiss() }.show()
    }
    private fun createEarthquakeSource(): GeoJsonSource {
        return geoJsonSource(EARTHQUAKE_SOURCE_ID) {
            url(EARTHQUAKE_SOURCE_URL)
        }
    }

    //CREATING HEAT_MAP LAYER
    private fun createHeatmapLayer(): HeatmapLayer {
        return heatmapLayer(
            HEATMAP_LAYER_ID,
            EARTHQUAKE_SOURCE_ID
        ) {
            maxZoom(9.0)
            sourceLayer("HEATMAP_LAYER_SOURCE")
// Begin color ramp at 0-stop with a 0-transparancy color
// to create a blur-like effect.
            heatmapColor(
                interpolate {
                    linear()
                    heatmapDensity()
                    stop {
                        literal(0)
                        rgba(33.0, 102.0, 172.0, 0.0)
                    }
                    stop {
                        literal(0.2)
                        rgb(103.0, 169.0, 207.0)
                    }
                    stop {
                        literal(0.4)
                        rgb(209.0, 229.0, 240.0)
                    }
                    stop {
                        literal(0.6)
                        rgb(253.0, 219.0, 240.0)
                    }
                    stop {
                        literal(0.8)
                        rgb(239.0, 138.0, 98.0)
                    }
                    stop {
                        literal(1)
                        rgb(178.0, 24.0, 43.0)
                    }
                }
            )
// Increase the heatmap weight based on frequency and property magnitude
            heatmapWeight(
                interpolate {
                    linear()
                    get { literal("mag") }
                    stop {
                        literal(0)
                        literal(0)
                    }
                    stop {
                        literal(6)
                        literal(1)
                    }
                }
            )
// Increase the heatmap color weight weight by zoom level
// heatmap-intensity is a multiplier on top of heatmap-weight
            heatmapIntensity(
                interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0)
                        literal(1)
                    }
                    stop {
                        literal(9)
                        literal(3)
                    }
                }
            )
// Adjust the heatmap radius by zoom level
            heatmapRadius(
                interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(0)
                        literal(2)
                    }
                    stop {
                        literal(9)
                        literal(20)
                    }
                }
            )
// Transition from heatmap to circle layer by zoom level
            heatmapOpacity(
                interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(7)
                        literal(1)
                    }
                    stop {
                        literal(9)
                        literal(0)
                    }
                }
            )
        }
    }
    //CREATING CIRCLE LAYER
    private fun createCircleLayer(): CircleLayer {
        return circleLayer(
            CIRCLE_LAYER_ID,
            EARTHQUAKE_SOURCE_ID
        ) {
            circleRadius(
                interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(7)
                        interpolate {
                            linear()
                            get { literal("mag") }
                            stop {
                                literal(1)
                                literal(1)
                            }
                            stop {
                                literal(6)
                                literal(4)
                            }
                        }
                    }
                    stop {
                        literal(16)
                        interpolate {
                            linear()
                            get { literal("mag") }
                            stop {
                                literal(1)
                                literal(5)
                            }
                            stop {
                                literal(6)
                                literal(50)
                            }
                        }
                    }
                }
            )
            circleColor(
                interpolate {
                    linear()
                    get { literal("mag") }
                    stop {
                        literal(1)
                        rgba(33.0, 102.0, 172.0, 0.0)
                    }
                    stop {
                        literal(2)
                        rgb(102.0, 169.0, 207.0)
                    }
                    stop {
                        literal(3)
                        rgb(209.0, 229.0, 240.0)
                    }
                    stop {
                        literal(4)
                        rgb(253.0, 219.0, 199.0)
                    }
                    stop {
                        literal(5)
                        rgb(239.0, 138.0, 98.0)
                    }
                    stop {
                        literal(6)
                        rgb(178.0, 24.0, 43.0)
                    }
                }
            )
            circleOpacity(
                interpolate {
                    linear()
                    zoom()
                    stop {
                        literal(7)
                        literal(0)
                    }
                    stop {
                        literal(8)
                        literal(1)
                    }
                }
            )
            circleStrokeColor("white")
            circleStrokeWidth(0.1)
        }
    }

 fun cameraAnimation(targetPoint :Point, initPoint:Point) {
     var CAMERA_TARGET = cameraOptions {
         center(targetPoint)
         zoom(3.0)
     }


     mapView?.camera?.apply {
         val bearing = createBearingAnimator(cameraAnimatorOptions(-45.0)) {
             duration = 4000
             interpolator = AccelerateDecelerateInterpolator()
         }
         val zoom = createZoomAnimator(
             cameraAnimatorOptions(14.0) {
                 startValue(12.0)
             }
         ) {
             duration = 4000
             interpolator = AccelerateDecelerateInterpolator()
         }
         val pitch = createPitchAnimator(
             cameraAnimatorOptions(55.0) {
                 startValue(0.0)
             }
         ) {
             duration = 4000
             interpolator = AccelerateDecelerateInterpolator()
         }
         playAnimatorsSequentially(zoom, pitch, bearing)
     }

 }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.mapmenu, menu)
        return super.onCreateOptionsMenu(menu)
    }
    val MAIN_KEY:String="selected_style"
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val shpf = getSharedPreferences(MAIN_KEY, MODE_PRIVATE)
        val editor = shpf.edit()

        if (item.itemId == R.id.htmap) {
//            lineEnabled=false
//            Toast.makeText(this, "U cant draw lines here ", Toast.LENGTH_SHORT).show()
            mapboxMap!!.apply {
                loadStyleUri(
                    styleUri = if(value==1)
                    {Style.DARK} else if(value==2) { Style.MAPBOX_STREETS } else if(value==3)
                    {Style.SATELLITE_STREETS}else{Style.TRAFFIC_DAY}
                ) { style -> addRuntimeLayers(style) }
                setMapProjection(MapProjection.Globe)

                mapboxMap!!.setCamera(
                        CameraOptions.Builder()
                            .center(Point.fromLngLat(-122.4192,37.7627))
                            .zoom(1.0)
                            .build()
                    )
            }
        } else if (item.itemId == R.id.Map3d) {
            lineEnabled=false
            Toast.makeText(this, "U cant draw lines here ", Toast.LENGTH_SHORT).show()
            Toast.makeText(this, "Zoom-In to see the 3D buildings", Toast.LENGTH_SHORT).show()
            setup3DBuildings()
            cameraFor3Dbuildings(Point.fromLngLat(-122.4192,37.7627))

        } else if(item.itemId == R.id.lineMap){
            lineEnabled=true
            mapView?.getMapboxMap()?.loadStyleUri(if(value==1)
            {Style.DARK} else if(value==2) { Style.MAPBOX_STREETS } else if(value==3)
            {Style.SATELLITE_STREETS}else{Style.TRAFFIC_DAY}
                )
        }
        else   if (item.itemId == R.id.dark) {
            editor.putInt("style", 1)
//            lineEnabled=false
//            Toast.makeText(this, "U cant draw lines here ", Toast.LENGTH_SHORT).show()
            mapboxMap!!.apply {
                loadStyleUri(
                    styleUri = Style.DARK
                ) {}
            }
        } else if (item.itemId == R.id.street) {
            editor.putInt("style", 2)

            mapboxMap!!.apply {
                loadStyleUri(
                    styleUri = Style.MAPBOX_STREETS
                ) {}
            }

        } else if(item.itemId == R.id.sati){
            editor.putInt("style", 3)
            mapboxMap!!.apply {
                loadStyleUri(
                    styleUri = Style.SATELLITE_STREETS
                ) {}
            }
        }else if(item.itemId==R.id.trf)
        {
            editor.putInt("style",4)
            mapboxMap!!.apply {
                loadStyleUri(
                    styleUri = Style.TRAFFIC_DAY
                ) {}
            }
        }
        editor.apply()
        return super.onOptionsItemSelected(item)
    }
     var flagx=0
    fun horizontalView(view:View) {
        if (flagx == 0) {
            var point = mapboxMap?.cameraState?.center
            mapView!!.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(12.0)
                    .pitch(45.0)
                    .build()
            )
            flagx=1
        } else if(flagx==1)
        {
            var point = mapboxMap?.cameraState?.center
            mapView!!.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(12.0)
                    .pitch(0.0)
                    .build())
            flagx=0
        }
    }
    fun globe(view: View)
    {
        mapboxMap?.apply { setMapProjection(MapProjection.Globe)}
        mapboxMap!!.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(-122.4192,37.7627))
                .zoom(1.0)
                .build()
        )
    }
}



