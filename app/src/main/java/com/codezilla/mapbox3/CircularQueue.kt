package com.codezilla.mapbox3

import com.mapbox.geojson.Point

class CircularQueue(totalSize:Int) {
    public val list:Array<Point> = Array(totalSize,{ i: Int -> Point.fromLngLat(-122.486052,37.830348) })
    public var head = -1
    public var tail = -1
    fun isFull() = (tail + 1) % list.size == head
    fun isEmpty() = head == -1

    fun enqueue(value: Point){
        if (isFull()){
            return
        }
       if(head==-1)head=0
        tail = (tail+1) % list.size
        list[tail] = value

    }

    fun dequeue(){
//      var a:Point = ""
        if(head==-1)
        {return}
        if (head==tail) {
            head=-1
            tail=-1
        }
        else{//            a = list[head]
            head = (head +1) % list.size
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
        var count:Int=1
        if(head==-1)
            return 0
        if(head==tail && head!=-1)
         return 1
        while(ptr != tail)
        {
            ptr = ptr % list.size
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