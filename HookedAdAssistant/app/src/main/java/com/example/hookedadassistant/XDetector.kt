package com.example.hookedadassistant
import android.graphics.Bitmap
import kotlin.math.abs
object XDetector {
    fun likelyCloseX(bitmap: Bitmap): Boolean {
        val w=bitmap.width; val h=bitmap.height
        if(w<200 || h<300) return false
        val top=(h*0.035).toInt(); val bottom=(h*0.25).toInt()
        val side=(w*0.28).toInt()
        return scanRegion(bitmap,0,top,side,bottom) || scanRegion(bitmap,w-side,top,w,bottom)
    }
    private fun scanRegion(b: Bitmap,l:Int,t:Int,r:Int,bot:Int):Boolean {
        val step=8
        var y=t
        while(y+48<bot){
            var x=l
            while(x+48<r){
                if(tileScore(b,x,y,48)>=0.62) return true
                x+=step
            }
            y+=step
        }
        return false
    }
    private fun tileScore(b:Bitmap,x0:Int,y0:Int,s:Int):Double {
        var sum=0.0; var n=0
        for(y in y0 until y0+s step 4) for(x in x0 until x0+s step 4){ sum+=lum(b.getPixel(x,y)); n++ }
        val mean=sum/n
        fun contrast(x:Int,y:Int)=abs(lum(b.getPixel(x,y))-mean)
        var diag=0.0; var off=0.0; var dn=0; var on=0
        for(i in 5 until s-5){
            for(d in -2..2){
                val a=(x0+i+d).coerceIn(x0,x0+s-1); val c=(x0+(s-1-i)+d).coerceIn(x0,x0+s-1)
                diag+=contrast(a,y0+i); diag+=contrast(c,y0+i); dn+=2
            }
            val q=s/4
            for(xx in intArrayOf(q,3*q)){ off+=contrast(x0+xx,y0+i); on++ }
        }
        val ds=diag/dn; val os=if(on>0) off/on else 1.0
        if(ds<38.0) return 0.0
        return (ds/(ds+os+1.0)).coerceIn(0.0,1.0)
    }
    private fun lum(c:Int):Double {
        val r=(c shr 16) and 255; val g=(c shr 8) and 255; val bl=c and 255
        return 0.2126*r+0.7152*g+0.0722*bl
    }
}
