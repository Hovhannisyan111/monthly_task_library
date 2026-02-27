// ─── Flag file paths ──────────────────────────────────────────

def retryFlagPath(def script) {
    return script.env.JENKINS_HOME + '/jobs/' + script.env.JOB_NAME + '/monthly_retry.flag'
}

def retryFlagExists(def script) {
    return script.fileExists(retryFlagPath(script))
}

def writeRetryFlag(def script) {
    def f = retryFlagPath(script)
    script.sh "mkdir -p \"\$(dirname '" + f + "')\" && touch '" + f + "'"
}

def clearRetryFlag(def script) {
    def f = retryFlagPath(script)
    if (script.fileExists(f)) {
        script.sh "rm -f '" + f + "'"
    }
}

// ─── Core logic ───────────────────────────────────────────────

def isMonthlyTaskDue(def script) {
    if (retryFlagExists(script)) {
        script.echo 'Retry flag found — monthly task is pending.'
        return true
    }
    def today     = new Date().format('d').toInteger()
    def targetDay = script.env.MONTHLY_DAY.toInteger()
    if (today == targetDay) {
        script.echo 'Today is day ' + today + ' — monthly task is due.'
        return true
    }
    script.echo 'Monthly task not due (today=' + today + ', target=' + targetDay + ').'
    return false
}

def handleFailure(def script) {
    if (isMonthlyTaskDue(script)) {
        script.echo 'Build failed on monthly run day — scheduling retry...'
        writeRetryFlag(script)

        def retryIntervalMinutes = script.env.RETRY_INTERVAL_MIN.toInteger()
        def retryTime = new Date(System.currentTimeMillis() + (retryIntervalMinutes * 60 * 1000))
        def cronExpr  = retryTime.format('m H d M') + ' *'

        script.echo 'Retry cron: ' + cronExpr
        script.properties([
            script.pipelineTriggers([
                script.cron(cronExpr)
            ])
        ])
    } else {
        script.echo 'Not the monthly run day — no retry scheduled.'
    }
}

def clearAll(def script) {
    clearRetryFlag(script)
    script.properties([
        script.pipelineTriggers([])
    ])
    script.echo 'All flags and triggers cleared.'
}
