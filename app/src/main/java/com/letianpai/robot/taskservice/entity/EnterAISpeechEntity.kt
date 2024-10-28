package com.letianpai.robot.taskservice.entity

data class EnterAISpeechEntity (
    @JvmField
    var type: String? = null,
    @JvmField
    var clazz: String? = null,
    @JvmField
    var packageName: String? = null,
    var intentName: String? = null,
    @JvmField
    var intentType: String? = null,
    var taskName: String? = null,
    var skillId: String? = null,
    var skillName: String? = null
)
