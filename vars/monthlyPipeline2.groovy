def isMonthlyTaskDue(def script) {
    if (executedThisMonthFlagExists(script)) {
        script.echo 'Monthly task already ran successfully this month — skipping.'
        return false
    }
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

    if (today > targetDay && !executedThisMonthFlagExists(script)) {
        script.echo 'Past day ' + targetDay + ' with no successful run this month — task is overdue.'
        return true
    }
    script.echo 'Monthly task not due (today=' + today + ', target=' + targetDay + ').'
    return false
}

def resetMonthlyFlagIfNewMonth(def script) {
    def f = executedFlagPath(script)
    if (!script.fileExists(f)) {
        return
    }
    def flagMonth = script.readFile(f).trim()
    def thisMonth = new Date().format('MM-yyyy')
    if (flagMonth != thisMonth) {
        script.echo 'New month detected (' + flagMonth + ' -> ' + thisMonth + ') — resetting executed flag.'
        script.sh "rm -f '" + f + "'"
    }
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
    writeExecutedThisMonthFlag(script)
    script.properties([
        script.pipelineTriggers([])
    ])
    script.echo 'Monthly task succeeded. Flags updated, triggers cleared.'
}

// ─── Retry flag ───────────────────────────────────────────────

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

// ─── Executed-this-month flag ─────────────────────────────────

def executedFlagPath(def script) {
    return script.env.JENKINS_HOME + '/jobs/' + script.env.JOB_NAME + '/monthly_executed.flag'
}

def executedThisMonthFlagExists(def script) {
    def f = executedFlagPath(script)
    if (!script.fileExists(f)) {
        return false
    }
    def flagMonth = script.readFile(f).trim()
    def thisMonth = new Date().format('MM-yyyy')
    return flagMonth == thisMonth
}

def writeExecutedThisMonthFlag(def script) {
    def f         = executedFlagPath(script)
    def thisMonth = new Date().format('MM-yyyy')
    script.sh "mkdir -p \"\$(dirname '" + f + "')\" && echo '" + thisMonth + "' > '" + f + "'"
}
