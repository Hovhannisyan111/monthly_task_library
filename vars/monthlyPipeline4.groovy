def isMonthlyTaskDue(def script) {
    def today     = new Date().format('d').toInteger()
    def targetDay = script.env.MONTHLY_DAY.toInteger()
    def thisMonth = new Date().format('MM-yyyy')
    def retryF    = flagPath(script, 'retry')
    def doneF     = flagPath(script, 'done')

    // Already succeeded this month
    if (script.fileExists(doneF) && script.readFile(doneF).trim() == thisMonth) {
        script.echo 'Already ran this month — skipping.'
        return false
    }
    // Retry pending from a previous failed build
    if (script.fileExists(retryF)) {
        script.echo 'Retry flag found — running.'
        return true
    }
    // It's the scheduled day or we're past it without a successful run
    if (today >= targetDay) {
        script.echo 'Monthly task is due (day ' + today + ').'
        return true
    }

    script.echo 'Not due yet (today=' + today + ', target=' + targetDay + ').'
    return false
}

def onSuccess(def script) {
    script.sh "echo '" + new Date().format('MM-yyyy') + "' > '" + flagPath(script, 'done') + "'"
    script.sh "rm -f '" + flagPath(script, 'retry') + "'"
    script.echo 'Monthly task done. Flags updated.'
}

def onFailure(def script) {
    if (isMonthlyTaskDue(script)) {
        script.sh "echo 'build=" + script.env.BUILD_NUMBER + "' > '" + flagPath(script, 'retry') + "'"
        script.echo 'Retry flag set — will retry on next build.'
    }
}

def resetIfNewMonth(def script) {
    def doneF = flagPath(script, 'done')
    if (script.fileExists(doneF) && script.readFile(doneF).trim() != new Date().format('MM-yyyy')) {
        script.sh "rm -f '" + doneF + "' '" + flagPath(script, 'retry') + "'"
        script.echo 'New month — flags reset.'
    }
}

// Single path helper — keeps paths consistent and DRY
def flagPath(def script, String type) {
    return script.env.JENKINS_HOME + '/jobs/' + script.env.JOB_NAME + '/monthly_' + type + '.flag'
}
