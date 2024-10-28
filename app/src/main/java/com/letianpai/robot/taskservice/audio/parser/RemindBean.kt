package com.letianpai.robot.taskservice.audio.parser

class RemindBean {
    var content: List<Content>? = null

    /**
     * {
     * "operation":"创建",
     * "period":"下午",
     * "time":"17:00:00",
     * "object":"闹钟",
     * "recent_tsp":1675414800,
     * "timestamp":1675414800,
     * "vid":"15149624026738428637",
     * "absuolutely":true,
     * "date":"20230203"
     * }
     */
    class Content {
        var operation: String? = null
        var period: String? = null
        var time: String? = null
        var `object`: String? = null
        var recent_tsp: Long = 0
        var timestamp: Long = 0
        var vid: String? = null
        var absuolutely: Boolean? = null
        var date: String? = null
        var repeat: String? = null
        var event: String? = null
        var time_interval: String? = null
        var time_left: Int = 0

        override fun toString(): String {
            return "Content{" +
                    "operation='" + operation + '\'' +
                    ", period='" + period + '\'' +
                    ", time='" + time + '\'' +
                    ", object='" + `object` + '\'' +
                    ", recent_tsp=" + recent_tsp +
                    ", timestamp=" + timestamp +
                    ", vid='" + vid + '\'' +
                    ", absuolutely=" + absuolutely +
                    ", date='" + date + '\'' +
                    ", repeat='" + repeat + '\'' +
                    ", event='" + event + '\'' +
                    ", time_interval='" + time_interval + '\'' +
                    ", time_left=" + time_left +
                    '}'
        }
    }
}
