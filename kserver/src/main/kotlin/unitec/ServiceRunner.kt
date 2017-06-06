package unitec
import spark.Spark.*
import com.google.maps.PlacesApi
import com.google.maps.model.*
import com.google.maps.*
import com.google.maps.GeoApiContext
import com.google.maps.DirectionsApi
import com.google.maps.model.TravelMode
import com.beust.klaxon.*
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import org.w3c.dom.css.RGBColor

import java.util.Base64
import java.util.UUID
import java.io.UnsupportedEncodingException
import java.awt.image.BufferedImage;
import sun.security.krb5.Confounder.bytes
import java.awt.Color
import java.awt.image.DataBufferByte
import java.awt.image.BufferedImage.TYPE_INT_BGR
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import java.io.File


class ServiceRunner {
    fun getRoutes(body: String): String {
        var response = "Http 200{\"ruta\":["
        try {
            val parser: Parser = Parser()
            val stringBuilder: StringBuilder = StringBuilder(body)
            val json: JsonObject = parser.parse(stringBuilder) as JsonObject

            println(println("Origen : ${json.string("origen")}, Destino : ${json.string("destino")}"))

            val context = GeoApiContext().setApiKey("AIzaSyAUOMk8n8nhxUiSTEXq06jNth_kiV_s55E")
            var results = DirectionsApi.newRequest(context).origin(json.string("origen")).destination(json.string("destino")).mode(TravelMode.DRIVING).await()
            var cont = 0
            for(i in results.routes[0].legs[0].steps ) { // equivalent of 1 <= i && i <= 10
                if(cont != 0)
                    response+=","
                response += "{\"lat\":"+ i.startLocation.lat+","+"\"lng\":"+i.startLocation.lng+"},"
                response += "{\"lat\":"+ i.endLocation.lat+","+"\"lng\":"+i.endLocation.lng+"}"
                cont++
            }
            response+="]}"
            println(response)
        }
        catch (e: Exception) {
            return "Http 400{\"error\":\"Revise formato de JSON o Valores de los Parametros sean Correctos: ${e.toString()} \"}"
        }
        return response
    }
    fun getRestaurants(body: String): String {
        var response = "Http 200{\"ruta\":["
        try {
            val parser: Parser = Parser()
            val stringBuilder: StringBuilder = StringBuilder(body)
            val json: JsonObject = parser.parse(stringBuilder) as JsonObject

            println(println("Origen : ${json.string("origen")}"))


            val context = GeoApiContext().setApiKey("AIzaSyAUOMk8n8nhxUiSTEXq06jNth_kiV_s55E")
            var location = GeocodingApi.newRequest(context).address( json.string("origen") ).await()[0].geometry.location
            var result = PlacesApi.nearbySearchQuery(context, location).radius(5000).keyword("keyword").openNow(true).type(PlaceType.RESTAURANT).awaitIgnoreError()

            var cont = 0
            for(i in result.results  ) { // equivalent of 1 <= i && i <= 10
                if(cont != 0)
                    response+=","
                response += "{\"lat\":"+ i.geometry.location.lat+","+"\"lng\":"+i.geometry.location.lng+"}"
                cont++
            }
            response+="]}"
            println(response)
            return response
        }
        catch (e: Exception) {
            return "Http 400{\"error\":\"Revise formato de JSON o Valores de los Parametros sean Correctos: ${e.toString()} \"}"
        }
    }
    fun grayImage( body: String ): String {
        var response = ""
        try {
            val parser: Parser = Parser()
            val stringBuilder: StringBuilder = StringBuilder(body)
            val json: JsonObject = parser.parse(stringBuilder) as JsonObject

            println(println("Origen : ${json.string("nombre")}"))

            val base64decodedBytes = Base64.getDecoder().decode(  json.string("data")  )
            val img = ImageIO.read(ByteArrayInputStream( base64decodedBytes ))
            for (x in 0..img.width -1 ){
                for (y in 0..img.height - 1){
                    val c = Color(img.getRGB(x,y))
                    val red = c.red
                    val green = c.green
                    val blue = c.blue
                    val prom = (red + green + blue) / 3
                    img.setRGB(x, y, Color(prom, prom, prom).rgb)
                }
            }
            var nombre = json.string("nombre")
            var list = nombre?.split(".")
            nombre = list!![0]+"(Blanco y Negro)."+list[1]

            val outputfile = File(nombre)
            ImageIO.write(img , "bmp", outputfile)
            val base64encodedString = Base64.getEncoder().encodeToString( outputfile.readBytes() )

            response = "Http 200{\"nombre\" :\""+nombre+"\", \"data\":\""+base64encodedString+"\"}"
            return response
        }
        catch (e: Exception) {
            return "Http 400{\"error\":\"Revise formato de JSON o Valores de los Parametros sean Correctos: ${e.toString()} \"}"
        }
    }
    fun reziseImage( body: String ): String {
        try {
            val parser: Parser = Parser()
            val stringBuilder: StringBuilder = StringBuilder(body)
            val json: JsonObject = parser.parse(stringBuilder) as JsonObject

            println(println("Origen : ${json.string("nombre")}"))

            val base64decodedBytes = Base64.getDecoder().decode(  json.string("data")  )
            val img = ImageIO.read(ByteArrayInputStream( base64decodedBytes ))

            var newH = img.height/2
            var newW = img.width/2
            val tH = json.obj("tamaño")!!.int("alto")
            val tW = json.obj("tamaño")!!.int("ancho")
            var contx = 0
            var conty = 0
            var sendImg = img

            if (tW != null) {
                while( tW < newW  ){
                    val image = BufferedImage(newW, newW, BufferedImage.TYPE_INT_RGB)
                    for(y in 0..newH-1){
                        for (x in 0..newW-1){
                            if( contx+2 < img.width && conty+2 < img.height ){
                                val col1 = Color(img.getRGB(contx, conty))
                                val col2 = Color(img.getRGB(contx+1, conty))
                                val col3 = Color(img.getRGB(contx, conty+1))
                                val col4 = Color(img.getRGB(contx+1, conty+1))
                                val red = (col1.red + col2.red + col3.red + col4.red)/4
                                val green = (col1.green + col2.green + col3.green + col4.green)/4
                                val blue = (col1.blue + col2.blue + col3.blue + col4.blue)/4
                                image.setRGB(x, y, Color(red, green, blue).rgb )
                                contx += 2
                            }
                        }
                        contx = 0
                        conty += 2
                    }
                    newH /= 2
                    newW /= 2
                    sendImg = image
                }
            }
            var nombre = json.string("nombre")
            var list = nombre?.split(".")
            nombre = list!![0]+"(Reducida)."+list[1]
            val outputfile = File(nombre)
            ImageIO.write(sendImg , "bmp", outputfile)
            val base64encodedString = Base64.getEncoder().encodeToString( outputfile.readBytes() )
            var response = "Http 200{\"nombre\" :\""+nombre+"\", \"data\":\""+base64encodedString+"\"}"
            return response
        }
        catch (e: Exception) {
            return "Http 400{\"error\":\"Revise formato de JSON o o Valores de los Parametros sean Correctos: ${e.toString()} \"}"
        }
    }

    fun run(){
        println("Hello")
        port(8080)

        post("/ejercicio1", { req, res -> getRoutes(req.body()) })
        post("/ejercicio2", { req, res -> getRestaurants(req.body()) })
        post("/ejercicio3", { req, res -> grayImage( req.body() )  })
        post("/ejercicio4", { req, res -> reziseImage(req.body() ) })
    }
}