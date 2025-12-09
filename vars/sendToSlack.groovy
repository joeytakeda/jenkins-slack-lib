#!/usr/bin/env groovy

def call(Map config = [:]) {

    def currentStatus = currentBuild.currentResult ?: 'SUCCESS'
    def previousStatus = currentBuild.previousBuild?.result
    def isFailure = currentStatus == 'FAILURE'
    def isUnstable = currentStatus == 'UNSTABLE'
    def isRecovery = currentStatus == 'SUCCESS' && (previousStatus == 'FAILURE' || previousStatus == 'UNSTABLE')

    if (!isFailure && !isUnstable && !isRecovery) {
        // echo "slackNotify: Build is ${currentStatus} (Previous: ${previousStatus}). No notification required."
        return
    }

    // Configuration Defaults
    def channel = config.channel
    def token = config.tokenCredentialId

    // 4. Define Visuals based on Status
    def colorMap = [
        'FAILURE' : '#danger',
        'UNSTABLE': '#warning',
        'RECOVERY': '#good'
    ]
    
    def iconMap = [
        'FAILURE' : ':x:',
        'UNSTABLE': ':warning:',
        'RECOVERY': ':white_check_mark:'
    ]

    def statusKey = isRecovery ? 'RECOVERY' : currentStatus
    def color = colorMap[statusKey]
    def icon = iconMap[statusKey]
    def statusText = isRecovery ? "Back to Normal" : currentStatus


    def jobName = env.JOB_NAME
    def buildNum = env.BUILD_NUMBER
    def buildUrl = env.BUILD_URL
    def duration = currentBuild.durationString.replace(' and counting', '')

    // Build Block Kit JSON components (Keys explicitly quoted)
    def headerText = "${icon} ${jobName} (Build #${buildNum}): ${statusText}"
    def fallbackMessage = "${headerText} - ${buildUrl}"
    
    def consoleLink = "<${buildUrl}console|Console Log>"
    def buildLink = "<${buildUrl}|View Build>"
    
    // Define Blocks
    def blocks = [
        [
            "type": "header",
            "text": [
                "type": "plain_text",
                "text": headerText,
                "emoji": true
            ]
        ],
        [
            "type": "section",
            "fields": [

                [
                    "type": "mrkdwn",
                    "text": consoleLink
                ], 
                [
                    "type": "mrkdwn",
                    "text": buildLink
                ], 
                [
                    "type": "mrkdwn",
                    "text": "*Duration:*\n${duration}"
                ]
            ]
        ]
        // [
        //     "type": "divider"
        // ],

        // [
        //     "type": "section",
        //     "text": [
        //         "type": "mrkdwn",
        //         "text": linkText
        //     ]
        // ]
    ]

    // 7. Send Notification
    try {
        slackSend(
            color: color,
            message: fallbackMessage,
            blocks: blocks,
            channel: channel,
            tokenCredentialId: token
        )
    } catch (Exception e) {
        echo "slackNotify failed: ${e.message}"
    }
}