package com.letianpai.robot.taskservice.audio.parser

/**
 * @author liujunbin
 */
data class AudioCommand (
    // {"number":"3","direction":"前","intentName":"走X步","skillId":"2023011300000095","skillName":"false","taskName":"转向"}
    var number: String? = null,
    var direction: String? = null,
    var intentName: String? = null,
    var skillId: String? = null,
    var skillName: String? = null,
    var taskName: String? = null
)
