package com.example.hookedadassistant

import android.graphics.Bitmap
import android.graphics.Rect
import kotlin.math.abs

object XDetector {
    data class Detection(val rect: Rect, val kind: String, val score: Double)

    fun findControl(bitmap: Bitmap): Detection? {
        val w=bitmap.width; val h=bitmap.height
        if(w<200||h<300)return null
        val top=(h*.005).toInt(); val bottom=(h*.30).toInt(); val side=(w*.42).toInt()

        // Semantic priority: a close X always wins over arrows/next and store CTAs.
        val rightX=scanForX(bitmap,w-side,top,w,bottom)
        val leftX=scanForX(bitmap,0,top,side,bottom)
        listOfNotNull(rightX,leftX).maxByOrNull{it.score}?.let{return it}

        val rightArrow=findCornerArrow(bitmap,true)
        val leftArrow=findCornerArrow(bitmap,false)
        listOfNotNull(rightArrow,leftArrow).maxByOrNull{it.score}?.let{return it}

        findTopLeftNext(bitmap)?.let{return it}
        return findTopRightAction(bitmap)
    }

    fun likelyCloseControl(bitmap:Bitmap)=findControl(bitmap)!=null

    private fun scanForX(b:Bitmap,l:Int,t:Int,r:Int,bot:Int):Detection?{
        var best:Detection?=null
        val sizes=intArrayOf(24,28,32,36,40,48,56,64,72,80,88)
        for(s in sizes){val step=(s/7).coerceAtLeast(4);var y=t;while(y+s<bot){var x=l;while(x+s<r){val score=xScore(b,x,y,s);if(score>=.47&&(best==null||score>best.score))best=Detection(Rect(x,y,x+s,y+s),"X / Schliessen",score+1.0);x+=step};y+=step}}
        return best
    }

    private fun findCornerArrow(b:Bitmap,right:Boolean):Detection?{
        val w=b.width;val h=b.height
        val l=if(right)(w*.78).toInt() else 0;val r=if(right)w else (w*.22).toInt();val bot=(h*.18).toInt()
        var best:Detection?=null
        val sizes=intArrayOf(32,40,48,56,64,72,80)
        for(s in sizes){val step=(s/6).coerceAtLeast(5);var y=(h*.01).toInt();while(y+s<bot){var x=l;while(x+s<r){val sc=chevronScore(b,x,y,s);if(sc>.50&&(best==null||sc>best.score))best=Detection(Rect(x,y,x+s,y+s),"Weiter / Skip",sc+.60);x+=step};y+=step}}
        return best
    }

    private fun chevronScore(b:Bitmap,x0:Int,y0:Int,s:Int):Double{
        val q=s/4;val cx=x0+s/2;val cy=y0+s/2
        var diag=0.0;var n=0;var bg=0.0;var bn=0
        for(i in -q..q){for(off in -2..2){val x=(cx+abs(i)+off).coerceIn(x0,x0+s-1);val y=(cy+i).coerceIn(y0,y0+s-1);diag+=lum(b.getPixel(x,y));n++};bg+=lum(b.getPixel((x0+s/5).coerceAtMost(x0+s-1),(cy+i).coerceIn(y0,y0+s-1)));bn++}
        if(n==0||bn==0)return 0.0;val d=diag/n;val o=bg/bn;return ((d-o+80.0)/255.0).coerceIn(0.0,1.0)
    }

    private fun findTopRightAction(b:Bitmap):Detection?{
        val w=b.width;val h=b.height;val roi=Rect((w*.55).toInt(),(h*.01).toInt(),(w*.99).toInt(),(h*.14).toInt())
        var bright=0;var dark=0;var green=0;var total=0;var minX=roi.right;var minY=roi.bottom;var maxX=roi.left;var maxY=roi.top
        for(y in roi.top until roi.bottom step 3)for(x in roi.left until roi.right step 3){val c=b.getPixel(x,y);val rr=(c shr 16)and 255;val g=(c shr 8)and 255;val bl=c and 255;val ll=.2126*rr+.7152*g+.0722*bl;if(ll>205){bright++;minX=minOf(minX,x);maxX=maxOf(maxX,x);minY=minOf(minY,y);maxY=maxOf(maxY,y)};if(ll<55)dark++;if(g>95&&g>rr*1.15&&g>bl*1.10)green++;total++}
        if(total==0)return null;val br=bright.toDouble()/total;val dr=dark.toDouble()/total;val gr=green.toDouble()/total;val wide=maxX>minX&&(maxX-minX)>w*.09&&(maxY-minY)>h*.012
        if(!((br>.025&&dr>.28&&wide)||(br>.018&&gr>.18&&wide)))return null
        return Detection(Rect((minX-w*.035).toInt().coerceAtLeast(roi.left),(minY-h*.012).toInt().coerceAtLeast(roi.top),(maxX+w*.035).toInt().coerceAtMost(roi.right),(maxY+h*.012).toInt().coerceAtMost(roi.bottom)),"Store / Install",.35)
    }

    private fun findTopLeftNext(b:Bitmap):Detection?{val w=b.width;val h=b.height;val r=Rect((w*.02).toInt(),(h*.03).toInt(),(w*.32).toInt(),(h*.16).toInt());var bright=0;var dark=0;var total=0;for(y in r.top until r.bottom step 4)for(x in r.left until r.right step 4){val l=lum(b.getPixel(x,y));if(l>205)bright++;if(l<70)dark++;total++};if(total==0)return null;val br=bright.toDouble()/total;val dr=dark.toDouble()/total;return if(br in .015.. .20&&dr>.35)Detection(r,"Next / Weiter",.65)else null}

    private fun xScore(b:Bitmap,x0:Int,y0:Int,s:Int):Double{val margin=(s*.16).toInt().coerceAtLeast(2);val thickness=(s*.075).toInt().coerceIn(1,5);val sample=(s/14).coerceAtLeast(2);var mean=0.0;var count=0;for(y in y0 until y0+s step sample)for(x in x0 until x0+s step sample){mean+=lum(b.getPixel(x,y));count++};if(count==0)return 0.0;mean/=count;fun c(x:Int,y:Int)=abs(lum(b.getPixel(x,y))-mean);var diag=0.0;var dn=0;var bg=0.0;var bn=0;for(i in margin until s-margin){for(d in -thickness..thickness){val yy=(y0+i).coerceIn(y0,y0+s-1);diag+=c((x0+i+d).coerceIn(x0,x0+s-1),yy);diag+=c((x0+s-1-i+d).coerceIn(x0,x0+s-1),yy);dn+=2};bg+=c(x0+s/5,y0+i)+c(x0+4*s/5,y0+i);bn+=2};if(dn==0||bn==0)return 0.0;val d=diag/dn;val o=bg/bn;if(d<15)return 0.0;return(d/(d+o+1)+(c(x0+s/2,y0+s/2)/255)*.10).coerceIn(0.0,1.0)}
    private fun lum(c:Int):Double{val r=(c shr 16)and 255;val g=(c shr 8)and 255;val b=c and 255;return .2126*r+.7152*g+.0722*b}
}
