package com.gorunning

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.gorunning.LoginActivity.Companion.useremail
import com.gorunning.Utility.animateViewofFloat
import com.gorunning.Utility.deleteRunAndLinkedData
import com.gorunning.Utility.setHeightLinearLayout
import java.io.File

class RunsAdapter(private val runsList: ArrayList<Runs>) :
    RecyclerView.Adapter<RunsAdapter.MyViewHolder>() {

    private var minimized = true
    private lateinit var context: Context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RunsAdapter.MyViewHolder {
        //Tomar el contexto de la activity (el parent es RecordActivity)
        context = parent.context

        //Cada elemento debe representarse visualmente con algo (item xml)
        val itemView = LayoutInflater.from(context).inflate(R.layout.card_run, parent, false)
        return MyViewHolder(itemView)
    }

    //Vinculacion de datos
    override fun onBindViewHolder(holder: RunsAdapter.MyViewHolder, position: Int) {
        //Position = que debe hacer con cada elemento del arrays

        //Runs = el arrays en la posicion [] (0, 1, 2, ect......en cual?)
        val run: Runs = runsList[position]
        //Con esto se tienen los datos completos de cada registro concreto y administrarlo a cada elemento

        //Ly del cuerpo ocultarlo
        setHeightLinearLayout(holder.lyDataRunBody, 0)
        //Desplazarlo hacia arriba
        holder.lyDataRunBodyContainer.translationY = -200f

        //Administrar la flecha para mostrar/ocultar el ly del cuerpo
        holder.ivHeaderOpenClose.setOnClickListener {
            //si esta minimizado -> mostrarlo y girar la flecha
            if (minimized) {
                //Variable para inflar el ly segun tenga foto o no la carrera
                var h = 600
                if(run.countPhotos!! > 0) h = 700

                setHeightLinearLayout(holder.lyDataRunBody, h)
                animateViewofFloat(holder.lyDataRunBodyContainer, "translationY", 0f, 300L)
                holder.ivHeaderOpenClose.setRotation(180f)
                minimized = false
            }
            //si esta desplegado -> ocultarlo y girar la flecha
            else {
                holder.lyDataRunBodyContainer.translationY = -200f
                setHeightLinearLayout(holder.lyDataRunBody, 0)
                holder.ivHeaderOpenClose.setRotation(0f)
                minimized = true
            }
        }

        //Fechas
        var day = run.date?.subSequence(8, 10)
        var n_month = run.date?.subSequence(5, 7)
        var month: String? = null
        var year = run.date?.subSequence(0, 4)

        when (n_month) {
            "01" -> month = "ENE"
            "02" -> month = "FEB"
            "03" -> month = "MAR"
            "04" -> month = "ABR"
            "05" -> month = "MAY"
            "06" -> month = "JUN"
            "07" -> month = "JUL"
            "08" -> month = "AGO"
            "09" -> month = "SEP"
            "10" -> month = "OCT"
            "11" -> month = "NOV"
            "12" -> month = "DIC"
        }
        var date: String = "$day-$month-$year"
        holder.tvDate.text = date
        holder.tvHeaderDate.text = date

        //Hora
        holder.tvStartTime.text = run.startTime?.subSequence(0,
            5)  //Hasta el 5 para no mostrar los segundos, solo hora y min
        holder.tvDurationRun.text = run.duration
        holder.tvHeaderDuration.text = run.duration!!.subSequence(0, 5).toString() + " HH"

        //Si tiene retos de duracion -> se le asigna el dato que tuviera
        if (!run.challengeDuration.isNullOrEmpty())
            holder.tvChallengeDurationRun.text = run.challengeDuration
        else  //Si no -> ocultarlo
            setHeightLinearLayout(holder.lyChallengeDurationRun, 0)

        //Si tiene retos de distancia -> se le asigna el dato que tuviera
        if (run.challengeDistance != null)
            holder.tvChallengeDistanceRun.text = run.challengeDistance.toString()
        else  //Si no -> ocultarlo
            setHeightLinearLayout(holder.lyChallengeDistance, 0)

        //Si tiene retos de intervalos -> se le asigna el dato que tuviera
        if (run.intervalMode != null) {
            var details: String = "${run.intervalDuration}mins. ("
            details += "${run.runningTime}/${run.walkingTime})"
            holder.tvIntervalRun.text = details
        } else  //Si no -> ocultarlo
            setHeightLinearLayout(holder.lyIntervalRun, 0)

        //Cuanta distancia ha recorrido
        holder.tvDistanceRun.setText(run.distance.toString())
        holder.tvHeaderDistance.setText(run.distance.toString() + " KM")
        //Desnniveles max/min
        holder.tvMaxUnevennessRun.setText(run.maxAltitude.toString())
        holder.tvMinUnevennessRun.setText(run.minAltitude.toString())
        //Velocidades alcanzadas
        holder.tvAvgSpeedRun.setText(run.avgSpeed.toString())
        holder.tvHeaderAvgSpeed.setText(run.avgSpeed.toString() + " KM/H")
        holder.tvMaxSpeedRun.setText(run.maxSpeed.toString())


        //Que datos tiene en el campo medalla ?
        when (run.medalDistance) {
            "gold" -> {
                //Establecer la medalla en el cuerpo y encabezado
                holder.ivMedalDistance.setImageResource(R.drawable.medalgold)
                holder.ivHeaderMedalDistance.setImageResource(R.drawable.medalgold)
                holder.tvMedalDistanceTitle.setText(R.string.CardMedalDistance)
            }
            "silver" -> {
                holder.ivMedalDistance.setImageResource(R.drawable.medalsilver)
                holder.ivHeaderMedalDistance.setImageResource(R.drawable.medalsilver)
                holder.tvMedalDistanceTitle.setText(R.string.CardMedalDistance)
            }
            "bronze" -> {
                holder.ivMedalDistance.setImageResource(R.drawable.medalbronze)
                holder.ivHeaderMedalDistance.setImageResource(R.drawable.medalbronze)
                holder.tvMedalDistanceTitle.setText(R.string.CardMedalDistance)
            }
        }
        when (run.medalAvgSpeed) {
            "gold" -> {
                holder.ivMedalAvgSpeed.setImageResource(R.drawable.medalgold)
                holder.ivHeaderMedalAvgSpeed.setImageResource(R.drawable.medalgold)
                holder.tvMedalAvgSpeedTitle.setText(R.string.CardMedalAvgSpeed)
            }
            "silver" -> {
                holder.ivMedalAvgSpeed.setImageResource(R.drawable.medalsilver)
                holder.ivHeaderMedalAvgSpeed.setImageResource(R.drawable.medalsilver)
                holder.tvMedalAvgSpeedTitle.setText(R.string.CardMedalAvgSpeed)
            }
            "bronze" -> {
                holder.ivMedalAvgSpeed.setImageResource(R.drawable.medalbronze)
                holder.ivHeaderMedalAvgSpeed.setImageResource(R.drawable.medalbronze)
                holder.tvMedalAvgSpeedTitle.setText(R.string.CardMedalAvgSpeed)
            }
        }
        when (run.medalMaxSpeed) {
            "gold" -> {
                holder.ivMedalMaxSpeed.setImageResource(R.drawable.medalgold)
                holder.ivHeaderMedalMaxSpeed.setImageResource(R.drawable.medalgold)
                holder.tvMedalMaxSpeedTitle.setText(R.string.CardMedalMaxSpeed)
            }
            "silver" -> {
                holder.ivMedalMaxSpeed.setImageResource(R.drawable.medalsilver)
                holder.ivHeaderMedalMaxSpeed.setImageResource(R.drawable.medalsilver)
                holder.tvMedalMaxSpeedTitle.setText(R.string.CardMedalMaxSpeed)
            }
            "bronze" -> {
                holder.ivMedalMaxSpeed.setImageResource(R.drawable.medalbronze)
                holder.ivHeaderMedalMaxSpeed.setImageResource(R.drawable.medalbronze)
                holder.tvMedalMaxSpeedTitle.setText(R.string.CardMedalMaxSpeed)
            }
        }

        //Si hay foto -> la cargamos
        if (run.lastimage != "") {
            //Guardar la imagen (ruta)
            var path = run.lastimage
            //Referencia de almacenamiento -> capturar el elemento de la ruta
            val storageRef = FirebaseStorage.getInstance().reference.child(path!!)

            //Temporalmente transformarlo en un archivo y mostrarlo
            var localfile = File.createTempFile("tempImage", "jpg")
            //En esa ruta -> capturar el archivo
            storageRef.getFile(localfile)
                //Si se descargo exitoso -> transformarlo a mapa de bit y cargarlo
                .addOnSuccessListener {
                    val bitmap = BitmapFactory.decodeFile(localfile.absolutePath)
                    //Extra: cambiar la calidad de la imagen


                    //OPCION METADATOS: crear otra referenciade la ruta
                    val metaRef = FirebaseStorage.getInstance().getReference(run.lastimage!!)

                    //OPCION METADATOS: crear una tarea
                    val metadata: Task<StorageMetadata> = metaRef.metadata
                    metadata.addOnSuccessListener {
                        //OPCION METADATOS: exito ->
                        //Variable para guardar la orientacion
                        var or = it.getCustomMetadata("orientation")
                        if (or == "horizontal") {

                            //si es horizontal
                            var porcent = 200 / bitmap.width.toFloat()
                            //Indicar que el ly tome solo el porcentaje y no el total de la altura de la imagen (ahorro de recursos)
                            //la altura dle archivo seria bitmap.size  -> por eso se hace el "porcent"
                            setHeightLinearLayout(holder.lyPicture, (bitmap.width * porcent).toInt())
                            //inflar la imagen
                            holder.ivPicture.setImageBitmap(bitmap)

                        }
                        //OPCION METADATOS: si no (es vertical) -> lo roto 90°
                        else {
                            var porcent = 100 / bitmap.height.toFloat()
                            //OPCION METADATOS:
                            setHeightLinearLayout(holder.lyPicture, (bitmap.width * porcent).toInt())
                            holder.ivPicture.setImageBitmap(bitmap)
                            holder.ivPicture.setRotation(90f)
                        }
                    }
                    //OPCION METADATOS: fallo ->
                    metadata.addOnFailureListener {
                    }
                }


                //Si no -> toast
                .addOnFailureListener {
                    Toast.makeText(context, "fallo al cargar la imagen", Toast.LENGTH_SHORT).show()
                }
        }

        //Boton reproducir (RecordActivity)
        holder.tvPlay.setOnClickListener {
            var idRun = run.date + run.startTime
            idRun = idRun.replace(":", "")
            idRun = idRun.replace("/", "")

            //ENVIO DE PARAMETROS
            val intent = Intent(context, RunActivity::class.java)

            val inParameter = intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            inParameter.putExtra("user", run.user)
            inParameter.putExtra("idRun", idRun)


            inParameter.putExtra("countPhotos", run.countPhotos)
            inParameter.putExtra("lastimage", run.lastimage)

            inParameter.putExtra("centerLatitude", run.centerLatitude)
            inParameter.putExtra("centerLongitude", run.centerLongitude)

            inParameter.putExtra("date", run.date)
            inParameter.putExtra("startTime", run.startTime)
            inParameter.putExtra("duration", run.duration)
            inParameter.putExtra("distance", run.distance)
            inParameter.putExtra("maxSpeed", run.maxSpeed)
            inParameter.putExtra("avgSpeed", run.avgSpeed)
            inParameter.putExtra("minAltitude", run.minAltitude)
            inParameter.putExtra("maxAltitude", run.maxAltitude)
            inParameter.putExtra("medalDistance", run.medalDistance)
            inParameter.putExtra("medalAvgSpeed", run.medalAvgSpeed)
            inParameter.putExtra("medalMaxSpeed", run.medalMaxSpeed)
            inParameter.putExtra("activatedGPS", run.activatedGPS)
            inParameter.putExtra("sport", run.sport)
            inParameter.putExtra("intervalMode", run.intervalMode)
            inParameter.putExtra("intervalDuration", run.intervalDuration)
            inParameter.putExtra("runningTime", run.runningTime)
            inParameter.putExtra("walkingTime", run.walkingTime)
            inParameter.putExtra("challengeDistance", run.challengeDistance)
            inParameter.putExtra("challengeDuration", run.challengeDuration)

            //Lanzar la actividad con los aprametros
            context.startActivity(intent)
        }

        //Boton eliminar (RecordActivity)
        holder.tvDelete.setOnClickListener {
            //Pasar el ID de la runs
            var id: String = useremail + run.date + run.startTime
            id = id.replace(":", "")
            id = id.replace("/", "")

            //Crear un objeto de la carrera con todos los datos
            var currentRun = Runs()
            currentRun.distance = run.distance
            currentRun.avgSpeed = run.avgSpeed
            currentRun.maxSpeed = run.maxSpeed
            currentRun.duration = run.duration
            currentRun.activatedGPS = run.activatedGPS
            currentRun.date = run.date
            currentRun.startTime = run.startTime
            currentRun.user = run.user
            currentRun.sport = run.sport

            //AlertDialog aqui
//            deleteRunAndLinkedData(id, currentRun.sport!!, holder.lyDataRunHeader, currentRun)
//            runsList.removeAt(position)
//            notifyItemRemoved(position)

            AlertDialog.Builder(context)
                .setTitle(R.string.delete_dialog)
                .setMessage(R.string.accept_delete)
                .setPositiveButton(android.R.string.ok,
                    DialogInterface.OnClickListener { dialog, which ->
                        //boton OK pulsado
                        deleteRunAndLinkedData(id,
                            currentRun.sport!!,
                            holder.lyDataRunHeader,
                            currentRun)
                        //Eliminar elemento del arrays
                        runsList.removeAt(position)
                        //Notificar al adaptador
                        notifyItemRemoved(position)
                    })
                .setNegativeButton(android.R.string.cancel,
                    DialogInterface.OnClickListener { dialog, which ->
                        //boton cancel pulsado
                    })
                .setCancelable(true)
                .show()
        }
    }

    //Cuantos elementos va a tener el arrays -> tamalo del arrays
    override fun getItemCount(): Int {
        return runsList.size
    }

    //Con esta clase se maneja cada items
    public class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        //Variables para administrar cada vista
        val lyDataRunHeader: LinearLayout = itemView.findViewById(R.id.lyDataRunHeader)
        val tvHeaderDate: TextView = itemView.findViewById(R.id.tvHeaderDate)
        val tvHeaderDuration: TextView = itemView.findViewById(R.id.tvHeaderDuration)
        val tvHeaderDistance: TextView = itemView.findViewById(R.id.tvHeaderDistance)
        val tvHeaderAvgSpeed: TextView = itemView.findViewById(R.id.tvHeaderAvgSpeed)
        val ivHeaderMedalDistance: ImageView = itemView.findViewById(R.id.ivHeaderMedalDistance)
        val ivHeaderMedalAvgSpeed: ImageView = itemView.findViewById(R.id.ivHeaderMedalAvgSpeed)
        val ivHeaderMedalMaxSpeed: ImageView = itemView.findViewById(R.id.ivHeaderMedalMaxSpeed)
        val ivHeaderOpenClose: ImageView = itemView.findViewById(R.id.ivHeaderOpenClose)

        val lyDataRunBody: LinearLayout = itemView.findViewById(R.id.lyDataRunBody)
        val lyDataRunBodyContainer: LinearLayout = itemView.findViewById(R.id.lyDataRunBodyContainer)

        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvStartTime: TextView = itemView.findViewById(R.id.tvStartTime)


        val tvDurationRun: TextView = itemView.findViewById(R.id.tvDurationRun)
        val lyChallengeDurationRun: LinearLayout = itemView.findViewById(R.id.lyChallengeDurationRun)
        val tvChallengeDurationRun: TextView = itemView.findViewById(R.id.tvChallengeDurationRun)
        val lyIntervalRun: LinearLayout = itemView.findViewById(R.id.lyIntervalRun)
        val tvIntervalRun: TextView = itemView.findViewById(R.id.tvIntervalRun)


        val tvDistanceRun: TextView = itemView.findViewById(R.id.tvDistanceRun)
        val lyChallengeDistance: LinearLayout = itemView.findViewById(R.id.lyChallengeDistance)
        val tvChallengeDistanceRun: TextView = itemView.findViewById(R.id.tvChallengeDistanceRun)
        val lyUnevennessRun: LinearLayout = itemView.findViewById(R.id.lyUnevennessRun)
        val tvMaxUnevennessRun: TextView = itemView.findViewById(R.id.tvMaxUnevennessRun)
        val tvMinUnevennessRun: TextView = itemView.findViewById(R.id.tvMinUnevennessRun)


        val tvAvgSpeedRun: TextView = itemView.findViewById(R.id.tvAvgSpeedRun)
        val tvMaxSpeedRun: TextView = itemView.findViewById(R.id.tvMaxSpeedRun)

        val ivMedalDistance: ImageView = itemView.findViewById(R.id.ivMedalDistance)
        val tvMedalDistanceTitle: TextView = itemView.findViewById(R.id.tvMedalDistanceTitle)
        val ivMedalAvgSpeed: ImageView = itemView.findViewById(R.id.ivMedalAvgSpeed)
        val tvMedalAvgSpeedTitle: TextView = itemView.findViewById(R.id.tvMedalAvgSpeedTitle)
        val ivMedalMaxSpeed: ImageView = itemView.findViewById(R.id.ivMedalMaxSpeed)
        val tvMedalMaxSpeedTitle: TextView = itemView.findViewById(R.id.tvMedalMaxSpeedTitle)


        val ivPicture: ImageView = itemView.findViewById(R.id.ivPicture)

        val lyPicture: LinearLayout = itemView.findViewById(R.id.lyPicture)
        val tvPlay: TextView = itemView.findViewById(R.id.tvPlay)
        val tvDelete: TextView = itemView.findViewById(R.id.tvDelete)
    }

}
