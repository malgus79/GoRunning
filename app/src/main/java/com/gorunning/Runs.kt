package com.gorunning

class Runs(

    var date: String? = null,
    var startTime: String? = null,
    var user: String? = null,
    var duration: String? = null,

    //Intervalos
    var intervalMode: Boolean? = null,
    var intervalDuration: Int? = null,
    var runningTime: String? = null,
    var walkingTime: String? = null,

    //Records
    var challengeDuration: String? = null,
    var challengeDistance: Double? = null,

    var distance: Double? = null,
    var maxSpeed: Double? = null,
    var avgSpeed: Double? = null,

    //GPS
    var minAltitude: Double? = null,
    var maxAltitude: Double? = null,
    var minLatitude: Double? = null,
    var maxLatitude: Double? = null,
    var minLongitude: Double? = null,
    var maxLongitude: Double? = null,

    var centerLatitude: Double? = null,
    var centerLongitude: Double? = null,

    //Deporte
    var sport: String? = null,
    var activatedGPS: Boolean? = null,

    //Medallas x categorias
    var medalDistance: String? = null,
    var medalAvgSpeed: String? = null,
    var medalMaxSpeed: String? = null,
    var countPhotos: Int? = null,
    var lastimage: String? = null

    )