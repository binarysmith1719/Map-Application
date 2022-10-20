package com.codezilla.mapbox3

import com.mapbox.geojson.Point

class PointQueue(totalSize:Int) {
    public val list:Array<Point> = Array(totalSize,{ i: Int -> Point.fromLngLat(-50.0000,50.0000) })
    public var head = -1
    public var tail = -1
    fun isFull() = tail == list.size-1
    fun isEmpty() : Boolean{
        if(head == -1)
        {return true}
        return false
    }

    fun enqueue(value: Point){
        if (isFull()){
            return
        }
       if(head==-1){head=0}
        tail=tail+1
        list[tail] = value
    }

    fun dequeue(){

        if(head==-1)
        {return}
        if(head==tail) {
            head=-1
            tail=-1
        }
        else{//            a = list[head]
            head=head +1
        }
//        return a
    }
    fun getPoint(x:Int):Point
    {
          return list[x]
    }
    fun getSize():Int
    {
        var ptr:Int=head
        var count:Int=0
        if(head==-1)
            return 0
        if(head==tail && head!=-1)
         return 1
        while(ptr != tail+1)
        {
            count++
            ptr++
        }
        return count
    }

//    fun display()
//    {
//        for ((i,v) in list) {
//            println("Index:$i and value:$v")
//        }
//    }
}