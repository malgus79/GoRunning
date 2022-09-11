package com.gorunning

import android.animation.ObjectAnimator
import android.content.ContentValues
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.gorunning.LoginActivity.Companion.useremail
import com.gorunning.MainActivity.Companion.activatedGPS
import com.gorunning.MainActivity.Companion.countPhotos
import com.gorunning.MainActivity.Companion.sportSelected
import com.gorunning.MainActivity.Companion.totalsBike
import com.gorunning.MainActivity.Companion.totalsRollerSkate
import com.gorunning.MainActivity.Companion.totalsRunning
import com.gorunning.MainActivity.Companion.totalsSelectedSport
import java.util.concurrent.TimeUnit

object Utility {

    private var totalsChecked: Int = 0

    //Conseguir una cadena de textos -> desde segundos
    fun getFormattedTotalTime(secs: Long): String {
        var seconds: Long = secs
        var total: String =""

        //1 dia = 86400s
        //1 mes (30 dias) = 2592000s
        //365 dias = 31536000s

        var years: Int = 0
        while (seconds >=  31536000) { years++; seconds-=31536000; }

        var months: Int = 0
        while (seconds >=  2592000) { months++; seconds-=2592000; }

        var days: Int = 0
        while (seconds >=  86400) { days++; seconds-=86400; }

        if (years > 0) total += "${years}y "
        if (months > 0) total += "${months}m "
        if (days > 0) total += "${days}d "

        total += getFormattedStopWatch(seconds*1000)

        return total
    }

