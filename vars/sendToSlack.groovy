#!/usr/bin/env groovy

import groovy.json.JsonOutput

def call(Map config = [:]) {
    // 1. Determine Build Status and Previous Status
    def currentStatus = currentBuild.currentResult ?: 'SUCCESS'
    def previousStatus = currentBuild.previousBuild?.result
    def isFailure = currentStatus == 'FAILURE'
    def isUnstable = currentStatus == 'UNSTABLE'
    def isRecovery = currentStatus == 'SUCCESS' && (previousStatus == 'FAILURE' || previousStatus == 'UNSTABLE')

    // 2. Filter: Only notify on Failure, Unstable, or Recovery
    if (!isFailure && !isUnstable && !isRecovery) {
        echo "slackNotify: Build is ${currentStatus} (Previous: ${previousStatus}). No notification required."
        return
    }

    // 3. Configuration Defaults
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

    // 5. Gather Metadata
    def jobName = env.JOB_NAME
    def buildNum = env.BUILD_NUMBER
    def buildUrl = env.BUILD_URL
    def branch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'unknown'
    def duration = currentBuild.durationString.replace(' and counting', '')

    // 6. Build Block Kit JSON components
    def headerText = "${icon} ${statusText}: ${jobName} #${buildNum}"
    def fallbackMessage = "${headerText} - ${buildUrl}"
    
    // Construct Buttons
    def buttons = [
        [
            type: "button",
            text: [type: "plain_text", text: "View Build"],
            url: buildUrl,
            style: isFailure ? "danger" : "primary"
        ],
        [
            type: "button",
            text: [type: "plain_text", text: "Console"],
            url: "${buildUrl}console"
        ]
    ]

    // Define Blocks
    def blocks = [
        [
            type: "header",
            text: [
                type: "plain_text",
                text: headerText,
                emoji: true
            ]
        ],
        [
            type: "section",
            fields: [
                [
                    type: "mrkdwn",
                    text: "*Branch:*\n${branch}"
                ],
                [
                    type: "mrkdwn",
                    text: "*Duration:*\n${duration}"
                ]
            ]
        ],
        [
            type: "divider"
        ],
        [
            type: "actions",
            elements: buttons
        ]
    ]

    // 7. Send Notification
    try {
        def blocksJson = JsonOutput.toJson(blocks)

        slackSend(
            color: color,
            message: fallbackMessage,
            blocks: blocksJson,
            channel: channel,
            tokenCredentialId: token
        )
    } catch (Exception e) {
        echo "slackNotify failed: ${e.message}"
    }
}