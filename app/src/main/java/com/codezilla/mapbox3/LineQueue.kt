package com.codezilla.mapbox3

import com.mapbox.geojson.Point

class LineQueue(totalSize:Int) {
        public val list:Array<String> = Array(totalSize,{ i: Int -> "null" })
        public var head = -1
        public var tail = -1
        fun isFull() = (tail + 1) % list.size == head
        fun isEmpty() = head == -1

        fun enqueue(value: String){
            if (isFull()){
                return
            }
            if(head==-1)head=0
            tail = (tail+1) % list.size
            list[tail] = value

        }

        fun dequeue():String{
            var a:String = ""
            if(head==-1)
            {return ""}
            a=list[head]
            if (head==tail) {
                head=-1
                tail=-1
            }
            else{//            a = list[head]
                head = (head +1) % list.size
            }
        return a
        }
        fun getPoint(x:Int): String
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