    //Conseguir una cadena de textos desde segundos
    fun getFormattedStopWatch(ms: Long): String{
        var milliseconds = ms
        val hours = TimeUnit.MILLISECONDS.toHours(milliseconds)
        milliseconds -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds)
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds)

        return "${if (hours < 10) "0" else ""}$hours:" +
                "${if (minutes < 10) "0" else ""}$minutes:" +
                "${if (seconds < 10) "0" else ""}$seconds"
    }

    //Conseguir segundos desde una cadena de textos
    fun getSecFromWatch (watch: String): Int{

        var secs = 0
        var w: String = watch
        if (w.length == 5) w= "00:" + w

        // 00:00:00
        secs += w.subSequence(0,2).toString().toInt() * 3600  //1 hora = 3600 seg
        secs += w.subSequence(3,5).toString().toInt() * 60    //1 min = 60 seg
        secs += w.subSequence(6,8).toString().toInt()

        return secs
    }

    /* FUNCIONES DE ANIMACION Y CAMBIOS DE ATRIBUTOS */

    //Fun para establecer la altura de un linear layout
    fun setHeightLinearLayout(ly: LinearLayout, value: Int){
        val params: LinearLayout.LayoutParams = ly.layoutParams as LinearLayout.LayoutParams
        //La altura sea igual al valor que le pasemos
        params.height = value
        //ly tome como parametro ese params
        ly.layoutParams = params
    }
    //Animacion para Int
    fun animateViewofInt(v: View, attr: String, value: Int, time: Long){
        ObjectAnimator.ofInt(v, attr, value).apply{
            duration = time
            start()
        }
    }
    //Animacion para Float
    fun animateViewofFloat(v: View, attr: String, value: Float, time: Long){
        ObjectAnimator.ofFloat(v, attr, value).apply{
            duration = time
            start()
        }
    }

    //Alterar el string para dejar solo 2 decimales
    fun roundNumber(data: String, decimals: Int) : String{
        var d : String = data
        var p= d.indexOf(".", 0)

        if (p != null){
            var limit: Int = p+decimals +1
            if (d.length <= p+decimals+1) limit = d.length //-1
            d = d.subSequence(0, limit).toString()
        }
        return d
    }

    /* FUNCIONES DE BORRADO DE CARRERA */
    fun deleteRunAndLinkedData(idRun: String, sport: String, ly: LinearLayout, cr: Runs){

        //cr = current runs

        /* ORDEN DEL ALGORITMO (instrucciones para ejecutar) */
        //1-si teniamos el gps, borrar las ubicaciones
        //2-si habia fotos, borrar las fotos
        //3-revisar los totales
        //4-revisarlos records
        //5-borrar la carrera

        if (activatedGPS) deleteLocations(idRun, useremail)
        if (countPhotos > 0) deletePicutresRun(idRun)
        updateTotals(cr)
        checkRecords(cr, sport, useremail)
        deleteRun(idRun, sport, ly)
    }
    //Borrar las ubicaciones
    private fun deleteLocations(idRun: String, user: String){
        //La ruta es -> /locations/gamcba7982@gmail.com/20220903000935
        //subsecuencia desde el final del "user", hasta el final de la ruta -> esa seria la locations
        var idLocations = idRun.subSequence(user.length, idRun.length).toString()

        var dbLocations = FirebaseFirestore.getInstance()
        //La ruta total seria:
        dbLocations.collection("locations/$user/$idLocations")
            .get()
            .addOnSuccessListener { documents->
                //Recibir aqui todoo lo que tiene esa coleccion
                for (docLocation in documents){
                    //Borrar el documento en cuestion -> en el bucle va a borrar cada uno de los doc de la coleccion
                    var dbLoc = FirebaseFirestore.getInstance()
                    dbLoc.collection("locations/$user/$idLocations").document(docLocation.id)
                        //Al quedar sin datos la coleccion -> se elimina sola en firestore
                        .delete()
                }
            }
            .addOnFailureListener { exception ->
                Log.w(ContentValues.TAG, "Error getting documents: ", exception)
            }
    }
    //2-si habia fotos, borrar las fotos
    private fun deletePicutresRun(idRun: String){
        //Necesitamos saber la carpeta en la que esta esa carrera para borrar las fotos dentro
        //A partir del email en adelante
        var idFolder = idRun.subSequence(useremail.length, idRun.length).toString()
        //Instancia de la referencia que se va a borrar
        val delRef = FirebaseStorage.getInstance().getReference("images/$useremail/$idFolder")
        //Var para administrar el almacenamiento
        val storage = Firebase.storage
        //Lista de todas las referencias a borrar (caso de que el usuario tenga varias fotos en una misma carrera)
        val listRef = storage.reference.child("images/$useremail/$idFolder")
        //listar todos los elementos de esa lista
        listRef.listAll()

            /**  TODO se comenta por dar error
            .addOnSuccessListener { (items, prefixes) ->
                //Por cada uno de los prefijos
                prefixes.forEach { prefix ->
                    // All the prefixes under listRef.
                    // You may call listAll() recursively on them.
                }
                //Con cada elemento -> capturar su referencia y su ruta
                items.forEach { item ->
                    val storageRef = storage.reference
                    val deleteRef = storageRef.child((item.path))
                    deleteRef.delete()

                }
            }
            .addOnFailureListener {
            }
            **/
    }
    //3-revisar los totales (al borrar una carrera los totales se actualizan)
    private fun updateTotals(cr: Runs){
        //que se reste la distancia del registro que acaba de conseguir
        totalsSelectedSport.totalDistance = totalsSelectedSport.totalDistance!! - cr.distance!!  //TODO fixear "!!" por "?"
        //restarle 1 carrera que se le sumo
        totalsSelectedSport.totalRuns = totalsSelectedSport.totalRuns!! - 1  //TODO fixear "!!" por "?"
        //lo que tenia menos los segundos de la carrera que acabamos de tener
        totalsSelectedSport.totalTime = totalsSelectedSport.totalTime!! - (getSecFromWatch(cr.duration!!))  //TODO fixear "!!" por "?"

        //TODO Mejora para actualizar BD al eliminar carrera
        //Actualizacion en la BD
        var collection = "totals$sportSelected"
        var dbUpdateTotals = FirebaseFirestore.getInstance()

        //Ubicacion en la BD -> coleccion, documento, campo
        dbUpdateTotals.collection(collection).document(useremail)

        //Actualizacion de totales al eliminar la unica carrera en listado
        if (totalsSelectedSport.totalRuns!! <= 0)
        dbUpdateTotals.collection(collection).document(useremail)
            .update("totalDistance", 0)
        else { dbUpdateTotals.collection(collection).document(useremail)
            .update("totalDistance", totalsSelectedSport.totalDistance) }

        if (totalsSelectedSport.totalRuns!! <= 0)
        dbUpdateTotals.collection(collection).document(useremail)
            .update("totalRuns", 0)
        else { dbUpdateTotals.collection(collection).document(useremail)
            .update("totalRuns", totalsSelectedSport.totalRuns) }

        if (totalsSelectedSport.totalRuns!! <= 0)
        dbUpdateTotals.collection(collection).document(useremail)
            .update("totalTime", 0)
        else { dbUpdateTotals.collection(collection).document(useremail)
            .update("totalTime", totalsSelectedSport.totalTime) }

        //Actualizacion de records al eliminar la unica carrera en listado
        if (totalsSelectedSport.totalRuns!! <= 0) {
            dbUpdateTotals.collection(collection).document(useremail)
                .update("recordAvgSpeed", 0)
            dbUpdateTotals.collection(collection).document(useremail)
                .update("recordSpeed", 0)
            dbUpdateTotals.collection(collection).document(useremail)
                .update("recordDistance", 0)
        }

    }
    //4-revisarlos records (comprobar si hay un record o no)
    private fun checkRecords(cr: Runs, sport: String, user: String){

        totalsChecked = 0

        checkDistanceRecord(cr, sport, user)
        checkAvgSpeedRecord(cr, sport, user)
        checkMaxSpeedRecord(cr, sport, user)
    }
    //Comprobacion de records de Distancia
    private fun checkDistanceRecord(cr: Runs, sport: String, user: String){
        //Si la distancia actual es = al record
        if (cr.distance!! == totalsSelectedSport.recordDistance){
            //Consulta a la BD
            var dbRecords = FirebaseFirestore.getInstance()
            dbRecords.collection("runs$sport")
                .orderBy("distance", Query.Direction.DESCENDING)
                //hacer un filtro (IMPORTANTE: al ejecutar no funciona siguiendo la doc, antes hay que hacer un INDEX)
                .whereEqualTo("user", user)
                .get()
                .addOnSuccessListener { documents ->
                    //si hay un solo registro (el primero de la app) y se borra -> el records va a cer cero
                    if (documents.size() <= 1)  totalsSelectedSport.recordDistance = 0.0  //TODO fixear "<= 1" por "== 1" ?
                    //si hay mas registros (obtener el siguiente al record) -> el segundo registro es el [1] porque estan ordenados
                    else  totalsSelectedSport.recordDistance = documents.documents[1].get("distance").toString().toDouble()

                    //Ahora se sabe cual es el siguiente elemento que va a pasar a ser el nuevo record
                    var collection = "totals$sport"
                    var dbUpdateTotals = FirebaseFirestore.getInstance()
                    dbUpdateTotals.collection(collection).document(user)
                        .update("recordDistance", totalsSelectedSport.recordDistance)

                    totalsChecked++
                    //Si es 3 -> actualizar los totales del deporte
                    if (totalsChecked == 3) refreshTotalsSport(sport)

                }
                .addOnFailureListener { exception ->
                    Log.w(ContentValues.TAG, "Error getting documents WHERE EQUAL TO: ", exception)
                }
        }
    }
    //Comprobacion de records de Velocidad promedio
    private fun checkAvgSpeedRecord(cr: Runs, sport: String, user: String){
        if (cr.avgSpeed!! == totalsSelectedSport.recordAvgSpeed){
            var dbRecords = FirebaseFirestore.getInstance()
            dbRecords.collection("runs$sport")
                .orderBy("avgSpeed", Query.Direction.DESCENDING)
                .whereEqualTo("user", user)
                .get()
                .addOnSuccessListener { documents ->

                    if (documents.size() <= 1)  totalsSelectedSport.recordAvgSpeed = 0.0  //TODO fixear "<= 1" por "== 1" ?
                    else  totalsSelectedSport.recordAvgSpeed = documents.documents[1].get("avgSpeed").toString().toDouble()

                    var collection = "totals$sport"
                    var dbUpdateTotals = FirebaseFirestore.getInstance()
                    dbUpdateTotals.collection(collection).document(user)
                        .update("recordAvgSpeed", totalsSelectedSport.recordAvgSpeed)

                    totalsChecked++
                    if (totalsChecked == 3) refreshTotalsSport(sport)

                }
                .addOnFailureListener { exception ->
                    Log.w(ContentValues.TAG, "Error getting documents WHERE EQUAL TO: ", exception)
                }
        }
    }
    //Comprobacion de records de Velocidad maxima
    private fun checkMaxSpeedRecord(cr: Runs, sport: String, user: String){
        if (cr.maxSpeed!! == totalsSelectedSport.recordSpeed){
            var dbRecords = FirebaseFirestore.getInstance()
            dbRecords.collection("runs$sport")
                .orderBy("maxSpeed", Query.Direction.DESCENDING)
                .whereEqualTo("user", user)
                .get()
                .addOnSuccessListener { documents ->

                    if (documents.size() <= 1)  totalsSelectedSport.recordSpeed = 0.0  //TODO fixear "<= 1" por "== 1" ?
                    else  totalsSelectedSport.recordSpeed = documents.documents[1].get("maxSpeed").toString().toDouble()

                    var collection = "totals$sport"
                    var dbUpdateTotals = FirebaseFirestore.getInstance()
                    dbUpdateTotals.collection(collection).document(user)
                        .update("recordSpeed", totalsSelectedSport.recordSpeed)

                    totalsChecked++
                    if (totalsChecked == 3) refreshTotalsSport(sport)

                }
                .addOnFailureListener { exception ->
                    Log.w(ContentValues.TAG, "Error getting documents WHERE EQUAL TO: ", exception)
                }
        }
    }
    //Actualizar los totales del deporte
    private fun refreshTotalsSport(sport: String){
        when (sport){
            "Bike"-> totalsBike = totalsSelectedSport
            "RollerSkate"-> totalsRollerSkate = totalsSelectedSport
            "Running"-> totalsRunning = totalsSelectedSport
        }
    }

    //5-borrar la carrera
    private fun deleteRun(idRun: String, sport: String, ly: LinearLayout){
        //Instancia de firestore
        var dbRun = FirebaseFirestore.getInstance()
        //ruta del documento
        dbRun.collection("runs$sport").document(idRun)
            .delete()
                //Informar el borrado del documento
            .addOnSuccessListener {
                Snackbar.make(ly, "Registro Borrado", Snackbar.LENGTH_LONG).setAction("OK"){
                    ly.setBackgroundColor(Color.CYAN)
                }.show()
            }
            .addOnFailureListener {
                Snackbar.make(ly, "Error al borrar el registro", Snackbar.LENGTH_LONG).setAction("OK"){
                    ly.setBackgroundColor(Color.CYAN)
                }.show()
            }
    }
}