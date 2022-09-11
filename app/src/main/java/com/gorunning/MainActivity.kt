package com.gorunning

import android.Manifest
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.appwidget.AppWidgetManager
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import com.facebook.login.LoginManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.gorunning.Constants.INTERVAL_LOCATION
import com.gorunning.Constants.LIMIT_DISTANCE_ACCEPTED_BIKE
import com.gorunning.Constants.LIMIT_DISTANCE_ACCEPTED_ROLLERSKATE
import com.gorunning.Constants.LIMIT_DISTANCE_ACCEPTED_RUNNING
import com.gorunning.Constants.key_challengeAutofinish
import com.gorunning.Constants.key_challengeDistance
import com.gorunning.Constants.key_challengeDurationHH
import com.gorunning.Constants.key_challengeDurationMM
import com.gorunning.Constants.key_challengeDurationSS
import com.gorunning.Constants.key_challengeNofify
import com.gorunning.Constants.key_hardVol
import com.gorunning.Constants.key_intervalDuration
import com.gorunning.Constants.key_maxCircularSeekBar
import com.gorunning.Constants.key_modeChallenge
import com.gorunning.Constants.key_modeChallengeDistance
import com.gorunning.Constants.key_modeChallengeDuration
import com.gorunning.Constants.key_modeInterval
import com.gorunning.Constants.key_notifyVol
import com.gorunning.Constants.key_progressCircularSeekBar
import com.gorunning.Constants.key_provider
import com.gorunning.Constants.key_runningTime
import com.gorunning.Constants.key_selectedSport
import com.gorunning.Constants.key_softVol
import com.gorunning.Constants.key_userApp
import com.gorunning.Constants.key_walkingTime
import com.gorunning.LoginActivity.Companion.providerSession
import com.gorunning.LoginActivity.Companion.useremail
import com.gorunning.Utility.animateViewofFloat
import com.gorunning.Utility.animateViewofInt
import com.gorunning.Utility.deleteRunAndLinkedData
import com.gorunning.Utility.getFormattedStopWatch
import com.gorunning.Utility.getFormattedTotalTime
import com.gorunning.Utility.getSecFromWatch
import com.gorunning.Utility.roundNumber
import com.gorunning.Utility.setHeightLinearLayout
import me.tankery.lib.circularseekbar.CircularSeekBar
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(),
    NavigationView.OnNavigationItemSelectedListener,
    OnMapReadyCallback,
    GoogleMap.OnMyLocationButtonClickListener, //Herencia obligatoria -> para que el usuario pueda irse a su posicion
    GoogleMap.OnMyLocationClickListener //Herencia obligatoria
{

    companion object{
        lateinit var mainContext: Context

        var activatedGPS: Boolean = true
        lateinit var sportSelected : String

        lateinit var totalsSelectedSport: Totals
        lateinit var totalsBike: Totals
        lateinit var totalsRollerSkate: Totals
        lateinit var totalsRunning: Totals

        val REQUIRED_PERMISSIONS_GPS =
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,  //este permiso va SIEMPRE
                Manifest.permission.ACCESS_FINE_LOCATION)  //este permiso solo cuando se necesite presicion

        var countPhotos: Int = 0
        var lastimage: String = ""

        lateinit var chronoWidget: String
        lateinit var distanceWidget: String

    }

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor


    private var mHandler: Handler? = null
    private var mInterval = 1000
    private var timeInSeconds = 0L
    private var rounds: Int = 1
    private var startButtonClicked = false


    private var widthScreenPixels: Int = 0
    private var heightScreenPixels: Int = 0
    private var widthAnimations: Int = 0

    private lateinit var drawer: DrawerLayout

    private lateinit var csbChallengeDistance: CircularSeekBar
    private lateinit var csbCurrentDistance: CircularSeekBar
    private lateinit var csbRecordDistance: CircularSeekBar

    private lateinit var csbCurrentAvgSpeed: CircularSeekBar
    private lateinit var csbRecordAvgSpeed: CircularSeekBar

    private lateinit var csbCurrentSpeed: CircularSeekBar
    private lateinit var csbCurrentMaxSpeed: CircularSeekBar
    private lateinit var csbRecordSpeed: CircularSeekBar

    private lateinit var tvDistanceRecord: TextView
    private lateinit var tvAvgSpeedRecord: TextView
    private lateinit var tvMaxSpeedRecord: TextView

    private lateinit var tvChrono: TextView
    private lateinit var fbCamara: FloatingActionButton

    private lateinit var swIntervalMode: Switch
    private lateinit var npDurationInterval: NumberPicker
    private lateinit var tvRunningTime: TextView
    private lateinit var tvWalkingTime: TextView
    private lateinit var csbRunWalk: CircularSeekBar

    private lateinit var swChallenges: Switch

    private lateinit var npChallengeDistance: NumberPicker
    private lateinit var npChallengeDurationHH: NumberPicker
    private lateinit var npChallengeDurationMM: NumberPicker
    private lateinit var npChallengeDurationSS: NumberPicker
    private var challengeDistance: Float = 0f
    private var challengeDuration: Int = 0

    private lateinit var cbNotify: CheckBox
    private lateinit var cbAutoFinish: CheckBox

    private lateinit var swVolumes: Switch
    private var mpNotify : MediaPlayer? = null
    private var mpHard : MediaPlayer? = null
    private var mpSoft : MediaPlayer? = null
    private lateinit var sbHardVolume : SeekBar
    private lateinit var sbSoftVolume : SeekBar
    private lateinit var sbNotifyVolume : SeekBar

//    private var stateActivity: Boolean? = null

    private lateinit var sbHardTrack : SeekBar
    private lateinit var sbSoftTrack : SeekBar

    private lateinit var lyPopupRun: LinearLayout

    private var ROUND_INTERVAL = 300
    private var hardTime : Boolean = true
    private var TIME_RUNNING: Int = 0

    private var LIMIT_DISTANCE_ACCEPTED: Double = 0.0
