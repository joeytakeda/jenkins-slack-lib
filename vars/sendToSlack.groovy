#!/usr/bin/env groovy

/**
 * Send notifications based on build status string
 * Derived from:
 * https://github.com/bitwiseman/jenkins-pipeline-shared/blob/master/vars/sendNotifications.groovy
 */
def call(String buildStatus = 'STARTED') {
  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'
 // Default values
  def color = 'RED'
  def colorCode = '#FF0000'
  def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
  def summary = "${subject} (${env.BUILD_URL})"
  def details = """<p>${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>"""

  // Override default values based on build status
  if (buildStatus == 'CHANGED' || buildStatus == "SUCCESSFUL") {
    color = 'GREEN'
    colorCode = '#00FF00'
   
  } else if (buildStatus == 'UNSTABLE') {
    color = 'YELLOW'
    colorCode = '#FFFF00'
  }


  if (buildStatus == 'STARTED' || buildStatus == 'SUCCESSFUL') {
  } else {
      slackSend (color: colorCode, message: summary)
  }
}