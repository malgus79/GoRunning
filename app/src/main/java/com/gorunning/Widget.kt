package com.gorunning

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.gorunning.MainActivity.Companion.chronoWidget
import com.gorunning.MainActivity.Companion.distanceWidget

class Widget : AppWidgetProvider() {

    override fun onUpdate(context: Context?,appWidgetManager: AppWidgetManager?, appWidgetIds: IntArray?) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)

        //Si tiene widget y no son nulos
        if (appWidgetIds != null && appWidgetManager != null && context != null ){
            //Recorremos el arrays de widget y a todos los actualizamos
            for (appWidgetId in appWidgetIds){
                //Controlar la vista. Es RemoteViews porque este archivo no esta vinculado a ningun xml
                val views = RemoteViews(context.packageName, R.layout.widget)
                //Establecer color, tamaño, texto, etc
                views.setTextViewText(R.id.tvWidgetChrono, chronoWidget)
                views.setTextViewText(R.id.tvWidgetDistance, distanceWidget)

                //Añadir funcionalidades al widget. No se usan OnClickListener -> si no PendingIntent que reciben una view y un pending intent
                var iLogin : PendingIntent = Intent(context, LoginActivity::class.java).let{ intent->
                    PendingIntent.getActivity(context, 0, intent, 0) }
                views.apply{ setOnClickPendingIntent(R.id.ivUser, iLogin)}

                val iMain : PendingIntent = Intent(context,MainActivity::class.java).let { intent ->
                    PendingIntent.getActivity(context,0, intent, 0) }
                views.apply{ setOnClickPendingIntent(R.id.ivRun,iMain)}

                //Hacer referencia al manager para que se actualicen los datos
                appWidgetManager.updateAppWidget(appWidgetId, views)

            }
        }
    }
}