//    private lateinit var sportSelected : String  //TODO fixeado -> se envio al companion para utilizarse en UTILITY.KT

    private lateinit var map: GoogleMap
    private var mapCentered = true
    private lateinit var listPoints: Iterable<LatLng>

    private val PERMISSION_ID = 42
    private val LOCATION_PERMISSION_REQ_CODE = 1000

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var flagSavedLocation = false

    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private var init_lt: Double = 0.0
    private var init_ln: Double = 0.0

    private var distance: Double = 0.0
    private var maxSpeed: Double = 0.0
    private var avgSpeed: Double = 0.0
    private var speed: Double = 0.0

    private var minAltitude: Double? = null
    private var maxAltitude: Double? = null
    private var minLatitude: Double? = null
    private var maxLatitude: Double? = null
    private var minLongitude: Double? = null
    private var maxLongitude: Double? = null

    private lateinit var levelBike: Level
    private lateinit var levelRollerSkate: Level
    private lateinit var levelRunning: Level
    private lateinit var levelSelectedSport: Level

    private lateinit var levelsListBike: ArrayList<Level>
    private lateinit var levelsListRollerSkate: ArrayList<Level>
    private lateinit var levelsListRunning: ArrayList<Level>

    private var sportsLoaded: Int = 0

    private lateinit var dateRun: String
    private lateinit var startTimeRun: String

    private lateinit var medalsListBikeDistance: ArrayList<Double>
    private lateinit var medalsListBikeAvgSpeed: ArrayList<Double>
    private lateinit var medalsListBikeMaxSpeed: ArrayList<Double>

    private lateinit var medalsListRollerSkateDistance: ArrayList<Double>
    private lateinit var medalsListRollerSkateAvgSpeed: ArrayList<Double>
    private lateinit var medalsListRollerSkateMaxSpeed: ArrayList<Double>

    private lateinit var medalsListRunningDistance: ArrayList<Double>
    private lateinit var medalsListRunningAvgSpeed: ArrayList<Double>
    private lateinit var medalsListRunningMaxSpeed: ArrayList<Double>

    private lateinit var medalsListSportSelectedDistance: ArrayList<Double>
    private lateinit var medalsListSportSelectedAvgSpeed: ArrayList<Double>
    private lateinit var medalsListSportSelectedMaxSpeed: ArrayList<Double>

    private var recDistanceGold: Boolean = false
    private var recDistanceSilver: Boolean = false
    private var recDistanceBronze: Boolean = false
    private var recAvgSpeedGold: Boolean = false
    private var recAvgSpeedSilver: Boolean = false
    private var recAvgSpeedBronze: Boolean = false
    private var recMaxSpeedGold: Boolean = false
    private var recMaxSpeedSilver: Boolean = false
    private var recMaxSpeedBronze: Boolean = false

    private lateinit var widget: Widget
    private lateinit var mAppWidgetManager: AppWidgetManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //Toast.makeText(this, "Hola $useremail", Toast.LENGTH_SHORT).show()
        mainContext = this

        initObjects()

        initToolBar()
        initNavigationView()
        initPermissionsGPS()

        initWidget()
        //En este punto se genera otro hilo independiente
        loadFromDB()
    }
    //Inicializar el widget
    private fun initWidget(){
        //Lammar a sus constuctores
        widget = Widget()
        mAppWidgetManager = AppWidgetManager.getInstance(mainContext)!!
        //Actualizar el widget
        updateWidegts()
    }
    //Actualizar el widget (cada 1 segundo)
    private fun updateWidegts(){
        //Darles los valores del crono y la distancia
        chronoWidget = tvChrono.text.toString()
        distanceWidget = roundNumber(distance.toString(),1)

        //Declarar los widgets que haya
        val intent = Intent(application, Widget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE

        val ids = mAppWidgetManager.getAppWidgetIds(ComponentName(application, Widget::class.java))
        //La accion que se haga sera sobre todos los widgets que haya (el usuario puede tener mas de 1 widget)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        //Emision de datos -> este intent le envia la info a todos
        sendBroadcast(intent)  //Por eso en el manifest esta el  <receiver/<intent-filter>/<action -> APPWIDGET_UPDATE
    }

    //Modificar la funcionalidad del BackPressed
    override fun onBackPressed() {
        //super.onBackPressed()

        //Si el popUp esta abierto
        if (lyPopupRun.isVisible) closePopUpRun()
        else {
            //Preguntar si el menu se encuentra desplegado -> cerrarlo
            if (drawer.isDrawerOpen(GravityCompat.START))
                drawer.closeDrawer(GravityCompat.START)
            else
            //SI esta iniciada la carrera
                if (timeInSeconds > 0L) {                       //resetClicked()  //finalizarla la carrera

                    startButtonClicked = false
                    stopTime()
                    manageEnableButtonsRun(true, true)

                    if (hardTime) mpHard?.pause()
                    else mpSoft?.pause()
                }
                //Si ya se encuentra cerrado -> preguntar cerrar sesion
                else alertSignOut()

        }
    }

    //Alert del signOut
    private fun alertSignOut() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.alertSignOutTitle))
            .setMessage(R.string.alertSignOutTDescription)
            .setPositiveButton(android.R.string.ok,
                DialogInterface.OnClickListener { dialog, which ->
                    //boton OK pulsado
                    signOut()
                })
            .setNegativeButton(android.R.string.cancel,
                DialogInterface.OnClickListener { dialog, which ->
                    //boton cancel pulsado
                })
            .setCancelable(true)
            .show()
    }

    //Configurar la toolbar
    private fun initToolBar(){
        //Declarar la variable
        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar_main)
        //Establecerla como toolbar
        setSupportActionBar(toolbar)

        //Administrar el drawer y toggle
        drawer = findViewById(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar, R.string.bar_title,
            R.string.navigation_drawer_close)

        //Añade el listener de menu (activarlo al desplazarse)
        drawer.addDrawerListener(toggle)

        //Sincroniza el listener
        toggle.syncState()
    }
    //Inicializar la cabecera (navigation view)
    private fun initNavigationView(){
        //Guardar en una variable el nav view
        var navigationView: NavigationView = findViewById(R.id.nav_view)
        //Establece listener al menu
        navigationView.setNavigationItemSelectedListener(this)

        //Inflar la cabecera
        var headerView: View = LayoutInflater.from(this).inflate(R.layout.nav_header_main, navigationView, false)
        //Borrar los datos y volver a crearlos, para actualizar los datos, cada vez que se consulta el header
        navigationView.removeHeaderView(headerView)
        navigationView.addHeaderView(headerView)

        //Asociar el mensaje del nombre de usuario en la cabecera
        var tvUser: TextView = headerView.findViewById(R.id.tvUser)
        tvUser.text = useremail
    }


    //Asignar al cronometro una cadena de inicio
    private fun initStopWatch() {
        tvChrono.text = getString(R.string.init_stop_watch_value)
    }
    //Configuracion del cronometro
    private fun initChrono(){
        tvChrono = findViewById(R.id.tvChrono)
        tvChrono.setTextColor(ContextCompat.getColor( this, R.color.white))
        initStopWatch()

        //Progreso de la barra debajo del cronometro
        widthScreenPixels = resources.displayMetrics.widthPixels
        heightScreenPixels = resources.displayMetrics.heightPixels

        widthAnimations = widthScreenPixels

        val lyChronoProgressBg = findViewById<LinearLayout>(R.id.lyChronoProgressBg)
        val lyRoundProgressBg = findViewById<LinearLayout>(R.id.lyRoundProgressBg)
        lyChronoProgressBg.translationX = -widthAnimations.toFloat()
        lyRoundProgressBg.translationX = -widthAnimations.toFloat()

        //Administrar el tv del reset
        val tvReset: TextView = findViewById(R.id.tvReset)
        tvReset.setOnClickListener { resetClicked()  }

        //Camara desactivada al inicio
        fbCamara = findViewById(R.id.fbCamera)
        fbCamara.isVisible = false
    }
    //Configuracion y valores de los layouts
    private fun hideLayouts(){
        //Padre/Hijo
        var lyMap = findViewById<LinearLayout>(R.id.lyMap)
        var lyFragmentMap = findViewById<LinearLayout>(R.id.lyFragmentMap)
        val lyIntervalModeSpace = findViewById<LinearLayout>(R.id.lyIntervalModeSpace)
        val lyIntervalMode = findViewById<LinearLayout>(R.id.lyIntervalMode)
        val lyChallengesSpace = findViewById<LinearLayout>(R.id.lyChallengesSpace)
        val lyChallenges = findViewById<LinearLayout>(R.id.lyChallenges)
        val lySettingsVolumesSpace = findViewById<LinearLayout>(R.id.lySettingsVolumesSpace)
        val lySettingsVolumes = findViewById<LinearLayout>(R.id.lySettingsVolumes)

        var lySoftTrack = findViewById<LinearLayout>(R.id.lySoftTrack)
        var lySoftVolume = findViewById<LinearLayout>(R.id.lySoftVolume)

        //Lamando a la fun de la Utility -> para pasarle los parametros al layout
        //A los padres -> valor cero
        setHeightLinearLayout(lyMap, 0)
        setHeightLinearLayout(lyIntervalModeSpace,0)
        setHeightLinearLayout(lyChallengesSpace,0)
        setHeightLinearLayout(lySettingsVolumesSpace,0)
        setHeightLinearLayout(lySoftTrack,0)
        setHeightLinearLayout(lySoftVolume,0)

        //A los hijos -> traslation -300f
        lyFragmentMap.translationY = -300f
        lyIntervalMode.translationY = -300f
        lyChallenges.translationY = -300f
        lySettingsVolumes.translationY = -300f
    }
    //Inicializacion de metricas
    private fun initMetrics(){
        csbCurrentDistance = findViewById(R.id.csbCurrentDistance)
        csbChallengeDistance = findViewById(R.id.csbChallengeDistance)
        csbRecordDistance = findViewById(R.id.csbRecordDistance)

        csbCurrentAvgSpeed = findViewById(R.id.csbCurrentAvgSpeed)
        csbRecordAvgSpeed = findViewById(R.id.csbRecordAvgSpeed)

        csbCurrentSpeed = findViewById(R.id.csbCurrentSpeed)
        csbCurrentMaxSpeed = findViewById(R.id.csbCurrentMaxSpeed)
        csbRecordSpeed = findViewById(R.id.csbRecordSpeed)

        csbCurrentDistance.progress = 0f
        csbChallengeDistance.progress = 0f

        csbCurrentAvgSpeed.progress = 0f

        csbCurrentSpeed.progress = 0f
        csbCurrentMaxSpeed.progress = 0f

        tvDistanceRecord = findViewById(R.id.tvDistanceRecord)
        tvAvgSpeedRecord = findViewById(R.id.tvAvgSpeedRecord)
        tvMaxSpeedRecord = findViewById(R.id.tvMaxSpeedRecord)

        tvDistanceRecord.text = ""
        tvAvgSpeedRecord.text = ""
        tvMaxSpeedRecord.text = ""
    }
    //Declaracion de los Switchs
    private fun initSwitchs(){
        swIntervalMode = findViewById(R.id.swIntervalMode)
        swChallenges = findViewById(R.id.swChallenges)
        swVolumes = findViewById(R.id.swVolumes)
    }
    //Configuracion "Carrera con Intervalos"
    private fun initIntervalMode(){
        npDurationInterval = findViewById(R.id.npDurationInterval)
        tvRunningTime = findViewById(R.id.tvRunningTime)
        tvWalkingTime = findViewById(R.id.tvWalkingTime)
        csbRunWalk = findViewById(R.id.csbRunWalk)

        //Valores del 1 al 60, con intervalos de 5
        npDurationInterval.minValue = 1
        npDurationInterval.maxValue = 60
        npDurationInterval.value = 5
        npDurationInterval.wrapSelectorWheel = true
        //Formato de 2 cifras
        npDurationInterval.setFormatter(NumberPicker.Formatter { i -> String.format("%02d", i) })

        //Cada vez que cambie el valor en el number pickers
        npDurationInterval.setOnValueChangedListener { picker, oldVal, newVal ->
            csbRunWalk.max = (newVal*60).toFloat()
            csbRunWalk.progress = csbRunWalk.max/2

            //Congifurar los tv
            tvRunningTime.text = getFormattedStopWatch(((newVal*60/2)*1000).toLong()).subSequence(3,8)
            tvWalkingTime.text = tvRunningTime.text

            //Configurar el semicirculo
            ROUND_INTERVAL = newVal * 60
            TIME_RUNNING = ROUND_INTERVAL / 2
        }

        csbRunWalk.max = 300f
        csbRunWalk.progress = 150f
        csbRunWalk.setOnSeekBarChangeListener(object :
            CircularSeekBar.OnCircularSeekBarChangeListener {
            //Barra semi circular
            override fun onProgressChanged(circularSeekBar: CircularSeekBar,progress: Float,fromUser: Boolean) {

                if (fromUser){
                    var STEPS_UX: Int = 15
                    //Si la ronda es mayor 10 min (600) -> se cambie cada 1 min (60)
                    if (ROUND_INTERVAL > 600) STEPS_UX = 60
                    //Si la ronda es mayor 30 min (1800) -> se cambie cada 5 min (300)
                    if (ROUND_INTERVAL > 1800) STEPS_UX = 300
                    var set: Int = 0
                    var p = progress.toInt()

                    var limit = 60
                    if (ROUND_INTERVAL > 1800) limit = 300

                    if (p%STEPS_UX != 0 && progress != csbRunWalk.max){
                        while (p >= limit) p -= limit
                        while (p >= STEPS_UX) p -= STEPS_UX
                        if (STEPS_UX-p > STEPS_UX/2) set = -1 * p
                        else set = STEPS_UX-p

                        if (csbRunWalk.progress + set > csbRunWalk.max)
                            csbRunWalk.progress = csbRunWalk.max
                        else
                            csbRunWalk.progress = csbRunWalk.progress + set

                    }

                    //En caso de que el usuario haya marcado el intervalo de correr en CERO -> no aceptar ese modo de uso
                    if (csbRunWalk.progress == 0f) manageEnableButtonsRun(false, false)
                    else manageEnableButtonsRun(false, true)
                }

                //tv de "Run"
                tvRunningTime.text = getFormattedStopWatch((csbRunWalk.progress.toInt() *1000).toLong()).subSequence(3,8)
                //tv de "Walk"
                tvWalkingTime.text = getFormattedStopWatch(((ROUND_INTERVAL- csbRunWalk.progress.toInt())*1000).toLong()).subSequence(3,8)
                //Actualizar el Time Running
                TIME_RUNNING = getSecFromWatch(tvRunningTime.text.toString())
            }

            override fun onStopTrackingTouch(seekBar: CircularSeekBar) {
            }

            override fun onStartTrackingTouch(seekBar: CircularSeekBar) {
            }
        })
    }
    //Configuracion del challenge
    private fun initChallengeMode(){
        npChallengeDistance = findViewById(R.id.npChallengeDistance)
        npChallengeDurationHH = findViewById(R.id.npChallengeDurationHH)
        npChallengeDurationMM = findViewById(R.id.npChallengeDurationMM)
        npChallengeDurationSS = findViewById(R.id.npChallengeDurationSS)

        npChallengeDistance.minValue = 1
        npChallengeDistance.maxValue = 300
        npChallengeDistance.value = 10
        npChallengeDistance.wrapSelectorWheel = true


        //Cada vez que cambie el numere pickers
        npChallengeDistance.setOnValueChangedListener { picker, oldVal, newVal ->
            challengeDistance = newVal.toFloat()
            csbChallengeDistance.max = newVal.toFloat()
            csbChallengeDistance.progress = newVal.toFloat()
            challengeDuration = 0

            //Actualizar el max, en cado de que sea mayor al actual record
            if (csbChallengeDistance.max > csbRecordDistance.max)
                csbCurrentDistance.max = csbChallengeDistance.max
        }

        //Horas (valor = 1, como un reto por defecto)
        npChallengeDurationHH.minValue = 0
        npChallengeDurationHH.maxValue = 23
        npChallengeDurationHH.value = 1
        npChallengeDurationHH.wrapSelectorWheel = true
        npChallengeDurationHH.setFormatter(NumberPicker.Formatter { i -> String.format("%02d", i) })

        //Minutos
        npChallengeDurationMM.minValue = 0
        npChallengeDurationMM.maxValue = 59
        npChallengeDurationMM.value = 0
        npChallengeDurationMM.wrapSelectorWheel = true
        npChallengeDurationMM.setFormatter(NumberPicker.Formatter { i -> String.format("%02d", i) })

        //Segundos
        npChallengeDurationSS.minValue = 0
        npChallengeDurationSS.maxValue = 59
        npChallengeDurationSS.value = 0
        npChallengeDurationSS.wrapSelectorWheel = true
        npChallengeDurationSS.setFormatter(NumberPicker.Formatter { i -> String.format("%02d", i) })

        //Ver el reto que ingreso el usuario (el tiempo en el number picker)
        npChallengeDurationHH.setOnValueChangedListener { picker, oldVal, newVal ->
            getChallengeDuration(newVal, npChallengeDurationMM.value, npChallengeDurationSS.value)
        }
        npChallengeDurationMM.setOnValueChangedListener { picker, oldVal, newVal ->
            getChallengeDuration(npChallengeDurationHH.value, newVal, npChallengeDurationSS.value)
        }
        npChallengeDurationSS.setOnValueChangedListener { picker, oldVal, newVal ->
            getChallengeDuration(npChallengeDurationHH.value, npChallengeDurationMM.value, newVal)
        }
        cbNotify = findViewById<CheckBox>(R.id.cbNotify)
        cbAutoFinish = findViewById<CheckBox>(R.id.cbAutoFinish)
    }

    //Establecer los volumenes
    private fun setVolumes(){
        //Establecer que hacer cuando el usuario cambie la barra de volumen del sbHardVolume
        sbHardVolume.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            //Establecer volumen izquierdo y derecho
            override fun onProgressChanged(p0: SeekBar?, i: Int, p2: Boolean) {
                mpHard?.setVolume(i/100.0f, i/100.0f)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) { }
            override fun onStopTrackingTouch(p0: SeekBar?) { }
        })
        //Establecer que hacer cuando el usuario cambie la barra de volumen del sbSoftVolume
        sbSoftVolume.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, i: Int, p2: Boolean) {
                mpSoft?.setVolume(i/100.0f, i/100.0f)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) { }
            override fun onStopTrackingTouch(p0: SeekBar?) { }
        })
        //Establecer que hacer cuando el usuario cambie la barra de volumen del sbNotifyVolume
        sbNotifyVolume.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, i: Int, p2: Boolean) {
                mpNotify?.setVolume(i/100.0f, i/100.0f)
            }
            override fun onStartTrackingTouch(p0: SeekBar?) { }
            override fun onStopTrackingTouch(p0: SeekBar?) { }
        })
    }
    //Administrar los tracks
    private fun updateTimesTrack(timesH: Boolean, timesS: Boolean){
//        if (timesH){
//           val tvHardPosition = findViewById<TextView>(R.id.tvHardPosition)
//           val tvHardRemaining = findViewById<TextView>(R.id.tvHardRemaining)
//           tvHardPosition.text = getFormattedStopWatch(mpHard!!.currentPosition.toLong())
//           tvHardRemaining.text = "-" + getFormattedStopWatch( mpHard!!.duration.toLong() - sbHardTrack.progress.toLong())
//        }
//        if (timesS){
//           val tvSoftPosition = findViewById<TextView>(R.id.tvSoftPosition)
//           val tvSoftRemaining = findViewById<TextView>(R.id.tvSoftRemaining)
//           tvSoftPosition.text = getFormattedStopWatch(mpSoft!!.currentPosition.toLong())
//           tvSoftRemaining.text = "-" + getFormattedStopWatch( mpSoft!!.duration.toLong() - sbSoftTrack.progress.toLong())
//        }
        val sbHardTrack = findViewById<SeekBar>(R.id.sbHardTrack)
        val sbSoftTrack = findViewById<SeekBar>(R.id.sbSoftTrack)

        //Si update Hard
        if (timesH){
            val tvHardPosition = findViewById<TextView>(R.id.tvHardPosition)
            val tvHardRemaining = findViewById<TextView>(R.id.tvHardRemaining)
            //Pasar a string la duracion
            tvHardPosition.text = getFormattedStopWatch(sbHardTrack.progress.toLong())
            //Duracion menos el progress = lo que queda de pista
            tvHardRemaining.text = "-" + getFormattedStopWatch( mpHard!!.duration.toLong() - sbHardTrack.progress.toLong())
        }
        //Si update Soft
        if (timesS){
            val tvSoftPosition = findViewById<TextView>(R.id.tvSoftPosition)
            val tvSoftRemaining = findViewById<TextView>(R.id.tvSoftRemaining)
            tvSoftPosition.text = getFormattedStopWatch(sbSoftTrack.progress.toLong())
            tvSoftRemaining.text = "-" + getFormattedStopWatch( mpSoft!!.duration.toLong() - sbSoftTrack.progress.toLong())
        }
    }
    //Control de la pista musical
    private fun setProgressTracks(){
        val sbHardTrack = findViewById<SeekBar>(R.id.sbHardTrack)
        val sbSoftTrack = findViewById<SeekBar>(R.id.sbSoftTrack)
        //Maximo del sb = lo que dure la pista
        sbHardTrack.max = mpHard!!.duration
        sbSoftTrack.max = mpSoft!!.duration
//        sbHardTrack.isEnabled = false
//        sbSoftTrack.isEnabled = false
        //Actualizar los tracks
        updateTimesTrack(true, true)

        //Implementacion cuando el usuario eliga un punto de la barra mpHard -> se deplace a ese punto de la pista
        sbHardTrack.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, i: Int, fromUser: Boolean) {
                //Que se ejecute cuando lo haga el usuario
                if (fromUser){
                    //Pausar la pista
                    mpHard?.pause()
                    //Desplazar hacia el punto elegido por el usuario
                    mpHard?.seekTo(i)
                    //Reanudar la pista
                    mpHard?.start()

                    if (!(timeInSeconds > 0L && hardTime && startButtonClicked)) mpHard?.pause()

                    //Actualizar los tracks -> solo Hard
                    updateTimesTrack(true, false)
                }
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })

        //Implementacion cuando el usuario eliga un punto de la barra mpSoft -> se deplace a ese punto de la pista
        sbSoftTrack.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, i: Int, fromUser: Boolean) {
                //Que se ejecute cuando lo haga el usuario
                if (fromUser){
                    //Pausar la pista
                    mpSoft?.pause()
                    //Desplazar hacia el punto elegido por el usuario
                    mpSoft?.seekTo(i)
                    //Reanudar la pista
                    mpSoft?.start()
                    if (!(timeInSeconds > 0L && !hardTime && startButtonClicked)) mpSoft?.pause()

                    //Actualizar los tracks -> solo Soft
                    updateTimesTrack(false, true)
                }
            }
            override fun onStartTrackingTouch(p0: SeekBar?) {}
            override fun onStopTrackingTouch(p0: SeekBar?) {}
        })
    }
    //Administrar media players
    private fun initMusic(){
        //Media players con sus recursos correspondientes
        mpNotify = MediaPlayer.create(this, R.raw.micmic)
        mpHard = MediaPlayer.create(this, R.raw.hard_music)
        mpSoft = MediaPlayer.create(this, R.raw.soft_music)

        //Hacer un bucle para que cuando termine vuelva a comenzar
        mpHard?.isLooping = true
        mpSoft?.isLooping = true
        mpNotify?.isLooping = false

        //Administrar el volumen
        sbHardVolume = findViewById(R.id.sbHardVolume)
        sbSoftVolume = findViewById(R.id.sbSoftVolume)
        sbNotifyVolume = findViewById(R.id.sbNotifyVolume)

        sbHardTrack = findViewById(R.id.sbHardTrack)
        sbHardTrack.isEnabled = false
        sbSoftTrack = findViewById(R.id.sbSoftTrack)
        sbSoftTrack.isEnabled = false

        setVolumes()
        setProgressTracks()
    }

    //Iniciar el media player de notificacion
    private fun notifySound(){
        mpNotify?.start()
    }

    //Inicializar los objetos
    private fun initObjects(){
        initChrono()
        hideLayouts()
        initMetrics()
        initSwitchs()
        initIntervalMode()
        initChallengeMode()
        initMusic()
        hidePopUpRun()

        initMap()

        initTotals()
        initLevels()
        initMedals()

        initPreferences()
        recoveryPreferences()
    }
    //Configurar totales
    private fun initTotals(){
        totalsBike = Totals()
        totalsRollerSkate = Totals()
        totalsRunning = Totals()

        totalsBike.totalRuns = 0
        totalsBike.totalDistance = 0.0
        totalsBike.totalTime = 0
        totalsBike.recordDistance = 0.0
        totalsBike.recordSpeed = 0.0
        totalsBike.recordAvgSpeed = 0.0

        totalsRollerSkate.totalRuns = 0
        totalsRollerSkate.totalDistance = 0.0
        totalsRollerSkate.totalTime = 0
        totalsRollerSkate.recordDistance = 0.0
        totalsRollerSkate.recordSpeed = 0.0
        totalsRollerSkate.recordAvgSpeed = 0.0

        totalsRunning.totalRuns = 0
        totalsRunning.totalDistance = 0.0
        totalsRunning.totalTime = 0
        totalsRunning.recordDistance = 0.0
        totalsRunning.recordSpeed = 0.0
        totalsRunning.recordAvgSpeed = 0.0
    }
    //Configurar totales
    private fun initLevels(){
        //Invocar al constructor
        levelSelectedSport = Level()
        levelBike = Level()
        levelRollerSkate = Level()
        levelRunning = Level()

        //Limpiar arrays
        levelsListBike = arrayListOf()
        levelsListBike.clear()

        levelsListRollerSkate = arrayListOf()
        levelsListRollerSkate.clear()

        levelsListRunning = arrayListOf()
        levelsListRunning.clear()

        //Nivel 1 de cada deporte
        levelBike.name = "turtle"
        levelBike.image = "level_1"
        levelBike.RunsTarget = 5
        levelBike.DistanceTarget = 50

        levelRollerSkate.name = "turtle"
        levelRollerSkate.image = "level_1"
        levelRollerSkate.RunsTarget = 5
        levelRollerSkate.DistanceTarget = 5

        levelRunning.name = "turtle"
        levelRunning.image = "level_1"
        levelRunning.RunsTarget = 1
        levelRunning.DistanceTarget = 1
    }
    //Configurar el medallero+
    private fun initMedals(){
        //Limpiar todos los arrays
        medalsListSportSelectedDistance = arrayListOf()
        medalsListSportSelectedAvgSpeed = arrayListOf()
        medalsListSportSelectedMaxSpeed = arrayListOf()
        medalsListSportSelectedDistance.clear()
        medalsListSportSelectedAvgSpeed.clear()
        medalsListSportSelectedMaxSpeed.clear()

        medalsListBikeDistance = arrayListOf()
        medalsListBikeAvgSpeed = arrayListOf()
        medalsListBikeMaxSpeed = arrayListOf()
        medalsListBikeDistance.clear()
        medalsListBikeAvgSpeed.clear()
        medalsListBikeMaxSpeed.clear()

        medalsListRollerSkateDistance = arrayListOf()
        medalsListRollerSkateAvgSpeed = arrayListOf()
        medalsListRollerSkateMaxSpeed = arrayListOf()
        medalsListRollerSkateDistance.clear()
        medalsListRollerSkateAvgSpeed.clear()
        medalsListRollerSkateMaxSpeed.clear()

        medalsListRunningDistance = arrayListOf()
        medalsListRunningAvgSpeed = arrayListOf()
        medalsListRunningMaxSpeed = arrayListOf()
        medalsListRunningDistance.clear()
        medalsListRunningAvgSpeed.clear()
        medalsListRunningMaxSpeed.clear()
    }
    //Resetear el medallero
    private fun resetMedals(){
        recDistanceGold = false
        recDistanceSilver = false
        recDistanceBronze = false
        recAvgSpeedGold = false
        recAvgSpeedSilver = false
        recAvgSpeedBronze = false
        recMaxSpeedGold = false
        recMaxSpeedSilver = false
        recMaxSpeedBronze = false
    }
    //Carga de datos de la DB

    private fun loadFromDB(){
        loadTotalsUser()
        loadMedalsUser()
    }
    //Cargar Totales del usuario de cada deporte
    private fun loadTotalsUser(){
        //Carga de totales de cada coleccion diferente
        loadTotalSport("Bike")
        loadTotalSport("RollerSkate")
        loadTotalSport("Running")
    }
    //Carga de datos para los totales
    private fun loadTotalSport(sport: String){
        //Variable concatenada para cada deporte
        var collection = "totals$sport"
        //Variable donde tener la instancia
        var dbTotalsUser = FirebaseFirestore.getInstance()
        //Ruta de las colecciones (Firebase) -> Documento
        dbTotalsUser.collection(collection).document(useremail)
            .get()  //Capturar la info
            //Ver si lo recibio con exito o no
            .addOnSuccessListener { document ->
                //Si tiene datos -> guardarlos
                if (document.data?.size != null){
                    //Recibimos un documento (datos) -> transformarlo a data class
                    var total = document.toObject(Totals::class.java)
                    when (sport){
                        //Los datos inicializados con valor "0" -> ahora sean los datos capturados de la base de datos
                        "Bike" -> totalsBike = total!!
                        "RollerSkate" -> totalsRollerSkate = total!!
                        "Running" -> totalsRunning = total!!
                    }
                }
                //Si no -> crear los datos (escribir los datos)
                else{
                    //Instancia de Firestore
                    val dbTotal: FirebaseFirestore = FirebaseFirestore.getInstance()
                    //Crear el documento en la coleccion
                    dbTotal.collection(collection).document(useremail).set(hashMapOf(
                        "recordAvgSpeed" to 0.0,
                        "recordDistance" to 0.0,
                        "recordSpeed" to 0.0,
                        "totalDistance" to 0.0,
                        "totalRuns" to 0,
                        "totalTime" to 0
                    ))
                }
                //Saber en que nivel se encuentra
                //TODO esta fun se ejecuta en este lugar, en el addOnSuccessListener, porque estamos seguros de que SI tiene datos -> en otro hilo podria fallar
                sportsLoaded++
                setLevelSport(sport)
                //Cuando hayan cargado los 3 deportes
                if (sportsLoaded == 3) selectSport(sportSelected)
            }
            //En caso de fallo
            .addOnFailureListener { exception ->
                Log.d("ERROR loadTotalsUser", "get failed with ", exception)
            }
    }
    //Nivel de los deportes
    private fun setLevelSport(sport: String){
        //Instancia para operar en la BD
        val dbLevels: FirebaseFirestore = FirebaseFirestore.getInstance()
        //Ruta de las colecciones (Firebase) -> Coleccion completa
        dbLevels.collection("levels$sport")
            .get()
            .addOnSuccessListener { documents ->
                //Recibimos un conjunto de documentos
                for (document in documents){
                    when (sport){
                        //A cada documento (array) -> transformarlo al formato de la data class y añadir todos los documentos
                        "Bike" -> levelsListBike.add(document.toObject(Level::class.java))
                        "RollerSkate" -> levelsListRollerSkate.add(document.toObject(Level::class.java))
                        "Running" -> levelsListRunning.add(document.toObject(Level::class.java))
                    }
                }
                //Aca se hace la comprobacion del nivel x cada deporte
                when (sport){
                    "Bike" -> setLevelBike()
                    "RollerSkate" -> setLevelRollerSkate()
                    "Running" -> setLevelRunning()
                }
            }
            //En caso de fallo
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }
    }
    //Comprobacion de niveles de Bike (en ly del drawer)
    private fun setLevelBike(){
        var lyNavLevelBike = findViewById<LinearLayout>(R.id.lyNavLevelBike)
        //Si los datos del Bike = 0 -> ocultar el ly
        if (totalsBike.totalTime!! == 0) setHeightLinearLayout(lyNavLevelBike, 0)
        //Si no -> mostrar ly y asignarles los valores
        else{
            //Comprobar segun los totales capturados a que nivel corresponden
            setHeightLinearLayout(lyNavLevelBike, 300)
            for (level in levelsListBike){
                //Si el total esta por debajo del requisito -> ese es el nivel al que corresponde
                if (totalsBike.totalRuns!! < level.RunsTarget!!  //TODO fixeado "<" por "<="
                    || totalsBike.totalDistance!! <= level.DistanceTarget!!){  //TODO fixeado "<" por "<="

                    //Nivel al que corresponde
                    levelBike.name = level.name!!
                    levelBike.image = level.image!!
                    levelBike.RunsTarget = level.RunsTarget!!
                    levelBike.DistanceTarget = level.DistanceTarget!!

                    //Salir del for
                    break
                }
            }

            //Variables para administrar todos esos elementos (iv y tv)
            var ivLevelBike = findViewById<ImageView>(R.id.ivLevelBike)
            var tvTotalTimeBike = findViewById<TextView>(R.id.tvTotalTimeBike)
            var tvTotalRunsBike = findViewById<TextView>(R.id.tvTotalRunsBike)
            var tvTotalDistanceBike = findViewById<TextView>(R.id.tvTotalDistanceBike)
            var tvNumberLevelBike = findViewById<TextView>(R.id.tvNumberLevelBike)

            //Capturar el nivel en string
            var levelText = "${getString(R.string.level)} ${levelBike.image!!.subSequence(6,7).toString()}"
            //Pintarl el tv con ese dato
            tvNumberLevelBike.text = levelText

            //Tiempo total acumulado
            var tt = getFormattedTotalTime(totalsBike.totalTime!!.toLong())
            tvTotalTimeBike.text = tt

            //Asignar las imagen de cada nivel
            when (levelBike.image){
                "level_1" -> ivLevelBike.setImageResource(R.drawable.level_1)
                "level_2" -> ivLevelBike.setImageResource(R.drawable.level_2)
                "level_3" -> ivLevelBike.setImageResource(R.drawable.level_3)
                "level_4" -> ivLevelBike.setImageResource(R.drawable.level_4)
                "level_5" -> ivLevelBike.setImageResource(R.drawable.level_5)
                "level_6" -> ivLevelBike.setImageResource(R.drawable.level_6)
                "level_7" -> ivLevelBike.setImageResource(R.drawable.level_7)
            }
            //Circular Bar
            tvTotalRunsBike.text = "${totalsBike.totalRuns}/${levelBike.RunsTarget}"
            var porcent = totalsBike.totalDistance!!.toInt() * 100 / levelBike.DistanceTarget!!.toInt()
            tvTotalDistanceBike.text = "${porcent.toInt()}%"

            //CircularSeekBar
            var csbDistanceBike = findViewById<CircularSeekBar>(R.id.csbDistanceBike)
            csbDistanceBike.max = levelBike.DistanceTarget!!.toFloat()
            if (totalsBike.totalDistance!! >= levelBike.DistanceTarget!!.toDouble())
                csbDistanceBike.progress = csbDistanceBike.max
            else
                csbDistanceBike.progress = totalsBike.totalDistance!!.toFloat()

            //Progress de las carreras
            var csbRunsBike = findViewById<CircularSeekBar>(R.id.csbRunsBike)
            csbRunsBike.max = levelBike.RunsTarget!!.toFloat()
            if (totalsBike.totalRuns!! >= levelBike.RunsTarget!!.toInt())
                csbRunsBike.progress = csbRunsBike.max
            else
                csbRunsBike.progress = totalsBike.totalRuns!!.toFloat()
        }
    }
    //Comprobacion de niveles de RollerSkate (en ly del drawer)
    private fun setLevelRollerSkate(){
        var lyNavLevelRollerSkate = findViewById<LinearLayout>(R.id.lyNavLevelRollerSkate)
        if (totalsRollerSkate.totalTime!! == 0) setHeightLinearLayout(lyNavLevelRollerSkate, 0)
        else{

            setHeightLinearLayout(lyNavLevelRollerSkate, 300)
            for (level in levelsListRollerSkate){
                if (totalsRollerSkate.totalRuns!! < level.RunsTarget!!.toInt()  //TODO fixeado "<" por "<="
                    || totalsRollerSkate.totalDistance!! <= level.DistanceTarget!!.toDouble()){  //TODO fixeado "<" por "<="

                    levelRollerSkate.name = level.name!!
                    levelRollerSkate.image = level.image!!
                    levelRollerSkate.RunsTarget = level.RunsTarget!!
                    levelRollerSkate.DistanceTarget = level.DistanceTarget!!

                    break
                }
            }

            var ivLevelRollerSkate = findViewById<ImageView>(R.id.ivLevelRollerSkate)
            var tvTotalTimeRollerSkate = findViewById<TextView>(R.id.tvTotalTimeRollerSkate)
            var tvTotalRunsRollerSkate = findViewById<TextView>(R.id.tvTotalRunsRollerSkate)
            var tvTotalDistanceRollerSkate = findViewById<TextView>(R.id.tvTotalDistanceRollerSkate)

            var tvNumberLevelRollerSkate = findViewById<TextView>(R.id.tvNumberLevelRollerSkate)
            var levelText = "${getString(R.string.level)} ${levelRollerSkate.image!!.subSequence(6,7).toString()}"
            tvNumberLevelRollerSkate.text = levelText

            var tt = getFormattedTotalTime(totalsRollerSkate.totalTime!!.toLong())
            tvTotalTimeRollerSkate.text = tt

            when (levelRollerSkate.image){
                "level_1" -> ivLevelRollerSkate.setImageResource(R.drawable.level_1)
                "level_2" -> ivLevelRollerSkate.setImageResource(R.drawable.level_2)
                "level_3" -> ivLevelRollerSkate.setImageResource(R.drawable.level_3)
                "level_4" -> ivLevelRollerSkate.setImageResource(R.drawable.level_4)
                "level_5" -> ivLevelRollerSkate.setImageResource(R.drawable.level_5)
                "level_6" -> ivLevelRollerSkate.setImageResource(R.drawable.level_6)
                "level_7" -> ivLevelRollerSkate.setImageResource(R.drawable.level_7)
            }


            tvTotalRunsRollerSkate.text = "${totalsRollerSkate.totalRuns}/${levelRollerSkate.RunsTarget}"

            var porcent = totalsRollerSkate.totalDistance!!.toInt() * 100 / levelRollerSkate.DistanceTarget!!.toInt()
            tvTotalDistanceRollerSkate.text = "${porcent.toInt()}%"

            var csbDistanceRollerSkate = findViewById<CircularSeekBar>(R.id.csbDistanceRollerSkate)
            csbDistanceRollerSkate.max = levelRollerSkate.DistanceTarget!!.toFloat()
            if (totalsRollerSkate.totalDistance!! >= levelRollerSkate.DistanceTarget!!.toDouble())
                csbDistanceRollerSkate.progress = csbDistanceRollerSkate.max
            else
                csbDistanceRollerSkate.progress = totalsRollerSkate.totalDistance!!.toFloat()

            var csbRunsRollerSkate = findViewById<CircularSeekBar>(R.id.csbRunsRollerSkate)
            csbRunsRollerSkate.max = levelRollerSkate.RunsTarget!!.toFloat()
            if (totalsRollerSkate.totalRuns!! >= levelRollerSkate.RunsTarget!!.toInt())
                csbRunsRollerSkate.progress = csbRunsRollerSkate.max
            else
                csbRunsRollerSkate.progress = totalsRollerSkate.totalRuns!!.toFloat()
        }
    }
    //Comprobacion de niveles de Running (en ly del drawer)
    private fun setLevelRunning(){
        var lyNavLevelRunning = findViewById<LinearLayout>(R.id.lyNavLevelRunning)
        if (totalsRunning.totalTime!! == 0) setHeightLinearLayout(lyNavLevelRunning, 0)
        else{

            setHeightLinearLayout(lyNavLevelRunning, 300)
            for (level in levelsListRunning){
                if (totalsRunning.totalRuns!! < level.RunsTarget!!.toInt()  //TODO fixeado "<" por "<="
                    || totalsRunning.totalDistance!! < level.DistanceTarget!!.toDouble()){    //TODO fixeado "<" por "<="

                    levelRunning.name = level.name!!
                    levelRunning.image = level.image!!
                    levelRunning.RunsTarget = level.RunsTarget!!
                    levelRunning.DistanceTarget = level.DistanceTarget!!

                    break
                }
            }

            var ivLevelRunning = findViewById<ImageView>(R.id.ivLevelRunning)
            var tvTotalTimeRunning = findViewById<TextView>(R.id.tvTotalTimeRunning)
            var tvTotalRunsRunning = findViewById<TextView>(R.id.tvTotalRunsRunning)
            var tvTotalDistanceRunning = findViewById<TextView>(R.id.tvTotalDistanceRunning)


            var tvNumberLevelRunning = findViewById<TextView>(R.id.tvNumberLevelRunning)
            var levelText = "${getString(R.string.level)} ${levelRunning.image!!.subSequence(6,7).toString()}"
            tvNumberLevelRunning.text = levelText

            var tt = getFormattedTotalTime(totalsRunning.totalTime!!.toLong())
            tvTotalTimeRunning.text = tt

            when (levelRunning.image){
                "level_1" -> ivLevelRunning.setImageResource(R.drawable.level_1)
                "level_2" -> ivLevelRunning.setImageResource(R.drawable.level_2)
                "level_3" -> ivLevelRunning.setImageResource(R.drawable.level_3)
                "level_4" -> ivLevelRunning.setImageResource(R.drawable.level_4)
                "level_5" -> ivLevelRunning.setImageResource(R.drawable.level_5)
                "level_6" -> ivLevelRunning.setImageResource(R.drawable.level_6)
                "level_7" -> ivLevelRunning.setImageResource(R.drawable.level_7)
            }

            tvTotalRunsRunning.text = "${totalsRunning.totalRuns}/${levelRunning.RunsTarget}"
            var porcent = totalsRunning.totalDistance!!.toInt() * 100 / levelRunning.DistanceTarget!!.toInt()
            tvTotalDistanceRunning.text = "${porcent.toInt()}%"

            var csbDistanceRunning = findViewById<CircularSeekBar>(R.id.csbDistanceRunning)
            csbDistanceRunning.max = levelRunning.DistanceTarget!!.toFloat()
            if (totalsRunning.totalDistance!! >= levelRunning.DistanceTarget!!.toDouble())
                csbDistanceRunning.progress = csbDistanceRunning.max
            else
                csbDistanceRunning.progress = totalsRunning.totalDistance!!.toFloat()

            var csbRunsRunning = findViewById<CircularSeekBar>(R.id.csbRunsRunning)
            csbRunsRunning.max = levelRunning.RunsTarget!!.toFloat()
            if (totalsRunning.totalRuns!! >= levelRunning.RunsTarget!!.toInt())
                csbRunsRunning.progress = csbRunsRunning.max
            else
                csbRunsRunning.progress = totalsRunning.totalRuns!!.toFloat()

        }
    }

    private fun loadMedalsUser(){
        //Cargar los top 3 de cada deporte
        loadMedalsBike()
        loadMedalsRollerSkate()
        loadMedalsRunning()
    }

    //Carga de medallas Bike
    private fun loadMedalsBike(){
        var dbRecords = FirebaseFirestore.getInstance()
        //Ordenarlo por distancia -> para obtener el top 3
        dbRecords.collection("runsBike")
            .orderBy("distance", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents){
                    //Si el documento que recibo tiene en el campo "user" -> useremail (del usuario que esta usando actualmente)
                    if (document["user"] == useremail)
                    //Añadir ese registro al array correspondiente
                        medalsListBikeDistance.add (document["distance"].toString().toDouble())
                    //Si ya tiene 3
                    if (medalsListBikeDistance.size == 3) break
                }
                //Mientras aun no tenga 3 carreras -> agregar un "cero" a ese array
                while (medalsListBikeDistance.size < 3) medalsListBikeDistance.add(0.0)

            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }

        dbRecords.collection("runsBike")
            .orderBy("avgSpeed", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (document["user"] == useremail)
                        medalsListBikeAvgSpeed.add (document["avgSpeed"].toString().toDouble())
                    if (medalsListBikeAvgSpeed.size == 3) break
                }
                while (medalsListBikeAvgSpeed.size < 3) medalsListBikeAvgSpeed.add(0.0)

            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }

        dbRecords.collection("runsBike")
            .orderBy("maxSpeed", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (document["user"] == useremail)
                        medalsListBikeMaxSpeed.add (document["maxSpeed"].toString().toDouble())
                    if (medalsListBikeMaxSpeed.size == 3) break
                }
                while (medalsListBikeMaxSpeed.size < 3) medalsListBikeMaxSpeed.add(0.0)

            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }
    }
    //Carga de medallas RollerSkate
    private fun loadMedalsRollerSkate(){
        var dbRecords = FirebaseFirestore.getInstance()
        dbRecords.collection("runsRollerSkate")
            .orderBy("distance", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents){
                    if (document["user"] == useremail)
                        medalsListRollerSkateDistance.add (document["distance"].toString().toDouble())
                    if (medalsListRollerSkateDistance.size == 3) break
                }
                while (medalsListRollerSkateDistance.size < 3) medalsListRollerSkateDistance.add(0.0)

            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }

        dbRecords.collection("runsRollerSkate")
            .orderBy("avgSpeed", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (document["user"] == useremail)
                        medalsListRollerSkateAvgSpeed.add (document["avgSpeed"].toString().toDouble())
                    if (medalsListRollerSkateAvgSpeed.size == 3) break
                }
                while (medalsListRollerSkateAvgSpeed.size < 3) medalsListRollerSkateAvgSpeed.add(0.0)

            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }

        dbRecords.collection("runsRollerSkate")
            .orderBy("maxSpeed", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (document["user"] == useremail)
                        medalsListRollerSkateMaxSpeed.add (document["maxSpeed"].toString().toDouble())
                    if (medalsListRollerSkateMaxSpeed.size == 3) break
                }
                while (medalsListRollerSkateMaxSpeed.size < 3) medalsListRollerSkateMaxSpeed.add(0.0)

            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }
    }
    //Carga de medallas Running
    private fun loadMedalsRunning(){
        var dbRecords = FirebaseFirestore.getInstance()
        dbRecords.collection("runsRunning")
            .orderBy("distance", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents){
                    if (document["user"] == useremail)
                        medalsListRunningDistance.add (document["distance"].toString().toDouble())
                    if (medalsListRunningDistance.size == 3) break
                }
                while (medalsListRunningDistance.size < 3) medalsListRunningDistance.add(0.0)

            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }

        dbRecords.collection("runsRunning")
            .orderBy("avgSpeed", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (document["user"] == useremail)
                        medalsListRunningAvgSpeed.add (document["avgSpeed"].toString().toDouble())
                    if (medalsListRunningAvgSpeed.size == 3) break
                }
                while (medalsListRunningAvgSpeed.size < 3) medalsListRunningAvgSpeed.add(0.0)

            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }

        dbRecords.collection("runsRunning")
            .orderBy("maxSpeed", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (document["user"] == useremail)
                        medalsListRunningMaxSpeed.add (document["maxSpeed"].toString().toDouble())
                    if (medalsListRunningMaxSpeed.size == 3) break
                }
                while (medalsListRunningMaxSpeed.size < 3) medalsListRunningMaxSpeed.add(0.0)

            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }
    }

    //Configurar sharedPreferences
    private fun initPreferences(){
        sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE)
        //Usar a "sharedPreferences" como editor
        editor = sharedPreferences.edit()

        //Guardar la configuracion de la carrera: los datos de la carrera se guardaran cuando termine
    }
    //Recuperar las preferences  (cargar los datos)
    private fun recoveryPreferences(){
        //A todos los elementos -> darle el valor que se guardo en las claves correspondientes

        //Si el email que se guardo es el mismo del usuario que esta usando ahora la app
        //TODO para el caso de que pueda ser nulo el dato -> usamos un string como mensaje
        if (sharedPreferences.getString(key_userApp, "null") == useremail){
            sportSelected = sharedPreferences.getString(key_selectedSport, "Running").toString()

            swIntervalMode.isChecked = sharedPreferences.getBoolean(key_modeInterval, false)
            if (swIntervalMode.isChecked){
                npDurationInterval.value = sharedPreferences.getInt(key_intervalDuration, 5)
                ROUND_INTERVAL = npDurationInterval.value*60
                csbRunWalk.progress = sharedPreferences.getFloat(key_progressCircularSeekBar, 150.0f)
                csbRunWalk.max = sharedPreferences.getFloat(key_maxCircularSeekBar, 300.0f)
                tvRunningTime.text = sharedPreferences.getString(key_runningTime, "2:30")
                tvWalkingTime.text = sharedPreferences.getString(key_walkingTime, "2:30")
                swIntervalMode.callOnClick()
            }

            swChallenges.isChecked = sharedPreferences.getBoolean(key_modeChallenge, false)
            if (swChallenges.isChecked){
                swChallenges.callOnClick()
                if (sharedPreferences.getBoolean(key_modeChallengeDuration, false)){
                    npChallengeDurationHH.value = sharedPreferences.getInt(key_challengeDurationHH, 1)
                    npChallengeDurationMM.value = sharedPreferences.getInt(key_challengeDurationMM, 0)
                    npChallengeDurationSS.value = sharedPreferences.getInt(key_challengeDurationSS, 0)
                    getChallengeDuration(npChallengeDurationHH.value,npChallengeDurationMM.value,npChallengeDurationSS.value)
                    challengeDistance = 0f

                    showChallenge("duration")
                }
                if (sharedPreferences.getBoolean(key_modeChallengeDistance, false)){
                    npChallengeDistance.value = sharedPreferences.getInt(key_challengeDistance, 10)
                    challengeDistance = npChallengeDistance.value.toFloat()
                    challengeDuration = 0

                    showChallenge("distance")
                }
            }
            cbNotify.isChecked = sharedPreferences.getBoolean(key_challengeNofify, true)
            cbAutoFinish.isChecked = sharedPreferences.getBoolean(key_challengeAutofinish, false)

            sbHardVolume.progress = sharedPreferences.getInt(key_hardVol, 100)
            sbSoftVolume.progress = sharedPreferences.getInt(key_softVol, 100)
            sbNotifyVolume.progress = sharedPreferences.getInt(key_notifyVol, 100)
        }
        else sportSelected = "Running"
    }
    //Guardar los datos que se hayan introducido
    private fun savePreferences(){
        //Borrar toda la info del editor
        editor.clear()
        //Aplicar nueva info
        editor.apply{

            //Para campo la siguiente estructura:
            //put +  clave/valor (nombre del dato + valor del dato)
            putString(key_userApp, useremail)
            putString(key_provider, providerSession)

            putString(key_selectedSport, sportSelected)

            putBoolean(key_modeInterval, swIntervalMode.isChecked)
            putInt(key_intervalDuration, npDurationInterval.value)
            putFloat(key_progressCircularSeekBar, csbRunWalk.progress)
            putFloat(key_maxCircularSeekBar, csbRunWalk.max)
            putString(key_runningTime, tvRunningTime.text.toString())
            putString(key_walkingTime, tvWalkingTime.text.toString())

            putBoolean(key_modeChallenge, swChallenges.isChecked)
            putBoolean(key_modeChallengeDuration, !(challengeDuration == 0))
            putInt(key_challengeDurationHH, npChallengeDurationHH.value)
            putInt(key_challengeDurationMM, npChallengeDurationMM.value)
            putInt(key_challengeDurationSS, npChallengeDurationSS.value)
            putBoolean(key_modeChallengeDistance, !(challengeDistance == 0f))
            putInt(key_challengeDistance, npChallengeDistance.value)


            putBoolean(key_challengeNofify, cbNotify.isChecked)
            putBoolean(key_challengeAutofinish, cbAutoFinish.isChecked)

            putInt(key_hardVol, sbHardVolume.progress)
            putInt(key_softVol, sbSoftVolume.progress)
            putInt(key_notifyVol, sbNotifyVolume.progress)

        }.apply()
    }
    //Alert Dialog de preferences
    private fun alertClearPreferences(){
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.alertClearPreferencesTitle))
            .setMessage(getString(R.string.alertClearPreferencesDescription))
            .setPositiveButton(android.R.string.ok,
                DialogInterface.OnClickListener{dialgo, which ->
                    callClearPreferences()
                })
            .setNegativeButton(android.R.string.cancel,
                DialogInterface.OnClickListener{dialgo, which ->

                })
            .setCancelable(true)
            .show()
    }
    //Limpiar preferences
    private fun callClearPreferences(){
        editor.clear().apply()
        Toast.makeText(this, "Tus ajustes han sido reestablecidos :)", Toast.LENGTH_SHORT).show()
    }

    //Items del menu
    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId){
            R.id.nav_item_record -> callRecordActivity()
            R.id.nav_item_clearpreferences -> alertClearPreferences()
            R.id.nav_item_signout -> alertSignOut()
        }

        //Cerrar el drawer al seleccionar un items
        drawer.closeDrawer(GravityCompat.START)

        return true
    }
    //Cerrar sesion publica
    fun callSignOut(view: View){
        signOut()
    }
    //Cerrar sesion privada
    private fun signOut(){
        useremail = ""

        //Si el usuario entro por facebook, hacer el logout:
        if (providerSession == "Facebook")  LoginManager.getInstance().logOut()

        //Cerrar sesion en firebase
        FirebaseAuth.getInstance().signOut()
        startActivity (Intent(this, LoginActivity::class.java))
        Toast.makeText(this, "Sesión finalizada", Toast.LENGTH_SHORT).show()
    }

    //Llamada el RecordActivity
    private fun callRecordActivity() {
        //Si esta corriendo -> detener la carrera y al regresar seguir con la carrera
        if (startButtonClicked) manageStartStop()

        val intent = Intent(this, RecordActivity::class.java)
        startActivity(intent)
    }
    //Inflar "Carreras con intervalos"
    fun inflateIntervalMode(){
        //Padre
        val lyIntervalMode = findViewById<LinearLayout>(R.id.lyIntervalMode)
        //Hijo
        val lyIntervalModeSpace = findViewById<LinearLayout>(R.id.lyIntervalModeSpace)

        var lySoftTrack = findViewById<LinearLayout>(R.id.lySoftTrack)
        var lySoftVolume = findViewById<LinearLayout>(R.id.lySoftVolume)
        var tvRounds = findViewById<TextView>(R.id.tvRounds)

        //Preguntar si esta activo el Switch
        if (swIntervalMode.isChecked){
            animateViewofInt(swIntervalMode, "textColor", ContextCompat.getColor(this, R.color.orange), 500)
            setHeightLinearLayout(lyIntervalModeSpace, 600)
            animateViewofFloat(lyIntervalMode, "translationY", 0f, 500)

            //Desplazar el cronometro hacia la izquierda para que aparezca el tv de las rounds
            animateViewofFloat (tvChrono, "translationX", -90f, 500)
            tvRounds.setText(R.string.rounds)
            animateViewofInt(tvRounds, "textColor", ContextCompat.getColor(this, R.color.white), 500)

            //Darle valor a los tracks de la musica
            setHeightLinearLayout(lySoftTrack,120)
            setHeightLinearLayout(lySoftVolume,200)

            //Si esta abierto "Ajustes de Audio" -> darle un altura de 600 para que se vea la 3° linea de track (tiene 400 de altura)
            if (swVolumes.isChecked){
                var lySettingsVolumesSpace = findViewById<LinearLayout>(R.id.lySettingsVolumesSpace)
                setHeightLinearLayout(lySettingsVolumesSpace,600)
            }

            //Capturar los datos del tvRunningTime y pasarlo a string
            var tvRunningTime = findViewById<TextView>(R.id.tvRunningTime)
            TIME_RUNNING = getSecFromWatch(tvRunningTime.text.toString())
        }
        //Si esta desactivado el Switch
        else{
            //Cerrar el padre
            swIntervalMode.setTextColor(ContextCompat.getColor(this, R.color.white))
            setHeightLinearLayout(lyIntervalModeSpace,0)
            lyIntervalMode.translationY = -200f

            //Poner el cronometro en su punto original
            animateViewofFloat (tvChrono, "translationX", 0f, 500)
            //Al indicador de las rondas -> sin texto (quede escondido)
            tvRounds.text = ""
            //ly de la musica suave, cerrarlos
            setHeightLinearLayout(lySoftTrack,0)
            setHeightLinearLayout(lySoftVolume,0)
            if (swVolumes.isChecked){
                var lySettingsVolumesSpace = findViewById<LinearLayout>(R.id.lySettingsVolumesSpace)
                setHeightLinearLayout(lySettingsVolumesSpace,400)
            }
        }
    }
    //Inflar "Carreras con intervalos" (Publica)
    fun callInflateIntervalMode(v: View){
        inflateIntervalMode()
    }
    //Inflar "Marcar Objetivo"
    fun inflateChallenges(v: View){
        //Padre
        val lyChallengesSpace = findViewById<LinearLayout>(R.id.lyChallengesSpace)
        //Hijo
        val lyChallenges = findViewById<LinearLayout>(R.id.lyChallenges)

        //Preguntar si esta activo el Switch
        if (swChallenges.isChecked){
            //Cambiar color
            animateViewofInt(swChallenges, "textColor", ContextCompat.getColor(this, R.color.orange), 500)
            //Padre
            setHeightLinearLayout(lyChallengesSpace, 750)
            //Hijo
            animateViewofFloat(lyChallenges, "translationY", 0f, 500)
        }
        //Para ocultarlo
        else{
            swChallenges.setTextColor(ContextCompat.getColor(this, R.color.white))
            setHeightLinearLayout(lyChallengesSpace,0)
            lyChallenges.translationY = -300f

            //Volver a cero los datos
            challengeDistance = 0f
            challengeDuration = 0
        }
    }
    //"Duracion" (pestaña)
    fun showDuration(v: View){
        if (timeInSeconds.toInt() == 0) showChallenge("duration")
    }
    //"Distancia" (pestaña)
    fun showDistance(v:View){
        if (timeInSeconds.toInt() == 0) showChallenge("distance")
    }
    //Mostrar el challenge que el usuario clickea
    private fun showChallenge(option: String){
        var lyChallengeDuration = findViewById<LinearLayout>(R.id.lyChallengeDuration)
        var lyChallengeDistance = findViewById<LinearLayout>(R.id.lyChallengeDistance)
        var tvChallengeDuration = findViewById<TextView>(R.id.tvChallengeDuration)
        var tvChallengeDistance = findViewById<TextView>(R.id.tvChallengeDistance)

        //Cuando las opciones multiples son mas de 2, es mejor usar when
        when (option){
            "duration" ->{
                //hacer visible la pestaña (ly) de "Duracion" con un valor mas alto
                lyChallengeDuration.translationZ = 5f
                lyChallengeDistance.translationZ = 0f

                //Seleccionado: letra naranja y fondo negro
                tvChallengeDuration.setTextColor(ContextCompat.getColor(this, R.color.orange))
                tvChallengeDuration.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_dark))

                //No seleccionado: letra blanca y fondo gris (como inhabilitado)
                tvChallengeDistance.setTextColor(ContextCompat.getColor(this, R.color.white))
                tvChallengeDistance.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_medium))

                //Darle valores a las variables
                challengeDistance = 0f
                //Crear fun que nos devuelva la cantidad de segundos totales
                getChallengeDuration(npChallengeDurationHH.value, npChallengeDurationMM.value, npChallengeDurationSS.value)
            }
            "distance" -> {
                //hacer visible la pestaña (ly) de "Distancia" con un valor mas alto
                lyChallengeDuration.translationZ = 0f
                lyChallengeDistance.translationZ = 5f

                //No seleccionado: letra blanca y fondo gris (como inhabilitado)
                tvChallengeDuration.setTextColor(ContextCompat.getColor(this, R.color.white))
                tvChallengeDuration.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_medium))

                //Seleccionado: letra naranja y fondo negro
                tvChallengeDistance.setTextColor(ContextCompat.getColor(this, R.color.orange))
                tvChallengeDistance.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_dark))

                //Darle valores a las variables
                challengeDuration = 0
                challengeDistance = npChallengeDistance.value.toFloat()
            }
        }
    }
    //Crear los valores del challenge de "Duracion"
    private fun getChallengeDuration(hh: Int, mm: Int, ss: Int){
        var hours: String = hh.toString()
        if (hh<10) hours = "0"+hours
        var minutes: String = mm.toString()
        if (mm<10) minutes = "0"+minutes
        var seconds: String = ss.toString()
        if (ss<10) seconds = "0"+seconds

        challengeDuration = getSecFromWatch("${hours}:${minutes}:${seconds}")
    }
    //Inflar "Ajustes de Audio"
    fun inflateVolumes(v: View){

        val lySettingsVolumesSpace = findViewById<LinearLayout>(R.id.lySettingsVolumesSpace)
        val lySettingsVolumes = findViewById<LinearLayout>(R.id.lySettingsVolumes)

        //Preguntar si esta activo el Switch
        if (swVolumes.isChecked){
            animateViewofInt(swVolumes, "textColor", ContextCompat.getColor(this, R.color.orange), 500)
            var swIntervalMode = findViewById<Switch>(R.id.swIntervalMode)
            var value = 400
            //Si esta activado Carrera con Intervalos -> mod valor = 600
            if (swIntervalMode.isChecked) value = 600
            //Padre
            setHeightLinearLayout(lySettingsVolumesSpace, value)
            //Hijo
            animateViewofFloat(lySettingsVolumes, "translationY", 0f, 500)
        }
        //Si se quiere cerrar
        else{
            swVolumes.setTextColor(ContextCompat.getColor(this, R.color.white))
            setHeightLinearLayout(lySettingsVolumesSpace,0)
            lySettingsVolumes.translationY = -300f
        }
    }

    //Inicializacion del mapa o no -> darle al usuario la posibilidad de elegir
    private fun initMap(){

        //Inicializar el array (lista de puntos)
        listPoints = arrayListOf()
        //Limpiar el listado
        (listPoints as ArrayList<LatLng>).clear()

        createMapFragment()

        var lyOpenerButton = findViewById<LinearLayout>(R.id.lyOpenerButton)
        //Si tiene permisos -> habilitar la opcion de desplegar
        if (allPermissionsGrantedGPS()) lyOpenerButton.isEnabled = true
        //Si no -> false
        else  lyOpenerButton.isEnabled = false

    }
    //Miembro obligatorio
    override fun onMyLocationButtonClick(): Boolean {
        return false
    }
    //Miembro obligatorio
    override fun onMyLocationClick(p0: Location) {
    }
    //Creacion del mapa
    private fun createMapFragment(){
        val mapFragment = supportFragmentManager.findFragmentById(R.id.fragmentMap) as SupportMapFragment?
        mapFragment?.getMapAsync(this)
    }
    //Mapa override obligatorio
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        //Tipo de mapa
        googleMap.mapType = GoogleMap.MAP_TYPE_HYBRID
        enableMyLocation()  //Habilitar la localizacion
        //Posicionar el mapa a donde esta la ubicacion actual del usuario
        map.setOnMyLocationButtonClickListener (this)
        map.setOnMyLocationClickListener(this)
        map.setOnMapLongClickListener {  mapCentered = false }
        map.setOnMapClickListener { mapCentered = false  }

        //Antes de centrar -> capturar los datos
        manageLocation()
        //Fun que centre el mapa
        centerMap (init_lt ,init_ln)
    }
    //Luego de habilitar o no los permisos -> vuelve a la app con nuevos permisos habilitados o no
    override fun onRequestPermissionsResult(requestCode: Int,permissions: Array<out String>,grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode){
            LOCATION_PERMISSION_REQ_CODE -> {
                var lyOpenerButton = findViewById<LinearLayout>(R.id.lyOpenerButton)

                //Si ha sido aprobado el permiso -> ly enable
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                    lyOpenerButton.isEnabled = true
                //Si no -> desabilitarlo  (plantear si cerrarlo o no)
                else{
                    var lyMap = findViewById<LinearLayout>(R.id.lyMap)
                    //Si estaba desplegado -> encogerlo
                    if (lyMap.height > 0){
                        setHeightLinearLayout(lyMap, 0)

                        var lyFragmentMap = findViewById<LinearLayout>(R.id.lyFragmentMap)
                        lyFragmentMap.translationY= -300f

                        var ivOpenClose = findViewById<ImageView>(R.id.ivOpenClose)
                        ivOpenClose.setRotation(0f)
                    }
                    //Desabilitarlo
                    lyOpenerButton.isEnabled = false
                    //TODO aca puede estar un toast indicando al usuario que necesita habilitar los permisos de ubicacion
                }
            }
        }
    }
    //Habilitar mi ubicacion
    private fun enableMyLocation(){
        //Preguntar si el mapa no esta inicializado -> return y no hace nada
        if (!::map.isInitialized) return
        //SI no estan los permisos -> se sale
        if (ActivityCompat.checkSelfPermission(  this,Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED

            && ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLocation()
            return
        }
        else map.isMyLocationEnabled = true
    }
    //Desplazar la camara al punto que se haya indicado
    private fun centerMap(lt: Double, ln: Double){
        val posMap = LatLng(lt, ln)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(posMap, 16f), 1000, null)
    }
    //Cambiar el tipo de mapa
    fun changeTypeMap(v: View){
        var ivTypeMap = findViewById<ImageView>(R.id.ivTypeMap)
        //Si esta en modo hibrido -> cambiar a normal y la imagen
        if (map.mapType == GoogleMap.MAP_TYPE_HYBRID){
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            ivTypeMap.setImageResource(R.drawable.map_type_hybrid)
        }
        //Si no (esta en modo normal) -> cambiar a modo hibrido y la imagen tambien
        else{
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            ivTypeMap.setImageResource(R.drawable.map_type_normal)
        }
    }
    //Centrar el mapa a la ubicacion del usuario
    fun callCenterMap(v: View){
        mapCentered = true
        //Al inicio -> no se tienen datos -> se centran con lat/long inicial
        if (latitude == 0.0) centerMap(init_lt, init_ln)
        //Si no -> (es porque se tienen datos) llamar a centerMap
        else centerMap(latitude, longitude)
    }
    //Llamada para desplazar el mapa (mostrar/ocultar)
    fun callShowHideMap(v: View){
        //Preguntar si estan aprobados todos los permisos
        if (allPermissionsGrantedGPS()){
            //Padre ly
            var lyMap = findViewById<LinearLayout>(R.id.lyMap)
            //Hijo
            var lyFragmentMap = findViewById<LinearLayout>(R.id.lyFragmentMap)
            //Triangulito
            var ivOpenClose = findViewById<ImageView>(R.id.ivOpenClose)

            //Si esta oculto (height == 0) -> al darle click se desplega
            if (lyMap.height == 0){
                setHeightLinearLayout(lyMap, 1157)
                animateViewofFloat(lyFragmentMap, "translationY", 0f, 0)
                ivOpenClose.setRotation(180f)
                callCenterMap(v)  // TODO mejora implementada para mostrar el mapa al abrir el ly (independientemente de que haya comenzado carrera)
            }
            //En caso de que este desplegado -> encoger
            else{
                setHeightLinearLayout(lyMap, 0)
                lyFragmentMap.translationY= -300f
                ivOpenClose.setRotation(0f)
            }
        }
        //Si no, pedir los permisos
        else requestPermissionLocation()
    }

    //Comprobar que esten aprobados los permisos relacionados con el gps
    private fun initPermissionsGPS(){
        //Si tenemos los permisos aprobados
        if (allPermissionsGrantedGPS())
        //Capturar (get) todos los datos de localizacion
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        else
        //Solicitar los permisos
            requestPermissionLocation()
    }
    //Solicitar los permisos de localizacion
    private fun requestPermissionLocation(){
        ActivityCompat.requestPermissions(this, arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_ID)
    }
    //Solicitar los permisos de localizacion
    private fun allPermissionsGrantedGPS() = REQUIRED_PERMISSIONS_GPS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }
    //Activar el gps ?
    private fun isLocationEnabled(): Boolean{
        //Capturar el servicio del sistema
        var locationManager: LocationManager
                = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        //Retornar (habilitado) -> proveedor de gps o proveedor de la red
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
    //Activar la localizacion del usuario
    private fun activationLocation(){
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
    }

    //Comprobar los permisos de localizacion
    private fun checkPermission(): Boolean{
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }
    //Registrar la localizacion
    private fun manageLocation(){
        if (checkPermission()){

            //Comprobacion -> si no esta ACTIVADO
            if (isLocationEnabled()){
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
                    &&  ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED) {

                    //Solicitar ultima localizacion del usuario
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        requestNewLocationData()
                    }
                }
            }
            //ACTIVARLO
            else activationLocation()
        }
        //Solicitar los permisos
        else requestPermissionLocation()
    }
    //Capturar la ultima localizacion del usuario
    @SuppressLint("MissingPermission")
    private fun requestNewLocationData(){
        var mLocationRequest = com.google.android.gms.location.LocationRequest()
        mLocationRequest.priority = PRIORITY_HIGH_ACCURACY
        mLocationRequest.interval = 0
        mLocationRequest.fastestInterval = 0
        mLocationRequest.numUpdates = 1
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //Mandar la llamada de la captura de datos
        fusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallBack, Looper.myLooper())
    }
    //Callback
    private val mLocationCallBack = object: LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) {
            var mLastLocation : Location = locationResult.lastLocation!!

            //Capturar los datos de localizacion
            init_lt = mLastLocation.latitude
            init_ln = mLastLocation.longitude

            //Si la empezo la carrera -> ahora SI registrar la posicion
            if (timeInSeconds > 0L) registerNewLocation(mLastLocation)
        }
    }
    //Registrar la nueva localizacion
    private fun registerNewLocation(location: Location){
        //Guardar en variables las lon y lat actuales del usuario
        var new_latitude: Double = location.latitude
        var new_longitude: Double = location.longitude

        //Si la bandera esta ACTIVA
        if (flagSavedLocation){
            //En el segundo "cero" no hay datos para calcular
            if (timeInSeconds >= INTERVAL_LOCATION){
                var distanceInterval = calculateDistance(new_latitude, new_longitude)

                if ( distanceInterval <= LIMIT_DISTANCE_ACCEPTED) {
                    updateSpeeds(distanceInterval)
                    refreshInterfaceData()

                    //Guardar las localizaciones para la BD
                    saveLocation(location)

                    //Capturar los datos en en una variable, añadirlos al array y luego pintarlos
                    var newPos = LatLng (new_latitude, new_longitude)
                    //Se usa el AS porque el tipo de dato que google obliga a usar no tiene integrada esa funcionalidad
                    (listPoints as ArrayList<LatLng>).add(newPos)
                    //Pintar los puntos del array
                    createPolylines(listPoints)

                    //Comprobar nuevos record
                    checkMedals(distance, avgSpeed, maxSpeed)
                }
            }
        }
        //Cada vez que se registra esa variable -> guardarlas en las var globales
        latitude = new_latitude
        longitude = new_longitude

        if (mapCentered == true) centerMap(latitude, longitude)

        //Comprobacion de todos los valores
        if (minLatitude == null){
            minLatitude = latitude
            maxLatitude = latitude
            minLongitude = longitude
            maxLongitude = longitude
        }
        //Si latitud es < que la minima -> esa es la minima => actualizar el dato
        if (latitude < minLatitude!!) minLatitude = latitude
        //Si latitud es > que la maxima -> esa es la maxima => actualizar el dato
        if (latitude > maxLatitude!!) maxLatitude = latitude
        if (longitude < minLongitude!!) minLongitude = longitude
        if (longitude > maxLongitude!!) maxLongitude = longitude

        //Si la ubicacion generada tiene altitud -> hacer lo mismo que lat/long
        if (location.hasAltitude()){
            //Si no tiene altitud
            if (maxAltitude == null){
                maxAltitude = location.altitude
                minAltitude = location.altitude
            }
            //Si tiene altitud
            if (location.latitude > maxAltitude!!) maxAltitude = location.altitude
            if (location.latitude < minAltitude!!) minAltitude = location.altitude
        }
    }
    //Consultas para medallas
    private fun checkMedals(d: Double, aS: Double, mS: Double){
        //Si el nuevo dato representa una medalla
        if (d>0){
            //Consultar si es mayor que el primer registro
            if (d >= medalsListSportSelectedDistance.get(0)){
                recDistanceGold = true; recDistanceSilver = false; recDistanceBronze = false
                notifyMedal("distance", "gold", "PERSONAL")
            }
            else{
                //Consultar si es mayor que el siguiente registro (segundo registro)
                if (d >= medalsListSportSelectedDistance.get(1)){
                    recDistanceGold = false; recDistanceSilver = true; recDistanceBronze = false
                    notifyMedal("distance", "silver", "PERSONAL")
                }
                else{
                    //Consultar si es mayor que el tercer registro
                    if (d >= medalsListSportSelectedDistance.get(2)){
                        recDistanceGold = false; recDistanceSilver = false; recDistanceBronze = true
                        notifyMedal("distance", "bronze", "PERSONAL")
                    }
                }
            }
        }
        //avgSpeed
        if (aS > 0){
            if (aS >= medalsListSportSelectedAvgSpeed.get(0)){
                recAvgSpeedGold = true; recAvgSpeedSilver = false; recAvgSpeedBronze = false
                notifyMedal("avgSpeed", "gold", "PERSONAL")
            }
            else{
                if (aS >= medalsListSportSelectedAvgSpeed.get(1)){
                    recAvgSpeedGold = false; recAvgSpeedSilver = true; recAvgSpeedBronze = false
                    notifyMedal("avgSpeed", "silver", "PERSONAL")
                }
                else{
                    if (aS >= medalsListSportSelectedAvgSpeed.get(2)){
                        recAvgSpeedGold = false; recAvgSpeedSilver = false; recAvgSpeedBronze = true
                        notifyMedal("avgSpeed", "bronze", "PERSONAL")
                    }
                }
            }
        }
        //maxSpeed
        if (mS > 0){
            if (mS >= medalsListSportSelectedMaxSpeed.get(0)){
                recMaxSpeedGold = true; recMaxSpeedSilver = false; recMaxSpeedBronze = false
                notifyMedal("maxSpeed", "gold", "PERSONAL")
            }
            else{
                if (mS >= medalsListSportSelectedMaxSpeed.get(1)){
                    recMaxSpeedGold = false; recMaxSpeedSilver = true; recMaxSpeedBronze = false
                    notifyMedal("maxSpeed", "silver", "PERSONAL")
                }
                else{
                    if (mS >= medalsListSportSelectedMaxSpeed.get(2)){
                        recMaxSpeedGold = false; recMaxSpeedSilver = false; recMaxSpeedBronze = true
                        notifyMedal("maxSpeed", "bronze", "PERSONAL")
                    }
                }
            }
        }
    }
    //Notificaciones de medallas
    private fun notifyMedal(category: String, metal: String, scope: String){
        var CHANNEL_ID = "NEW $scope RECORD - $sportSelected"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        var textNotification =""
        when (metal){
            "gold"-> textNotification = "1ª "
            "silver"-> textNotification = "2ª "
            "bronze"-> textNotification = "3ª "
        }
        textNotification += "mejor marca personal en "
        when (category){
            "distance"-> textNotification += "distancia recorrida"
            "avgSpeed"-> textNotification += " velocidad promedio"
            "maxSpeed"-> textNotification += " velocidad máxima alcanzada"
        }

        var iconNotificacion: Int = 0
        when (metal){
            "gold" -> iconNotificacion = R.drawable.medalgold
            "silver"-> iconNotificacion = R.drawable.medalsilver
            "bronze"-> iconNotificacion = R.drawable.medalbronze
        }
        //Constructor
        var builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(iconNotificacion)
            .setContentTitle(CHANNEL_ID)
            .setContentText(textNotification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)


        var notificationId: Int = 0
        when (category){
            "distance"->
                when (metal){
                    "gold"->notificationId = 11
                    "silver"->notificationId = 12
                    "bronze"->notificationId = 13
                }
            "avgSpeed"->
                when (metal){
                    "gold"->notificationId = 21
                    "silver"->notificationId = 22
                    "bronze"->notificationId = 23
                }
            "maxSpeed"->
                when (metal){
                    "gold"->notificationId = 31
                    "silver"->notificationId = 32
                    "bronze"->notificationId = 33
                }
        }
        //fun que crea la notificacion
        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationId, builder.build())
        }
        //mpNotify?.start()  //TODO fixeado
    }


    //Calcular distancia dle intervalo (entre 2 puntos)
    private fun calculateDistance(n_lt: Double, n_lg: Double): Double{
        val radioTierra = 6371.0 //en kilómetros

        val dLat = Math.toRadians(n_lt - latitude)
        val dLng = Math.toRadians(n_lg - longitude)
        val sindLat = Math.sin(dLat / 2)
        val sindLng = Math.sin(dLng / 2)
        val va1 =
            Math.pow(sindLat, 2.0) + (Math.pow(sindLng, 2.0)
                    * Math.cos(Math.toRadians(latitude)) * Math.cos(
                Math.toRadians( n_lt  )
            ))
        val va2 = 2 * Math.atan2(Math.sqrt(va1), Math.sqrt(1 - va1))
        var n_distance =  radioTierra * va2

        //Verificacion de que la distancia esta entre los limites aceptados
        if (n_distance < LIMIT_DISTANCE_ACCEPTED) distance += n_distance

        //Registro de dist acumulada -> distancia total + dist de cada intervalo
        //distance += n_distance  //comentado luego de agregar el if () anterior
        return n_distance
    }
    //Actualizar las velocidades
    private fun updateSpeeds(d: Double) {
        //la distancia se calcula en km, asi que la pasamos a metros para el calculo de velocidadr
        //convertirmos m/s a km/h multiplicando por 3.6
        speed = ((d * 1000) / INTERVAL_LOCATION) * 3.6
        //si la velocidad es mayor a la maxima -> hay un nuevo record
        if (speed > maxSpeed) maxSpeed = speed
        avgSpeed = ((distance * 1000) / timeInSeconds) * 3.6
    }
    //Refrescar los datos de la interface (los 3 seekbar)
    private fun refreshInterfaceData(){
        //Administrar las 3 variables
        var tvCurrentDistance = findViewById<TextView>(R.id.tvCurrentDistance)
        var tvCurrentAvgSpeed = findViewById<TextView>(R.id.tvCurrentAvgSpeed)
        var tvCurrentSpeed = findViewById<TextView>(R.id.tvCurrentSpeed)
        tvCurrentDistance.text = roundNumber(distance.toString(), 2)
        tvCurrentAvgSpeed.text = roundNumber(avgSpeed.toString(), 1)
        tvCurrentSpeed.text = roundNumber(speed.toString(), 1)

        //Progresos actualizados
        csbCurrentDistance.progress = distance.toFloat()
        //la distancia total ha su perado el records ?
        if (distance > totalsSelectedSport.recordDistance!!){
            //Actualizar el tv
            tvDistanceRecord.text = roundNumber(distance.toString(), 1)
            //Cambiar el color
            tvDistanceRecord.setTextColor(ContextCompat.getColor(this, R.color.salmon_dark))

            //Indicar a los scb el limite para que no lo sobrepasen
            csbCurrentDistance.max = distance.toFloat()
            csbCurrentDistance.progress = distance.toFloat()

            //mpNotify?.start()  //TODO fixeado

            //Guardar en el total el valor acumulado
            totalsSelectedSport.recordDistance = distance
        }

        csbCurrentAvgSpeed.progress = avgSpeed.toFloat()
        if (avgSpeed > totalsSelectedSport.recordAvgSpeed!!){
            tvAvgSpeedRecord.text = roundNumber(avgSpeed.toString(), 1)
            tvAvgSpeedRecord.setTextColor(ContextCompat.getColor(this, R.color.salmon_dark))

            csbRecordAvgSpeed.max = avgSpeed.toFloat()
            csbRecordAvgSpeed.progress = avgSpeed.toFloat()
            csbCurrentAvgSpeed.max = avgSpeed.toFloat()

            //mpNotify?.start()  //TODO fixeado

            totalsSelectedSport.recordAvgSpeed = avgSpeed
        }

        if (speed > totalsSelectedSport.recordSpeed!!){
            tvMaxSpeedRecord.text = roundNumber(speed.toString(), 1)
            tvMaxSpeedRecord.setTextColor(ContextCompat.getColor(this, R.color.salmon_dark))

            csbRecordSpeed.max = speed.toFloat()
            csbRecordSpeed.progress = speed.toFloat()

            csbCurrentMaxSpeed.max = speed.toFloat()
            csbCurrentMaxSpeed.progress = speed.toFloat()

            csbCurrentSpeed.max = speed.toFloat()
            //mpNotify?.start()  //TODO fixeado

            totalsSelectedSport.recordSpeed = speed
        }
        else {

            //Si la velocidad = a la maxima -> cambiar el limite del circular seekbar
            if (speed == maxSpeed){
                csbCurrentMaxSpeed.max = csbRecordSpeed.max
                csbCurrentMaxSpeed.progress = speed.toFloat()

                csbCurrentSpeed.max = csbRecordSpeed.max
            }
        }
        csbCurrentSpeed.progress = speed.toFloat()
    }
    //Pintar los puntos del array
    private fun createPolylines(listPosition: Iterable<LatLng>){
        val polylineOptions = PolylineOptions()
            .width(25f)
            .color(ContextCompat.getColor(this, R.color.salmon_dark))
            .addAll(listPosition) //TODO lisPoint ?

        //Pintar la polilinea en el mapa
        val polyline = map.addPolyline(polylineOptions)
        //Diseño redondo
        polyline.startCap = RoundCap()
    }
    //Guardar la localizacion para la BD
    private fun saveLocation(location: Location){
        //Crear la subcarpeta
        var dirName = dateRun + startTimeRun
        dirName = dirName.replace("/", "")
        dirName = dirName.replace(":", "")

        //El numero de "segundo" (tiempo) de la ubicacion
        var docName = timeInSeconds.toString()
        while (docName.length < 4) docName = "0" + docName

        //Para saber el punto de max velocidad
        var ms: Boolean
        ms = speed == maxSpeed && speed > 0

        //Instancia de la BD
        var dbLocation = FirebaseFirestore.getInstance()
        //Almacenar en la BD todas las localizaciones
        dbLocation.collection("locations/$useremail/$dirName").document(docName).set(hashMapOf(
            "time" to SimpleDateFormat("HH:mm:ss").format(Date()),
            "latitude" to location.latitude,
            "longitude" to location.longitude,
            "altitude" to location.altitude,
            "hasAltitude" to location.hasAltitude(),
            "speedFromGoogle" to location.speed,
            "speedFromMe" to speed,
            "maxSpeed" to ms,
            "color" to tvChrono.currentTextColor
        ))
    }

    //Seleccion Bike
    fun selectBike(v: View){
        if (timeInSeconds.toInt() == 0) selectSport("Bike")
    }
    //Seleccion RollerSkate
    fun selectRollerSkate(v: View){
        if (timeInSeconds.toInt() == 0) selectSport("RollerSkate")
    }
    //Seleccion Running
    fun selectRunning(v: View){
        if (timeInSeconds.toInt() == 0) selectSport("Running")
    }
    //Administrar deporte elegido
    private fun selectSport(sport: String){
        sportSelected = sport

        var lySportBike = findViewById<LinearLayout>(R.id.lySportBike)
        var lySportRollerSkate = findViewById<LinearLayout>(R.id.lySportRollerSkate)
        var lySportRunning = findViewById<LinearLayout>(R.id.lySportRunning)

        when (sport){
            "Bike"->{
                LIMIT_DISTANCE_ACCEPTED = LIMIT_DISTANCE_ACCEPTED_BIKE
                //Poner en naranja el deporte elegido y los demas en gris
                lySportBike.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.blue_trans))
                lySportRollerSkate.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
                lySportRunning.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))

                //El nivel de ese deporte -> es el nivel seleccionado
                levelSelectedSport = levelBike
                //Los totales de ese deporte -> es el total del nivel seleccionado
                totalsSelectedSport = totalsBike

                //Cuando seleccionamos el deporte -> guardar esos tops en las listas genericas
                medalsListSportSelectedDistance = medalsListBikeDistance
                medalsListSportSelectedAvgSpeed = medalsListBikeAvgSpeed
                medalsListSportSelectedMaxSpeed = medalsListBikeMaxSpeed

            }
            "RollerSkate"->{
                LIMIT_DISTANCE_ACCEPTED = LIMIT_DISTANCE_ACCEPTED_ROLLERSKATE
                //Poner en naranja el deporte elegido y los demas en gris
                lySportBike.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
                lySportRollerSkate.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.blue_trans))
                lySportRunning.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))

                levelSelectedSport = levelRollerSkate
                totalsSelectedSport = totalsRollerSkate

                //Cuando seleccionamos el deporte -> guardar esos tops en las listas genericas
                medalsListSportSelectedDistance = medalsListRollerSkateDistance
                medalsListSportSelectedAvgSpeed = medalsListRollerSkateAvgSpeed
                medalsListSportSelectedMaxSpeed = medalsListRollerSkateMaxSpeed

            }
            "Running"->{
                LIMIT_DISTANCE_ACCEPTED = LIMIT_DISTANCE_ACCEPTED_RUNNING
                //Poner en naranja el deporte elegido y los demas en gris
                lySportBike.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
                lySportRollerSkate.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.gray_medium))
                lySportRunning.setBackgroundColor(ContextCompat.getColor(mainContext, R.color.blue_trans))

                levelSelectedSport = levelRunning
                totalsSelectedSport = totalsRunning

                //Cuando seleccionamos el deporte -> guardar esos tops en las listas genericas
                medalsListSportSelectedDistance = medalsListRunningDistance
                medalsListSportSelectedAvgSpeed = medalsListRunningAvgSpeed
                medalsListSportSelectedMaxSpeed = medalsListRunningMaxSpeed

            }
        }
        //Actualizar esos datos (arriba del cronometro)
        refreshCBSsSport()
        refreshRecords()
    }
    //Refrescar el CircularSeeBar
    private fun refreshCBSsSport(){
        //El max -> sea el record
        csbRecordDistance.max = totalsSelectedSport.recordDistance?.toFloat()!!
        //El progress -> sea el record
        csbRecordDistance.progress = totalsSelectedSport.recordDistance?.toFloat()!!

        csbRecordAvgSpeed.max = totalsSelectedSport.recordAvgSpeed?.toFloat()!!
        csbRecordAvgSpeed.progress = totalsSelectedSport.recordAvgSpeed?.toFloat()!!

        csbRecordSpeed.max = totalsSelectedSport.recordSpeed?.toFloat()!!
        csbRecordSpeed.progress = totalsSelectedSport.recordSpeed?.toFloat()!!

        //El punto actual de Distance -> el max del record
        csbCurrentDistance.max = csbRecordDistance.max
        //El punto actual de AvgSpeed -> el max del record
        csbCurrentAvgSpeed.max = csbRecordAvgSpeed.max
        //El punto actual de Speed -> el max del record
        csbCurrentSpeed.max = csbRecordSpeed.max
        //El punto actual de max Speed -> el max del record
        csbCurrentMaxSpeed.max = csbRecordSpeed.max
        //El progres de la MaxSpeed = 0
        csbCurrentMaxSpeed.progress = 0f

    }
    //Refrescar el Records
    private fun refreshRecords(){  //TODO fixeado a fun publica
        //Distancia
        if (totalsSelectedSport.recordDistance!! > 0)
            tvDistanceRecord.text = totalsSelectedSport.recordDistance.toString()
        else
            tvDistanceRecord.text = ""
        //AvgSpeed
        if (totalsSelectedSport.recordAvgSpeed!! > 0)
            tvAvgSpeedRecord.text = totalsSelectedSport.recordAvgSpeed.toString()
        else
            tvAvgSpeedRecord.text = ""
        //Speed
        if (totalsSelectedSport.recordSpeed!! > 0)
            tvMaxSpeedRecord.text = totalsSelectedSport.recordSpeed.toString()
        else
            tvMaxSpeedRecord.text = ""
    }
    //Actualizar datos en Firestore
    private fun updateTotalsUser(){
        //Hacer referencia al documento en cuestion -> cambiar datos existentes

        //Despues de cada carrera -> tenemos 1 carrera mas
        totalsSelectedSport.totalRuns = totalsSelectedSport.totalRuns!! + 1  //TODO fixeado "!!" por "?"
        //A la distancia total -> agregar la distancia recorrida
        totalsSelectedSport.totalDistance = totalsSelectedSport.totalDistance!! + distance  //TODO fixeado "!!" por "?"
        //Al tiempo total acumulado -> agregar el tiempo total de la ultima carrera
        totalsSelectedSport.totalTime = totalsSelectedSport.totalTime!! + timeInSeconds.toInt()  //TODO fixeado "!!" por "?"

        //Si la distancia es mayor al record -> hay un nuevo record
        if (distance > totalsSelectedSport.recordDistance!!){
            totalsSelectedSport.recordDistance = distance
        }
        //Si la maxSpeed es mayor al record -> hay un nuevo record
        if (maxSpeed > totalsSelectedSport.recordSpeed!!){
            totalsSelectedSport.recordSpeed = maxSpeed
        }
        //Si la avgSpeed es mayor al record -> hay un nuevo record
        if (avgSpeed > totalsSelectedSport.recordAvgSpeed!!){
            totalsSelectedSport.recordAvgSpeed = avgSpeed
        }
        //Redondear a 1 decimal
        totalsSelectedSport.totalDistance = roundNumber(totalsSelectedSport.totalDistance.toString(),2).toDouble()
        totalsSelectedSport.recordDistance = roundNumber(totalsSelectedSport.recordDistance.toString(),2).toDouble()
        totalsSelectedSport.recordSpeed = roundNumber(totalsSelectedSport.recordSpeed.toString(),2).toDouble()
        totalsSelectedSport.recordAvgSpeed = roundNumber(totalsSelectedSport.recordAvgSpeed.toString(),2).toDouble()

        //Actualizacion en la BD
        var collection = "totals$sportSelected"
        var dbUpdateTotals = FirebaseFirestore.getInstance()

        //Ubicacion en la BD -> coleccion, documento, campo
        dbUpdateTotals.collection(collection).document(useremail)
            //actualizar con clave y valor
            .update("recordAvgSpeed", totalsSelectedSport.recordAvgSpeed)
        //idem para cada uno de los siguientes:
        dbUpdateTotals.collection(collection).document(useremail)
            .update("recordDistance", totalsSelectedSport.recordDistance)
        dbUpdateTotals.collection(collection).document(useremail)
            .update("recordSpeed", totalsSelectedSport.recordSpeed)
        dbUpdateTotals.collection(collection).document(useremail)
            .update("totalDistance", totalsSelectedSport.totalDistance)
        dbUpdateTotals.collection(collection).document(useremail)
            .update("totalRuns", totalsSelectedSport.totalRuns)
        dbUpdateTotals.collection(collection).document(useremail)
            .update("totalTime", totalsSelectedSport.totalTime)

        //Volver a actualizar el total del deporte en cuestion
        when (sportSelected){
            "Bike" -> {
                totalsBike = totalsSelectedSport

                //Asignar a las categorias los datos capturados
                medalsListBikeDistance = medalsListSportSelectedDistance
                medalsListBikeAvgSpeed = medalsListSportSelectedAvgSpeed
                medalsListBikeMaxSpeed = medalsListSportSelectedMaxSpeed
            }
            "RollerSkate" -> {
                totalsRollerSkate = totalsSelectedSport

                //Asignar a las categorias los datos capturados
                medalsListRollerSkateDistance = medalsListSportSelectedDistance
                medalsListRollerSkateAvgSpeed = medalsListSportSelectedAvgSpeed
                medalsListRollerSkateMaxSpeed = medalsListSportSelectedMaxSpeed
            }
            "Running" -> {
                totalsRunning = totalsSelectedSport

                //Asignar a las categorias los datos capturados
                medalsListRunningDistance = medalsListSportSelectedDistance
                medalsListRunningAvgSpeed = medalsListSportSelectedAvgSpeed
                medalsListRunningMaxSpeed = medalsListSportSelectedMaxSpeed
            }
        }
    }

    //Manejar el boton del cronometro publica
    fun startOrStopButtonClicked (v: View){
        //manageRun()
        manageStartStop()
    }
    //Comprobacion de los permisos e invocacion a manageRun
    private fun manageStartStop(){
        //Si no esta en carrera -> como no tienes el gps activado, lo quieres activar ?
        if (timeInSeconds == 0L && isLocationEnabled() == false){
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.alertActivationGPSTitle))
                .setMessage(getString(R.string.alertActivationGPSDescription))
                .setPositiveButton(R.string.aceptActivationGPS,
                    DialogInterface.OnClickListener { dialog, which ->
                        activationLocation()
                    })
                .setNegativeButton(R.string.ignoreActivationGPS,
                    DialogInterface.OnClickListener { dialog, which ->
                        activatedGPS = false
                        manageRun()
                    })
                .setCancelable(true)  //Que se pueda cancelar
                .show()
        }
        else manageRun()
    }
    //Manejar el boton del cronometro privada
    private fun manageRun(){
        //Al inicio de la carrera
        if (timeInSeconds.toInt() == 0){

            //Guardar la fecha y hora de carrera
            dateRun = SimpleDateFormat("yyyy/MM/dd").format(Date())
            startTimeRun = SimpleDateFormat("HH:mm:ss").format(Date())

            //Activar y desactivar las opciones cuando esta corriendo el cronometro
            fbCamara.isVisible = true

            swIntervalMode.isClickable = false
            npDurationInterval.isEnabled = false
            csbRunWalk.isEnabled = false

            swChallenges.isClickable = false
            npChallengeDistance.isEnabled = false
            npChallengeDurationHH.isEnabled = false
            npChallengeDurationMM.isEnabled = false
            npChallengeDurationSS.isEnabled = false

            tvChrono.setTextColor(ContextCompat.getColor(this, R.color.chrono_running))

//            sbHardTrack.isEnabled = true
//            sbSoftTrack.isEnabled = true

            mpHard?.start()


            //Si esta ACTIVADO el gps -> poner la flag en false y luego el true
            //para que borre la ultima localizacion y tome la actual
            if (activatedGPS){
                flagSavedLocation = false
                manageLocation()
                flagSavedLocation = true
                manageLocation()
            }
        }

        //Si clickea a EMPEZAR
        if (!startButtonClicked){
            startButtonClicked = true
            startTime()
            manageEnableButtonsRun(false, true)

            if (hardTime) mpHard?.start()
            else mpSoft?.start()

//            //Iniciar la musica
//            //Si esta de color rojo -> INICIAR hard
//            if (tvChrono.getCurrentTextColor() == ContextCompat.getColor(this, R.color.chrono_running))
//                mpHard?.start()
//            //Si esta de color azul -> INICIAR soft
//            if (tvChrono.getCurrentTextColor() == ContextCompat.getColor(this, R.color.chrono_walking))
//                mpSoft?.start()
        }

        //Clikear por segunda vez -> STOP
        else {
            startButtonClicked = false
            stopTime()
            manageEnableButtonsRun(true, true)

            if (hardTime) mpHard?.pause()
            else mpSoft?.pause()

//            //Si esta de color rojo -> PAUSAR hard
//            if (tvChrono.getCurrentTextColor() == ContextCompat.getColor(this, R.color.chrono_running))
//                mpHard?.pause()
//            //Si esta de color azul -> PAUSAR soft
//            if (tvChrono.getCurrentTextColor()  == ContextCompat.getColor(this, R.color.chrono_walking))
//                mpSoft?.pause()
        }
    }
    //Administrar los botones de la carrera
    private fun manageEnableButtonsRun(e_reset: Boolean, e_run: Boolean){
        val tvReset = findViewById<TextView>(R.id.tvReset)
        val btStart = findViewById<LinearLayout>(R.id.btStart)
        val btStartLabel = findViewById<TextView>(R.id.btStartLabel)
        tvReset.setEnabled(e_reset)
        btStart.setEnabled(e_run)

        //Si reset esta habilitado
        if (e_reset){
            //Cambiar color
            tvReset.setBackgroundColor(ContextCompat.getColor(this, R.color.chrono_running))
            //Elevarlo para que aparezca
            animateViewofFloat(tvReset, "translationY", 0f, 500)
        }
        else{
            //Fondo gris
            tvReset.setBackgroundColor(ContextCompat.getColor(this, R.color.gray))
            //Ocultarlo
            animateViewofFloat(tvReset, "translationY", 150f, 500)
        }

        //Boton de la carrera - Si esta ACTIVO
        if (e_run){
            if (startButtonClicked){
                //Cambiar el color a (rojo) verde y dejar un label de "stop"
                btStart.background = getDrawable(R.drawable.circle_background_topause)
                btStartLabel.setText(R.string.stop)
            }
            //Si esata PAUSADO
            else{
                //Cambiar el color a amarillo y dejar un label de "start"
                btStart.background = getDrawable(R.drawable.circle_background_toplay)
                btStartLabel.setText(R.string.start)
            }
        }
        //Si no esta ACTIVO
        else btStart.background = getDrawable(R.drawable.circle_background_todisable)
    }
    //Ejecutar le looper en el contexto del main
    private fun startTime(){
        mHandler = Handler(Looper.getMainLooper())
        chronometer.run()
    }
    //Detener tiempo
    private fun stopTime(){
        mHandler?.removeCallbacks(chronometer)
    }
    //Configurar el runnable - Cronometro
    private var chronometer: Runnable = object : Runnable {
        //Codigo de ejecucion
        override fun run() {
            //Dentro del try es lo que se va a ejecutar siempre
            try{
                //Aumentar el progress track mpHard
                if (mpHard!!.isPlaying){
                    val sbHardTrack: SeekBar = findViewById(R.id.sbHardTrack)
                    sbHardTrack.progress = mpHard!!.currentPosition
                    sbHardTrack.isEnabled = true //TODO ok

                }
                //Aumentar el progress track mpSoft
                if (mpSoft!!.isPlaying){
                    val sbSoftTrack: SeekBar = findViewById(R.id.sbSoftTrack)
                    sbSoftTrack.progress = mpSoft!!.currentPosition
                    sbSoftTrack.isEnabled = true //TODO ok
                }
                //Actualizar los tracks -> ambos
                updateTimesTrack(true, true)

                //Si esta ACTIVADO el gps y se calcula la localizacion cada 4 segundos
                if (activatedGPS && timeInSeconds.toInt() % INTERVAL_LOCATION == 0) manageLocation()

                //Si esta activada el switch "modo intervalo"
                if (swIntervalMode.isChecked){
                    //Comrpobar si hay que dejar de correr para empezar a caminar
                    checkStopRun(timeInSeconds)
                    //Comrpobar si hay que empezar una nueva ronda
                    checkNewRound(timeInSeconds)
                }
                //Aumentar en 1 el cronometro
                timeInSeconds += 1
                updateStopWatchView()

                //Dar la orden que se actualice cada 1 segundo el crono del widget
                updateWidegts()

            } finally {
                //Determinar el tiempo entre cada ejecucion
                mHandler!!.postDelayed(this, mInterval.toLong())
            }
        }
    }
    //Hacer visible el cronometro al usuario
    private fun updateStopWatchView(){
        tvChrono.text = getFormattedStopWatch(timeInSeconds * 1000)
    }


    //Administrar el tv del reset
    private fun resetClicked(){
        savePreferences()
        saveDataRun()

        updateTotalsUser()
        setLevelSport(sportSelected)

        showPopUp()

        //resetVariablesRun()  //TODO trasladada a "closePopUpRun()"
        resetTimeView()
        resetInterface()
        //resetMedals()  //TODO trasladada a "closePopUpRun()"
    }

    //Guardado de los datos de la carrera
    private fun saveDataRun(){

        //Dato UNICO para identificar la carrera (para que no existan mas de un ID x usuario ni entre usuarios)
        var id:String = useremail + dateRun + startTimeRun
        id = id.replace(":", "")
        id = id.replace("/", "")

        var saveDuration = tvChrono.text.toString()

        var saveDistance = roundNumber(distance.toString(),1)
        var saveAvgSpeed = roundNumber(avgSpeed.toString(),1)
        var saveMaxSpeed = roundNumber(maxSpeed.toString(),1)

//        // TODO Fixeado OK
//        var centerLatitude = (minLatitude?.plus(maxLatitude!!))?.div(2)
//        var centerLongitude = (minLongitude?.plus(maxLongitude!!))?.div(2)

        var centerLatitude = (minLatitude!! + maxLatitude!!) / 2
        var centerLongitude = (minLongitude!! + maxLongitude!!) / 2

        //Variables de los record obtenidos
        var medalDistance = "none"
        var medalAvgSpeed = "none"
        var medalMaxSpeed = "none"

        if (recDistanceGold) medalDistance = "gold"
        if (recDistanceSilver) medalDistance = "silver"
        if (recDistanceBronze) medalDistance = "bronze"

        if (recAvgSpeedGold) medalAvgSpeed = "gold"
        if (recAvgSpeedSilver) medalAvgSpeed = "silver"
        if (recAvgSpeedBronze) medalAvgSpeed = "bronze"

        if (recMaxSpeedGold) medalMaxSpeed = "gold"
        if (recMaxSpeedSilver) medalMaxSpeed = "silver"
        if (recMaxSpeedBronze) medalMaxSpeed = "bronze"


        //Instancia para conectar a la BD
        var collection = "runs$sportSelected"
        var dbRun = FirebaseFirestore.getInstance()
        dbRun.collection(collection).document(id).set(hashMapOf(
            "user" to useremail,
            "date" to dateRun,
            "startTime" to startTimeRun,
            "sport" to sportSelected,
            "activatedGPS" to activatedGPS,
            "duration" to saveDuration,
            "distance" to saveDistance.toDouble(),
            "avgSpeed" to saveAvgSpeed.toDouble(),
            "maxSpeed" to saveMaxSpeed.toDouble(),
            "minAltitude" to minAltitude,
            "maxAltitude" to maxAltitude,
            "minLatitude" to minLatitude,
            "maxLatitude" to maxLatitude,
            "minLongitude" to minLongitude,
            "maxLongitude" to maxLongitude,
            "centerLatitude" to centerLatitude,
            "centerLongitude" to centerLongitude,
            "medalDistance" to medalDistance,
            "medalAvgSpeed" to medalAvgSpeed,
            "medalMaxSpeed" to medalMaxSpeed,
            "lastimage" to lastimage,
            "countPhotos" to countPhotos
        ))

        //Si esta activado INTERVALOS
        if (swIntervalMode.isChecked){
            dbRun.collection(collection).document(id).update("intervalMode", true)
            dbRun.collection(collection).document(id).update("intervalDuration", npDurationInterval.value)
            dbRun.collection(collection).document(id).update("runningTime", tvRunningTime.text.toString())
            dbRun.collection(collection).document(id).update("walkingTime", tvWalkingTime.text.toString())
        }

        //Si esta activado RETOS (distancia o tiempo)
        if (swChallenges.isChecked){
            if (challengeDistance > 0f)
                dbRun.collection(collection).document(id).update("challengeDistance", roundNumber(challengeDistance.toString(), 1).toDouble())
            if (challengeDuration > 0)
                dbRun.collection(collection).document(id).update("challengeDuration", getFormattedStopWatch(challengeDuration.toLong()))
        }

    }
    //Borrar carrera
    fun deleteRun(v:View){
        //se necesetita saber a que coleccion atacar (que deporte) y el identificador (que documento borrar)

        //Alert de borrado (OPCIONAL)
        //showDialogDelete()

        //identificador
        var id:String = useremail + dateRun + startTimeRun
        id = id.replace(":", "")
        id = id.replace("/", "")

        var lyPopUpRun = findViewById<LinearLayout>(R.id.lyPopupRun)

        var currentRun = Runs()
        currentRun.distance = roundNumber(distance.toString(),1).toDouble()
        currentRun.avgSpeed = roundNumber(avgSpeed.toString(),1).toDouble()
        currentRun.maxSpeed = roundNumber(maxSpeed.toString(),1).toDouble()
        currentRun.duration = tvChrono.text.toString()

        deleteRunAndLinkedData(id, sportSelected, lyPopUpRun, currentRun)
        loadMedalsUser()  //cargar el top 3 del usuario -> recalcular el medallero luego de eliminar una carrera
        setLevelSport(sportSelected)  //Volver a calcular los totales de los niveles
        closePopUpRun()

    }

    //Variables a resetear
    private fun resetVariablesRun(){
        timeInSeconds = 0
        rounds = 1
        hardTime = true

        distance = 0.0
        maxSpeed = 0.0
        avgSpeed = 0.0

        minAltitude = null
        maxAltitude = null
        minLatitude = null
        maxLatitude = null
        minLongitude = null
        maxLongitude = null

        //Limpiar el listado
        (listPoints as ArrayList<LatLng>).clear()

        challengeDistance = 0f
        challengeDuration = 0

        //Resetear los datos cada vez que se termine una carrera. Prepararlo para la siguiente
        activatedGPS = true
        flagSavedLocation = false

        countPhotos = 0

//        initStopWatch()  //TODO ver si descomentar o no

    }
    //Actualizar lo que el usuario ve
    private fun resetTimeView(){
        initStopWatch()  //TODO ver si comentar o no
        manageEnableButtonsRun(false, true)

        //val btStart: LinearLayout = findViewById(R.id.btStart)
        //btStart.background = getDrawable(R.drawable.circle_background_toplay)
        tvChrono.setTextColor(ContextCompat.getColor(this, R.color.white))
    }
    //Administrar la interface al resetear botnon de carrera
    private fun resetInterface(){

        //Desactivar la camara
        fbCamara.isVisible = false

        //Volver a cero los tv "actuales"
        val tvCurrentDistance: TextView = findViewById(R.id.tvCurrentDistance)
        val tvCurrentAvgSpeed: TextView = findViewById(R.id.tvCurrentAvgSpeed)
        val tvCurrentSpeed: TextView = findViewById(R.id.tvCurrentSpeed)
        tvCurrentDistance.text = "0.0"
        tvCurrentAvgSpeed.text = "0.0"
        tvCurrentSpeed.text = "0.0"

        //Volver a color gris los records
        tvDistanceRecord.setTextColor(ContextCompat.getColor(this, R.color.gray_dark))
        tvAvgSpeedRecord.setTextColor(ContextCompat.getColor(this, R.color.gray_dark))
        tvMaxSpeedRecord.setTextColor(ContextCompat.getColor(this, R.color.gray_dark))

        //Volver a cero los 3 progress
        csbCurrentDistance.progress = 0f
        csbCurrentAvgSpeed.progress = 0f
        csbCurrentSpeed.progress = 0f
        csbCurrentMaxSpeed.progress = 0f

        val tvRounds: TextView = findViewById(R.id.tvRounds) as TextView
        tvRounds.text = getString(R.string.rounds)

        val lyChronoProgressBg = findViewById<LinearLayout>(R.id.lyChronoProgressBg)
        val lyRoundProgressBg = findViewById<LinearLayout>(R.id.lyRoundProgressBg)
        lyChronoProgressBg.translationX = -widthAnimations.toFloat()
        lyRoundProgressBg.translationX = -widthAnimations.toFloat()

        //Habilitar nuevamente las opicones iniciales
        swIntervalMode.isClickable = true
        npDurationInterval.isEnabled = true
        csbRunWalk.isEnabled = true
        inflateIntervalMode() //TODO ver si comentar o no

        swChallenges.isClickable = true
        npChallengeDistance.isEnabled = true
        npChallengeDurationHH.isEnabled = true
        npChallengeDurationMM.isEnabled = true
        npChallengeDurationSS.isEnabled = true


        sbHardTrack.isEnabled = false //TODO ok
        sbSoftTrack.isEnabled = false //TODO ok

    }


    //Actualizar el progres (ly) de la ronda
    private fun updateProgressBarRound(secs: Long){
        var s = secs.toInt()
        while (s>=ROUND_INTERVAL) s-=ROUND_INTERVAL
        s++

        //Preguntar si esta en rojo
        var lyRoundProgressBg = findViewById<LinearLayout>(R.id.lyRoundProgressBg)
        if (tvChrono.getCurrentTextColor() == ContextCompat.getColor(this, R.color.chrono_running)){

            //Hacer le desplazamiento cada 1 segundo
            var movement = -1 * (widthAnimations-(s*widthAnimations/TIME_RUNNING)).toFloat()
            animateViewofFloat(lyRoundProgressBg, "translationX", movement, 1000L)

        }
        //Preguntar si esta en azul
        if (tvChrono.getCurrentTextColor() == ContextCompat.getColor(this, R.color.chrono_walking)){
            s-= TIME_RUNNING
            //Desplazamiento del progress (ly)
            var movement = -1 * (widthAnimations-(s*widthAnimations/(ROUND_INTERVAL-TIME_RUNNING))).toFloat()
            animateViewofFloat(lyRoundProgressBg, "translationX", movement, 1000L)

        }
    }
    //Comprobacion del check Stop
    private fun checkStopRun(Secs: Long){
        //Asignar a una var aux los valores para mod Secs (Var Secs no se puede modificar internamente)
        var secAux : Long = Secs
        //Mientras que la duracion de los segundos sea mayor que los intervalos -> se resta  (primer semicirculo del intervalo)
        while (secAux.toInt() > ROUND_INTERVAL) secAux -= ROUND_INTERVAL

        //Si los segundos que quedan es igual el time running -> dejar de correr
        if (secAux.toInt() == TIME_RUNNING){
            //Cambiar el color de la barra a azul -> caminar
            tvChrono.setTextColor(ContextCompat.getColor(this, R.color.chrono_walking))

            //Asignar el ly como variable para cambiarle el color
            val lyRoundProgressBg = findViewById<LinearLayout>(R.id.lyRoundProgressBg)
            lyRoundProgressBg.setBackgroundColor(ContextCompat.getColor(this, R.color.chrono_walking))
            //Ponerlo al inicio de todoo
            lyRoundProgressBg.translationX = -widthAnimations.toFloat()

            //Cuando dejar de correr para empezar a caminar
            mpHard?.pause()
            notifySound()
            mpSoft?.start()

            hardTime = false
        }
        //Segundo semicirculo del intervalo
        else updateProgressBarRound(Secs)
    }
    //Verificar si hay segunda ronda
    private fun checkNewRound(Secs: Long){
        //Si los tiempos que se lleva son multiplos de las rondas
        if (Secs.toInt() % ROUND_INTERVAL == 0 && Secs.toInt() > 0){
            val tvRounds: TextView = findViewById(R.id.tvRounds) as TextView
            //Empezar una nueva ronda
            rounds++
            tvRounds.text = "Round $rounds"

            //Llevar a la linea inicial el progres (ly) y ponerlo en rojo al comenzar la nueva ronda
            tvChrono.setTextColor(ContextCompat.getColor( this, R.color.chrono_running))
            val lyRoundProgressBg = findViewById<LinearLayout>(R.id.lyRoundProgressBg)
            lyRoundProgressBg.setBackgroundColor(ContextCompat.getColor(this, R.color.chrono_running))
            lyRoundProgressBg.translationX = -widthAnimations.toFloat()

            //Cuando empieza una nueva ronda
            mpSoft?.pause()
            notifySound()
            mpHard?.start()

            hardTime = true
        }
        //Aumentar el progress (ly)
        else updateProgressBarRound(Secs)
    }

    //Mostrar el popUp
    private fun showPopUp(){
        //inhabilitar la parte de atras
        var rlMain = findViewById<RelativeLayout>(R.id.rlMain)
        rlMain.isEnabled = false

        //habilitar el popUp
        lyPopupRun.isVisible = true

        //Traer el padre al centro de la pantalla (antes estaba a "400" ahora lo pongo en "0")
        var lyWindow = findViewById<LinearLayout>(R.id.lyWindow)
        ObjectAnimator.ofFloat(lyWindow, "translationX", 0f ).apply {
            duration = 200L
            start()
        }
        //Cargar todos los datos del popUp
        loadDataPopUp()
    }
    //Cargar popUp
    private fun loadDataPopUp(){
        showHeaderPopUp()
        showMedals()
        showDataRun()
    }
    //Mostrar cabecera
    private fun showHeaderPopUp(){
        //Administrar las variables
        var csbRunsLevel = findViewById<CircularSeekBar>(R.id.csbRunsLevel)
        var csbDistanceLevel = findViewById<CircularSeekBar>(R.id.csbDistanceLevel)
        var tvTotalRunsLevel = findViewById<TextView>(R.id.tvTotalRunsLevel)
        var tvTotalDistanceLevel = findViewById<TextView>(R.id.tvTotalDistanceLevel)

        var ivSportSelected = findViewById<ImageView>(R.id.ivSportSelected)
        var ivCurrentLevel = findViewById<ImageView>(R.id.ivCurrentLevel)
        var tvTotalDistance = findViewById<TextView>(R.id.tvTotalDistance)
        var tvTotalTime = findViewById<TextView>(R.id.tvTotalTime)

        //Seleccionar el deporte -> refrescar el nivel de cada uno y su imagen
        when (sportSelected){
            "Bike" ->{
                levelSelectedSport = levelBike
                setLevelBike()
                ivSportSelected.setImageResource(R.mipmap.bike)
            }
            "RollerSkate" -> {
                levelSelectedSport = levelRollerSkate
                setLevelRollerSkate()
                ivSportSelected.setImageResource(R.mipmap.rollerskate)
            }
            "Running" -> {
                levelSelectedSport = levelRunning
                setLevelRunning()
                ivSportSelected.setImageResource(R.mipmap.running)
            }
        }

        //Asignar los datos a cada tv
        var tvNumberLevel = findViewById<TextView>(R.id.tvNumberLevel)
        var levelText = "${getString(R.string.level)} ${levelSelectedSport.image!!.subSequence(6,7).toString()}"
        tvNumberLevel.text = levelText

        //Actualizar los csb de runs
        csbRunsLevel.max = levelSelectedSport.RunsTarget!!.toFloat()
        csbRunsLevel.progress = totalsSelectedSport.totalRuns!!.toFloat()
        if (totalsSelectedSport.totalRuns!! > levelSelectedSport.RunsTarget!!.toInt()){
            csbRunsLevel.max = levelSelectedSport.RunsTarget!!.toFloat()
            csbRunsLevel.progress = csbRunsLevel.max
        }
        //Actualizar los csb de distancia
        csbDistanceLevel.max = levelSelectedSport.DistanceTarget!!.toFloat()
        csbDistanceLevel.progress = totalsSelectedSport.totalDistance!!.toFloat()
        if (totalsSelectedSport.totalDistance!! > levelSelectedSport.DistanceTarget!!.toInt()){
            csbDistanceLevel.max = levelSelectedSport.DistanceTarget!!.toFloat()
            csbDistanceLevel.progress = csbDistanceLevel.max
        }
        //las carreras corridas
        tvTotalRunsLevel.text = "${totalsSelectedSport.totalRuns!!}/${levelSelectedSport.RunsTarget!!}"

        //Mostrar "K" para abreviar miles en Distancia Total y Distancia Ojetivo
        var td = totalsSelectedSport.totalDistance!!
        var td_k: String = td.toString()
        if (td > 1000) td_k = (td/1000).toInt().toString() + "K"
        var ld = levelSelectedSport.DistanceTarget!!.toDouble()
        var ld_k: String = ld.toInt().toString()
        if (ld > 1000) ld_k = (ld/1000).toInt().toString() + "K"
        tvTotalDistance.text = "${td_k}/${ld_k} kms"

        //En porcentaje
        var porcent = (totalsSelectedSport.totalDistance!!.toDouble() *100 / levelSelectedSport.DistanceTarget!!.toDouble()).toInt()
        tvTotalDistanceLevel.text = "$porcent%"

        //Imagen de cada nivel
        when (levelSelectedSport.image){
            "level_1" -> ivCurrentLevel.setImageResource(R.drawable.level_1)
            "level_2" -> ivCurrentLevel.setImageResource(R.drawable.level_2)
            "level_3" -> ivCurrentLevel.setImageResource(R.drawable.level_3)
            "level_4" -> ivCurrentLevel.setImageResource(R.drawable.level_4)
            "level_5" -> ivCurrentLevel.setImageResource(R.drawable.level_5)
            "level_6" -> ivCurrentLevel.setImageResource(R.drawable.level_6)
            "level_7" -> ivCurrentLevel.setImageResource(R.drawable.level_7)
        }

        //Totales dle tiempo acumulado
        var formatedTime = getFormattedTotalTime(totalsSelectedSport.totalTime!!.toLong())
        tvTotalTime.text = getString(R.string.PopUpTotalTime) + formatedTime
    }
    //Mostrar medallas en el popUp
    private fun showMedals(){

        val ivMedalDistance = findViewById<ImageView>(R.id.ivMedalDistance)
        val ivMedalAvgSpeed = findViewById<ImageView>(R.id.ivMedalAvgSpeed)
        val ivMedalMaxSpeed = findViewById<ImageView>(R.id.ivMedalMaxSpeed)

        val tvMedalDistanceTitle = findViewById<TextView>(R.id.tvMedalDistanceTitle)
        val tvMedalAvgSpeedTitle = findViewById<TextView>(R.id.tvMedalAvgSpeedTitle)
        val tvMedalMaxSpeedTitle = findViewById<TextView>(R.id.tvMedalMaxSpeedTitle)
        //preguntar si la categoria es true -> mostrar imagen de la medalla
        if (recDistanceGold) ivMedalDistance.setImageResource(R.drawable.medalgold)
        if (recDistanceSilver) ivMedalDistance.setImageResource(R.drawable.medalsilver)
        if (recDistanceBronze) ivMedalDistance.setImageResource(R.drawable.medalbronze)
        //Cualquiera que se cumpla -> mostrar el tv de descripcion de medalla
        if (recDistanceGold || recDistanceSilver || recDistanceBronze)
            tvMedalDistanceTitle.setText(R.string.medalDistanceDescription)

        if (recAvgSpeedGold) ivMedalAvgSpeed.setImageResource(R.drawable.medalgold)
        if (recAvgSpeedSilver) ivMedalAvgSpeed.setImageResource(R.drawable.medalsilver)
        if (recAvgSpeedBronze) ivMedalAvgSpeed.setImageResource(R.drawable.medalbronze)
        if (recAvgSpeedGold || recAvgSpeedSilver || recAvgSpeedBronze)
            tvMedalAvgSpeedTitle.setText(R.string.medalAvgSpeedDescription)

        if (recMaxSpeedGold) ivMedalMaxSpeed.setImageResource(R.drawable.medalgold)
        if (recMaxSpeedSilver) ivMedalMaxSpeed.setImageResource(R.drawable.medalsilver)
        if (recMaxSpeedBronze) ivMedalMaxSpeed.setImageResource(R.drawable.medalbronze)
        if (recMaxSpeedGold || recMaxSpeedSilver || recMaxSpeedBronze)
            tvMedalMaxSpeedTitle.setText(R.string.medalMaxSpeedDescription)
/*
        //Solucion reto propuesto N°367:
        var lyMedalsRun = findViewById<LinearLayout>(R.id.lyMedalsRun)

        // Hide medals block
        if(activatedGPS){
            //lyMedalsRun.visibility = View.VISIBLE
            lyMedalsRun.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT // LayoutParams: android.view.ViewGroup.LayoutParams
        }
        else {
            //lyMedalsRun.visibility = View.GONE
            lyMedalsRun.getLayoutParams().height = 0
        }
        lyMedalsRun.requestLayout() //It is necesary to refresh the screen

*/
    }
    //Mostrar datos de carrera
    private fun showDataRun(){
        //Varibles a administrar
        var tvDurationRun = findViewById<TextView>(R.id.tvDurationRun)
        var lyChallengeDurationRun = findViewById<LinearLayout>(R.id.lyChallengeDurationRun)
        var tvChallengeDurationRun = findViewById<TextView>(R.id.tvChallengeDurationRun)
        var lyIntervalRun = findViewById<LinearLayout>(R.id.lyIntervalRun)
        var tvIntervalRun = findViewById<TextView>(R.id.tvIntervalRun)
        var tvDistanceRun = findViewById<TextView>(R.id.tvDistanceRun)
        var lyChallengeDistancePopUp = findViewById<LinearLayout>(R.id.lyChallengeDistancePopUp)
        var tvChallengeDistanceRun = findViewById<TextView>(R.id.tvChallengeDistanceRun)
        var lyUnevennessRun = findViewById<LinearLayout>(R.id.lyUnevennessRun)
        var tvMaxUnevennessRun = findViewById<TextView>(R.id.tvMaxUnevennessRun)
        var tvMinUnevennessRun = findViewById<TextView>(R.id.tvMinUnevennessRun)
        var tvAvgSpeedRun = findViewById<TextView>(R.id.tvAvgSpeedRun)
        var tvMaxSpeedRun = findViewById<TextView>(R.id.tvMaxSpeedRun)
        var lyCurrentDatas = findViewById<LinearLayout>(R.id.lyCurrentDatas)

        //Solucion propuesta reto N° 367
        // Hide two right down blocks
        if(activatedGPS){
            //lyCurrentDatas.visibility = View.VISIBLE
            lyCurrentDatas.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT // LayoutParams: android.view.ViewGroup.LayoutParams
        }
        else {
            //lyCurrentDatas.visibility = View.GONE
            lyCurrentDatas.getLayoutParams().height = 0
        }
        lyCurrentDatas.requestLayout() //It is necesary to refresh the screen
        //Hasta aca reto N° 367


        //Duracion de la carrera -> lo que diga el cronometro
        tvDurationRun.setText(tvChrono.text)
        //Si es con reto de duracion -> mostrar el ly correspondiente
        if (challengeDuration > 0){
            setHeightLinearLayout(lyChallengeDurationRun, 120)
            tvChallengeDurationRun.setText(getFormattedStopWatch((challengeDuration*1000).toLong()))
        }
        //Si no -> poner ly en cero para que no lo muestre
        else  setHeightLinearLayout(lyChallengeDurationRun, 0)

        //Si es con reto de intervalos -> mostrar el ly correspondiente
        if (swIntervalMode.isChecked){
            setHeightLinearLayout(lyIntervalRun, 120)
            var details: String = "${npDurationInterval.value}mins. ("
            details += "${tvRunningTime.text} / ${tvWalkingTime.text})"

            //Mostrar los datos del intervalo
            tvIntervalRun.setText(details)
        }
        //Si no -> poner ly en cero para que no lo muestre
        else setHeightLinearLayout(lyIntervalRun, 0)

        //Distancia de la carrera
        tvDistanceRun.setText(roundNumber(distance.toString(), 2))
        //Si es con reto de distancia -> mostrar el ly correspondiente
        if (challengeDistance > 0f){
            setHeightLinearLayout(lyChallengeDistancePopUp, 120)
            tvChallengeDistanceRun.setText(challengeDistance.toString())
        }
        //Si no -> poner ly en cero para que no lo muestre
        else setHeightLinearLayout(lyChallengeDistancePopUp, 0)

        //Desnivel de la carrera (altitud)
        //Si no tiene datos -> ocultamos
        if (maxAltitude == null) setHeightLinearLayout(lyUnevennessRun, 0)
        //En caso de que SI se capturaron datos -> mostrar el ly correspondiente
        else{
            setHeightLinearLayout(lyUnevennessRun, 120)
            tvMaxUnevennessRun.setText(maxAltitude!!.toInt().toString())
            tvMinUnevennessRun.setText(minAltitude!!.toInt().toString())
        }
        //Velocidad media/max
        tvAvgSpeedRun.setText(roundNumber(avgSpeed.toString(), 1))
        tvMaxSpeedRun.setText(roundNumber(maxSpeed.toString(), 1))
    }
    //Llamar a la fun de "cerrar" el popUp
    fun closePopUp (v: View){
        closePopUpRun()
    }
    //Llamar al "hidePopUpRun()" y revertir el "showPopUp()"
    private fun closePopUpRun(){
        hidePopUpRun()
        var rlMain = findViewById<RelativeLayout>(R.id.rlMain)
        rlMain.isEnabled = true

        //Cuando se cierra el popUp -> resetear las variables
        resetVariablesRun()
        resetMedals()
        selectSport(sportSelected)
        updateWidegts()
    }
    //Visibilidad de popup
    private fun hidePopUpRun(){
        var lyWindow = findViewById<LinearLayout>(R.id.lyWindow)
        //Desplazar el popur a la derecha al padre
        lyWindow.translationX = 400f
        //Al hijo ponerle invisible
        lyPopupRun = findViewById(R.id.lyPopupRun)
        lyPopupRun.isVisible = false
    }

    //Consultar si desea cerrar sesion
    private fun showDialogLogOut(callback: (() -> Unit)? = null) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.log_out_dialog))
            .setMessage(getString(R.string.accept_log_out))
            .setNegativeButton(getString(R.string.no)) { _, _ -> callback?.invoke() }
            .setPositiveButton(getString(R.string.yes)) { _, _ -> signOut() }
            .show()
    }
    //Consultar si desea borrar la carrera
    private fun showDialogDelete(callback: (() -> Unit)? = null) {
        //identificador
        var id:String = useremail + dateRun + startTimeRun
        id = id.replace(":", "")
        id = id.replace("/", "")

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_dialog))
            .setMessage(getString(R.string.accept_delete))
            .setNegativeButton(getString(R.string.no)) { _, _ -> callback?.invoke() }
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
//                deleteRunAndLinkedData(id, sportSelected)
                closePopUpRun()
            }
            .show()
    }

    //Tomar foto (popUp)
    fun takePicture(v: View){
        val intent = Intent(this, Camara::class.java)

        //Enviar parametros. FLAG_ACTIVITY_CLEAR_TOP = limpiar
        val inParameter = intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        inParameter.putExtra("dateRun", dateRun)
        inParameter.putExtra("startTimeRun", startTimeRun)

        startActivity(intent)
    }
    //Compartir (popUp) publica
    fun shareRun(v: View){
        callShareRun()
    }
    //Compartir (popUp)
    private fun callShareRun(){
        //Id de la carrera
        var idRun = dateRun + startTimeRun
        idRun = idRun.replace(":", "")
        idRun = idRun.replace("/", "")

        //Punto de comienzo de la carrera
        var centerLatitude: Double = 0.0
        var centerLongitude: Double = 0.0
        //Si tiene gps -> realizar el calculo
        if (activatedGPS == true ){
            centerLatitude = ((minLatitude!! + maxLatitude!!)/2)
            centerLongitude = ((minLongitude!! + maxLongitude!!)/2)
        }
        //Variables a utilizar como parametros
        var saveDuration = tvChrono.text.toString()
        var saveDistance = roundNumber(distance.toString(),1)
        var saveMaxSpeed = roundNumber(maxSpeed.toString(),1)
        var saveAvgSpeed = roundNumber(avgSpeed.toString(),1)

        var medalDistance = "none"
        var medalAvgSpeed = "none"
        var medalMaxSpeed = "none"

        if (recDistanceGold) medalDistance = "gold"
        if (recDistanceSilver) medalDistance = "silver"
        if (recDistanceBronze) medalDistance = "bronze"

        if (recAvgSpeedGold) medalAvgSpeed = "gold"
        if (recAvgSpeedSilver) medalAvgSpeed = "silver"
        if (recAvgSpeedBronze) medalAvgSpeed = "bronze"

        if (recMaxSpeedGold) medalMaxSpeed = "gold"
        if (recMaxSpeedSilver) medalMaxSpeed = "silver"
        if (recMaxSpeedBronze) medalMaxSpeed = "bronze"

        //ENVIO DE PARAMETROS
        val intent = Intent(this, RunActivity::class.java)

        //Variable para administrar los parametros
        val inParameter = intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        inParameter.putExtra("user", useremail)
        inParameter.putExtra("idRun", idRun)
        inParameter.putExtra("centerLatitude", centerLatitude)
        inParameter.putExtra("centerLongitude", centerLongitude)

        inParameter.putExtra("countPhotos", countPhotos)
        inParameter.putExtra("lastimage", lastimage)

        inParameter.putExtra("date", dateRun)
        inParameter.putExtra("startTime", startTimeRun)
        inParameter.putExtra("duration", saveDuration)
        inParameter.putExtra("distance", saveDistance.toDouble())
        inParameter.putExtra("maxSpeed", saveMaxSpeed.toDouble())
        inParameter.putExtra("avgSpeed", saveAvgSpeed.toDouble())
        inParameter.putExtra("minAltitude", minAltitude)
        inParameter.putExtra("maxAltitude", maxAltitude)
        inParameter.putExtra("medalDistance", medalDistance)
        inParameter.putExtra("medalAvgSpeed", medalAvgSpeed)
        inParameter.putExtra("medalMaxSpeed", medalMaxSpeed)
        inParameter.putExtra("activatedGPS", activatedGPS)
        inParameter.putExtra("sport", sportSelected)
        inParameter.putExtra("intervalMode", swIntervalMode.isChecked)

        //Valortes opcionales: Intervalos
        if (swIntervalMode.isChecked){
            inParameter.putExtra("intervalDuration", npDurationInterval.value)
            inParameter.putExtra("runningTime", tvRunningTime.text.toString())
            inParameter.putExtra("walkingTime", tvWalkingTime.text.toString())
        }//Valortes opcionales: Retos
        if (swChallenges.isChecked){
            if (challengeDistance > 0f)
                inParameter.putExtra("challengeDistance", roundNumber(challengeDistance.toString(), 1).toDouble())
            if (challengeDuration > 0)
                inParameter.putExtra("challengeDuration", getFormattedStopWatch(challengeDuration.toLong()))
        }
        //Nivel, Record -> para pintar la cabecera
        inParameter.putExtra("level_n", levelSelectedSport.name)
        inParameter.putExtra("image_level", levelSelectedSport.image)
        inParameter.putExtra("distanceTarget",levelSelectedSport.DistanceTarget!!.toDouble())
        inParameter.putExtra("distanceTotal", totalsSelectedSport.totalDistance)
        inParameter.putExtra("runsTarget", levelSelectedSport.RunsTarget!!.toInt())
        inParameter.putExtra("runsTotal", totalsSelectedSport.totalRuns)

        startActivity(intent)

    }
}