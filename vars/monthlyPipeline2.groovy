def isMonthlyTaskDue(def script) {
    // Retry pending from a previous failed attempt
    if (retryFlagExists(script)) {
        script.echo 'Retry flag found — monthly task is pending.'
        return true
    }
    def today     = new Date().format('d').toInteger()
    def targetDay = script.env.MONTHLY_DAY.toInteger()

    // It's the scheduled day
    if (today == targetDay) {
        script.echo 'Today is day ' + today + ' — monthly task is due.'
        return true
    }

    // Passed the scheduled day this month but never ran successfully
    if (today > targetDay && !executedThisMonthFlagExists(script)) {
        script.echo 'Passed day ' + targetDay + ' without successful run — monthly task is overdue.'
        return true
    }

    script.echo 'Monthly task not due (today=' + today + ', target=' + targetDay + ').'
    return false
}

def handleFailure(def script) {
    if (isMonthlyTaskDue(script)) {
        script.echo 'Build failed while monthly task was due — scheduling retry tomorrow at 06:00...'
        writeRetryFlag(script)

        def tomorrow = new Date() + 1
        def cronExpr = tomorrow.format('0 6 d M') + ' *'

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
    writeExecutedThisMonthFlag(script)   // mark as done for this month
    script.properties([
        script.pipelineTriggers([])
    ])
    script.echo 'Monthly task succeeded. Flags updated, triggers cleared.'
}

// Called once a month (e.g. in a housekeeping job or first build of new month)
// to reset the executed flag so next month's run is allowed.
// Automatically handled inside isMonthlyTaskDue when the month rolls over.
def resetMonthlyFlagIfNewMonth(def script) {
    if (executedThisMonthFlagExists(script)) {
        def flagMonth  = readExecutedMonthFlag(script)
        def thisMonth  = new Date().format('MM-yyyy')
        if (flagMonth != thisMonth) {
            script.echo 'New month detected — resetting executed flag.'
            clearExecutedThisMonthFlag(script)
        }
    }
}

// ─── Retry flag ───────────────────────────────────────────────

private def retryFlagPath(def script) {
    return script.env.JENKINS_HOME + '/jobs/' + script.env.JOB_NAME + '/monthly_retry.flag'
}

private def retryFlagExists(def script) {
    return script.fileExists(retryFlagPath(script))
}

private def writeRetryFlag(def script) {
    def f = retryFlagPath(script)
    script.sh "mkdir -p \"\$(dirname '" + f + "')\" && touch '" + f + "'"
}

private def clearRetryFlag(def script) {
    def f = retryFlagPath(script)
    if (script.fileExists(f)) {
        script.sh "rm -f '" + f + "'"
    }
}

// ─── Executed-this-month flag ─────────────────────────────────
// Contains the month it ran (e.g. "02-2026") so we can detect
// when a new month starts and reset it automatically.

private def executedFlagPath(def script) {
    return script.env.JENKINS_HOME + '/jobs/' + script.env.JOB_NAME + '/monthly_executed.flag'
}

private def executedThisMonthFlagExists(def script) {
    if (!script.fileExists(executedFlagPath(script))) {
        return false
    }
    def flagMonth = readExecutedMonthFlag(script)
    def thisMonth = new Date().format('MM-yyyy')
    return flagMonth == thisMonth
}

private def readExecutedMonthFlag(def script) {
    return script.readFile(executedFlagPath(script)).trim()
}

private def writeExecutedThisMonthFlag(def script) {
    def f         = executedFlagPath(script)
    def thisMonth = new Date().format('MM-yyyy')
    script.sh "mkdir -p \"\$(dirname '" + f + "')\" && echo '" + thisMonth + "' > '" + f + "'"
}

private def clearExecutedThisMonthFlag(def script) {
    def f = executedFlagPath(script)
    if (script.fileExists(f)) {
        script.sh "rm -f '" + f + "'"
    }
}
